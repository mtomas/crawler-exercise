package com.dcvc.crawler

import RootWatcher.StartWatcher
import akka.actor.{ActorSystem, PoisonPill, Props}

import scala.concurrent.duration._
import scala.concurrent.Await

object Crawler {

	implicit val system = ActorSystem()

	def main(args: Array[String]): Unit = {

		val url = args(0)
		val keyword = args(1)

		println(s"Starting with root url ${url} and keyword: ${keyword}")

		val pageIndex = system.actorOf(Props(new PageIndex))
		val supervisor = system.actorOf(Props(new RootWatcher(pageIndex, keyword)))

		supervisor ! StartWatcher(url)
		//supervisor can be stopped like this: supervisor ! StopWatcher
		Await.result(system.whenTerminated, 10 minutes)
	}

}
