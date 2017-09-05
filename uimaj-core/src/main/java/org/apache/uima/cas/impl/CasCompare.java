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
package org.apache.uima.cas.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.Pair;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;

/**
 * Used by tests for Binary Compressed de/serialization code.
 * Used by test app: XmiCompare.
 * 
 * Compare 2 CASes, with perhaps different type systems.
 * If the type systems are different, construct a type mapper and use that
 *   to selectively ignore types or features not in other type system
 *   
 * The Mapper is from CAS1 -&gt; CAS2  
 * 
 * When computing the things to compare from CAS1, filter to remove
 * feature structures not reachable via indexes or refs
 *   
 * The index definitions are not compared.
 * The indexes are used to locate the FSs to be compared.
 *   
 * Reports are produced to System.out and System.err as a side effect
 *   System.out: status messages, type system comparison
 *   System.err: mismatch comparison information
 *   
 * Usage:  
 *   Use the static compareCASes method for default comparisons
 *   Use the multi-step approach for more complex comparisons:
 *     - Make an instance of this class, passing in the two CASes.
 *     - Set any additional configuration (e.g., cc.compareAll(true); )
 *     - Do any transformations needed on the CASes to account for known but allowed differences:
 *         -- example: for certain type:feature string values, normalize to the same canonical value
 *         -- example: for certain type:feature string arrays, where the order is not important, sort them
 *         -- example: for certain type:feature FSArrays, where the order is not important, sort them
 *            --- using the sortFSArray method
 *     - Do any configuration to specify congruence sets for String values
 *        -- these are specific to type / feature specs
 *        -- range can be string or string array - if string array, the spec includes the index or -1
 *           to indicate all indexes
 *              
 * How it works
 *   Prepare arrays of all the FSs in the CAS
 *     - for each of 2 CASes to be compared
 *     - 2 arrays: 
 *        -- all FSs in any index in any view
 *        -- the above, plus all FSs reachable via references
 *   
 *   The comparison of FSs is done, one FS at a time.  
 *     - in order to determine the right FSs to compare with each other, the FSs for each CAS
 *       are sorted.
 *   
 *   The sort and the CAS compare both use a Compare method.
 *     - sorting skips items not in the other type system, including features
 *     -   (only possible if comparing two CASes with different type systems, of course)
 *     
 *   Compare
 *     - used for two purposes:
 *       a) sorting FSs belonging to one CAS
 *          - can be used by caller to pre-sort any array values where the 
 *            compare should be for set equality (in other words, ignore the order)
 *       b) comparing a FS in one CAS with a FS in the other CAS
 *         
 *     sort keys, in order:
 *       1) type
 *       2) if primitive array: sort based on 
 *           - size
 *           - iterating thru all array items
 *       3) All the features, considered in an order where non-refs are sorted before refs. 
 *       
 *     comparing values:
 *       primitives - value comparison
 *       refs - compare the ref'd FS, while recording reference paths
 *            - stop when reach a compare point where the pair being compaired has been seen
 *            - stop at any point if the two FSs compare unequal
 *            - at the stop point, if compare is equal, check the reference paths, and 
 *                report unequal reference paths (different cycle lengths, or different total lengths,
 *                see the Prev data structure)
 *                   
 *     Context information, reused across compares:
 *       prevCompare - if a particular pair of FSs compared equal
 *                      -- used to speed up comparison
 *                      -- used to stop recursive loops of references
 *                       
 *       prev1, prev2 - reset for each top level FS compare
 *                    - not reset for inner FS compares of fs-reference values)
 *                      holds information about the reference path for chains of FS references           
 *     
 */

public class CasCompare {

  /**
   * Compare 2 CASes, with perhaps different type systems.
   *   - using default configuration.
   *   
   * @param c1 CAS to compare
   * @param c2 CAS to compare
   * @return true if equal (for types / features in both)
   */
  
  public static boolean compareCASes(CASImpl c1, CASImpl c2) {
    return new CasCompare(c1, c2).compareCASes();
  }
  
  /**
   * hold info about previous compares, to break cycles in references
   * The comparison records cycles and can distinguish different cyclic graphs.
   * When a cycle exists, it looks like:
   *    a b c d e f g h i     a cycle starting wFBitith a, with refs ending up at i
   *              ^     v     and then looping back to f
   *              *-----*
   *              
   * This data structure measures both the cycle Length (in this example, 4)
   * and the size of the starting part before hitting the loop (in this case 5)  
   * 
   * Note: when using, if two FSs end up comparing equal, the instances must be
   *       rolled back 1 item to allow further items to be compared in the chain.
   *       Example:  a -> b -> c -> d
   *         d's compared equal, c may have ref next to "e".
   */
  private static class Prev {

    /** ordered list of traversed FSs, including duplicates */
    private final ArrayList<TOP> fsList = new ArrayList<>();

    /** length of the cycle, excluding any leading ref chain
     *  -1 until some cycle is detected */
    private int cycleLen = -1;
    
    /** length of the leading ref chain, excludes any cycle part 
     *  -1 until some cycle is detected */
    private int cycleStart = -1;
    
    /** ref to the top of the chain; used as a boolean flag */
    TOP prevCompareTop; 
    
    void clear() {
      fsList.clear();
      cycleLen = -1;
      cycleStart = -1;
      prevCompareTop = null;
    }
    
