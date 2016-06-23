/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.internal.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;

import org.apache.uima.jcas.cas.TOP;

/**
 * A set of FSs, ordered using a comparator 
 * Not thread-safe, use on single thread only
 * 
 * Use: set-sorted indexes in UIMA
 * 
 * Entries kept in order in 1 big ArrayList
 * 
 * Adds optimized:
 *   - maintain high mark, if >, add to end
 *   - batch adds other than above
 *     -- do when reference needed
 *     -- sort the to be added
 *   - to add to pos p, shift elements in p to higher, insert   
 * 
 * shifting optimization: 
 *   removes replace element with null
 *   shift until hit null 
 *   
 * bitset: 1 for avail slot
 *   used to compute move for array copy
 * 
 *   
 */
public class OrderedFsSet_array implements NavigableSet<TOP> {
//  public boolean specialDebug = false;
  final private static boolean TRACE = false;
  final private static boolean MEASURE = false;
  final private static int DEFAULT_MIN_SIZE = 8;  // power of 2 please
  final private static int MAX_DOUBLE_SIZE = 1024 * 1024 * 4;  // 4 million, power of 2 please
  final private static int MIN_SIZE = 8;
   
//  final private static MethodHandle getActualArray;
//  
//  static {
//    Field f;
//    try {
//      f = ArrayList.class.getDeclaredField("array");
//    } catch (NoSuchFieldException e) {
//      try {
//        f = ArrayList.class.getDeclaredField("elementData");
//      } catch (NoSuchFieldException e2) {
//        throw new RuntimeException(e2);
//      }
//    }
//    
//    f.setAccessible(true);
//    try {
//      getActualArray = Misc.UIMAlookup.unreflectGetter(f);
//    } catch (IllegalAccessException e) {
//      throw new RuntimeException(e);
//    }
//  }
  
    
  private TOP[] a = new TOP[DEFAULT_MIN_SIZE];
  private int a_nextFreeslot = 0;
  private int a_firstUsedslot = 0;
  
  private final ArrayList<TOP> batch = new ArrayList<>();
  
  final private Comparator<TOP> comparatorWithID;
  final public Comparator<TOP> comparatorWithoutID;
  private int size = 0;
  private int maxSize = 0;
  
  private TOP highest = null;
  private int nullBlockStart = -1;  // inclusive
  private int nullBlockEnd = -1 ;    // exclusive
  
  private boolean doingBatchAdds = false;
  private int modificationCount = 0;
  private int lastRemovedPos = -1;
    
  private StringBuilder tr = TRACE ? new StringBuilder() : null;
  public OrderedFsSet_array(Comparator<TOP> comparatorWithID, Comparator<TOP> comparatorWithoutID) {
    this.comparatorWithID = comparatorWithID;
    this.comparatorWithoutID = comparatorWithoutID;
  }

  @Override
  public Comparator<? super TOP> comparator() {
    return comparatorWithID;
  }

  @Override
  public TOP first() {
    processBatch();
    if (size == 0) {
      throw new NoSuchElementException();
    }
    for (int i = a_firstUsedslot; i < a_nextFreeslot; i++) {
      TOP item = a[i];
      if (null != item) {
        if (i > a_firstUsedslot) {
          a_firstUsedslot = i;
        }
        return item; 
      }
    }
    Misc.internalError();
    return null;
  }

  @Override
  public TOP last() {
    processBatch();
    if (size == 0) {
      throw new NoSuchElementException();
    }
    for (int i = a_nextFreeslot - 1; i >= a_firstUsedslot; i--) {
      TOP item = a[i];
      if (item != null) {
        if (i < a_nextFreeslot - 1) {
          a_nextFreeslot = i + 1;
        }
        return item;
      }
    }
    Misc.internalError();
    return null;
  }

  @Override
  public int size() {
    processBatch();
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0 && batch.size() == 0;
  }

  @Override
  public boolean contains(Object o) {
    if (o == null) {
      throw new IllegalArgumentException();
    }
    if (isEmpty()) {
      return false;
    }
    TOP fs = (TOP) o;
    processBatch();
    return find(fs) >= 0;
  }

  @Override
  public Object[] toArray() {
    Object [] r = new Object[size()];
    int i = 0;
    for (TOP item : a) {
      if (item != null) {
        r[i++] = item;
      }
    }
    return r;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a1) {
    if (a1.length < size()) {
      a1 = (T[]) Array.newInstance(a.getClass(), size());
    }
    int i = 0;
    for (TOP item : a) {
      if (item != null) {
        a1[i++] = (T) item;
      }
    }
    return a1;
  }

  /**
   * Note: doesn't implement the return value; always returns true;
   * @param fs -
   * @return -
   */
  @Override
  public boolean add(TOP fs) {
    if (highest == null) {
      addNewHighest(fs);
      return true;
    }
    
    int c = comparatorWithID.compare(fs, highest);
    if (c > 0) {
      addNewHighest(fs);
      return true;
    }
    
    if (c == 0) {
      return false;
    }
    
    batch.add(fs);
    if (MEASURE) {
      addNotToEndCount ++;
    }
    return true;
  }
  
  private void addNewHighest(TOP fs) {
    highest = fs;
    ensureCapacity(1);
    a[a_nextFreeslot++] = fs;
    incrSize();
    if (MEASURE) {
      addToEndCount++;
    }
    return;
  }
  
  private void incrSize() {
    size++;
    maxSize = Math.max(maxSize, size);
    modificationCount++;
  }
  
