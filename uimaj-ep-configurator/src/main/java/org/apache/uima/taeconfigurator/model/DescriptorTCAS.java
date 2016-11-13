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

package org.apache.uima.taeconfigurator.model;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;

// TODO: Auto-generated Javadoc
/**
 * Model part: CAS corresponding to the descriptor being edited.
 */

public class DescriptorTCAS extends AbstractModelPart {

  /** The cached result. */
  private CAS cachedResult;

  /**
   * Instantiates a new descriptor TCAS.
   *
   * @param modelRoot the model root
   */
  public DescriptorTCAS(MultiPageEditor modelRoot) {
    super(modelRoot);
  }

  /**
   * Validate.
   *
   * @throws ResourceInitializationException the resource initialization exception
   */
  public void validate() throws ResourceInitializationException {

    AnalysisEngineDescription ae = (AnalysisEngineDescription) modelRoot.getAeDescription().clone();
    // speedup = replace typeSystem with resolved imports version
    if (ae.isPrimitive()) {
      TypeSystemDescription tsd = modelRoot.getMergedTypeSystemDescription();
      if (null != tsd)
        tsd = (TypeSystemDescription) tsd.clone();
      ae.getAnalysisEngineMetaData().setTypeSystem(tsd);
    }
    ae.getAnalysisEngineMetaData().setFsIndexCollection(modelRoot.getMergedFsIndexCollection());
    ae.getAnalysisEngineMetaData().setTypePriorities(modelRoot.getMergedTypePriorities());
    try {
      // long time = System.currentTimeMillis();
      // System.out.println("Creating TCas model");
      cachedResult = modelRoot.createCas(ae, casCreateProperties, modelRoot.createResourceManager());
      // System.out.println("Finished Creating TCas model; time= " +
      // (System.currentTimeMillis() - time));
      if (null == cachedResult)
        throw new InternalErrorCDE("null result from createTCas");
    } catch (CASAdminException e) {
      throw new ResourceInitializationException(e);
    }
    dirty = false;
    modelRoot.allTypes.dirty = true;
  }

  /**
   * Gets the.
   *
   * @return a CAS for the model descriptor
   */
  public CAS get() {
    if (dirty) {
      update();
    }
    return cachedResult;
  }

  /**
   * Sets the.
   *
   * @param tcas the tcas
   */
  public void set(CAS tcas) {
    cachedResult = tcas;
    dirty = false;
  }

  /**
   * Update.
   */
  private void update() {
    try {
      validate();
    } catch (ResourceInitializationException e) {
      throw new InternalErrorCDE("Unexpected Exception", e);
    }
  }
}
