package com.dcvc.crawler

import java.net.{SocketTimeoutException, URL}
import javax.net.ssl.{SSLHandshakeException, SSLProtocolException}

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume}
import akka.actor.{Actor, ActorRef, Cancellable, OneForOneStrategy, PoisonPill, Props}
import com.dcvc.crawler.ChildCrawler.PageUrl
import com.dcvc.crawler.RootWatcher.{ProcessNext, StartWatcher, StopWatcher}
import org.jsoup.Jsoup
import akka.pattern.ask
import akka.util.Timeout
import com.dcvc.crawler.PageIndex.GetPageRecords
import sun.security.validator.ValidatorException

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.Success


class RootWatcher(pageIndex: ActorRef, keyword: String) extends Actor {

  implicit val timeout = Timeout(5 seconds)

  import context.dispatcher

  var leftToProcess = List.empty[URL]

  var tick: Cancellable = _

  var limit = 150

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1 minute) {
      case _: SocketTimeoutException   => Restart
      case _: Exception                => Escalate
      case _: SSLProtocolException     => Resume
      case _: SSLHandshakeException    => Resume
      case _: ValidatorException       => Resume
    }

  override def receive: Receive = {
    case StartWatcher(url) => { println(s"Starting crawling at: ${url}"); parse(new URL(url));}
    case all @ Links(urls) => {
      if(all.links.size + leftToProcess.size > limit) {
        leftToProcess ++= all.links.take(limit - leftToProcess.size)
      } else if(leftToProcess.size != limit) {
        leftToProcess ++= all.links
      } else {println("ignoring others, limit reached")}
      println(s"Current number of pages: ${leftToProcess.size}")
      startProcessing()
    }
  }

  private def isProcessing: Receive = {
    case ProcessNext => {

      leftToProcess match {
        case head :: restOfPages => {
          val childCrawler = context.system.actorOf(Props(new ChildCrawler(pageIndex, keyword)))
          childCrawler ! PageUrl(head)
          leftToProcess = restOfPages
          println(s"${restOfPages.length} pages left..")
        }

        case Nil => tick.cancel(); self ! StopWatcher
      }
    }

    case StopWatcher => {
      println("Stopping watcher and printing results..")
      val pageResults: Future[List[String]] = (pageIndex ? GetPageRecords).mapTo[List[String]]
      val replyTo = sender()
      pageResults.onComplete {
        case Success(lines) => println(s"Result: ${lines}"); replyTo ! lines
        case ex => println(s"Failure obatining page index result: ${ex}")
      }
      context.system.terminate()
    }

    case msg =>
  }

  def startProcessing() = {
    context.become(isProcessing)
    tick = context.system.scheduler.schedule(0 millis, 1500 millis, self, ProcessNext)
  }

  def parse(url: URL): Unit = {
    val link: String = url.toString
    val response = Jsoup.connect(link).ignoreContentType(true)
      .userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36").timeout(timeout.duration.toMillis.toInt).execute()

    val contentType: String = response.contentType
    val links = if (contentType.startsWith("text/html")) {
      val content = response.parse()
      val links: List[URL] = content.getElementsByTag("a").asScala.map(e => e.attr("href")).filter(s =>
        URLValidator.validate(s)).map(link => new URL(link)).toList
      Links(links)
    } else Links(List())
    if(links.links.size < limit) limit = links.links.size // if there is less links than limit we need to adjust crawling kickstart condition
    self ! links
  }
}

object RootWatcher {
  case object ProcessNext
  case object StopWatcher
  case class StartWatcher(url: String)
}