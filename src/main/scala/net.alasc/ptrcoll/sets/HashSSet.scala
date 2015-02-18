package net.alasc.ptrcoll
package sets

import scala.annotation.{switch, tailrec}
import scala.{specialized => sp}
import scala.reflect.ClassTag

import spire.algebra.Order
import spire.syntax.cfor._

import syntax.all._

trait HashSSet[@specialized(Int) A] extends SSet[A] {
  protected[sets] def items: Array[A]
  protected[sets] def buckets: Array[Byte]
  protected[sets] def len: Int
  protected[sets] def used: Int
  protected[sets] def mask: Int
  protected[sets] def limit: Int

}

trait HashSSetImpl[@specialized(Int) A] extends HashSSet[A] with PointableAtImpl[A] { self =>
  /** Slots for items. */
  var items: Array[A]
  /** Status of the slots in the hash table.
    * 
    * 0 = unused
    * 2 = once used, now empty but not yet overwritten
    * 3 = used
    */ 
  var buckets: Array[Byte]
  /** Number of defined slots. */
  var len: Int //
  /** Number of used slots (used >= len). */
  var used: Int

  // hashing internals
  /** size - 1, used for hashing. */
  var mask: Int
  /** Point at which we should grow. */
  var limit: Int

  // SSet implementation

  def size = len

  /**
    * Return whether the item is found in the HashSSet or not.
    * 
    * On average, this is an O(1) operation; the (unlikely) worst-case
    * is O(n).
    */
  final def apply(item: A): Boolean = {
    @inline @tailrec def loop(i: Int, perturbation: Int): Boolean = {
      val j = i & mask
      val status = buckets(j)
      if (status == 0) {
        false
      } else if (status == 3 && items(j) == item) {
        true
      } else {
        loop((i << 2) + i + perturbation + 1, perturbation >> 5)
      }
    }
    val i = item.## & 0x7fffffff
    loop(i, i)
  }

  /**
    * Adds item to the set.
    * 
    * Returns whether or not the item was added. If item was already in
    * the set, this method will do nothing and return false.
    * 
    * On average, this is an amortized O(1) operation; the worst-case
    * is O(n), which will occur when the set must be resized.
    */
  def add(item: A): Boolean = {
    @inline @tailrec def loop(i: Int, perturbation: Int): Boolean = {
      val j = i & mask
      val status = buckets(j)
      if (status == 3) {
        if (items(j) == item)
          false
        else
          loop((i << 2) + i + perturbation + 1, perturbation >> 5)
      } else if (status == 2 && apply(item)) {
        false
      } else {
        items(j) = item
        buckets(j) = 3
        len += 1
        if (status == 0) {
          used += 1
          if (used > limit) grow()
        }
        true
      }
    }
    val i = item.## & 0x7fffffff
    loop(i, i)
  }

  /**
   * Remove an item from the set.
   * 
   * Returns whether the item was originally in the set or not.
   * 
   * This is an amortized O(1) operation.
   */
  final def remove(item: A): Boolean = {
    @inline @tailrec def loop(i: Int, perturbation: Int): Boolean = {
      val j = i & mask
      val status = buckets(j)
      if (status == 3 && items(j) == item) {
        buckets(j) = 2
        len -= 1
        true
      } else if (status == 0) {
        false
      } else {
        loop((i << 2) + i + perturbation + 1, perturbation >> 5)
      }
    }
    val i = item.## & 0x7fffffff
    loop(i, i)
  }

  /**
    * Grow the underlying array to best accomodate the set's size.
    * 
    * To preserve hashing access speed, the set's size should never be
    * more than 66% of the underlying array's size. When this size is
    * reached, the set needs to be updated (using this method) to have a
    * larger array.
    * 
    * The underlying array's size must always be a multiple of 2, which
    * means this method grows the array's size by 2x (or 4x if the set
    * is very small). This doubling helps amortize the cost of
    * resizing, since as the set gets larger growth will happen less
    * frequently. This method returns a null of type Unit1[A] to
    * trigger specialization without allocating an actual instance.
    * 
    * Growing is an O(n) operation, where n is the set's size.
    */
  final def grow(): Unit1[A] = {
    val next = buckets.length * (if (buckets.length < 10000) 4 else 2)
    val set = HashSSet.ofAllocatedSize[A](next)
    cfor(0)(_ < buckets.length, _ + 1) { i =>
      if (buckets(i) == 3) set += items(i)
    }
    absorb(set)
    null
  }