  // debug
  private void validateA() {
    int sz = 0;
    for (TOP item : a) {
      if (item != null) {
        sz++;
      }
    }
    if (sz != size) {
      System.out.println("debug ");
    }
    assert sz == size;
    for (int i = 0; i < a_firstUsedslot; i++) {
      assert a[i] == null;
    }
    for (int i = a_nextFreeslot; i < a.length; i++) {
      assert a[i] == null;
    }
  }

  private void ensureCapacity(int incr) {
    int szNeeded = a_nextFreeslot + incr;
    if (szNeeded <= a.length) {
      return;
    }
    int sz = a.length;
    do {
      sz = (sz < MAX_DOUBLE_SIZE) ? (sz << 1) : (sz + MAX_DOUBLE_SIZE);
    } while (sz < szNeeded);
    
    TOP[] aa = new TOP[sz];
    System.arraycopy(a, 0, aa, 0, a_nextFreeslot);
    a = aa;
  }
  
  private boolean shrinkCapacity() {
    int nextSmallerSize = getNextSmallerSize(2);
    if (nextSmallerSize == MIN_SIZE) {
      return false;
    }
    if (maxSize < nextSmallerSize) {
      a = new TOP[getNextSmallerSize(1)];
      maxSize = 0;
      return true;
    }
    maxSize = 0; 
    return false;
  }
  
  /**
   * get next smaller size
   * @param n number of increments
   * @return the size
   */
  private int getNextSmallerSize(int n) {
    int sz = a.length;
    if (sz <= MIN_SIZE) {
      return MIN_SIZE;
    }
    for (int i = 0; i < n; i ++) {
      sz = (sz > MAX_DOUBLE_SIZE) ? (sz - MAX_DOUBLE_SIZE) : sz >> 1;
    }
    return sz;
  }
  
  private void processBatch() {
    if (batch.size() != 0) {
//      validateA();
      doProcessBatch();
    }
  }
  
  /**
   * Because multiple threads can be "reading" the CAS and using iterators,
   * the sync must insure that the setting of batch.size() to 0 occurs after
   * all the adding is done.
   * 
   * This keeps other threads blocked until the batch is completely processed.
   */
  private void doProcessBatch() {
    synchronized (batch) {
      int batchSize = batch.size();
      if (batchSize == 0) {
        return;  // another thread did this
      }
      if (doingBatchAdds == true) {
        return;  // bypass recursive calls from Eclipse IDE on same thread
      }
      try {
       
        doingBatchAdds = true;
        if (MEASURE) {
          batchAddCount ++;
          batchAddTotal += batchSize;
          batchCountHistogram[31 - Integer.numberOfLeadingZeros(batchSize)] ++;
        }
        
        int nbrNewSlots = 1;
        
        if (batchSize > 1) {
          // Sort the items to add 
          Collections.sort(batch, comparatorWithID);
          TOP prev = batch.get(batchSize - 1);
        
//          nbrNewSlots = batch.size();
          // count dups (to reduce excess allocations)
          //   deDups done using the comparatorWithID
          final boolean useEq = comparatorWithID != comparatorWithoutID;  // true for Sorted, false for set
          for (int i = batchSize - 2; i >= 0; i--) {
            TOP item = batch.get(i);
            if (useEq ? (item == prev) : (comparatorWithID.compare(item, prev) == 0)) {
              batch.set(i + 1, null); // need to do this way so the order of adding is the same as v2
              if (i + 1 == batchSize - 1) {
                batchSize --;  // start with non-null when done
              }
            } else {
              prev = item;
              nbrNewSlots++;
            }
          }
        } 
        
        int i_batch = batchSize - 1;
        int insertPosOfAddedSpace = 0;
        TOP itemToAdd;
        // skip entries already found
        while ((itemToAdd = batch.get(i_batch)) == null || (insertPosOfAddedSpace = find(itemToAdd)) >= 0) {
          // skip any entries at end of list if they're already in the set
          i_batch--;
          nbrNewSlots --;
          if (i_batch < 0) {
            batch.clear();
            return; // all were already in the index
          }
        }
        
        insertPosOfAddedSpace = (- insertPosOfAddedSpace) - 1;
        // insertPos is insert point, i_batch is index of first batch element to insert
        // there may be other elements in batch that duplicate; these won't be inserted, but 
        //   there will be space lost in this case
        
        int indexOfNewItem = insertSpace(insertPosOfAddedSpace, nbrNewSlots) // returns index of a non-null item
                                                                           // the new item goes one spot to the left of this
            - 1;  // inserts nulls at the insert point, shifting other cells down
    
        // process first item
        insertItem(indexOfNewItem, itemToAdd);
//        TOP prevItem = itemToAdd;
        if (indexOfNewItem + 1 == a_nextFreeslot) {
          highest = itemToAdd;
        }
        nbrNewSlots --;
        
        int bPos = i_batch - 1; // next after first one from end
        for (; bPos >= 0; bPos --) {
          itemToAdd = batch.get(bPos);
          if (null == itemToAdd) {
            continue;  // skipping a duplicate
          }
          int pos = findRemaining(itemToAdd);
    
          if (pos >= 0) {
            continue;  // already in the list
          }
          pos = (-pos) - 1;
              
          indexOfNewItem = pos - 1;
          if (nullBlockStart == 0) {
            // this and all the rest of the elements are lower, insert in bulk
            insertItem(indexOfNewItem--, itemToAdd);
            nbrNewSlots --;
            bPos--;
            
            for (;bPos >= 0; bPos--) {          
              itemToAdd = batch.get(bPos);
              if (itemToAdd == null) {
                continue;
              }
              insertItem(indexOfNewItem--, itemToAdd);
              nbrNewSlots --;  // do this way to respect skipped items due to == to prev        
            }
            break;
          }
          
          if (indexOfNewItem == -1 || null != a[indexOfNewItem]) {
            indexOfNewItem = shiftFreespaceDown(pos, nbrNewSlots) - 1;  // results in null being available at pos - 1
          }
          insertItem(indexOfNewItem, itemToAdd);
          nbrNewSlots --;
        }
        
        if (nbrNewSlots > 0) {
          // have extra space left over due to dups not being added
          // If this space is not at beginning, move space to beginning or end (whichever is closer)
          if (indexOfNewItem - nbrNewSlots > 0) { 
            // space is not at beginning
          
            int mid = (a_nextFreeslot + a_firstUsedslot) >>> 1;  // overflow aware 
            if (indexOfNewItem < mid) {
              // move to beginning
              System.arraycopy(a, indexOfNewItem - nbrNewSlots, a, 0, nbrNewSlots);
              a_firstUsedslot += nbrNewSlots;
            } else {
              // move to end
              System.arraycopy(a, indexOfNewItem, a, indexOfNewItem - nbrNewSlots, a_nextFreeslot - indexOfNewItem);
              Arrays.fill(a, a_nextFreeslot - nbrNewSlots, a_nextFreeslot, null);
              a_nextFreeslot -= nbrNewSlots;
            }
          }
        }
        nullBlockStart = nullBlockEnd = -1;
//        validateA();
        batch.clear();
      } finally {
        doingBatchAdds = false;
      }
    }
  }
  
