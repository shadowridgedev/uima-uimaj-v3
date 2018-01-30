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

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.Annotator_ImplBase;
import org.apache.uima.analysis_engine.annotator.TextAnnotator;
import org.apache.uima.cas.CAS;

/**
 * Annotator class used for testing errors
 * 
 */
public class ErrorAnnotator extends Annotator_ImplBase implements TextAnnotator {
  /**
   * @see org.apache.uima.analysis_engine.annotator.TextAnnotator#process(CAS,ResultSpecification)
   */
  public void process(CAS aCAS, ResultSpecification aResultSpec) throws AnnotatorProcessException {
    if ("ERROR".equals(aCAS.getDocumentText())) {
      throw new RuntimeException("Test Error");
    }
    
    if ("LOG".equals(aCAS.getDocumentText())) {
      try {
        getContext().getLogger().warn("Test Warn Log");
      } catch (AnnotatorContextException e) {
        throw new RuntimeException("Internal test failure");
      }
    }
  }
}
