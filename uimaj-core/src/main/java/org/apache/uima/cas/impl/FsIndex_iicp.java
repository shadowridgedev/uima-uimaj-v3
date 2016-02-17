package org.apache.uima.cas.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;

/**
 * FsIndex_iicp (iicp)
 * 
 * A pair of an leaf index and an iterator cache. An iterator cache is the set of all leaf-indexes necessary
 * to create an iterator for the type of the index.
 * 
 *  The cache includes the index for the type of this index, as well as all subtypes.
 *  
 * compareTo() is based on types and the comparator of the index.
 * 
 * T is the Java cover class of the top type (root) in the index set
 * 
 * Also includes a lazily initialized reference to a corresponding FSIndexFlat instance.
 * 
 * This class is package private to share with FSIndexFlat
 * For Internal Use
 */  
class FsIndex_iicp<T extends FeatureStructure> 
          implements Comparable<FsIndex_iicp<? extends FeatureStructure>>,
                     Comparator<FeatureStructure>,
                     LowLevelIndex<T> {

//  private final static boolean DEBUG = false;

  final FSIndexRepositoryImpl fsIndexRepositoryImpl;
  /**
   *  The "root" index, i.e., index of the type of the iterator.
   *  default visibility to make it accessible by FSIndexFlat
   */
  final FsIndex_singletype<T> fsIndex_singletype;
  
  /**
   * A list of indexes (the sub-indexes that we need for an iterator). 
   * I.e., one index for each type that's subsumed by the iterator's type; 
   * includes the iterator's type leaf index too.
   * 
   * This is set up lazily on first need, to avoid extra work when won't be accessed
   */
  ArrayList<FsIndex_singletype<FeatureStructure>> cachedSubFsLeafIndexes = null;
  
  // VOLATILE to permit double-checked locking technique
  private volatile boolean isIteratorCacheSetup = false;
    
  /**
   * The type codes corresponding to the cachedSubFsLeafIndexes, set up lazily
   */
  int[] sortedTypeCodes;

  FsIndex_iicp(FsIndex_singletype<T> fsIndex_singletype) {
    this.fsIndex_singletype = fsIndex_singletype;
    fsIndexRepositoryImpl = fsIndex_singletype.casImpl.indexRepository;
//      setAndTestMask = fsIndexRepositoryImpl.cas.getTypeSystemImpl().getSetAndTestMasks(fsIndex_singletype.getTypeCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()).append(", index=");
    sb.append(fsIndex_singletype).append('\n');
    if (!isIteratorCacheSetup) {
      sb.append(" cache not set up yet");
    } else {  
      int len = Math.min(3,  cachedSubFsLeafIndexes.size());
      for (int i = 0; i < len; i++) {
        FsIndex_singletype<FeatureStructure> lii = cachedSubFsLeafIndexes.get(i); 
        sb.append("  cache ").append(i++);
        sb.append("  ").append(lii).append('\n');
      }
      if (cachedSubFsLeafIndexes.size() > 3) {
        sb.append(" ... and " + (cachedSubFsLeafIndexes.size() - 3) + " more\n");
      }
    }
    return sb.toString();
  }
  
//    FSIterator<T> createFSIterator(boolean is_unordered) {
//      return fsIndexRepositoryImpl.createFSIterator(this, is_unordered);
//    }


  /**
   * Two iicps are equal if and only if:
   *   - the types they index are the same, and
   *   - the comparators are equal, and
   *   - the indexing stragtegy (bag/set/sorted) are the same
   * Used when creating the index iterator cache to select from the
   *   set of all instances of these the one that goes with the same index definition
   * Used by CasComplete serialization to merge multiple index names referring to the same index  
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof FsIndex_iicp)) {
      return false;
    }
    @SuppressWarnings("rawtypes")
    final FsIndex_iicp iicp = (FsIndex_iicp) o;
    return
        this.getIndexingStrategy() == iicp.getIndexingStrategy() &&
        this.fsIndex_singletype.getComparatorImplForIndexSpecs().equals(iicp.fsIndex_singletype.getComparatorImplForIndexSpecs()); 
  }

// Implement a hashCode; 
// previously tried just throwing an exception, but if the hashCode method throws, 
//   then the Eclipse debugger fails to show the object saying 
//   com.sun.jdi.InvocationException occurred invoking method. 
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
      // next hashCode includes the type
    result = prime * result + this.fsIndex_singletype.getComparatorImplForIndexSpecs().hashCode();  
    result = prime * result + this.getIndexingStrategy();
    return result;
  }

  
  // Populate the cache.
  // For read-only CASes, this may be called on multiple threads, so do some synchronization
      
  void createIndexIteratorCache() {
    // using double-checked sync - see http://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
    if (isIteratorCacheSetup) {
      return;
    }
    synchronized (this) {
      if (isIteratorCacheSetup) {
        return;
      }
      
      final TypeImpl rootType = (TypeImpl) this.fsIndex_singletype.getComparatorImplForIndexSpecs().getType();
      final int indexKind = this.getIndexingStrategy();
      int size = (indexKind == FSIndex.DEFAULT_BAG_INDEX) ? 1 : 1 + (int) rootType.getAllSubtypes().count();

      final ArrayList<FsIndex_singletype<FeatureStructure>> tempSubIndexCache = new ArrayList<FsIndex_singletype<FeatureStructure>>();
      sortedTypeCodes = (indexKind == FSIndex.SORTED_INDEX) ? new int[size] : null;

      initOneType(rootType, tempSubIndexCache, indexKind);
      
      
//      Stream<TypeImpl> typePlusSubtypes = (indexKind == FSIndex.DEFAULT_BAG_INDEX) 
//          ? Stream.of(rootType)
//          : Stream.concat(Stream.of(rootType), rootType.getAllSubtypes()); 
//      
//      
//      typePlusSubtypes  // for the type + all its subtypes, being set up for this one index:
//      
//          // map to the IndexesForType structure, which is a list of all FsIndex_iicp's defined for each type
//         .map(typeImpl -> fsIndexRepositoryImpl.getIndexesForType(typeImpl.getCode()).indexesForType)
//      
//         // map to the "equal" element in the list. and then its fsIndex_singletype
//         .map(ift -> ift.get(ift.indexOf(this)).fsIndex_singletype)
//         
//         // collect (ordered) into ArrayList and if SORTED_INDEX, collect into int array the typecodes, ordered
//         .forEachOrdered(singleIndex -> {if (indexKind == FSIndex.SORTED_INDEX) {
//                                           sortedTypeCodes[tempSubIndexCache.size()] = singleIndex.getTypeCode();
//                                         }
//                                         tempSubIndexCache.add(singleIndex);});
 
      this.cachedSubFsLeafIndexes = tempSubIndexCache; 
      if (this.getIndexingStrategy() == FSIndex.SORTED_INDEX) {
        Arrays.sort(sortedTypeCodes);
      }
      // assign to "volatile" at end, after all initialization is complete
      this.isIteratorCacheSetup = true;
    }  // end of synchronized block
  }
  
  private void initOneType(TypeImpl ti, ArrayList<FsIndex_singletype<FeatureStructure>> cache, int indexKind) {
        
    final FsIndex_singletype<FeatureStructure> singleIndex =  fsIndexRepositoryImpl.getIndexBySpec(
           ti.getCode(),
           getIndexingStrategy(),
           getComparatorImplForIndexSpecs())
        .fsIndex_singletype;
        
    if (indexKind == FSIndex.SORTED_INDEX) {
      sortedTypeCodes[cache.size()] = singleIndex.getTypeCode();
    }
    
    cache.add(singleIndex);
    if (indexKind != FSIndex.DEFAULT_BAG_INDEX) {
      for (TypeImpl subti : ti.getDirectSubtypes()) {
        initOneType(subti, cache, indexKind);        
      }
    }
  }

  
  /**
   * Maybe not used 3/2015
   * 
   * Compares first using the type code of the main types
   *   If those are equal,
   *   Compares using the comparatorForIndexSpecs objects
   * @see java.lang.Comparable#compareTo(Object)
   * 
   */
  public int compareTo(FsIndex_iicp<? extends FeatureStructure> cp) {
    final int typeCode1 = ((TypeImpl) this.fsIndex_singletype.getType()).getCode();
    final int typeCode2 = ((TypeImpl) cp.fsIndex_singletype.getType()).getCode();
    if (typeCode1 < typeCode2) {
      return -1;
    } else if (typeCode1 > typeCode2) {
      return 1;
    } else { // types are equal
      return this.fsIndex_singletype.getComparatorImplForIndexSpecs()
          .compareTo(cp.fsIndex_singletype.getComparatorImplForIndexSpecs());
    }
  }

  /**
   * 
   * @return the sum of the sizes of the indexes of the type + all subtypes
   */
  public int size() {
    createIndexIteratorCache();  // does nothing if already created
    return cachedSubFsLeafIndexes.stream().mapToInt(iicp -> iicp.size()).sum();
  }
  
  public boolean isEmpty() {
    createIndexIteratorCache();  
    for (FsIndex_singletype<FeatureStructure> index : cachedSubFsLeafIndexes) {
      if (index.size() > 0) {
        return false;
      }
    }
    return true;
  }
  
  boolean has1OrMoreEntries() {
    createIndexIteratorCache();  // does nothing if already created
    final ArrayList<FsIndex_singletype<FeatureStructure>> localIc = this.cachedSubFsLeafIndexes;
    final int len = localIc.size();
    for (int i = 0; i < len; i++) {
      if (localIc.get(i).size() > 0) {
        return true;
      };
    }
    return false;
  }
  
  /**
   * A faster version of size() when there are lots of subtypes
   * The cache must be already set up
   * 
   * Guess by adding the sizes of up to the first 3 type/subtypes, 
   * then add 1 more for each subtype in addition.
   * 
   * @return a guess at the size, done quickly
   */
  int guessedSize() {
    final ArrayList<FsIndex_singletype<FeatureStructure>> localIc = this.cachedSubFsLeafIndexes;
    final int len = localIc.size();
    final int lim = Math.min(3, len);
    int size = 0;
    for (int i = 0; i < lim; i++) {
      size += localIc.get(i).size();
    }
    size += len - lim;
    return size;
  }
     
  
//    Int2IntArrayMapFixedSize createIndexUpdateCountsAtReset() {
//      Int2IntArrayMapFixedSize m = new Int2IntArrayMapFixedSize(sortedTypeCodes.length);
//      captureIndexUpdateCounts(m);
//      return m;
//    }
//    
//    void captureIndexUpdateCounts() {
//      captureIndexUpdateCounts(this.flatIndex.indexUpdateCountsResetValues);
//    }
  
//    private void captureIndexUpdateCounts(Int2IntArrayMapFixedSize m) {
//      final int[] localSortedTypeCodes = sortedTypeCodes;
//      for (int i = 0; i < localSortedTypeCodes.length; i++) {
//        m.putAtIndex(i, detectIllegalIndexUpdates[localSortedTypeCodes[i]]);
//      } 
//    }
  
//    boolean isUpdateFreeSinceLastCounterReset() {
//      final Int2IntArrayMapFixedSize typeCode2updateCount = this.flatIndex.indexUpdateCountsResetValues;
//      final int[] localSortedTypeCodes = sortedTypeCodes;
//      for (int i = 0; i < localSortedTypeCodes.length; i++) {
//        if (typeCode2updateCount.getAtIndex(i) != detectIllegalIndexUpdates[localSortedTypeCodes[i]]) {
//          return false;
//        }
//      }
//      return true;
//    }
      
//    boolean isUpdateFreeSinceLastCounterReset(final int typeCode) {
//      return this.flatIndex.indexUpdateCountsResetValues.get(typeCode, sortedTypeCodes) == 
//          detectIllegalIndexUpdates[typeCode];
//    }
    
  boolean subsumes(int superType, int subType) {
    return getCASImpl().getTypeSystemImpl().subsumes(superType,  subType);
  }
  
  // debug
  CASImpl getCASImpl() {
    return fsIndex_singletype.casImpl;
  }

  // for flat index support
//    void addToIteratedSortedIndexes() {
//      iteratedSortedIndexes.add(this);
//    }
  
  // flatIndex is null except for sorted indexes
//    private boolean hasFlatIndex() {
//      if (! FSIndexFlat.enabled) {
//        return false;
//      } else {
//        return isIteratorCacheSetup && (flatIndex != null) && flatIndex.hasFlatIndex();
//      }
//    }
  
  <T2 extends FeatureStructure> FsIndex_singletype<T2> getNoSubtypeIndexForType(Type type) {
    createIndexIteratorCache();
    for (FsIndex_singletype<FeatureStructure> noSubtypeIndex : cachedSubFsLeafIndexes) {
      if (noSubtypeIndex.getType() == type) {
        return (FsIndex_singletype<T2>) noSubtypeIndex;
      }
    }
    return null;
  }
  
  FSIndexRepositoryImpl getFSIndexRepositoryImpl() {
    return fsIndexRepositoryImpl;
  }

  FsIndex_singletype<T> getFsIndex_singleType() {
    return fsIndex_singletype;
  }
  
  boolean isDefaultBagIndex () {
    return getIndexingStrategy() == FSIndex.DEFAULT_BAG_INDEX;
  }

  boolean isSetIndex () {
    return getIndexingStrategy() == FSIndex.SET_INDEX;
  }

  @Override
  public int ll_compare(int ref1, int ref2) {
    return fsIndex_singletype.ll_compare(ref1,  ref2);
  }

  @Override
  public int getIndexingStrategy() {
    return fsIndex_singletype.getIndexingStrategy();
  }

  @Override
  public FSIndexComparator getComparatorForIndexSpecs() {
    return fsIndex_singletype.getComparatorForIndexSpecs();
  }
  
  public FSIndexComparatorImpl getComparatorImplForIndexSpecs() {
    return fsIndex_singletype.getComparatorImplForIndexSpecs();
  }

  @Override
  public int compare(FeatureStructure fs1, FeatureStructure fs2) {
    return fsIndex_singletype.compare(fs1,  fs2);
  }
    
  @Override
  public boolean contains(FeatureStructure fs) {
    return find(fs) != null;
  }

  @Override
  public T find(FeatureStructure fs) {
    createIndexIteratorCache();  // does nothing if already created
    
    for (FsIndex_singletype<FeatureStructure> idx : cachedSubFsLeafIndexes) {
     FeatureStructure result = idx.find(fs);
      if (result != null) {
        return (T) result;
      }
    }
    return null;
  }

  @Override
  public Type getType() {
    return fsIndex_singletype.getType();
  }

  @Override
  public CASImpl getCasImpl() {
    return fsIndex_singletype.casImpl;
  }
  
  public FSIterator<T> iterator() {
    createIndexIteratorCache();  
   
    return (cachedSubFsLeafIndexes.size() == 1)
           ? (FSIterator<T>) fsIndex_singletype.iterator()
           : fsIndex_singletype.isSorted()
             ? new FsIterator_subtypes_ordered<T>(this)
             : new FsIterator_aggregation_common<T>(new FsIterator_subtypes_unordered<T>(this).iterators, fsIndex_singletype);
  } 
  
  public FSIterator<T> iteratorUnordered() {
    createIndexIteratorCache();  
    
    return (cachedSubFsLeafIndexes.size() == 1)
           ? (FSIterator<T>) fsIndex_singletype.iterator()
           : new FsIterator_aggregation_common<T>(new FsIterator_subtypes_unordered<T>(this).iterators, fsIndex_singletype); 
  }

  /**
   * Iterator over arbitrary Feature Structures, but also filters out non-AnnotationFS FeatureStructures
   * @param ambiguous true for normal iteration, false to do unambiguous iteration
   * @return the iterator
   */
  /* 
   * Implementation note: this is different from the Subiterator in that it can be over an iterator that that
   * includes non- annotationFS for the unambiguous case, in which case these are filtered out.
   */
  @Override
  public LowLevelIterator<T> ll_iterator(boolean ambiguous) {
    if (!ambiguous) {
      return new LLUnambiguousIteratorImpl<T>((LowLevelIterator<FeatureStructure>) iterator());
     } else {
       return (LowLevelIterator<T>) iterator();
     }
  }
  
//  /* ***********************************
//   *  Support for withSnapshotIterators
//   *  using proxy
//   * ***********************************/
//  private final static Class<?>[] proxyInterface = new Class<?>[] {FSIndex.class};
//  
//  private class ProxySnapshotHandler implements InvocationHandler {
//    @Override
//    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//      if ("iterator".equals(method.getName())) {
//        if (getIndexingStrategy() == FSIndex.SORTED_INDEX) {
//          return iterator(IteratorExtraFunction.SNAPSHOT);
//        }
//        return iterator(IteratorExtraFunction.UNORDERED_SNAPSHOT);
//      }
//      // pass thru all other methods
//      return method.invoke(args);
//    }    
//  }
    
  @Override
  public FSIndex<T> withSnapshotIterators() {
    return new FsIndex_snapshot<>(this);
  }

  public FSIndexRepositoryImpl getFsRepositoryImpl() {
    return getCasImpl().indexRepository;
  }

}