  /**
   * 
   * @param indexToUpdate - the index in the array to update with the item to add
   * @param itemToAdd -
   */
  private void insertItem(int indexToUpdate, TOP itemToAdd) {
    try {
    assert indexToUpdate >= 0;
    assert null == a[indexToUpdate];
    } catch (AssertionError e) {
      if (TRACE) {
        System.err.println("OrderedFsSet_array caught assert.  array values around indexToUpdate: ");
        for (int i = indexToUpdate - 2; i < indexToUpdate + 3; i++) {
          if (i >= 0 && i < a.length) {
            System.err.format("a[%,d]: %s%n", i, a[i].toString(2));
          } else {
            System.err.format("a[%,d}: out-of-range%n", i);
          }
        }
        System.err.format("trace info: %n%s", tr);
      }
      throw e;
    }
    a[indexToUpdate] = itemToAdd;
    incrSize();
    if (indexToUpdate < a_firstUsedslot) {
      a_firstUsedslot = indexToUpdate;  
    }
    if (nullBlockEnd == indexToUpdate + 1) {
      nullBlockEnd --;
    }
  }

  /**
   * Attempt to move a small amount; make use of both beginning and end free space.
   * 
   * @param positionToInsert position containing a value, to free up by moving the current free block
   *                         so that the last free element is at that (adjusted up) position.          
   * @param nbrNewSlots
   * @return adjusted positionToInsert, the free spot is just to the left of this position
   */
  private int insertSpace(int positionToInsert, int nbrNewSlots) {
    if (TRACE) {
      tr.setLength(0);
      tr.append("Tracing OrderedFsSet_array\n");
      tr.append(String.format("insertSpace called with positionToInsert: %,d nbrNewSlots: %,d%n", positionToInsert, nbrNewSlots));
    }
    if (nbrNewSlots == 1) {
      int distanceFromLastRemoved = (lastRemovedPos == -1) 
                                      ? Integer.MAX_VALUE 
                                      : (positionToInsert - lastRemovedPos);
      int distanceFromEnd = a_nextFreeslot - positionToInsert;
      int distanceFromFront = (0 == a_firstUsedslot)
                                ? Integer.MAX_VALUE
                                : positionToInsert - a_firstUsedslot;
      boolean useFront = distanceFromFront < distanceFromEnd;
      boolean useLastRemoved = Math.abs(distanceFromLastRemoved) < (useFront ? distanceFromFront : distanceFromEnd);
      
      if (TRACE) 
        tr.append(String.format("distances: %d %d %d, useFront: %s useLastRemoved: %s%n",
            distanceFromLastRemoved, distanceFromEnd, distanceFromFront, useFront, useLastRemoved));
      if (useLastRemoved) {  // due to find skipping over nulls, the distanceFromLastRemoved is never 0
        if (distanceFromLastRemoved > 0) {
          if (distanceFromLastRemoved != 1) { 
            nullBlockStart = lastRemovedPos;
            nullBlockEnd = lastRemovedPos + 1;
            shiftFreespaceUp(lastRemovedPos, nbrNewSlots);
          }
          lastRemovedPos = -1; 
          return positionToInsert;
        } else {
          nullBlockStart = lastRemovedPos;
          lastRemovedPos = -1;
          int r = shiftFreespaceDown(positionToInsert, nbrNewSlots);
          if (TRACE) 
            tr.append(String.format("shiftFreespaceDown result was %,d%n", r));
          return r;
        }
      }
      if (useFront) {
        nullBlockStart = a_firstUsedslot - 1;
//        if (null != a[nullBlockStart]) {
        if (a_firstUsedslot != positionToInsert) {
          // need to move the free slot if not already next to the insert position
          nullBlockEnd = a_firstUsedslot;
          shiftFreespaceUp(positionToInsert, nbrNewSlots);
        }
        a_firstUsedslot --;
        return positionToInsert;
      }
    }
    
    ensureCapacity(nbrNewSlots);
    nullBlockStart = a_nextFreeslot;
    nullBlockEnd = nullBlockStart + nbrNewSlots; 
    a_nextFreeslot += nbrNewSlots;
    int r = shiftFreespaceDown(positionToInsert, nbrNewSlots);
    if (TRACE) {
      tr.append(String.format("shiftFreespaceDown2 result was %,d, nullBlockStart: %,d nullBlockEnd: %,d a_nextFreeslot: %,d%n", 
          r, nullBlockStart, nullBlockEnd, a_nextFreeslot));
    }
    return r;
  }
  