    int compareCycleLen(Prev other) {
      return Integer.compare(cycleLen,  other.cycleLen);
    }
    
    int compareUsize(Prev other) {
      return Integer.compare(usize(), other.usize());
    }
    
    /**
     * called when returning from compare with equal result
     * If a loop exists, and the item being removed is the one that started the loopback,
     *   reset the loop info.
     * @param fs
     */
    void rmvLast(TOP fs) {
      int toBeRemoved = fsList.size() - 1;
      if (toBeRemoved == usize()) { // means removing the 1st item that looped back
        //  0 1 2 3 4 5 6
        //  a b c d e f g (c d e f g c d)  fsList
        //      ^              loop, cycleStart = 2 , cycleLength = 5
        
        //debug
        if (cycleLen < 0) { // never true, because usize() >= 0  ==> there's a cycle
          System.out.println("debug cycleLen");
          throw Misc.internalError();
        }
        assert cycleLen >= 0; 
        assert cycleStart >= 0;

        cycleLen = -1;   // since the loop is no more, reset the loop info
        cycleStart = -1;
      }
      fsList.remove(toBeRemoved);     
    }
    
    void addTop() {
      fsList.add(prevCompareTop);
      prevCompareTop = null;
    }
    
    void add(TOP fs) {
      if (cycleLen < 0) {
        int i = fsList.lastIndexOf(fs);
        if (i >= 0) { // first instance of a cycle detected
          cycleLen = fsList.size() - i;
          //  0 1 2 3 4 5 6
          //  a b c d e f g (c)
          //      ^           i == 2 == length of start segment
          cycleStart = i;  // cycleStart is length up to but not including the start of the cycle
        }
      }
      fsList.add(fs);
    }
   
    /** return size of ref chain including duplicates due to ref loops */
    int size() {
      return fsList.size();
    }
    
    /** return -2 or the length of the cycle including 1 loop */
    int usize() {
      return cycleStart + cycleLen;
    }
  }
  
  /** key for StringCongruenceSet */
  private static class ScsKey {

    final TypeImpl type;
    final FeatureImpl feature;
    final int index;
    
    ScsKey(TypeImpl type, FeatureImpl feature, int index) {
      this.type = type;
      this.feature = feature;
      this.index = index;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((feature == null) ? 0 : feature.hashCode());
      result = prime * result + index;
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ScsKey other = (ScsKey) obj;
      if (feature == null) {
        if (other.feature != null)
          return false;
      } else if (!feature.equals(other.feature))
        return false;
      if (index != other.index)
        return false;
      if (type == null) {
        if (other.type != null)
          return false;
      } else if (!type.equals(other.type))
        return false;
      return true;
    }    
  }

  private static class FeatLists {
    /**
     * first index is easy, easyArrays, refs, refArrays
     */
    final FeatureImpl[][] featsByEase = new FeatureImpl[4][];
    
    FeatLists(List<FeatureImpl> easy, 
              List<FeatureImpl> easyArrays, 
              List<FeatureImpl> refs, 
              List<FeatureImpl> refArrays) {
      featsByEase[0] = (FeatureImpl[]) easy.toArray(new FeatureImpl[easy.size()]);
      featsByEase[1] = (FeatureImpl[]) easyArrays.toArray(new FeatureImpl[easyArrays.size()]);
      featsByEase[2] = (FeatureImpl[]) refs.toArray(new FeatureImpl[refs.size()]);
      featsByEase[3] = (FeatureImpl[]) refArrays.toArray(new FeatureImpl[refArrays.size()]);
    }
  }
  
  final private Map<TypeImpl, FeatLists> type2featLists = new HashMap<>();
  
  final private CASImpl c1;
  final private CASImpl c2;
  final private TypeSystemImpl ts1;      
  final private TypeSystemImpl ts2;
    
//    private boolean compareStringArraysAsSets = false;
//    private boolean compareArraysByElement = false;
    /** if true, continues comparison and reporting after finding the first miscompare */
  private boolean compareAll = false;
//    private boolean compareFSArraysAsSets = false;
    
//    /** true when that FS._id (an Array of some kind) has been sorted */
//    final private BitSet alreadySorted1 = new BitSet();
//    final private BitSet alreadySorted2 = new BitSet();
    
    /**
     * This is used 
     *   - to speed up the compare - avoid comparing the same things multiple times, instead just use previous result
     *   - when doing the comparison to break recursion if asked to compare the same two things while comaring them.
     *   
     *   value is the result of previous comparison.
     */
  private final Map<Pair<TOP, TOP>, Integer> prevCompare = new HashMap<>();
  private final Prev prev1 = new Prev();
  private final Prev prev2 = new Prev();
  
//    private final Set<Pair<TOP, TOP>> miscompares = Collections.newSetFromMap(new HashMap<>());
        
//  private TOP fs1, fs2;
  private boolean isSrcCas;  // used for sorting with a CAS, to differentiate between src and target CASes
  final private StringBuilder mismatchSb = new StringBuilder();
  private boolean inSortContext = false;
  
  private boolean isTypeMapping;
  private final CasTypeSystemMapper typeMapper;
  
  private List<TOP> c1FoundFSs;
  private List<TOP> c2FoundFSs;
  
