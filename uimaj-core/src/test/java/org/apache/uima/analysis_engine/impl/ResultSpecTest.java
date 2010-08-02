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

package org.apache.uima.analysis_engine.impl;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.CAS;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

public class ResultSpecTest extends TestCase {

  private CAS cas;
  
  private static final String EN = "en";
  private static final String X  = "x-unspecified";
  private static final String EN_US = "en-us";
  private static final String I = "I";  // split designator
  
  /**
   * Tests for https://issues.apache.org/jira/browse/UIMA-1840
   */
  public void testIntersection() {
    checkl(X, I, X, I, X);
    checkl(X, I, EN, I,            EN);
    checkl(EN, I,            X, I, EN);
    checkl(X, I, EN_US, I,        EN_US);
    checkl(EN_US, I,       X, I, EN_US);
    
    checkl(EN, I, EN, I, EN);
    checkl(EN, I, EN_US, I, EN_US);
    checkl(EN_US, I, EN, I, EN_US);
    checkl(EN_US, I, EN_US, I, EN_US);
    
    checkl(X, EN, I, EN, X, I, X);
    checkl(X, EN, I, EN, I, EN);
    checkl(EN, EN_US, I, X, I, EN, EN_US);
  }
  
  private void checkl(String... args) {
    List<String> rs1List = new ArrayList<String>();
    List<String> rs2List = new ArrayList<String>();
    List<String> expList = new ArrayList<String>();
    List<String>[] tgts = new List[]{rs1List, rs2List, expList};
    int tgtI = 0;
    
    for (int i = 0; i < args.length; i++) { 
      if (args[i] == I) {
        tgtI ++;
      } else {
        tgts[tgtI].add(args[i]);            
      }
    }
    String[] rs1langs = rs1List.toArray(new String[rs1List.size()]);
    String[] rs2langs = rs2List.toArray(new String[rs2List.size()]);
    String[] explangs = expList.toArray(new String[expList.size()]);

    ResultSpecification_impl rs1 = new ResultSpecification_impl();
    ResultSpecification_impl rs2 = new ResultSpecification_impl();
    ResultSpecification_impl rsE = new ResultSpecification_impl(); //expected
    
    rs1.addResultType("T1", true, rs1langs);
    rs2.addResultType("T1", true, rs2langs);
    rsE.addResultType("T1", true, explangs);
    
    ResultSpecification_impl rsQ = ResultSpecification_impl.intersect(rs1, rs2);
    assertTrue(rsQ.equals(rsE));
  }
  
  public void testComputeAnalysisComponentResultSpec() throws Exception {
    try {
      AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(JUnitExtension.getFile("SequencerTest/Annotator1.xml")));
      PrimitiveAnalysisEngine_impl ae = (PrimitiveAnalysisEngine_impl) UIMAFramework
              .produceAnalysisEngine(aeDesc);
      CAS cas = ae.newCAS();
      ResultSpecification resultSpec = new ResultSpecification_impl();
      resultSpec.addResultType("uima.tt.TokenLikeAnnotation", true);
      resultSpec.setTypeSystem(cas.getTypeSystem());
      
      ResultSpecification_impl rs2 = new ResultSpecification_impl(cas.getTypeSystem());
      rs2.addCapabilities(ae.getAnalysisEngineMetaData().getCapabilities());
      ResultSpecification acResultSpec = ResultSpecification_impl.intersect(resultSpec, rs2);
      assertTrue(acResultSpec.containsType("uima.tt.TokenAnnotation"));
      assertFalse(acResultSpec.containsType("uima.tt.SentenceAnnotation"));
      assertFalse(acResultSpec.containsType("uima.tt.Lemma"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testComputeAnalysisComponentResultSpecInherit() throws Exception {
    try {
      AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(JUnitExtension.getFile("SequencerTest/Annotator1.xml")));
      PrimitiveAnalysisEngine_impl ae = (PrimitiveAnalysisEngine_impl) UIMAFramework
              .produceAnalysisEngine(aeDesc);
      CAS cas = ae.newCAS();
      ResultSpecification resultSpec = new ResultSpecification_impl(cas.getTypeSystem());
      resultSpec.addResultType("uima.tcas.Annotation", true);
      
      ResultSpecification_impl rs2 = new ResultSpecification_impl(cas.getTypeSystem());
      rs2.addCapabilities(ae.getAnalysisEngineMetaData().getCapabilities());
      ResultSpecification acResultSpec = ResultSpecification_impl.intersect(resultSpec, rs2);
      assertTrue(acResultSpec.containsType("uima.tt.TokenAnnotation"));
      assertTrue(acResultSpec.containsType("uima.tt.SentenceAnnotation"));
      assertFalse(acResultSpec.containsType("uima.tt.Lemma"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
  private ResultSpecification createResultSpec(String language) {
    ResultSpecification resultSpec = new ResultSpecification_impl(cas.getTypeSystem());
    resultSpec.addResultType("Type1", true, new String[] {language});
    return resultSpec;
  }
    
  /**
   * Auxiliary method used by testProcess()
   * 
   * @param aTaeDesc
   *          description of TextAnalysisEngine to test
   */
  protected void _testProcess(AnalysisEngineDescription aed, String language) throws UIMAException {
    // create and initialize AnalysisEngine
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aed);
    cas = ae.newCAS();

    ResultSpecification resultSpec = createResultSpec(language);
    
    cas.setDocumentText("new test");
    ae.process(cas, resultSpec);
    cas.reset();
    ae.destroy();
  }

}