  /**
   * Shift a block of free space lower in the array.
   * This is done by shifting the space at the insert point
   *   for length = start of free block - insert point 
   *   to the right by the nbrNewSlots
   *   and then resetting (filling) the freed up space with null
   *   
   * Example:  u = used, f = free space
   * 
   * before                      |--| 
   * uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuffffffffuuuuuuu
   *                             ^ insert point
   * after                               |--|
   * uuuuuuuuuuuuuuuuuuuuuuuuuuuuffffffffuuuuuuuuuuu
   *                                     ^ insert point
   *                                    
   * before 
   * |------------------------------|
   * uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuffffffffuuuuuuu
   * ^ insert point
   * after   |------------------------------| 
   * ffffffffuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu
   *        ^ insert point
   *                                    
   * move up by nbrNewSlots
   * length to move = nullBlockStart - insert point
   * new insert point is nbrOfFreeSlots higher (this points to a filled spot, prev spot is free)
   * 
   * fill goes from original newInsertPoint, for min(nbrNewSlots, length of move)
   *   
   * hidden param:  nullBlockStart
   * @param newInsertPoint index of slot array, currently occupied, where an item is to be set into
   * @param nbrNewSlots - the size of the inserted space
   * @return the updated insert point, now moved up
   */
  private int shiftFreespaceDown(int newInsertPoint, int nbrNewSlots) {
    int lengthToMove = nullBlockStart - newInsertPoint;
    System.arraycopy(a, newInsertPoint, a, newInsertPoint + nbrNewSlots, lengthToMove);
    int lengthToClear = Math.min(nbrNewSlots, lengthToMove);
    Arrays.fill(a, newInsertPoint, newInsertPoint + lengthToClear, null);
    nullBlockStart = newInsertPoint;
    nullBlockEnd = nullBlockStart + nbrNewSlots;
    if (MEASURE) {
      moveSizeHistogram[32 - Integer.numberOfLeadingZeros(lengthToMove)] ++;
      movePctHistogram[lengthToMove* 10 / (a_nextFreeslot - a_firstUsedslot)] ++;
      fillHistogram[32 - Integer.numberOfLeadingZeros(lengthToClear)] ++;
    }
    return newInsertPoint + nbrNewSlots;
  }
  
  /**
   * Shift a block of free space higher in the array.
   * This is done by shifting the space at the insert point
   *   of length = insert point - (end+1) of free block 
   *   to the left by the nbrNewSlots
   *   and then resetting (filling) the freed up space with null
   *   
   * Example:  u = used, f = free space
   * 
   * before              |-|   << block shifted 
   * uuuuuuuuuuuuuuufffffuuuuuuuuuuuuuuuuuuuuuuuuuuu
   *                        ^ insert point
   * after          |-|   << block shifted
   * uuuuuuuuuuuuuuuuuufffffuuuuuuuuuuuuuuuuuuu
   *                        ^ insert point
   *                                    
   * before                                  |----|
   * uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuffffuuuuuuu
   *                                               ^ insert point
   *     note: insert point is never beyond last because
   *     those are added immediately
   * after                               |----|
   * uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuffffu
   *                                               ^ insert point
   *                                    
   * before       |--|   
   * uuuuuuuuuuuufuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu
   *                  ^ insert point
   * after       |--|
   * uuuuuuuuuuuuuuuufuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu
   *                  ^ insert point
   *                                    
   *                                    
   *                                    
   * move down by nbrNewSlots
   * length to move = insert point - null block end (which is 1 plus index of last free)
   * new insert point is the same as the old one (this points to a filled spot, prev spot is free)
   * 
   * fill goes from original null block end, for min(nbrNewSlots, length of move)
   *   
   * hidden param:  nullBlockEnd = 1 past end of last free slot
   * @param newInsertPoint index of slot array, currently occupied, where an item is to be set into
   * @param nbrNewSlots - the size of the inserted space
   * @return the updated insert point, now moved up
   */
  
  private int shiftFreespaceUp(int newInsertPoint, int nbrNewSlots) {
    int lengthToMove = newInsertPoint - nullBlockEnd;
    System.arraycopy(a, nullBlockEnd, a, nullBlockStart, lengthToMove);
    int lengthToClear = Math.min(nbrNewSlots, lengthToMove);
    Arrays.fill(a, newInsertPoint - lengthToClear, newInsertPoint, null);
    nullBlockStart = newInsertPoint - nbrNewSlots;
    nullBlockEnd = newInsertPoint;
    return newInsertPoint;
  }
    
//  /**
//   * @param from start of items to shift, inclusive
//   * @param to end of items to shift, exclusive
//   */
//  private void shiftBy2(int from, int to) {
//    if (to == -1) {
//      to = theArray.size();
//      theArray.add(null);
//      theArray.add(null);
//    }
//      try {
//        Object[] aa = (Object[]) getActualArray.invokeExact(theArray);
//        System.arraycopy(aa, from, aa, from + 2, to - from);
//      } catch (Throwable e) {
//        throw new RuntimeException(e);
//      }
//  }