  private final Map<ScsKey, String[]> stringCongruenceSets = new HashMap<>();
  private boolean isUsingStringCongruenceSets = false;
  private final TypeImpl fsArrayType;
    

          
  public CasCompare(CASImpl c1, CASImpl c2) {
    this.c1 = c1;
    this.c2 = c2;
    ts1 = c1.getTypeSystemImpl();
    ts2 = c2.getTypeSystemImpl();
    typeMapper = ts1.getTypeSystemMapper(ts2);
    isTypeMapping = (null != typeMapper);
    fsArrayType = ts1.fsArrayType;
  }
    
//  public void compareStringArraysAsSets(boolean v) {
//    compareStringArraysAsSets = v;
//  }
//  
//  public void compareFSArraysAsSets(boolean v) {
//    compareFSArraysAsSets = v;
//  }
//  
//  public void compareArraysByElement(boolean v) {
//    compareArraysByElement = v;
//  }
  
  /**
   * Continues the comparison after a miscompare (or not)
   * @param v true to continue the comparison after a miscompare
   */
  public void compareAll(boolean v) {
    compareAll = v;
  }
  
  
  /**
   * Add a set of strings that should be considered equal when doing string comparisons.
   * This is conditioned on the typename and feature name
   * @param t the fully qualified type name
   * @param f the feature short name
   * @param set a set of strings that should compare equal, if testing the type / feature
   * @param index if the item being compared is a reference to a string array, which index should be compared.
   *              Use -1 if not applicable.
   */
  public void addStringCongruenceSet(String t, String f, String[] set, int index) {
    TypeImpl t1 = ts1.getType(t);
    stringCongruenceSets.put(
        new ScsKey(t1, t1.getFeatureByBaseName(f), index),
        set);
    isUsingStringCongruenceSets = true;
  }
  
  /**
   * This does the actual comparison operation of the previously specified CASes
   * @return true if compare is OK
   */
  public boolean compareCASes() {
    boolean allOk = true;
//    final List<TOP> c1FoundFSs;
//    final List<TOP> c2FoundFSs;
    final boolean savedIsTypeMapping= isTypeMapping;
    mismatchSb.setLength(0);

    try {
      
//        processIndexedFeatureStructures(c1, false);
      Predicate<TOP> includeFilter = isTypeMapping ? (fs -> isTypeInTgt(fs)) : null;
      // get just the indexed ones
      c1FoundFSs = new AllFSs(c1, null, includeFilter, isTypeMapping ? typeMapper : null)
                        .getAllFSsAllViews_sofas()
                        .getAllFSs();
      
//        c1FoundFSs = fssToSerialize;  // all reachable FSs, filtered by CAS1 -> CAS2 type systems.
      
//        processIndexedFeatureStructures(c2, false);
      c2FoundFSs = new AllFSs(c2, null, null, null)
                     .getAllFSsAllViews_sofas()
                     .getAllFSs(); // get just the indexed ones.
      

      // if type systems are "isEqual()" still need to map because of feature validation testing
          
      int i1 = 0;
      int i2 = 0;
      final int sz1 = c1FoundFSs.size();
      final int sz2 = c2FoundFSs.size();
 
      isSrcCas = true;   // avoids sorting on types/features not present in ts2
      sort(c1FoundFSs);
      
      isSrcCas = false;  // avoids sorting on types/features not present in ts1
      sort(c2FoundFSs);
     
//      miscompares.clear();
      
      while (i1 < sz1 && i2 < sz2) {
        TOP fs1 = c1FoundFSs.get(i1);  // assumes the elements are in same order??
        TOP fs2 = c2FoundFSs.get(i2);

        clearPrevFss();
        prev1.prevCompareTop = fs1;
        prev2.prevCompareTop = fs2;

        if (isTypeMapping) {
          // skip compares for types that are missing in the other type system
          final boolean typeMissingIn1 = typeMapper.mapTypeTgt2Src(fs2._getTypeImpl()) == null;
          final boolean typeMissingIn2 = typeMapper.mapTypeSrc2Tgt(fs1._getTypeImpl()) == null;
          if (!typeMissingIn1 && !typeMissingIn2) {
            if (0 != compareFss(fs1, fs2, null, null)) {
              mismatchFsDisplay();
              if (!compareAll) return false;
              allOk = false;
              int tc = fs1._getTypeImpl().compareTo(fs2._getTypeImpl());
              if (tc < 0) {
                System.out.print("skiping first to align types ");
                while (tc < 0 && i1 < sz1) {
                  i1++;
                  tc = c1FoundFSs.get(i1)._getTypeImpl().compareTo(fs2._getTypeImpl());
                  System.out.print(".");
                }
                System.out.println("");
              } else if (tc > 0) {
                System.out.print("skiping second to align types ");
                while (tc > 0 && i2 < sz2) {
                  i2++;
                  tc = fs1._getTypeImpl().compareTo(c2FoundFSs.get(i2)._getTypeImpl());
                  System.out.print(".");
                }
                System.out.println("");
              }
            }
            i1++;
            i2++;
            continue;
          }
          if (typeMissingIn1 && typeMissingIn2) {
            Misc.internalError();
            i1++;
            i2++;
            continue;
          }
          if (typeMissingIn1) {
            System.out.println("debug - type missing in 1, but test fails for refs");
            i2++;
            continue;
          }
          if (typeMissingIn2) {
            Misc.internalError(); 
            i1++;
            continue;
          }
        } else {  // not type mapping
          if (0 != compareFss(fs1, fs2, null, null)) {
            
            mismatchFsDisplay();
            if (!compareAll) return false;
            allOk = false;
            int tc = fs1._getTypeImpl().compareTo(fs2._getTypeImpl());
            if (tc < 0) {
              System.out.print("skiping first to align types ");
              while (tc < 0 && i1 < sz1) {
                i1++;
                tc = c1FoundFSs.get(i1)._getTypeImpl().compareTo(fs2._getTypeImpl());
                System.out.print(".");
              }
              System.out.println("");
            } else if (tc > 0) {
              System.out.print("skiping second to align types ");
              while (tc > 0 && i2 < sz2) {
                i2++;
                tc = fs1._getTypeImpl().compareTo(c2FoundFSs.get(i2)._getTypeImpl());
                System.out.print(".");
              }
              System.out.println("");
            } 
            //debug
            else {
              // debug - realign for case where c1 has 1 extra fs
              if (i1 + 1 < sz1) {
                fs1 = c1FoundFSs.get(i1 + 1);
                clearPrevFss();
                prev1.prevCompareTop = fs1;
                prev2.prevCompareTop = fs2;
                if (0 == compareFss(fs1, fs2, null, null)) {
                  // realign
                  System.out.println("Skipping 1 to realign within same type " + fs1._getTypeImpl().getName());
                  i1++;
                }
              }
            }
          }
          i1++;
          i2++;
          continue;
        }
      }
      
      if (i1 == sz1 && i2 == sz2) {
        return allOk;  // end
      }
      
      if (isTypeMapping) {
        if (i1 < sz1) {
          System.err.format("%,d Feature Structures in CAS1 with no matches in CAS2, e.g. %s%n",
              sz1 - i1, c1FoundFSs.get(i1));
          return false;
        }

        while (i2 < sz2) {
          TOP fs = c2FoundFSs.get(i2);
          if (isTypeMapping && typeMapper.mapTypeTgt2Src(fs._getTypeImpl()) != null) {  // not a complete test, misses refs
            return false;  // have more FSs in c2 than in c1
          }
          i2++;
        }
        return true;
      }
      
      // not type mapping, and number of FS didn't match
      if (i1 < sz1) {
        System.err.format("CAS1 had %,d additional Feature Structures, e.g.: %s%n", sz1 - i1, c1FoundFSs.get(i1));
      } else {
        System.err.format("CAS2 had %,d additional Feature Structures, e.g.: %s%n", sz2 - i2, c2FoundFSs.get(i2));
      }
      return false;
    } finally {
      isTypeMapping = savedIsTypeMapping;
      clearPrevFss();
    }
  }
  
