package gopher.channels


import org.scalatest._
import scala.concurrent._
import scala.concurrent.duration._
import gopher._
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global

class FlowTerminationSuite extends FunSuite 
{

 
   test("flowTermination covariance assignment")  {
     
     val fUnit = PromiseFlowTermination[Unit]()
      // val fAny: FlowTermination[Any] = fUnit
     implicit val f_ : FlowTermination[_] = fUnit
     
     val qq = implicitly[FlowTermination[_]]
     
   }

   
   test("select with queue type") {
     import gopherApi._

     val channel = makeChannel[Int](100)

     val producer = channel.awriteAll(1 to 1000)
          
     var sum = 0;
     val consumer = Future {
       val sc = new Selector[Unit](gopherApi)
       def f(self: ContRead[Int,Unit]):Option[ContRead.In[Int]=>Future[Continuated[Unit]]] =
       {
         Some {
           case ContRead.Value(a) => sum = sum + a
                                     if (a == 1000) sc.doExit(())
                                     Future successful self
           case ContRead.Failure(e) => Future failed e
           case _     =>
                                     Future successful self
         }
       }
       sc.addReader(channel,f)
       Await.ready(sc.run, 10.second)
     }
   
     Await.ready(consumer, 10.second)
     
   }
    
   test("not propagate signals after exit") {
      
     import gopherApi._
     val channel = makeChannel[Int](100)
     var sum = 0
     val f = select.forever{
        case x: channel.read => sum += x
                select.shutdown()
     }
     val f2 = channel.awrite(1) 
     Await.result(f, 1 second)
     assert(sum == 1)
     val f3 = channel.awrite(2)
     Thread.sleep(1000)
     assert(sum == 1)

   }
  
  val gopherApi = CommonTestObjects.gopherApi
   
}
