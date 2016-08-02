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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.uima.cas.function.Consumer_T_withIOException;
import org.apache.uima.internal.util.Int2ObjHashMap;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.Obj2IntIdentityHashMap;
import org.apache.uima.jcas.cas.TOP;

/**
 * Common de/serialization for plain binary and compressed binary form 4
 * which both used to walk the cas using the sequential, incrementing id approach
 * 
 * Lifecycle:  
 *   There is 0/1 instance per CAS, representing the FSs at some point in time in that CAS.
 *   
 *   Creation:  
 *     non-delta serialization
 *     non-delta deserialization
 *     for delta serialization, a previous instance is used if available, otherwise a new csds is made
 *     
 *   Reset: 
 *     CAS Reset
 *     API call (for optimization - used after all delta deserializations into a particular CAS are complete.
 *     
 *   Logical constraints:
 *     - delta de/serialization must use an existing version of this,
 *        -- set during a previous non-delta de/serialization
 *        -- or created just in time via a scan of the cas
 */
public class CommonSerDesSequential {

  public static final boolean TRACE_SETUP = false;
  /**
   * a map from a fs to its addr in the modeled heap, == v2 style addr
   * 
   * created during serialization and deserialization
   * used during serialization to create addr info for index info serialization
   * 
   * For delta, the addr is the modeled addr for the full CAS including both above and below the line.
   */
  final Obj2IntIdentityHashMap<TOP> fs2addr = new Obj2IntIdentityHashMap<>(TOP.class, TOP._singleton);

  /**
   * a map from the modelled (v2 style) FS addr to the V3 FS
   * created when serializing (non-delta), deserializing (non-delta)
   *   augmented when deserializing(delta)
   * used when deserializing (delta and non-delta)
   * retained after deserializing (in case of subsequent delta (multiple) deserializations being combined) 
   * 
   * For delta, the addr is the modeled addr for the full CAS including both above and below the line.
   * 
   */
  final Int2ObjHashMap<TOP> addr2fs = new Int2ObjHashMap<>(TOP.class);  
  
  /**
   * The FSs in this list are not necessarily sequential, but is in ascending (simulated heap) order,
   *   needed for V2 compatibility of serialized forms.
   * This is populated from the main CAS's id-to-fs map, which is accessed once;
   *   Subsequent accessing of that could return different lists due to an intervening Garbage Collection.
   *   
   * Before accessing this, any pending items must be merged.  
   */
  final private List<TOP> sortedFSs = new ArrayList<>();  // holds the FSs sorted by id
  
  final private List<TOP> pending = new ArrayList<>();    // batches up FSs that need to be inserted into sortedFSs
  
  /**
   * The associated CAS
   */
  final private CASImpl baseCas;
  
  /**
   * The first free (available) simulated heap addr, also the last addr + length of that 
   */
  private int heapEnd;  // == the last addr + length of that

  public CommonSerDesSequential(CASImpl cas) {
    this.baseCas = cas.getBaseCAS();
  }
  
  public boolean isEmpty() {
    return sortedFSs.isEmpty() && pending.isEmpty();
  }

  /**
   * Must call in fs sorted order
   * @param fs
   */
  void addFS(TOP fs, int addr) {
    addFS1(fs, addr);
    sortedFSs.add(fs);
  }
  
  void addFS1(TOP fs, int addr) {
    fs2addr.put(fs, addr);
    addr2fs.put(addr, fs);
  }
  
  /**
   * For out of order calls
   * @param fs
   */
  void addFSunordered(TOP fs, int addr) {
    addFS1(fs, addr);
    pending.add(fs);
  }  
      
  void clear() {
    sortedFSs.clear();
    fs2addr.clear();
    addr2fs.clear();
    pending.clear();
    heapEnd = 0;
  }
  
  void setup(MarkerImpl mark, int fromAddr) {
    if (mark == null) {
      clear();
    }
    // local value as "final" to permit use in lambda below
    final int[] nextAddr = {fromAddr};
    if (TRACE_SETUP) System.out.println("Cmn serDes sequential setup called by: " + Misc.getCaller());

    List<TOP> allAboveMark = baseCas.walkReachablePlusFSsSorted(fs -> {
          addFS1(fs, nextAddr[0]);
          if (TRACE_SETUP) {
            System.out.format("Cmn serDes sequential setup: add FS id: %,4d addr: %,5d  type: %s%n", fs._id, nextAddr[0], fs._getTypeImpl().getShortName());
          }
          nextAddr[0] += BinaryCasSerDes.getFsSpaceReq(fs, fs._getTypeImpl());  
        }, mark, null, null);
    
    sortedFSs.addAll(allAboveMark);
    heapEnd = nextAddr[0];
//    if (heapEnd == 0) {
//      System.out.println("debug");
//    }
  }
  
//  /**
//   * called to augment an existing csds with information on FSs added after the mark was set
//   * @param mark -
//   */
//  void setup() { setup(1, 1); }
  
//  void walkSeqFSs(Consumer_T_withIOException<TOP> action) throws IOException {
//    for (TOP fs : sortedFSs) {
//      action.accept(fs);
//    }
//  }
//  
  List<TOP> getSortedFSs() {
    if (pending.size() != 0) {
      merge();
    }
    return sortedFSs;    
  }
  
  int getHeapEnd() { return heapEnd; }
  
  void setHeapEnd(int heapEnd) { this.heapEnd = heapEnd; }
  
  private void merge() {
    Collections.sort(pending, FeatureStructureImplC::compare);
    sortedFSs.addAll(pending);
    pending.clear();
    Collections.sort(sortedFSs, FeatureStructureImplC::compare);
  }
}
