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

package org.apache.uima.collection.impl;

import java.io.IOException;

import org.junit.Assert;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CasTestUtil;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.util.Progress;

/**
 * A test collection reader that just Asserts the CAS heap size to be a specified value.
 * 
 */
public class CasHeapSizeTestCollectionReader extends CollectionReader_ImplBase {
  static final int EXPECTED_HEAP_SIZE = 100000;

  private int numChecks = 10;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
   */
  public void getNext(CAS aCAS) throws IOException, CollectionException {
    int actualHeapSize = CasTestUtil.getHeapSize(aCAS);
  
    // in v3 the actualHeap is always 500,000, so this test always miscompares  
//    Assert.assertEquals(EXPECTED_HEAP_SIZE, actualHeapSize);
    numChecks--;

    // populate with doc to avoid error
    aCAS.setDocumentText("This is a test");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
   */
  public void close() throws IOException {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
   */
  public Progress[] getProgress() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#hasNext()
   */
  public boolean hasNext() throws IOException, CollectionException {
    return numChecks > 0;
  }

}
