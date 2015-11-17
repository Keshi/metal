package metal
package macros

import spire.macros.compat.{termName, freshTermName, resetLocalAttrs, Context, setOrig}
import spire.macros.{SyntaxUtil, InlineUtil}

import spire.algebra._

import MacroUtils._

object VPtrOps {

  def element[T:c.WeakTypeTag, E:c.WeakTypeTag](c: Context): c.Expr[E] = {
    import c.universe._
    val lhs = c.prefix.tree
    val eType = implicitly[c.WeakTypeTag[E]]
    val container = extractPath[T](c)
    c.Expr[E](q"$container.ptrElement[$eType](new VPtr[$container.Tag, $container.Cap]($lhs.raw))")
  }

  def key[T:c.WeakTypeTag, A:c.WeakTypeTag](c: Context): c.Expr[A] = {
    import c.universe._
    val lhs = c.prefix.tree
    val aType = implicitly[c.WeakTypeTag[A]]
    val container = extractPath[T](c)
    c.Expr[A](q"$container.ptrKey[$aType](new VPtr[$container.Tag, $container.Cap]($lhs.raw))")
  }

  def next[T <: Pointable#Tag:c.WeakTypeTag, C <: Nextable:c.WeakTypeTag](c: Context): c.Expr[Ptr[T, C]] = {
    import c.universe._
    val tagT = implicitly[c.WeakTypeTag[T]]
    val tagC = implicitly[c.WeakTypeTag[C]]
    val lhs = c.prefix.tree
    val container = extractPath[T](c)
    c.Expr[Ptr[T, C]](q"new Ptr[$tagT, $tagC]($container.ptrNext(new VPtr[$container.Tag, $container.Cap]($lhs.raw)).raw)")
  }

  def remove[T <: Pointable#Tag](c: Context): c.Expr[Unit] = {
    import c.universe._
    val lhs = c.prefix.tree
    val container = extractPath[T](c)
    c.Expr[Unit](q"$container.ptrRemove(new VPtr[$container.Tag, $container.Cap]($lhs.raw))")
  }

  def removeAndAdvance[T <: Pointable#Tag, C](c: Context): c.Expr[Ptr[T, C]] = {
    import c.universe._
    val lhs = c.prefix.tree
    val container = extractPath[T](c)
    val tagT = implicitly[c.WeakTypeTag[T]]
    val tagC = implicitly[c.WeakTypeTag[C]]
    c.Expr[Ptr[T, C]](q"new Ptr[$tagT, $tagC]($container.ptrRemoveAndAdvance(new VPtr[$container.Tag, $container.Cap]($lhs.raw)).raw)")
  }


  def update[T:c.WeakTypeTag, V:c.WeakTypeTag](c: Context)(newValue: c.Expr[V]): c.Expr[Unit] = {
    import c.universe._
    val lhs = c.prefix.tree
    val container = extractPath[T](c)
    val tagV = implicitly[c.WeakTypeTag[V]]
    c.Expr[Unit](q"$container.ptrUpdate[$tagV](new VPtr[$container.Tag, $container.Cap]($lhs.raw), $newValue)")
  }

  def update1[T:c.WeakTypeTag, V1:c.WeakTypeTag](c: Context)(newValue1: c.Expr[V1]): c.Expr[Unit] = {
    import c.universe._
    val lhs = c.prefix.tree
    val container = extractPath[T](c)
    val tagV1 = implicitly[c.WeakTypeTag[V1]]
    c.Expr[Unit](q"$container.ptrUpdate1[$tagV1](new VPtr[$container.Tag, $container.Cap]($lhs.raw), $newValue1)")
  }

  def update2[T:c.WeakTypeTag, V2:c.WeakTypeTag](c: Context)(newValue2: c.Expr[V2]): c.Expr[Unit] = {
    import c.universe._
    val lhs = c.prefix.tree
    val container = extractPath[T](c)
    val tagV2 = implicitly[c.WeakTypeTag[V2]]
    c.Expr[Unit](q"$container.ptrUpdate2[$tagV2](new VPtr[$container.Tag, $container.Cap]($lhs.raw), $newValue2)")
  }

  def value[T:c.WeakTypeTag, V:c.WeakTypeTag](c: Context): c.Expr[V] = {
    import c.universe._
    val lhs = c.prefix.tree
    val vType = implicitly[c.WeakTypeTag[V]]
    val container = extractPath[T](c)
    c.Expr[V](q"$container.ptrValue[$vType](new VPtr[$container.Tag, $container.Cap]($lhs.raw))")
  }

  def value1[T:c.WeakTypeTag, V1:c.WeakTypeTag](c: Context): c.Expr[V1] = {
    import c.universe._
    val lhs = c.prefix.tree
    val v1Type = implicitly[c.WeakTypeTag[V1]]
    val container = extractPath[T](c)
    c.Expr[V1](q"$container.ptrValue1[$v1Type](new VPtr[$container.Tag, $container.Cap]($lhs.raw))")
  }

  def value2[T:c.WeakTypeTag, V2:c.WeakTypeTag](c: Context): c.Expr[V2] = {
    import c.universe._
    val lhs = c.prefix.tree
    val v2Type = implicitly[c.WeakTypeTag[V2]]
    val container = extractPath[T](c)
    c.Expr[V2](q"$container.ptrValue2[$v2Type](new VPtr[$container.Tag, $container.Cap]($lhs.raw))")
  }

}