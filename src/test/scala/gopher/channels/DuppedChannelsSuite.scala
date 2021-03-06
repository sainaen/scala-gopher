package gopher.channels

import gopher._
import gopher.channels._
import gopher.tags._
import scala.language._
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import org.scalatest._
import org.scalatest.concurrent._

import scala.concurrent.ExecutionContext.Implicits.global

class DuppedChannelsSuite extends FunSuite with AsyncAssertions {

  
  test("duped input must show two") {
      val w = new Waiter
      val ch = gopherApi.makeChannel[String]()
      val dupped = ch.dup
      ch.awrite("1")
      val r1 = dupped._1.aread map { 
                x => w{ assert(x=="1") }
                w.dismiss()
              }
      val r2 = dupped._2.aread map { 
                x => w{ assert(x=="1") }
                w.dismiss()
               }
      w.await(timeout(10 seconds),Dismissals(2))
  }


  test("output is blocked by both inputs") {
      val ch = gopherApi.makeChannel[Int]()
      val aw=ch.awriteAll(1 to 100)
      val (in1, in2) = ch.dup
      val at1 = in1.atake(100)
      intercept[TimeoutException] {
        Await.ready(aw, 1 second) 
      }
      assert(!aw.isCompleted)
      assert(!at1.isCompleted)
      val at2 = in2.atake(100)
      Await.ready(at2, 1 second) 
      assert(aw.isCompleted)
  }
  
  test("on closing of main stream dupped outputs also closed.") {
      val ch = gopherApi.makeChannel[Int](1)
      val (in1, in2) = ch.dup
      val f1 = go {
          ch.write(1) 
          ch.close()
      }
      Await.ready(f1, 1 second) 
      val w = new Waiter
      in1.aread map { x =>  w(assert(x==1)); w.dismiss() } onComplete {
                           case Failure(ex) => w( throw ex )
                           case Success(_) =>
                                     in1.aread onFailure{  case ex => w(assert(ex.isInstanceOf[ChannelClosedException]));
                                                           w.dismiss() 
                                 }
      }
      w.await(timeout(10 seconds),Dismissals(2))
  }

  def gopherApi = CommonTestObjects.gopherApi

  
}
