
import language.experimental.macros

import scala.concurrent.Future
import scala.reflect.macros.Context

package object gopher 
{

  def go[A](x:A):Future[A] = macro goImpl[A]

  def goImpl[A](c:Context)(x: c.Expr[A]):c.Expr[Future[A]] =
  {
   import c.universe._
   System.err.println("goImpl, x="+x)
   System.err.println("goImpl, row="+showRaw(x))
   //
   //  Future {
   //     implicit val _deferrable = new ScopeContext
   //     val select = go.channels.createChannels
   //     try {
   //       transform(t, _deferrable)
   //     } catch {
   //       ..
   //     }
   //
   //  }
   val tree = Apply(
                Select(Select(Ident(newTermName("scala")), newTermName("concurrent")), newTermName("Future")), 
                List(Block(
                      //ValDef(Modifiers(), newTermName("select"), TypeTree(), Literal(Constant(1))),
                      c.resetAllAttrs(x.tree)
                    )     )
              )
                      
   System.err.println("goImpl, output="+tree)

   c.Expr[Future[A]](tree)           
  }

  val select = channels.SelectorMacroCaller

  object ~>
  {
    def unapply(s: channels.SelectorContext): Option[(channels.InputChannel[Any],Any)] = ??? //macro unapplyImpl
    
    // TODO: think how to defer macro expansion
    def unapplyImpl(c: Context)(s:c.Expr[channels.SelectorContext]):c.Expr[Option[(channels.InputChannel[Any],Any)]] =
    {
      import c.universe._
      // need to defer those to macro-expansion. [make error yet one macro ?]
      //c.error(s.tree.pos, " ~> unapply ourside select for loop")
      c.Expr[Option[(channels.InputChannel[Any],Any)]](reify(None).tree)
    }
    
  } 

  object ? 
  {
    def unapply(s: channels.SelectorContext): Option[(channels.InputChannel[Any],Any)] = ???
  }
  
  object <~
  {
    def unapply(s: channels.SelectorContext): Option[(channels.InputChannel[Any],Any)] = ???
  }

  object ! 
  {
    def unapply(s: channels.SelectorContext): Option[(channels.InputChannel[Any],Any)] = ???
  }
  
  
}
