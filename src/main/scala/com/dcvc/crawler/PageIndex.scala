package com.dcvc.crawler

import akka.actor.Actor
import com.dcvc.crawler.PageIndex.{GetPageRecords, PageRecord}

class PageIndex extends Actor {

  var pageRecords = scala.collection.mutable.ArrayBuffer.empty[String]

  override def receive: Receive = {
    case PageRecord(url) => println(s"Adding ${url} to page index results"); pageRecords += url
    case GetPageRecords => sender ! pageRecords.toList
  }
}

object PageIndex {
  case object GetPageRecords
  case class PageRecord(url: String)
}