  /**
    * Aborb the given set's contents into this set.
    * 
    * This method does not copy the other set's contents. Thus, this
    * should only be used when there are no saved references to the
    * other set. It is private, and exists primarily to simplify the
    * implementation of certain methods.
    * 
    * This is an O(1) operation, although it can potentially generate a
    * lot of garbage (if the set was previously large).
    */
  private[this] def absorb(that: HashSSet[A]): Unit1[A] = {
    items = that.items
    buckets = that.buckets
    len = that.len
    used = that.used
    mask = that.mask
    limit = that.limit
    null
  }

  // PtrTC implementation
  def pointer: Ptr = {
    var i = 0
    while (i < buckets.length && buckets(i) != 3) i += 1
    if (i < buckets.length) Ptr(i) else Ptr(-1)
  }
  def next(ptr: RawPtr): RawPtr = {
    var i = ptr.toInt + 1
    while (i < buckets.length && buckets(i) != 3) i += 1
    if (i < buckets.length) Ptr(i) else Ptr(-1)
  }
  def hasAt(ptr: RawPtr): Boolean = ptr != -1
  def at(ptr: RawPtr): A = items(ptr.toInt)
}

object HashSSet {
  @inline final def startSize = 8
  def empty[@sp(Int) A: ClassTag]: HashSSet[A] = ofSize(0)
  def apply[@sp(Int) A: ClassTag](items: A*): HashSSet[A] = {
    val s = ofSize[A](items.size)
    items.foreach { a => s += a }
    s
  }
  /**
    * Allocate an empty HashSSet, capable of holding n items without
    * resizing itself.
    * 
    * This method is useful if you know you'll be adding a large number
    * of elements in advance and you want to save a few resizes.
    */
  def ofSize[@sp(Int) A: ClassTag](n: Int) =
    ofAllocatedSize(n / 2 * 3)

  /**
    * Allocate an empty HashSSet, with underlying storage of size n.
    * 
    * This method is useful if you know exactly how big you want the
    * underlying array to be. In most cases ofSize() is probably what
    * you want instead.
    */
  private[ptrcoll] def ofAllocatedSize[@sp(Int) A: ClassTag](n: Int) = {
    val sz = Util.nextPowerOfTwo(n) match {
      case n if n < 0 => throw PtrCollOverflowError(n)
      case 0 => 8
      case n => n
    }
    new HashSSetImpl[A] {
      def ct = implicitly[ClassTag[A]]
      var items = new Array[A](sz)
      var buckets = new Array[Byte](sz)
      var len = 0
      var used = 0
      var mask = sz - 1
      var limit = (sz * 0.65).toInt
    }
  }
}

/*
trait HashSSetImpl[@specialized(Int) A] extends SortedSSet[A] with PointableAtImpl[A] { self =>
  var items: Array[A]
  var size: Int

  protected def findWhere(item: A): Int = {
    var lb = 0
    var ub = size
    while (lb < ub) {
      val m = (lb + ub) >>> 1
      val c = order.compare(items(m), item)
      if (c == 0) return m
      if (c < 0)
        lb = m + 1
      else
        ub = m
    }
    // now lb == ub
    if (lb == size) return ~size
    val c = order.compare(items(lb), item)
    if (c == 0) return lb
    if (c > 0) return ~lb
    sys.error("Should not happen")
  }

  def -=(item: A) = {
    val pos = findWhere(item)
    if (pos >= 0) {
      java.lang.System.arraycopy(items, pos + 1, items, pos, size - pos - 1)
      size -= 1
    }
    this
  }

  def apply(item: A): Boolean = findWhere(item) >= 0

  def +=(item: A) = {
    val pos = findWhere(item)
    if (pos < 0) {
      val ipos = ~pos
      val newItems = if (size < items.length) items else {
        val arr = new Array[A](items.length * 2)
        java.lang.System.arraycopy(items, 0, arr, 0, ipos)
        arr
      }
      java.lang.System.arraycopy(items, ipos, newItems, ipos + 1, size - ipos)
      items = newItems
      items(ipos) = item
      size += 1
    }
    this
  }
  def pointer: Ptr = if (size == 0) Ptr(-1) else Ptr(0)

    // hidden by SortedSSet cast
  def next(ptr: RawPtr) = if (ptr == size - 1) Ptr(-1) else Ptr(ptr + 1)
  def at(ptr: RawPtr): A = items(ptr.toInt)
  def hasAt(ptr: RawPtr) = ptr >= 0 && ptr < size
}

object SortedSSet {
  def empty[A:Order:ClassTag]: SortedSSet[A] = new SortedSSetImpl[A] {
    def ct = implicitly[ClassTag[A]]
    def order = Order[A]
    var items = new Array[A](8)
    var size = 0
  }
  def apply[A:Order:ClassTag](items: A*): SortedSSet[A] = {
    val s = empty[A]
    items.foreach { a => s += a }
    s
  }
}
 */
