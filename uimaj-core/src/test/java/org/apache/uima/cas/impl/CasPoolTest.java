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

import java.util.Collections;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.impl.ChildUimaContext_impl;
import org.apache.uima.impl.RootUimaContext_impl;
import org.apache.uima.impl.UimaContext_ImplBase;
import org.apache.uima.resource.CasManager;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.Session;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InstrumentationFacility;
import org.apache.uima.util.Logger;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.XMLInputSource;


public class CasPoolTest extends TestCase {

  
  // verify that several CASes in a pool in different views share the same type system
  
  public void testPool() throws Exception {
    try {
      
      AnalysisEngineDescription aed = (AnalysisEngineDescription)UIMAFramework.getXMLParser().parse(
              new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/TestPrimitiveTae1.xml")));
      
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aed);

      // define a caspool of size 2
      CasManager cm = ((UimaContext_ImplBase)ae.getUimaContext()).getResourceManager().getCasManager();
      cm.defineCasPool("uniqueString", 2, null);
      
      CAS c1 = cm.getCas("uniqueString");
      CAS c2 = cm.getCas("uniqueString");
      c1.getJCas();
      
      CAS c1v2 = c1.createView("view2");
      CAS c2v2 = c2.createView("view3");
      c2v2.getJCas();
      
      TypeSystem ts = c1.getTypeSystem();
      
      Assert.assertTrue(ts == c2.getTypeSystem());
      Assert.assertTrue(ts == c1v2.getTypeSystem());
      Assert.assertTrue(ts == c2v2.getTypeSystem());
      
      cm.releaseCas(c1v2);
      cm.releaseCas(c2);
      
      c1 = cm.getCas("uniqueString");
      c1.createView("mappedName");
      RootUimaContext_impl rootContext = new RootUimaContext_impl();
      ChildUimaContext_impl context = new ChildUimaContext_impl(rootContext, "abc", Collections.singletonMap(CAS.NAME_DEFAULT_SOFA, "mappedName"));
      c1.setCurrentComponentInfo(context.getComponentInfo());
      cm.releaseCas(c1);;

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