  /**
   * Sort an fsArray.  During the sort, links are followed.
   * The sorting is done in a clone of the array, and the original array is not updated.
   * Instead, a Runnable is returned, which may be invoked later to update the original array with the sorted copy.
   * This allows sorting to be done on the original item values.
   * @param fsArray the array to be sorted
   * @return a runnable, which (when invoked) updates the original array with the sorted result.
   */
  public Runnable sortFSArray(FSArray fsArray) {
    if (fsArray == null || fsArray.size() < 2) {
      return null;
    }
    TOP[] a = fsArray._getTheArray().clone();
    clearPrevFss();
    inSortContext = true;
    Arrays.sort(a, (TOP afs1, TOP afs2) -> {
      return compareRefs(afs1, afs2, null, null);
    });
    return () -> System.arraycopy(a, 0, fsArray._getTheArray(), 0, fsArray.size());
  }
  
  private void clearPrevFss() {
    prevCompare.clear();
    prev1.clear();
    prev2.clear();
  }
  
  /**
   * To work with Java sort, must implement the comparator contract:
   *   - compare(x, y) must be opposite compare(y, x)
   *   - compare(x, y) < 0 && compare(y, z) < 0 implies compare(x, z) < 0
   *   - compare(x, y) == 0 implies compare(x, z) same as compare(y, z) for any z
   * 
   * Inner part of compareRefs; that other method adds: 
   *   null-check
   *   type-mapping skips
   *   loop determination 
   *   
   * If not in a sort context, a miscompare generates messaging information.
   *   
   * @param callerTi - the type of another FS referencing this one, or null
   * @param callerFi - the feature of the another FS referencing this one, or null
   * 
   * @return the compare result
   * 
   */
  private int compareFss(TOP fs1, TOP fs2, TypeImpl callerTi, FeatureImpl callerFi) {
   
    if (fs1 == fs2) {
      return 0;
    }
    
    TypeImpl ti1 = fs1._getTypeImpl();
    TypeImpl ti2 = fs2._getTypeImpl();  // even if not type mapping, may be "equal" but not ==
    int r = 0;
    
    if (!inSortContext && isTypeMapping) {
      ti2 = typeMapper.mapTypeTgt2Src(ti2);
    }

    r = ti1.compareTo(ti2);
    if (r != 0) {
      if (!inSortContext) mismatchFs(fs1, fs2, "Different Types"); // types mismatch
      return r;
    }

//      if (!ti1.getName().equals(ti2.getName())) {
//          return mismatchFs("Type names miscompare"); // types mismatch
//        }
//      }
        
    if (ti1.isArray()) {
      return compareFssArray(fs1, fs2, callerTi, callerFi);
    } 

    FeatLists featLists = type2featLists.get(ti1);
    if (featLists == null) {
      type2featLists.put(ti1, featLists = computeFeatLists(ti1));
    }

    // compare features, non-refs first (for performance)
    for (FeatureImpl[] featSet : featLists.featsByEase) {
      for (FeatureImpl fi1 : featSet) {
        if (0 != (r = compareFeature(fs1, fs2, ti1, fi1))) {
          return r;
        }
      } 
    }
    return 0;
  }
     
