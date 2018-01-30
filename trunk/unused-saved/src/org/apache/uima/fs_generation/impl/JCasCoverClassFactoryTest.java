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
package org.apache.uima.fs_generation.impl;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.uima.type_system.TypeSystem;
import org.apache.uima.type_system.impl.TypeSystemImpl;
import org.junit.Test;

public class JCasCoverClassFactoryTest {

@Test
  public void testCreateJCasCoverClass() throws UnsupportedEncodingException  {
    
    TypeSystemImpl tsi = new TypeSystemImpl();
    
    FeatureStructureClassGen jcf = new FeatureStructureClassGen();
    
    byte[] br = jcf.createJCasCoverClass(tsi.getType(TypeSystem.TYPE_NAME_ANNOTATION));
    
    // convert generated byte codes for jvm into java source via decompiling
    
    UimaDecompiler ud = new UimaDecompiler();
    String sr = ud.decompile(TypeSystem.TYPE_NAME_ANNOTATION, br).toString("UTF-8");
    System.out.println(sr);
    
    
//    File file = JUnitExtension.getFile("JCasGen/typeSystemAllKinds.xml");
//    TypeSystemDescription tsDesc = UIMAFramework.getXMLParser().parseTypeSystemDescription(
//            new XMLInputSource(file));
//   
//    CAS cas = CasCreationUtils.createCas(tsDesc, null, null);
//    
//    FeatureStructureClassGen jcf = new FeatureStructureClassGen();
//    
//    byte[] r = jcf.createJCasCoverClass((TypeImpl) cas.getTypeSystem().getType("pkg.sample.name.All"));
//
//    Path root = Paths.get(".");  // should resolve to the project path
//    Path dir = root.resolve("temp/test/JCasGen");
//    dir.toFile().mkdirs();
//    Files.write(dir.resolve("testOutputAllKinds.class"), r);
//    
//    System.out.println("debug: generated byte array");
  }

}
