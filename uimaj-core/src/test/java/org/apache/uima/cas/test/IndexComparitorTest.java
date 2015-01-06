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

package org.apache.uima.cas.test;

import junit.framework.TestCase;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.LinearTypeOrderBuilderImpl;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * Test the variations possible for index compare functions
 * 
 */
public class IndexComparitorTest extends TestCase {

  CAS cas;

  TypeSystem ts;

  Type topType;

  Type integerType;

  Type type1;

  Type type1Sub1;

  Type type1Sub2;

  Feature type1Used;

  Feature type1Ignored;

  Feature type1Sub1Used;

  Feature type1Sub1Ignored;

  Feature type1Sub2Used;

  Feature type1Sub2Ignored;

  FSIndexRepositoryMgr irm;

  FSIndexRepository ir;

  FeatureStructure fss[][][];

  FSIndex<FeatureStructure> sortedType1;

  FSIndex<FeatureStructure> sortedType1TypeOrder;

  private FSIndex<FeatureStructure> setType1;

  private FSIndex<FeatureStructure> bagType1;

  private FSIndex<FeatureStructure> sortedType1Sub1;

  private FSIndex<FeatureStructure> setType1Sub1;

  private FSIndex<FeatureStructure> bagType1Sub1;

  private FSIndex<FeatureStructure> setType1TypeOrder;

  private FSIndex<FeatureStructure> bagType1TypeOrder;

  private FSIndex<FeatureStructure> sortedType1Sub1TypeOrder;

  private FSIndex<FeatureStructure> setType1Sub1TypeOrder;

  private FSIndex<FeatureStructure> bagType1Sub1TypeOrder;

  public IndexComparitorTest(String arg0) {
    super(arg0);
  }

  /**
   * class which sets up
   */

  private class SetupForIndexCompareTesting implements AnnotatorInitializer {
    /**
     * @see org.apache.uima.cas.test.AnnotatorInitializer#initTypeSystem(TypeSystemMgr)
     */
    public void initTypeSystem(TypeSystemMgr tsm) {
      // Add new types and features.
      topType = tsm.getTopType();
      integerType = tsm.getType("uima.cas.Integer");

      type1 = tsm.addType("Type1", topType);
      type1Sub1 = tsm.addType("Type1Sub1", type1);
      type1Sub2 = tsm.addType("Type1Sub2", type1);

      type1Used = tsm.addFeature("used", type1, integerType);
      type1Ignored = tsm.addFeature("ignored", type1, integerType);

      type1Sub1Used = tsm.addFeature("used", type1Sub1, integerType);
      type1Sub1Ignored = tsm.addFeature("ignored", type1Sub1, integerType);

      type1Sub2Used = tsm.addFeature("used", type1Sub2, integerType);
      type1Sub2Ignored = tsm.addFeature("ignored", type1Sub2, integerType);
    }

    
    public void initIndexes(FSIndexRepositoryMgr parmIrm, TypeSystem parmTs) {
      IndexComparitorTest.this.ts = parmTs;
      IndexComparitorTest.this.irm = parmIrm;
      parmIrm.createIndex(newComparator(type1), "SortedType1", FSIndex.SORTED_INDEX);
      parmIrm.createIndex(newComparator(type1), "SetType1", FSIndex.SET_INDEX);
      parmIrm.createIndex(newComparator(type1), "BagType1", FSIndex.BAG_INDEX);
      parmIrm.createIndex(newComparatorTypePriority(type1), "SortedType1TypeOrder",
              FSIndex.SORTED_INDEX);
      parmIrm.createIndex(newComparatorTypePriority(type1), "SetType1TypeOrder", FSIndex.SET_INDEX);
      parmIrm.createIndex(newComparatorTypePriority(type1), "BagType1TypeOrder", FSIndex.BAG_INDEX);

      parmIrm.createIndex(newComparator(type1Sub1), "SortedType1Sub1", FSIndex.SORTED_INDEX);
      parmIrm.createIndex(newComparator(type1Sub1), "SetType1Sub1", FSIndex.SET_INDEX);
      parmIrm.createIndex(newComparator(type1Sub1), "BagType1Sub1", FSIndex.BAG_INDEX);
      parmIrm.createIndex(newComparatorTypePriority(type1Sub1), "SortedType1Sub1TypeOrder",
              FSIndex.SORTED_INDEX);
      parmIrm.createIndex(newComparatorTypePriority(type1Sub1), "SetType1Sub1TypeOrder",
              FSIndex.SET_INDEX);
      parmIrm.createIndex(newComparatorTypePriority(type1Sub1), "BagType1Sub1TypeOrder",
              FSIndex.BAG_INDEX);

    }