  private int compareFeature(TOP fs1, TOP fs2, TypeImpl ti1, FeatureImpl fi1) {
    int r = 0;
    if (inSortContext && isTypeMapping) {
      if (isSrcCas && typeMapper.getTgtFeature(ti1, fi1) == null) {
        return 0; // skip tests for features not in target type system
                  // so when comparing CASs, the src value won't cause a miscompare
      }
      if (!isSrcCas && typeMapper.getSrcFeature(ti1,  fi1) == null) {
        return 0; // types/features belong to target in this case
      }
    }
    FeatureImpl fi2 = (!inSortContext && isTypeMapping) ? typeMapper.getTgtFeature(ti1, fi1) : fi1;
    if (fi2 != null) {
      r = compareSlot(fs1, fs2, fi1, fi2, ti1);
      if (0 != r) {
        if (!inSortContext) {
//          // debug
//          compareSlot(fs1, fs2, fi1, fi2, ti1);
          mismatchFs(fs1, fs2, fi1, fi2);
        }
        return r;
      }
    } // else we skip the compare - no slot in tgt for src
    return r;
  }
  
  /**
   * called during sort phase
   * @param ti - type being sorted
   * @return the feature lists for that type
   */
  private FeatLists computeFeatLists(TypeImpl ti) {
    List<FeatureImpl> easy = new ArrayList<>();  
    List<FeatureImpl> easyArrays = new ArrayList<>();  
    List<FeatureImpl> ref = new ArrayList<>();  
    List<FeatureImpl> refArrays = new ArrayList<>();
    
    for (FeatureImpl fi : ti.getFeatureImpls()) {
      
      if (isTypeMapping) {

        if (isSrcCas && typeMapper.getTgtFeature(ti, fi) == null) {
          continue; // skip for features not in target type system
                    // so when comparing CASs, the src value won't cause a miscompare
        }
        
        // probably not executed, since types discovered on first sort
        // except for a type that exists only the the target
        if (!isSrcCas && typeMapper.getSrcFeature(ti,  fi) == null) {
          continue; // types/features belong to target in this case
        }
        
      }
            
      TypeImpl range = fi.getRangeImpl();
      if (range.isArray()) {
        TypeImpl_array ra = (TypeImpl_array)range;
        if (ra.getComponentType().isRefType) {
          refArrays.add(fi);
        } else {
          easyArrays.add(fi);
        }
      } else {
        if (range.isRefType) {
          ref.add(fi);
        } else {
          easy.add(fi);
        }
      }
    }
    return new FeatLists(easy, easyArrays, ref, refArrays);
  }
  
  
//    private int compareFssArray() {
//      int r = compareFssArray((CommonArrayFS) fs1, (CommonArrayFS) fs2);
//      if (r != 0) {
//        if (!inSortContext) mismatchFs();
//      }
//      return r;
//    }
  
  private int compareFssArray(TOP fs1, TOP fs2, TypeImpl callerTi, FeatureImpl callerFi) {
    CommonArrayFS a1 = (CommonArrayFS) fs1;
    CommonArrayFS a2 = (CommonArrayFS) fs2;
    int r;
    
    int len1 = a1.size();
    int len2 = a2.size();
    r = Integer.compare(len1,  len2);
    if (r != 0) {
      if (!inSortContext) {
        mismatchFs(fs1, fs2);
      }
      return r;
    }
    
//    if (inSortContext && !compareArraysByElement) {
//      // quick approximate comparison of arrays, for sort purposes
//      return Integer.compare(((FeatureStructureImplC)fs1)._id, 
//                             ((FeatureStructureImplC)fs2)._id);
//    }
    TypeImpl ti = ((FeatureStructureImplC)a1)._getTypeImpl();
    SlotKind kind = ti.getComponentSlotKind();
    
    switch(kind) {
    case Slot_BooleanRef: r = compareAllArrayElements(fs1, fs2, len1, i -> Boolean.compare(
                                                                    ((BooleanArray)a1).get(i),
                                                                    ((BooleanArray)a2).get(i)));
      break;
    case Slot_ByteRef:    r = compareAllArrayElements(fs1, fs2, len1, i -> Byte.compare(
                                                                    ((ByteArray   )a1).get(i),
                                                                    ((ByteArray   )a2).get(i)));
      break;
    case Slot_ShortRef:   r = compareAllArrayElements(fs1, fs2, len1, i -> Short.compare(
                                                                    ((ShortArray  )a1).get(i),
                                                                    ((ShortArray  )a2).get(i)));
      break;
    case Slot_Int:     r = compareAllArrayElements(fs1, fs2, len1, i -> Integer.compare(
                                                                    ((IntegerArray)a1).get(i),
                                                                    ((IntegerArray)a2).get(i)));
      break;
    case Slot_LongRef:  r = compareAllArrayElements(fs1, fs2, len1, i -> Long.compare(
                                                                    ((LongArray   )a1).get(i),
                                                                    ((LongArray   )a2).get(i)));
      break;

    // don't compare floats / doubles directly - because two "equal" NaN are defined to miscompare
    case Slot_Float: r = compareAllArrayElements(fs1, fs2, len1, i -> Integer.compare(
                                                                    CASImpl.float2int(((FloatArray  )a1).get(i)), 
                                                                    CASImpl.float2int(((FloatArray  )a2).get(i))));
      break;

    // don't compare floats / doubles directly - because two "equal" NaN are defined to miscompare
    case Slot_DoubleRef: r = compareAllArrayElements(fs1, fs2, len1, i -> Long.compare(
                                                                    CASImpl.double2long(((DoubleArray)a1).get(i)), 
                                                                    CASImpl.double2long(((DoubleArray)a2).get(i))));
      break;
    case Slot_HeapRef: r = compareAllArrayElements(fs1, fs2, len1, i -> compareRefs( ((FSArray) a1).get(i), 
                                                                           ((FSArray) a2).get(i), 
                                                                           callerTi, 
                                                                           callerFi));
      break;
    case Slot_StrRef: r = compareAllArrayElements(fs1, fs2, len1, i -> compareStringsWithNull(
                                                                    ((StringArray)a1).get(i), 
                                                                    ((StringArray)a2).get(i), 
                                                                    callerTi, 
                                                                    callerFi, 
                                                                    i));
      break;
    default: 
      throw Misc.internalError(); 
    }
    
    return r;
  }
          
