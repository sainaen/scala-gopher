package gopher.channels

import akka.actor._
import scala.language._
import scala.concurrent._
import scala.collection.immutable._
import gopher._


/**
 * ChannelActor - actor, which leave
 */
class UnbufferedChannelActor[A](id:Long, api: GopherAPI) extends ChannelActor[A](id,api)
{

  protected[this] def onContWrite(cw:ContWrite[A,_]):Unit =
         if (closed) {
               cw.flowTermination.throwIfNotCompleted(new ChannelClosedException())
         } else if (!processReaders(cw))  {
               writers = writers :+ cw
         }
         

  protected[this] def onContRead(cr:ContRead[A,_]):Unit =
            if (closed) {
               processReaderClosed(cr)
            } else if (!processWriters(cr)) {
               readers = readers :+ cr;         
            }

  protected[this] def getNElements():Int = 0


  def processReaders(w: ContWrite[A,_]) : Boolean =
  {
    var done = false
    while(!(done || readers.isEmpty)) {
      val current = readers.head
      readers = readers.tail
      done = processReader(current,w)
    }
    done
  }

  private[this] def processReader[B,C](reader:ContRead[A,B],writer:ContWrite[A,C]): Boolean =
   reader.function(reader) match {
       case Some(f1) => 
              writer.function(writer) match {
                case Some((a,wcont)) =>
                      Future{
                        val cont = f1(ContRead.In value a)
                        api.continue(cont, reader.flowTermination)
                      }(api.executionContext)
                      api.continue(wcont, writer.flowTermination)
                      true
                case None =>
                      val cont = f1(ContRead.Skip)
                      api.continue(cont, reader.flowTermination)
                      false
              }
       case None =>
              false
   }

  def processWriters[C](reader:ContRead[A,C]): Boolean =
  {
    if (writers.isEmpty) {
      false
    } else {
      reader.function(reader) match {
         case Some(f1) =>
            var done = false
            while(!writers.isEmpty && !done) {
              val current = writers.head
              writers = writers.tail
              done = processWriter(current,f1,reader)
            }
            if (!done) {
              f1(ContRead.Skip)
            }
            done
         case None => true
      }
    }
  }

  private[this] def processWriter[B,C](writer:ContWrite[A,B],
                                       f1:ContRead.In[A]=>Future[Continuated[C]],
                                       reader:ContRead[A,C]): Boolean =
   writer.function(writer) match {
       case Some((a,wcont)) =>
                Future {
                  val rcont = f1(ContRead.In value a)
                  api.continue(rcont,reader.flowTermination)
                }
                api.continue(wcont,writer.flowTermination)
                true
       case None => 
                false
   }


  def stopIfEmpty: Boolean =
  {
   require(closed==true)
   stopReaders()
   stopWriters()
      if (nRefs == 0) {
        // here we leave 'closed' channels in actor-system untile they will be
        // garbage-collected.  TODO: think about actual stop ?
        self ! GracefullChannelStop
      }
      true
  }


  private[this] implicit def ec: ExecutionContext = api.executionContext


}