  /**
   * Never returns an index to a "null" (deleted) item.
   * If all items are LT key, returns - size - 1 
   * @param fs the key
   * @param pos first position to compare
   * @return the lowest position whose item is equal to or greater than fs;
   *         if not equal, the item's position is returned as -insertionPoint - 1. 
   *         If the key is greater than all elements, return -size - 1). 
   */
  private int find(TOP fs) {
    return binarySearch(fs);
  }
  
  private int findRemaining(TOP fs) {
    int pos = binarySearch(fs, a_firstUsedslot, nullBlockStart);
    return pos < 0 && ((-pos) - 1 == nullBlockStart) 
            ? ( -(nullBlockEnd) - 1) 
            : pos;
  }
    
  /**
   * Special version of binary search that ignores null values
   * @param fs the value to look for
   * @return the position whose non-null value is equal to fs, or is gt fs (in which case, return (-pos) - 1)
   */
  private int binarySearch(final TOP fs) {
    return binarySearch(fs, a_firstUsedslot, a_nextFreeslot);
  }
  
  private int binarySearch(final TOP fs, int start, int end) {

    if (start < 0 || end - start <= 0) {
      return (start < 0) ? -1 : ( (-start) - 1);  // means not found, insert at position start
    }
    int lower = start, upper = end;
    int prevUpperPos = -1;
    for (;;) {
    
      int mid = (lower + upper) >>> 1;  // overflow aware
      TOP item = a[mid];
      int delta = 0;
      int midup = mid;
      int middwn = mid;
      int pos = mid;
    
      while (null == item) {  // skip over nulls
        if (nullBlockStart != -1 && 
            middwn >= nullBlockStart && 
            midup  < nullBlockEnd) {
          // in the null block
          // move to edges
          midup  = nullBlockEnd;   // midup inclusive, nullBlockEnd exclusive
          middwn = nullBlockStart - 1; // middwn and nullBlockStart inclusive
        } else {
          delta ++;
        }
        boolean belowUpper = (pos = midup + delta) < upper;
        if (belowUpper && null != (item = a[pos])) {
          break;
        }
        boolean belowLower = (pos = middwn - delta) < lower;
        if (!belowLower && null != (item = a[pos])) {
          break;
        }
        if (! belowUpper && belowLower) {
          return (-prevUpperPos) - 1; // return previous
        }
      }
     
      int c = comparatorWithID.compare(fs, item);
      if (c == 0) {
        return pos;
      }
      
      if (c < 0) {  // fs is smaller than item at pos in array; search downwards
        upper = prevUpperPos = pos; 
        if (upper == lower) {
          return (-lower) - 1;
        }
      } else {  // fs is larger than item at pos in array; search upwards
        lower = pos + 1;
        if (lower == upper) {
          return (-upper) - 1;
        }
      }
    }
  }
  
  @Override
  public boolean remove(Object o) {
    processBatch();
    TOP fs = (TOP) o;
    
    int pos = find(fs);
    if (pos < 0) {
      return false;
    }
    
    // at this point, pos points to a spot that compares "equal" using the comparator
    // for sets, this is the single item that is in the index
    // for sorted, because find uses the compareWithID comparator, this is the unique equal element
    assert a[pos] != null;
    a[pos] = null;
    size --;
    modificationCount ++;
    if (size == 0) {
      clearResets();
    } else {
      if (pos == a_firstUsedslot) {
        do {  // removed the first used slot
          a_firstUsedslot ++;
        } while (a[a_firstUsedslot] == null);
      } else if (pos == a_nextFreeslot - 1) {
        do {
          a_nextFreeslot --;
        } while (a[a_nextFreeslot - 1] == null);
        highest = a[a_nextFreeslot - 1];
      } 
      
      if (size < ((a_nextFreeslot - a_firstUsedslot) >> 1) &&
          size > 8) {
        compressOutRemoves();
      } else if (lastRemovedPos > a_firstUsedslot &&
                 lastRemovedPos < (a_nextFreeslot - 1) ) {
        lastRemovedPos = pos;
      }
    }
    return true;
  }
  
  /**
   * When the main array between the first used slot and the next free slot has too many nulls 
   * representing removed items, scan and gc them.
   *   assumes: first used slot is not null, nextFreeslot - 1 is not null
   */
  private void compressOutRemoves() {
    int j = a_firstUsedslot + 1;
    for (int i = a_firstUsedslot + 1; i < a_nextFreeslot; i++, j++) {
      while (a[i] == null) {
        i ++;
      }
      if (i > j) {
        a[j] = a[i];
      }
    }
    
    Arrays.fill(a, j, a_nextFreeslot, null);
    a_nextFreeslot = j;
    lastRemovedPos = -1;
  }
  