  private int compareSlot(TOP fs1, TOP fs2, FeatureImpl fi1, FeatureImpl fi2, TypeImpl ti1) {
    SlotKind kind = fi1.getSlotKind();
    switch (kind) {
    case Slot_Int: return Integer.compare(fs1._getIntValueNc(fi1), fs2._getIntValueNc(fi2)); 
    case Slot_Short: return Short.compare(fs1._getShortValueNc(fi1), fs2._getShortValueNc(fi2));
    case Slot_Boolean: return Boolean.compare(fs1._getBooleanValueNc(fi1), fs2._getBooleanValueNc(fi2));
    case Slot_Byte: return Byte.compare(fs1._getByteValueNc(fi1), fs2._getByteValueNc(fi2));
          // don't compare floats / doubles directly - the NaN is defined to miscompare
    case Slot_Float: return Integer.compare(CASImpl.float2int(fs1._getFloatValueNc(fi1)), CASImpl.float2int(fs2._getFloatValueNc(fi2)));
    case Slot_HeapRef: return compareRefs(fs1._getFeatureValueNc(fi1), fs2._getFeatureValueNc(fi2), ti1, fi1);
    case Slot_StrRef: return compareStringsWithNull(fs1._getStringValueNc(fi1), fs2._getStringValueNc(fi2), ti1, fi1, -1);
    case Slot_LongRef: return Long.compare(fs1._getLongValueNc(fi1), fs2._getLongValueNc(fi2));
          // don't compare floats / doubles directly - the NaN is defined to miscompare
    case Slot_DoubleRef: return Long.compare(Double.doubleToRawLongBits(fs1._getDoubleValueNc(fi1)), Double.doubleToRawLongBits(fs2._getDoubleValueNc(fi2)));
    default: Misc.internalError(); return 0;     
    }
  }
  