    private FSIndexComparator newComparator(Type type) {
      FSIndexComparator c = irm.createComparator();
      c.setType(type);
      c.addKey(type1Used, FSIndexComparator.STANDARD_COMPARE);
      return c;
    }

    private FSIndexComparator newComparatorTypePriority(Type type) {
      FSIndexComparator comp = newComparator(type);
      comp.addKey(newTypeOrder(), FSIndexComparator.STANDARD_COMPARE);
      return comp;
    }

    private LinearTypeOrder newTypeOrder() {
      LinearTypeOrderBuilder ltob = new LinearTypeOrderBuilderImpl(ts);
      LinearTypeOrder order;
      try {
        ltob.add(new String[] { "Type1", "Type1Sub1", "Type1Sub2" });
        order = ltob.getOrder();
      } catch (CASException e) {
        throw new Error(e);
      }
      return order;
    }
  }

  public void setUp() throws Exception {
    try {
      this.cas = CASInitializer.initCas(new SetupForIndexCompareTesting());
      assertNotNull(cas);
      ir = cas.getIndexRepository();
      sortedType1 = ir.getIndex("SortedType1");
      setType1 = ir.getIndex("SetType1");
      bagType1 = ir.getIndex("BagType1");
      sortedType1Sub1 = ir.getIndex("SortedType1Sub1");
      setType1Sub1 = ir.getIndex("SetType1Sub1");
      bagType1Sub1 = ir.getIndex("BagType1Sub1");

      sortedType1TypeOrder = ir.getIndex("SortedType1TypeOrder");
      setType1TypeOrder = ir.getIndex("SetType1TypeOrder");
      bagType1TypeOrder = ir.getIndex("BagType1TypeOrder");
      sortedType1Sub1TypeOrder = ir.getIndex("SortedType1Sub1TypeOrder");
      setType1Sub1TypeOrder = ir.getIndex("SetType1Sub1TypeOrder");
      bagType1Sub1TypeOrder = ir.getIndex("BagType1Sub1TypeOrder");

      fss = new FeatureStructure[3][2][2];
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 2; j++) {
          ir.addFS(fss[0][i][j] = createFs(type1, i, j));
          ir.addFS(fss[1][i][j] = createFs(type1Sub1, i, j));
          ir.addFS(fss[2][i][j] = createFs(type1Sub2, i, j));
        }
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void tearDown() {
    fss = null;
    this.cas = null;
    this.ts = null;
    topType = null;
    integerType = null;
    type1 = null;
    type1Sub1 = null;
    type1Sub2 = null;

    type1Used = null;
    type1Ignored = null;
    type1Sub1Used = null;
    type1Sub1Ignored = null;
    type1Sub2Used = null;
    type1Sub2Ignored = null;
    
    irm = null;
    ir = null;
    sortedType1 = null;
    sortedType1TypeOrder = null;
    setType1 = null;
    bagType1 = null;
    sortedType1Sub1 = null;
    setType1Sub1 = null;
    bagType1Sub1 = null;
    setType1TypeOrder = null;
    bagType1TypeOrder = null;
    sortedType1Sub1TypeOrder = null;
    setType1Sub1TypeOrder = null;
    bagType1Sub1TypeOrder = null;
  }

  private FeatureStructure createFs(Type type, int i, int j) {
    FeatureStructure f = cas.createFS(type);
    f.setIntValue(type.getFeatureByBaseName("used"), i);
    f.setIntValue(type.getFeatureByBaseName("ignored"), j);
    return f;
  }

  public void testFindSubtype() throws Exception {
    cas.reset();
    
    ir.addFS(createFs(type1, 0, 0));
    ir.addFS(createFs(type1Sub1, 1, 1));
    FeatureStructure testprobe = createFs(type1Sub1, 1, 1);  // not in index, used only for key values
    
    assertFalse(sortedType1.contains(testprobe));
    
    assertTrue(sortedType1Sub1.contains(testprobe));
    
    FeatureStructure testProbeSuper = createFs(type1, 1, 1);
    
    assertTrue(sortedType1Sub1.contains(testProbeSuper));
  }
  
  public void testCompare() throws Exception {
    try {
      assertTrue(0 == sortedType1.compare(fss[0][0][0], fss[0][0][1]));
      assertTrue(1 == sortedType1.compare(fss[0][1][0], fss[0][0][0]));
      // type ignored in showing equals
      assertTrue(0 == sortedType1.compare(fss[1][0][0], fss[0][0][0]));
      assertTrue(0 == sortedType1.compare(fss[1][0][0], fss[2][0][0]));
      // type not ignored if type-order included
      assertTrue(0 == sortedType1TypeOrder.compare(fss[0][0][0], fss[0][0][1]));
      assertTrue(1 == sortedType1TypeOrder.compare(fss[1][0][0], fss[0][0][0]));

      assertTrue(0 == setType1.compare(fss[0][0][0], fss[0][0][1]));
      assertTrue(1 == setType1.compare(fss[0][1][0], fss[0][0][0]));
      // type ignored in showing equals
      assertTrue(0 == setType1.compare(fss[1][0][0], fss[0][0][0]));
      assertTrue(0 == setType1.compare(fss[1][0][0], fss[2][0][0]));
      // type not ignored if type-order included
      assertTrue(0 == setType1TypeOrder.compare(fss[0][0][0], fss[0][0][1]));
      assertTrue(1 == setType1TypeOrder.compare(fss[1][0][0], fss[0][0][0]));

      assertTrue(-1 == bagType1.compare(fss[0][0][0], fss[0][0][1]));
      assertTrue(1 == bagType1.compare(fss[0][1][0], fss[0][0][0]));
      assertTrue(1 == bagType1.compare(fss[1][0][0], fss[0][0][0]));
      assertTrue(-1 == bagType1.compare(fss[1][0][0], fss[2][0][0]));

      assertTrue(-1 == bagType1TypeOrder.compare(fss[0][0][0], fss[0][0][1]));
      assertTrue(1 == bagType1TypeOrder.compare(fss[1][0][0], fss[0][0][0]));

      // test contains
      FeatureStructure testType1_0_0 = createFs(type1, 0, 0);
      FeatureStructure testType1_1_0 = createFs(type1, 1, 0);
      FeatureStructure testType1_0_x = createFs(type1, 0, 17);
      FeatureStructure testTypeSub1_0_x = createFs(type1Sub1, 0, 17);
      FeatureStructure testTypeSub1_0_0 = createFs(type1Sub1, 0, 0);

      assertTrue(sortedType1.contains(testType1_0_0));
      assertTrue(sortedType1.contains(testType1_0_x));
      assertTrue(sortedType1.contains(testTypeSub1_0_x));
      assertTrue(setType1.contains(testType1_0_0));
      assertTrue(setType1.contains(testType1_0_x));
      assertTrue(setType1.contains(testTypeSub1_0_x));
      assertFalse(bagType1.contains(testType1_0_0));
      assertFalse(bagType1.contains(testType1_0_x));
      assertFalse(bagType1.contains(testTypeSub1_0_x));
      
      FeatureStructure testType1_0_0_eq = fss[0][0][0];
      FeatureStructure testType1_0_x_eq = fss[0][0][0];
      FeatureStructure testTypeSub1_0_x_eq = fss[1][0][0];

      assertTrue(sortedType1TypeOrder.contains(testType1_0_0));
      assertTrue(sortedType1TypeOrder.contains(testType1_0_x));
      // assertTrue(sortedType1TypeOrder.contains(testTypeSub1_0_x));
      assertTrue(setType1TypeOrder.contains(testType1_0_0));
      assertTrue(setType1TypeOrder.contains(testType1_0_x));
      // assertTrue(setType1TypeOrder.contains(testTypeSub1_0_x));
      assertFalse(bagType1TypeOrder.contains(testType1_0_0));
      assertFalse(bagType1TypeOrder.contains(testType1_0_x));
      assertFalse(bagType1TypeOrder.contains(testTypeSub1_0_x));

      // for (Iterator it = sortedType1TypeOrder.iterator(); it.hasNext();) {
      // System.out.println(it.next().toString());
      // }
      // assertTrue(sortedType1TypeOrder.contains(testTypeSub1_0_0));
      // assertTrue(sortedType1TypeOrder.contains(testTypeSub1_0_x));

      // test find

      assertNotNull(sortedType1.find(testType1_0_0));
      assertNotNull(sortedType1.find(testType1_0_x));
      assertNotNull(sortedType1.find(testTypeSub1_0_x));
      assertNotNull(setType1.find(testType1_0_0));
      assertNotNull(setType1.find(testType1_0_x));
      assertNotNull(setType1.find(testTypeSub1_0_x));
      assertNull(bagType1.find(testType1_0_0));
      assertNull(bagType1.find(testType1_0_x));
      assertNull(bagType1.find(testTypeSub1_0_x));

      assertNotNull(sortedType1TypeOrder.find(testType1_0_0));
      assertNotNull(sortedType1TypeOrder.find(testType1_0_x));
      // assertNotNull(sortedType1TypeOrder.find(testTypeSub1_0_x));
      assertNotNull(setType1TypeOrder.find(testType1_0_0));
      assertNotNull(setType1TypeOrder.find(testType1_0_x));
      // assertNotNull(setType1TypeOrder.find(testTypeSub1_0_x));
      assertNull(bagType1TypeOrder.find(testType1_0_0));
      assertNull(bagType1TypeOrder.find(testType1_0_x));
      assertNull(bagType1TypeOrder.find(testTypeSub1_0_x));

      // test iterator(fs)
      assertTrue(sortedType1.iterator(testType1_0_0).isValid());
      assertTrue(sortedType1.iterator(testType1_0_x).isValid());
      assertTrue(sortedType1.iterator(testTypeSub1_0_x).isValid());
      assertTrue(setType1.iterator(testType1_0_0).isValid());
      assertTrue(setType1.iterator(testType1_0_x).isValid());
      assertTrue(setType1.iterator(testTypeSub1_0_x).isValid());
      assertTrue(bagType1.iterator(testType1_0_0_eq).isValid());
      assertTrue(bagType1.iterator(testType1_0_x_eq).isValid());
      assertTrue(bagType1.iterator(testTypeSub1_0_x_eq).isValid());

      assertTrue(sortedType1TypeOrder.iterator(testType1_0_0).isValid());
      assertTrue(sortedType1TypeOrder.iterator(testType1_0_x).isValid());
      assertTrue(sortedType1TypeOrder.iterator(testTypeSub1_0_x).isValid());
      assertTrue(setType1TypeOrder.iterator(testType1_0_0).isValid());
      assertTrue(setType1TypeOrder.iterator(testType1_0_x).isValid());
      assertTrue(setType1TypeOrder.iterator(testTypeSub1_0_x).isValid());
      assertTrue(bagType1TypeOrder.iterator(testType1_0_0_eq).isValid());
      assertTrue(bagType1TypeOrder.iterator(testType1_0_x_eq).isValid());
      assertTrue(bagType1TypeOrder.iterator(testTypeSub1_0_x_eq).isValid());

      // assertTrue(fss[0][0][0].equals(sortedType1.iterator(testType1_0_0).get()));
      assertTrue(fss[0][1][0].equals(sortedType1.iterator(testType1_1_0).get()));
      // assertTrue(fss[0][0][0].equals(sortedType1.iterator(testType1_0_x).get()));
      // assertTrue(fss[0][0][0].equals(sortedType1.iterator(testTypeSub1_0_x).get()));
      assertTrue(fss[0][0][0].equals(setType1.iterator(testType1_0_0).get()));
      assertTrue(fss[0][0][0].equals(setType1.iterator(testType1_0_x).get()));
      assertTrue(fss[0][0][0].equals(setType1.iterator(testTypeSub1_0_x).get()));
      assertTrue(fss[0][0][0].equals(bagType1.iterator(testType1_0_0_eq).get()));
      assertTrue(fss[0][0][0].equals(bagType1.iterator(testType1_0_x_eq).get()));
      assertTrue(fss[1][0][0].equals(bagType1.iterator(testTypeSub1_0_x_eq).get()));

      // assertTrue(fss[0][0][0].equals(sortedType1.iterator(testType1_0_0).get()));
      assertTrue(fss[0][1][0].equals(sortedType1.iterator(testType1_1_0).get()));
      // assertTrue(fss[0][0][0].equals(sortedType1.iterator(testType1_0_x).get()));
      // assertTrue(fss[0][0][0].equals(sortedType1.iterator(testTypeSub1_0_x).get()));
      assertTrue(fss[0][0][0].equals(setType1.iterator(testType1_0_0).get()));
      assertTrue(fss[0][0][0].equals(setType1.iterator(testType1_0_x).get()));
      assertTrue(fss[0][0][0].equals(setType1.iterator(testTypeSub1_0_x).get()));
      assertTrue(fss[0][0][0].equals(bagType1.iterator(testType1_0_0_eq).get()));
      assertTrue(fss[0][0][0].equals(bagType1.iterator(testType1_0_x_eq).get()));
      assertTrue(fss[1][0][0].equals(bagType1.iterator(testTypeSub1_0_x_eq).get()));

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }

  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(IndexComparitorTest.class);
  }

}