  @Override
  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends TOP> c) {
    boolean changed = false;
    for (TOP item : c) {
      changed |= add(item);
    }
    return changed;
  }
  
  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public void clear() {
    if (isEmpty()) {
      return;
    }
    if (!shrinkCapacity()) {
      Arrays.fill(a, a_firstUsedslot, a_nextFreeslot, null);      
    }
    clearResets();
  }
  
  private void clearResets() {
    a_firstUsedslot = 0;
    a_nextFreeslot = 0;
    batch.clear();
    size = 0;
    maxSize = 0;
    nullBlockStart = -1;
    nullBlockEnd = -1;
    doingBatchAdds = false; // just for safety, not logically needed I think.
    highest = null;    
    modificationCount ++;
    lastRemovedPos = -1;
  }

  @Override
  public TOP lower(TOP fs) {
    int pos = lowerPos(fs);
    return (pos < 0) ? null : a[pos];
  }
  
  /**
   * @param fs element to test
   * @return pos of greatest element less that fs or -1 if no such
   */
  public int lowerPos(TOP fs) {
    processBatch();
    int pos = find(fs); // position of lowest item GE fs  
    pos = (pos < 0) ? ((-pos) - 2) : (pos - 1);
    // above line subtracts 1 from LE pos; pos is now lt, may be -1
    while (pos >= a_firstUsedslot) {
      if (a[pos] != null) {
        return pos;
      }
      pos --;
    }
    return -1; 
  }


  @Override
  public TOP floor(TOP fs) {
    int pos = floorPos(fs);
    return (pos < 0) ? null : a[pos];
  }
  
  public int floorPos(TOP fs) {
    processBatch();
    int pos = find(fs);  // position of lowest item GE fs
    if (pos < 0){
      pos = (-pos) - 2;
    }
    // pos is = or lt, may be -1
    while (pos >= a_firstUsedslot) {
      if (a[pos] != null) {
        return pos;
      }
      pos --;
    }
    return -1;
  }

  @Override
  public TOP ceiling(TOP fs) {
    int pos = ceilingPos(fs);
    return (pos < a_nextFreeslot) ? a[pos] : null;
  }
  
  
  public int ceilingPos(TOP fs) {
    processBatch();
    int pos = find(fs); // position of lowest item GE fs
    if (pos < 0){
      pos = (-pos) -1;
    } else {
      return pos;
    }
    
    while (pos < a_nextFreeslot) {
      if (a[pos] != null) {
        return pos;
      }
      pos ++;
    }
    return pos;
  }

  @Override
  public TOP higher(TOP fs) {
    int pos = higherPos(fs);
    return (pos < a_nextFreeslot) ? a[pos] : null;
  }

  public int higherPos(TOP fs) {
    processBatch();
    int pos = find(fs); // position of lowest item GE fs
    pos = (pos < 0) ? ((-pos) -1) : (pos + 1);
    
    while (pos < a_nextFreeslot) {
      if (a[pos] != null) {
        return pos;
      }
      pos ++;
    }
    return pos;
  }

  @Override
  public TOP pollFirst() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TOP pollLast() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<TOP> iterator() {
    processBatch();
    if (a_nextFreeslot == 0) {
      return Collections.emptyIterator();
    }
    return new Iterator<TOP>() {
      private int pos = a_firstUsedslot;
      { incrToNext(); 
        if (MEASURE) {
          int s = a_nextFreeslot - a_firstUsedslot;
          iterPctEmptySkip[(s - size()) * 10 / s] ++;
        }
      }
       
      @Override
      public boolean hasNext() {
        if (batch.size() > 0) {
          throw new ConcurrentModificationException();
        }
        return pos < a_nextFreeslot;
      }
      
      @Override
      public TOP next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        TOP r = a[pos++];
        incrToNext();
        return r;        
      }
      
      private void incrToNext() {
        while (pos < a_nextFreeslot) {
          if (a[pos] != null) {
            break;
          }
          pos ++;
        }
      }
    };
  }

  @Override
  public NavigableSet<TOP> descendingSet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<TOP> descendingIterator() {
    processBatch();
    return new Iterator<TOP>() {
      private int pos = a_nextFreeslot - 1;    // 2 slots:  next free = 2, first slot = 0
                                               // 1 slot:   next free = 1, first slot = 0
                                               // 0 slots:  next free = 0; first slot = 0 (not -1)
      { if (pos >= 0) {  // pos is -1 if set is empty
        decrToNext(); 
        }
      }
       
      @Override
      public boolean hasNext() {
        return pos >= a_firstUsedslot;
      }
      
      @Override
      public TOP next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        TOP r = a[pos--];
        decrToNext();
        return r;        
      }
      
      private void decrToNext() {
        while (pos >= a_firstUsedslot) {
          if (a[pos] != null) {
            break;
          }
          pos --;
        }
      }
    };
  }

  @Override
  public NavigableSet<TOP> subSet(TOP fromElement, boolean fromInclusive, TOP toElement, boolean toInclusive) {
    return new SubSet(fromElement, fromInclusive, toElement, toInclusive, false, null);
  }

  @Override
  public NavigableSet<TOP> headSet(TOP toElement, boolean inclusive) {
    if (isEmpty()) {
      return this; 
    }
    return subSet(first(), true, toElement, inclusive);     
  }

  @Override
  public NavigableSet<TOP> tailSet(TOP fromElement, boolean inclusive) {
    if (isEmpty()) {
      return this;
    }
    return subSet(fromElement, inclusive, last(), true);
  }

  @Override
  public SortedSet<TOP> subSet(TOP fromElement, TOP toElement) {
    return subSet(fromElement, true, toElement, false);
  }

  @Override
  public SortedSet<TOP> headSet(TOP toElement) {
    return headSet(toElement, false);
  }

  @Override
  public SortedSet<TOP> tailSet(TOP fromElement) {
    return tailSet(fromElement, true);
  }
  
  
  /**
   * This is used in a particular manner:
   *   only used to create iterators over that subset
   *     -- no insert/delete
   */
  public class SubSet implements NavigableSet<TOP> {
    final private TOP fromElement;
    final private TOP toElement;
    final private boolean fromInclusive;
    final private boolean toInclusive;
    
    final private int firstPosInRange;
    final private int lastPosInRange;  // inclusive
    
    final private TOP firstElementInRange;
    final private TOP lastElementInRange;
        
    private int sizeSubSet = -1; // lazy - computed on first ref

    SubSet(TOP fromElement, boolean fromInclusive, TOP toElement, boolean toInclusive) {
      this(fromElement, fromInclusive, toElement, toInclusive, true, comparatorWithID);
    }
    
    SubSet(TOP fromElement, boolean fromInclusive, TOP toElement, boolean toInclusive, boolean doTest, Comparator<TOP> comparator) {
  
      this.fromElement = fromElement;
      this.toElement = toElement;
      this.fromInclusive = fromInclusive;
      this.toInclusive = toInclusive;
      if (doTest && comparator.compare(fromElement, toElement) > 0) {
        throw new IllegalArgumentException();
      }
      processBatch();    
      firstPosInRange = fromInclusive ? ceilingPos(fromElement) : higherPos(fromElement);
      lastPosInRange  = toInclusive ? floorPos(toElement) : lowerPos(toElement);
      // lastPosInRange can be LT firstPosition if fromInclusive is false
      //   In this case, the subset is empty
      if (lastPosInRange < firstPosInRange) {
        firstElementInRange = null;
        lastElementInRange = null;
      } else {
        firstElementInRange = a[firstPosInRange];
        lastElementInRange = a[lastPosInRange];
      }
    }
    
    @Override
    public Comparator<? super TOP> comparator() {
      return comparatorWithID;
    }

    @Override
    public TOP first() {
      return firstElementInRange;
    }

    @Override
    public TOP last() {
      return lastElementInRange;
    }

    @Override
    public int size() {
      if (firstElementInRange == null) {
        return 0;
      }
      if (sizeSubSet == -1) {
        Iterator<TOP> it = iterator();
        int i = 0;
        while (it.hasNext()) {
          it.next();
          i++;
        }
        sizeSubSet = i;
      }
      return sizeSubSet;
    }

    @Override
    public boolean isEmpty() {
      return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
      TOP fs = (TOP) o;
      if (!isInRange(fs)) {
        return false;
      }
      return OrderedFsSet_array.this.contains(o);
    }

    @Override
    public Object[] toArray() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a1) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(TOP e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends TOP> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TOP lower(TOP fs) {
      if (lastElementInRange == null || isLeFirst(fs)) {
        return null;
      }
      // if the key is > lastElement, 
      //   return last element
      if (isGtLast(fs)) {
        return lastElementInRange;
      }
      // in range
      return OrderedFsSet_array.this.lower(fs);
    }

    @Override
    public TOP floor(TOP fs) {
      
      // if the key is < the first element in the range, return null
      if (lastElementInRange == null || isLtFirst(fs)) {
        return null;
      }
      
      // if the key is >= lastElement, 
      //   return last element
      if (isGeLast(fs)) {
        return lastElementInRange;
      }
      
      return OrderedFsSet_array.this.floor(fs);
    }

    @Override
    public TOP ceiling(TOP fs) {
      // if the key is > the last element in the range, return null
      if (firstElementInRange == null || isGtLast(fs)) {
        return null;
      }
      
      if (isLeFirst(fs)) {
        return firstElementInRange;
      }
      
      return OrderedFsSet_array.this.ceiling(fs);
    }

    @Override
    public TOP higher(TOP fs) {
      if (firstElementInRange == null || isGeLast(fs)) {
        return null;
      }
      
      if (isLtFirst(fs)) {
        return firstElementInRange;
      }
      
      return OrderedFsSet_array.this.higher(fs);
    }

    @Override
    public TOP pollFirst() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TOP pollLast() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<TOP> iterator() {
      if (firstElementInRange == null) {
        return Collections.emptyIterator();
      }
      return new Iterator<TOP>() {
        private int pos = firstPosInRange;
         
        @Override
        public boolean hasNext() {
          return pos <= lastPosInRange;  // lastPos is inclusive
        }
        
        @Override
        public TOP next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }

          TOP r = a[pos++];
          incrToNext();
          return r;        
        }
        
        private void incrToNext() {
          while (pos <= lastPosInRange) {
            if (a[pos] != null) {
              break;
            }
            pos ++;
          }
        }
      };
    }

    @Override
    public NavigableSet<TOP> descendingSet() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<TOP> descendingIterator() {
      if (firstElementInRange == null) {
        return Collections.emptyIterator();
      }
      return new Iterator<TOP>() {
        private int pos = lastPosInRange;
         
        @Override
        public boolean hasNext() {
          return pos >= firstPosInRange;  
        }
        
        @Override
        public TOP next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }

          TOP r = a[pos--];
          decrToNext();
          return r;        
        }
        
        private void decrToNext() {
          while (pos >= firstPosInRange) {
            if (a[pos] != null) {
              break;
            }
            pos --;
          }
        }
      };
    }

    @Override
    public NavigableSet<TOP> subSet(TOP fromElement1, boolean fromInclusive1, TOP toElement1,
        boolean toInclusive1) {
      if (!isInRange(fromElement1) || !isInRange(toElement1)) {
        throw new IllegalArgumentException();
      }
      return OrderedFsSet_array.this.subSet(fromElement1, fromInclusive1, toElement1, toInclusive1);  
    }

    @Override
    public NavigableSet<TOP> headSet(TOP toElement1, boolean inclusive) {
      return subSet(fromElement, fromInclusive, toElement1, inclusive);
    }

    @Override
    public NavigableSet<TOP> tailSet(TOP fromElement1, boolean inclusive) {
      return subSet(fromElement1, inclusive, toElement, toInclusive);
    }

    @Override
    public SortedSet<TOP> subSet(TOP fromElement1, TOP toElement1) {
      return subSet(fromElement1, true, toElement1, false);
    }

    @Override
    public SortedSet<TOP> headSet(TOP toElement1) {
      return headSet(toElement1, true);
    }

    @Override
    public SortedSet<TOP> tailSet(TOP fromElement1) {
      return tailSet(fromElement1, false);
    }
  
    private boolean isGtLast(TOP fs) {
      return comparatorWithID.compare(fs, lastElementInRange) > 0;      
    }
    
    private boolean isGeLast(TOP fs) {
      return comparatorWithID.compare(fs,  lastElementInRange) >= 0;
    }

    private boolean isLtFirst(TOP fs) {
      return comparatorWithID.compare(fs, firstElementInRange) < 0;
    }

    private boolean isLeFirst(TOP fs) {
      return comparatorWithID.compare(fs, firstElementInRange) <= 0;
    }
    
    private boolean isInRange(TOP fs) {
      return isInRangeLower(fs) && isInRangeHigher(fs);
    }
      
    private boolean isInRangeLower(TOP fs) {
      if (firstElementInRange == null) {
        return false;
      }
      int r = comparatorWithID.compare(fs, firstElementInRange);
      return fromInclusive ? (r >= 0) : (r > 0);
    }
    
    private boolean isInRangeHigher(TOP fs) {
      if (lastElementInRange == null) {
        return false;
      }
      int r = comparatorWithID.compare(fs, lastElementInRange);
      return toInclusive ? (r <= 0) : (r < 0);
    }
  }

  public int getModificationCount() {
    return modificationCount;
  }
  
  @Override
  public String toString() {
    processBatch();
    StringBuilder b = new StringBuilder();
    b.append("OrderedFsSet_array [a=");
    if (a != null) {
      boolean ft = true;
      for (TOP i : a) {
        if (ft) {
          ft = false;
        } else {
          b.append(",\n");
        }
        if (i != null) {
          b.append(i.toShortString());
        } else {
          b.append("null");
        }
  //      prettyPrint(0, 2, b, true); 
      }
    } else {
      b.append("null");
    }
    b   .append(", a_nextFreeslot=").append(a_nextFreeslot)
        .append(", a_firstUsedslot=").append(a_firstUsedslot)
        .append(", batch=").append(batch)
        .append(", origComparator=").append(comparatorWithID)
        .append(", size=").append(size)
        .append(", maxSize=").append(maxSize)
        .append(", highest=").append(highest)
        .append(", nullBlockStart=").append(nullBlockStart)
        .append(", nullBlockEnd=").append(nullBlockEnd).append("]");
    return b.toString();
  } 
 
  // these are approximate - don't take into account multi-thread access
  static private int addToEndCount = 0;
  static private int addNotToEndCount = 0;
  static private int batchCountHistogram[];
  static private int batchAddCount = 0; 
  static private int batchAddTotal = 0; // includes things not added because of dups
  static private int moveSizeHistogram[];
  static private int movePctHistogram[];
  static private int fillHistogram[];
  static private int iterPctEmptySkip[];
  
  static {
    if (MEASURE) {
      batchCountHistogram = new int[24];  // slot x = 2^x to (2^(x+1) - 1) counts
                                          // slot 0 = 1, slot 1 = 2-3, etc
      Arrays.fill(batchCountHistogram,  0);
      
      moveSizeHistogram = new int[24];
      movePctHistogram = new int[10];  // slot 0 = 0-9%  1 = 10-19% 9 = 90 - 100%
      fillHistogram = new int[24];
      
      iterPctEmptySkip = new int[10];
          
      Runtime.getRuntime().addShutdownHook(new Thread(null, () -> {
        System.out.println("Histogram measures of Ordered Set add / remove operations");
        System.out.format(" - Add to end: %,d,  batch add count: %,d  batch add tot: %,d%n", 
            addToEndCount, batchAddCount, batchAddTotal);
        for (int i = 0; i < batchCountHistogram.length; i++) {
          int v = batchCountHistogram[i];
          if (v == 0) continue;
          System.out.format(" batch size: %,d, count: %,d%n", 1 << i, v);
        }
        for (int i = 0; i < moveSizeHistogram.length; i++) {
          int v = moveSizeHistogram[i];
          if (v == 0) continue;
          System.out.format(" move size: %,d, count: %,d%n", 
              (i == 0) ? 0 : 1 << (i - 1), v);
        }
        for (int i = 0; i < movePctHistogram.length; i++) {
          int v = movePctHistogram[i];
          if (v == 0) continue;
          System.out.format(" move Pct: %,d - %,d, count: %,d%n", i*10, (i+1)*10, v);
        }
        for (int i = 0; i < fillHistogram.length; i++) {
          int v = fillHistogram[i];
          if (v == 0) continue;
          System.out.format(" fill size: %,d, count: %,d%n", 
              (i == 0) ? 0 : 1 << (i - 1), v);
        }
        for (int i = 0; i < iterPctEmptySkip.length; i++) {
          int v = iterPctEmptySkip[i];
          if (v == 0) continue;
          System.out.format(" iterator percent empty needing skip: %,d - %,d, count: %,d%n", i*10, (i+1)*10, v);
        }


      }, "dump measures OrderedFsSetSorted"));
    }

  }
}