  /**
   * Two uses cases supported:
   *   - comparing for sorting (within on type system)
   *      -- goal is to be able to compare two CASes
   *         --- ordering must guarantee that the equal FSs appear in the
   *         --- same order
   *   - comparing two FSs (maybe from different CASes)
   *     -- supporting missing types and features 
   *        -- happens when the two type systems are different
   *        -- the missing types and features are ignored in the comparison
   * 
   * Different reference chains
   *   This compare routine may be called recursively
   *     - use case: FS(a) has slot which is ref to 
   *                   FS(b) which has slot which is ref to
   *                     FS(c) 
   *       -- the type of a, b, c may all be different.
   *   
   *   Two reference chains for the two arguments may have different structures
   *     - Difference in two ways:  
   *       -- length of unique (same fs_id) FSs
   *       -- length of loop (if one exists at the end reached so far)
   *       
   *   IMPORTANT: the following 2 chains have different lengths, but this
   *   won't be discovered if the recursive descent stops too soon:
   *     - a -> b -> c  ( -> b )
   *     - c -> b ( -> c)
   *   At the 2nd recursion, we have b vs b, but haven't explored the chain 
   *   deeply enough to know the first one has length 3, and the 2nd length 2.            
   *       
   * Meaning of comparision of two refs:  
   *   - recursively defined
   *   - captures notion of reference chain differences
   *     -- means if two refs compare 0, the result may still be
   *        non-0 if the reference chains to get to these are different
   *       -- first compare on length of unique FSs
   *       -- if ==, compare on length of loop
   *   - if comparing (use case 2, two different type systems) with 
   *     type not existing in other type system, skip (treat as 0).
   * 
   * Complexities: the type rfs1 may not be in the target type system.
   *   For this case - treat rfs2 == null as "equal", rfs2 != null as not equal (always gt)
   *   Is assymetrical (?) - same logic isn't applied for reverse case.
   * @param rfs1 -
   * @param rfs2 -
   * @param fi -
   * @return -
   */
  private int compareRefs(final TOP rfs1, final TOP rfs2, TypeImpl callerTi, FeatureImpl callerFi) {
    if (rfs1 == null) {
      if (rfs2 != null) {
        return (!inSortContext && isTypeMapping &&
                typeMapper.mapTypeTgt2Src(rfs2._getTypeImpl()) == null)
                  ? 0   // no source type for this target type, treat as equal
                  : -1;
      }
      return 0;  // both are null.  no loops in ref chain possible 
    }

    // rfs1 != null at this point
    
     if (rfs2 == null) {
      return (!inSortContext && isTypeMapping &&
              typeMapper.mapTypeSrc2Tgt(rfs1._getTypeImpl()) == null)
                ? 0 // no target type for this target type, treat as equal
                : 1;  
    }
     
    if (rfs1 == rfs2) {
      // only for inSortContext
      return 0;
    }
    
    // next commented out to enable finding length of chain      
//      if (inSortContext && rfs1._id == rfs2._id) {  
//        return compareRefResult(rfs1, rfs2);
//      }
    
    // both are not null
    // do a recursive check 
    Pair<TOP, TOP> refs = new Pair<TOP, TOP>(rfs1, rfs2);
    Integer prevComp = prevCompare.get(refs);
     if (prevComp != null) {  
       int v = prevComp.intValue();
       return (v == 0) 
               ? compareRefResult(rfs1, rfs2) // stop recursion, return based on loops
               : v;    
    }
    prevCompare.put(refs, 0); // preset in case recursion compares this again
    
    // need special handling to detect cycles lengths that are back to the original
    if (prev1.prevCompareTop != null) {
      prev1.addTop();
      prev2.addTop();
    }
    
    prev1.add(rfs1);
    prev2.add(rfs2);
    assert prev1.fsList.size() > 0;
//    TOP savedFs1 = fs1;
//    TOP savedFs2 = fs2;
    
//    fs1 = rfs1;
//    fs2 = rfs2;
    try {
      int v = compareFss(rfs1, rfs2, callerTi, callerFi);
      if (v != 0) {
        prevCompare.put(refs, v);
      }
      return v;
    } finally {
      prev1.rmvLast(rfs1);
      prev2.rmvLast(rfs2);
      
//      fs1 = savedFs1;
//      fs2 = savedFs2;   
    }
  }
  
  /**
   * Returning because recursion detected a loop.
   * 
   * @param rfs1 -
   * @param rfs2 -
   * @return - -1 if ref chain 1 length < 2 or is the same length but loop length 1 < 2
   *            1 if ref chain 1 length > 2 or is the same length but loop length 1 > 2
   *            0 if ref chain lengths are the same and loop length is the same
   */
  private int compareRefResult(TOP rfs1, TOP rfs2) {
    if (prev1.size() <= 0) { 
      return 0;  // no recursion case
    }
    
    // had some recursion
    prev1.add(rfs1);
    prev2.add(rfs2);
    
    try { // only for finally block
      // compare cycleLen first, because if !=, all ref pairs in above chain are !=
      //   but if ==, then all cycle pairs are compare ==.
      int r = prev1.compareCycleLen(prev2);
      
      if (r != 0) {
        return r;
      }
      
      if (prev1.cycleLen > 0) {  // && is equal to prev2 cycle length
        return 0;  // at this level, the FSs are equal
      }
      
      return prev1.compareUsize(prev2);
      
    } finally {
      prev1.rmvLast(rfs1);
      prev2.rmvLast(rfs2);
    }
  }
      
  private int compareAllArrayElements(TOP fs1, TOP fs2, int len, IntUnaryOperator c) {
    int r = 0;
    for (int i = 0; i < len; i++) {
      r = c.applyAsInt(i);
      if (r != 0) {
        if (!inSortContext) mismatchFs(fs1, fs2, "Comparing array of length " + len);
        return r;
      }
    }
    // need to return 0 if == in sort context, otherwise violates the 
    // comparator contract
    return 0;
  }
  
