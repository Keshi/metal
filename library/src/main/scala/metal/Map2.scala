package metal

import scala.reflect.ClassTag
import scala.annotation.tailrec

import spire.algebra.Order
import spire.util.Opt

trait FMap2[K, V1, V2] extends FColl with ElementsKV1V2[K, V1, V2] with Searchable[K] with Enumerable with Values1[V1] with Values2[V2] with JavaMethods[FMap2[K, V1, V2]] { lhs =>

  implicit def K: Methods[K]
  implicit def V1: Methods[V1]
  implicit def V2: Methods[V2]

  type IType <: IMap2[K, V1, V2]
  type MType <: MMap2[K, V1, V2]

  def mutableCopy(): MMap2[K, V1, V2] with MType

  override def stringPrefix = "FMap2"

  final def ptrCastT(any: Any): Opt[FMap2[K, V1, V2]] = any match {
    case rhs: FMap2[K, V1, V2] if lhs.K == rhs.K && lhs.V1 == rhs.V1 && lhs.V2 == rhs.V2 => Opt(rhs)
    case _ => Opt.empty[FMap2[K, V1, V2]]
  }

  def keyArray(ptr: VPtr[Tag]): Array[K]
  def keyIndex(ptr: VPtr[Tag]): Int
  def value1Array(ptr: VPtr[Tag]): Array[V1]
  def value1Index(ptr: VPtr[Tag]): Int
  def value2Array(ptr: VPtr[Tag]): Array[V2]
  def value2Index(ptr: VPtr[Tag]): Int

  def ptrHash(ptr: VPtr[Tag]): Int = {
    val kh = K.hashElement(keyArray(ptr), keyIndex(ptr))
    val v1h = V1.hashElement(value1Array(ptr), value1Index(ptr))
    val v2h = V2.hashElement(value2Array(ptr), value2Index(ptr))
    kh ^ (v1h * 41) ^ (v2h * 41 * 41)
  }

  def ptrToString(ptr: VPtr[Tag]): String = {
    val ks = K.toStringElement(keyArray(ptr), keyIndex(ptr))
    val v1s = V1.toStringElement(value1Array(ptr), value1Index(ptr))
    val v2s = V2.toStringElement(value2Array(ptr), value2Index(ptr))
    s"$ks -> ($v1s, $v2s)"
  }

  final def ptrEquals(thisPtr: VPtr[Tag], that: FMap2[K, V1, V2]): Boolean =
    that.ptrFindFromArray(keyArray(thisPtr), keyIndex(thisPtr)) match {
      case VPtr(thatPtr) =>
        val thisA1 = value1Array(thisPtr)
        val thisI1 = value1Index(thisPtr)
        val thisA2 = value2Array(thisPtr)
        val thisI2 = value2Index(thisPtr)
        val thatA1 = that.value1Array(thatPtr)
        val thatI1 = that.value1Index(thatPtr)
        val thatA2 = that.value2Array(thatPtr)
        val thatI2 = that.value2Index(thatPtr)
        V1.equalsElement(thisA1, thisI1, thatA1, thatI1) &&
        V2.equalsElement(thisA2, thisI2, thatA2, thatI2)
    }
}

trait IMap2[K, V1, V2] extends IColl with FMap2[K, V1, V2]

trait MMap2[K, V1, V2] extends MColl with FMap2[K, V1, V2] with AddKeys[K] with Removable[K] with Updatable1[V1] with Updatable2[V2] {

  def result(): IMap2[K, V1, V2] with IType

}