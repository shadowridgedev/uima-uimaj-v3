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

package org.apache.uima;

import java.util.Map;
import java.util.Properties;

import org.apache.uima.analysis_engine.AnalysisEngineManagement;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.ComponentInfo;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.Session;
import org.apache.uima.util.Logger;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.Settings;

/**
 * Admin interface to the UIMA Context. Developer code should only use the {@link UimaContext}
 * interface. The methods on this interface are for the framework's use only. *
 */
public interface UimaContextAdmin extends UimaContext {
  /**
   * Initializes a root UimaContext.
   * 
   * @param aLogger
   *          the logger that will be returned by this UimaContext's {@link #getLogger()} method.
   * @param aResourceManager
   *          the ResourceManager that will be used by this UimaContext to locate and access
   *          external resource.
   * @param aConfigurationManager
   *          the ConfigurationManager that will be used by this UimaContext to access its
   *          configuration parameter settings.
   */
  public void initializeRoot(Logger aLogger, ResourceManager aResourceManager,
          ConfigurationManager aConfigurationManager);

  /**
   * Creates a UimaContext that is a child of this UimaContext.
   * 
   * @param aContextName
   *          a name for the new context, which must be unique with respect to all children of the
   *          parent context.
   * @param aSofaMappings
   *          mappings from child's sofa name to parent's sofa name. May be null.
   */
  public UimaContextAdmin createChild(String aContextName, Map<String, String> aSofaMappings);

  /**
   * Sets the Logger for this UimaContext. If this method is not called, the default logger ({@link org.apache.uima.UIMAFramework#getLogger()})
   * will be used.
   * 
   * @param aLogger
   *          the logger that will be returned by this UimaContext's {@link #getLogger()} method.
   */
  public void setLogger(Logger aLogger);

  /**
   * Sets the current ProcessTrace object, which will receive trace events generated by the
   * InstrumentationFacility.
   * 
   * @param aProcessTrace
   *          the ProcessTrace object to receive trace events
   */
  public void setProcessTrace(ProcessTrace aProcessTrace);

  /**
   * Gets the ResourceManager instance used by this UimaContext to resolve external resource
   * accesses.
   * 
   * @return the ResourceManager instance for this UimaContext
   */
  public ResourceManager getResourceManager();

  /**
   * Gets the ConfigurationManager instance used by this UimaContext to resolve configuration
   * parameter resource accesses.
   * 
   * @return the ConfigurationManager instance for this UimaContext
   */
  public ConfigurationManager getConfigurationManager();

  /**
   * Gets the fully-qualified name of this context. This is a slash-separated name consisting of
   * each containing context name back to the root. It always begins and ends with a slash. For
   * example, the context name for an annotator nested within two AnalysisEngines might look like:
   * <code>/MyTopLevelAnalysisEngine/MyComponentAnalysisEngine/MyAnnotator/</code>.
   * 
   * @return the qualified context name
   */
  public String getQualifiedContextName();

  /**
   * Sets the current session object. A default Session object is created when the UimaContext is
   * created. In a multi-client deployment, the deployment wrapper is responsible for ensuring that
   * an appropriate Session object is installed here prior to invoking components that use this
   * UimaContext.
   */
  public void setSession(Session aSession);

  /**
   * Gets the Root Context for this Resource. This is the top-level context for the outermost
   * aggregate component (AnalysisEngine or CollectionProcessingEngine).
   * 
   * @return root context
   */
  public UimaContextAdmin getRootContext();

  /**
   * Defines the CAS pool that this UimaContext must support. This method must be called before
   * {@link UimaContext#getEmptyCas(Class)} may be called.
   * 
   * @param aSize
   *          the minimum CAS pool size required
   * @param aPerformanceTuningSettings
   *          settings, including initial CAS heap size, for the AE
   * @param aSofaAware
   *          whether the component that will receive these CASes is sofa aware. This is needed to
   *          determine which view to get. Sofa-aware components get the base view; sofa-unaware
   *          components get the default text sofa view (or whatever is mapped to it).
   * @throws ResourceInitializationException
   *           if a CAS could not be created.
   */
  void defineCasPool(int aSize, Properties aPerformanceTuningSettings, boolean aSofaAware)
          throws ResourceInitializationException;

  /**
   * Gets an object that can be used to do monitoring or management of this AnalysisEngine.
   * 
   * @return an object exposing a management interface to this AE
   */
  public AnalysisEngineManagement getManagementInterface();

  /**
   * Gets the ComponentInfo object for this component, which can be passed to
   * {@link CAS#setCurrentComponentInfo(ComponentInfo)}.
   * 
   * @return the component info
   */
  public ComponentInfo getComponentInfo();
  
  /**
   * Gets an unmodifiable Map containing the mapping of component sofa name to
   * absolute sofa ID.
   * @return the Sofa map for this component
   */
  public Map<String, String> getSofaMap();
  
  /**
   * Called internally by the framework whenever the AnalysisComponent returns a CAS
   * from its next() method or calls cas.release(). Used to monitor the number of CASes
   * that the AnalysisComponent is using at any one time.
   * @param aCAS the CAS that was returned or released
   */
  public void returnedCAS(AbstractCas aCAS);
  
  /**
   * Returns a UUID-like unique name of this component.
   * 
   * @return - unique name of this component
   */
  public String getUniqueName();
  
  /**
   * Gets the settings to be used for external parameter overrides
   *  
   * @return the Settings object
   */
  public Settings getExternalOverrides();
  
  /**
   * Sets the Settings for external parameter overrides
   * 
   * @param externalOverrides
   */
  public void setExternalOverrides(Settings externalOverrides);

}