  private int compareStringsWithNull(String s1, String s2, TypeImpl t, FeatureImpl f, int index) {
    if (isUsingStringCongruenceSets) {
      String[] scs = stringCongruenceSets.get(new ScsKey(t, f, index));
      
      if (scs != null) {
        if (Misc.contains(scs, s1) &&
            Misc.contains(scs, s2)) {
          return 0;
        }
      }
    }
    
    if (null == s1) {
      return (null == s2) ? 0 : -1;
    }
    if (null == s2) {
      return 1;
    }
    return s1.compareTo(s2);
  }
  
//    private int skipOverTgtFSsNotInSrc(
//        int[] heap, int heapEnd, int nextFsIndex, CasTypeSystemMapper typeMapper) {
//      final TypeSystemImpl ts = typeMapper.tsTgt;
//      for (; nextFsIndex < heapEnd;) {
//        final int tCode = heap[nextFsIndex];
//        if (typeMapper.mapTypeCodeTgt2Src(tCode) != 0) { 
//          break;
//        }
//        nextFsIndex += incrToNextFs(heap, nextFsIndex, ts.getTypeInfo(tCode));
//      }
//      return nextFsIndex;
//    }
//    
//    public void initSrcTgtIdMapsAndStringsCompare () {
//
//      int iTgtHeap = isTypeMapping ? skipOverTgtFSsNotInSrc(c2heap, c2end, 1, typeMapper) : 1;
//      
//      
//      for (int iSrcHeap = 1; iSrcHeap < c1end;) {
//        final int tCode = c1heap[iSrcHeap];
//        final int tgtTypeCode = isTypeMapping ? typeMapper.mapTypeCodeSrc2Tgt(tCode) : tCode;
//        final boolean isIncludedType = (tgtTypeCode != 0);
//        
//        // record info for type
//        fsStartIndexes.addItemId(iSrcHeap, iTgtHeap, isIncludedType);  // maps src heap to tgt seq
//        
//        // for features in type - 
//        //    strings: accumulate those strings that are in the target, if optimizeStrings != null
//        //      strings either in array, or in individual values
//        //    byte (array), short (array), long/double (instance or array): record if entries in aux array are skipped
//        //      (not in the target).  Note the recording will be in a non-ordered manner (due to possible updates by
//        //       previous delta deserialization)
//        final TypeInfo srcTypeInfo = ts1.getTypeInfo(tCode);
//        final TypeInfo tgtTypeInfo = (isTypeMapping && isIncludedType) ? ts2.getTypeInfo(tgtTypeCode) : srcTypeInfo;
//              
//        // Advance to next Feature Structure, in both source and target heap frame of reference
//        if (isIncludedType) {
//          final int deltaTgtHeap = incrToNextFs(c1heap, iSrcHeap, tgtTypeInfo);
//          iTgtHeap += deltaTgtHeap;
//          if (isTypeMapping) {
//            iTgtHeap = skipOverTgtFSsNotInSrc(c2heap, c2end, iTgtHeap, typeMapper);
//          }
//        }
//        iSrcHeap += incrToNextFs(c1heap, iSrcHeap, srcTypeInfo);
//      }
//    } 

  private void mismatchFsDisplay() {
    String s = mismatchSb.toString();
    System.err.println(s);
    mismatchSb.setLength(0);
  }
  
  private void mismatchFs(TOP fs1, TOP fs2) {
    mismatchSb.append(String.format("Mismatched Feature Structures:%n %s%n %s%n", ps(fs1), ps(fs2)));
//    // debug
//    System.out.println("adding to miscompares: " + fs1._id + " " + fs2._id);
//    miscompares.add(new Pair<>(fs1, fs2));
  }

//    private boolean mismatchFs(int i1, int i2) {
//      System.err.format("Mismatched Feature Structures in srcSlot %d, tgtSlot %d%n %s%n %s%n", 
//          i1, i2, dumpHeapFs(c1, c1heapIndex, ts1), dumpHeapFs(c2, c2heapIndex, ts2));
//      return false;
//    }
  
//    private void mismatchFs(Feature fi) {
//      mismatchSb.append(String.format("Mismatched Feature Structures in feature %s%n %s%n %s%n", 
//          fi.getShortName(), fs1, fs2));
//    }
  
  private void mismatchFs(TOP fs1, TOP fs2, Feature fi, Feature fi2) {
    String mapmsg = fi.equals(fi2) 
                      ? ""
                      : "which mapped to target feature " + fi2.getShortName() + " ";
    mismatchSb.append(String.format("Mismatched Feature Structures in feature %s %s%n %s%n %s%n", 
        fi.getShortName(), mapmsg, ps(fs1), ps(fs2)));
  }
  
  private void mismatchFs(TOP fs1, TOP fs2, String msg) {
    mismatchSb.append(String.format("Mismatched Feature Structures, %s%n %s%n %s%n", 
        msg, ps(fs1), ps(fs2)));
  }
      
  /** called to sort all the FSs before doing the equality compares */
  private void sort(List<TOP> fss) {
    inSortContext = true;
    // do before sorting
    clearPrevFss();

    try {
      Collections.sort(fss,  
            (afs1, afs2) -> sortCompare(afs1, afs2));
//            (afs1, afs2) -> Integer.compare(afs1._id, afs2._id));
    } finally {
      inSortContext = false;
    }
  }
  
  /**
   * Used for sorting within one type system, for two instances of the same type
   * 
   * Uses field isSrcCas (boolean) to differentiate when being used to sort for srcCas vs tgtCas
   * 
   * When sorting where type mapping is happening between source and target CASs, skip compares for
   * features which are not in the opposite CAS.
   * 
   * @param scFs1 -
   * @param scFs2 -
   * @return -
   */
  private int sortCompare(TOP scFs1, TOP scFs2) {  
    
//    miscompares.clear();
    prev1.clear();
    prev2.clear();
    
    prev1.prevCompareTop = scFs1;
    prev2.prevCompareTop = scFs2;
    int r = compareFss(scFs1, scFs2, null, null);
    prev1.prevCompareTop = null;
    prev2.prevCompareTop = null;
    if (r == 0) {
      r = Integer.compare(scFs1._id, scFs2._id);
    }
    return r;
  }
      
  private boolean isTypeInTgt(TOP fs) {
    return !isTypeMapping || (null != typeMapper.mapTypeSrc2Tgt(fs._getTypeImpl()));
  }
  
  private String ps(TOP fs) { 
    StringBuilder sb = new StringBuilder();
    fs.prettyPrintShort(sb);
    return sb.toString();
  }
}
