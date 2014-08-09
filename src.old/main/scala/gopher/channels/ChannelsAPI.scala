package gopher.channels

import scala.language.experimental.macros._
import scala.reflect.macros._
import scala.reflect._
import scala.concurrent._
import scala.concurrent.duration._
import akka.actor._




trait ChannelsAPI[T <: ChannelsAPI[T]] {
  
  channelsAPI : T  =>      

  type ChannelsAPISelf = T  
    
  type IChannel[+A] <: InputChannelBase[A] with InputChannelOps[T,A]
  type OChannel[-A] <: OutputChannelBase[A] with OutputChannelOps[T,A]
  type IOChannel[A] <: InputOutputChannelBase[A] with InputChannelOps[T,A] 
                                                 with OutputChannelOps[T,A]

  type GTie <: Tie[T]
  type GFuture[T, A] <: Future[A]
        
  def makeChannel[A: ClassTag](capacity: Int = 1, tag: String=null)(implicit ecp: ChannelsExecutionContextProvider = DefaultChannelsExecutionContextProvider, 
                                                                              asp: ChannelsActorSystemProvider = DefaultChannelsActorSystemProvider,
                                                                              clf: ChannelsLoggerFactory = DefaultChannelsLoggerFactory
                                                                    ): IOChannel[A]
  
  def makeTie(tag:String=null)(
             implicit ecp: ChannelsExecutionContextProvider = DefaultChannelsExecutionContextProvider, 
                      asp: ChannelsActorSystemProvider = DefaultChannelsActorSystemProvider,
                      clf: ChannelsLoggerFactory = DefaultChannelsLoggerFactory ): GTie
  
 
  def  transformGo[A](c:Context)(code: c.Expr[A]): c.Expr[Future[A]]
    
  def  transformForSelect(c:Context)(code: c.Expr[ChannelsAPISelf#GTie => Unit]): c.Expr[Unit]
  
  def  transformForSelectOnce(c:Context)(code: c.Expr[ChannelsAPISelf#GTie => Unit]): c.Expr[Unit]
   
  type ReadActionRecord[A] = (ChannelsAPISelf#IChannel[A], ReadAction[A])
  type WriteActionRecord[A] = (ChannelsAPISelf#OChannel[A], WriteAction[A])
  
}