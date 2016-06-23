package com.dcvc.crawler

import java.net.URL
import javax.net.ssl.{SSLHandshakeException, SSLProtocolException}

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, OneForOneStrategy}
import com.dcvc.crawler.ChildCrawler.PageUrl
import com.dcvc.crawler.PageIndex.PageRecord
import org.jsoup.Jsoup
import sun.security.validator.ValidatorException
import scala.concurrent.duration._

import scala.collection.JavaConverters._

class ChildCrawler(pageIndex: ActorRef, keyword: String) extends Actor {

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1 minute) {
      case _: NullPointerException     => Restart
      case _: Exception                => Escalate
      case _: SSLProtocolException     => Stop
      case _: SSLHandshakeException    => Stop
      case _: ValidatorException       => Stop
    }

  override def receive: Receive = {
    case PageUrl(url: URL) => {
      //download page contents, parse and check keywords in text elements, if passes test then add to PageIndex
      val link: String = url.toString
      val response = Jsoup.connect(link).ignoreContentType(true)
        .userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36").execute()

      val contentType: String = response.contentType
      val hasKeyword = if(contentType.startsWith("text/html")) {
        val content = response.parse()
        content.getElementsContainingText(keyword).size() > 0
      } else false

      if(hasKeyword) {
        println(s"Page with URL: ${url.toString} contains keyword '${keyword}'")
        pageIndex ! PageRecord(url.toString)
      } else {
        println(s"Page with URL: ${url.toString} does not contain keyword '${keyword}'")
      }

      val hyperlinks = if (contentType.startsWith("text/html")) {
        val content = response.parse()
        val urls: List[URL] = content.getElementsByTag("a").asScala.map(e => e.attr("href")).filter(s =>
          URLValidator.validate(s)).map(link => new URL(link)).toList
        Links(urls)
      } else Links(List())
      sender() ! hyperlinks
    }
  }
}

object ChildCrawler {
  case class PageUrl(url: URL)
}
