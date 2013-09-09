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

package org.apache.uima.adapter.vinci;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.uima.UIMAFramework;
import org.apache.uima.adapter.vinci.util.Constants;
import org.apache.uima.adapter.vinci.util.VinciSaxParser;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineServiceStub;
import org.apache.uima.analysis_engine.impl.AnalysisEngineManagementImpl;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.collection.base_cpm.CasObjectProcessor;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.ResourceServiceStub;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.SaxDeserializer;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.TransportableFactory;
import org.apache.vinci.transport.VinciClient;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.context.VinciContext;
import org.apache.vinci.transport.document.AFrame;

public class VinciAnalysisEngineServiceStub implements AnalysisEngineServiceStub {
  private VinciClient mVinciClient;

  private Resource mOwner;
  
  /**
   * Timeout to use for process and collectionProcessComplete calls.
   */
  private int mTimeout;
  
  /**
   * Timeout to use for getMetaData calls.
   */
  private int mGetMetaDataTimeout;

  private static final boolean debug = System.getProperty("DEBUG") != null;
  
  /**
   * Value to return from callGetSupportedXCasVersions method for older services 
   * that don't actually implement this method.
   */
  private static final List SUPPORT_XCAS_V1 = Collections.unmodifiableList(
          Arrays.asList(new String[]{"1"}));
  
  public VinciAnalysisEngineServiceStub(String endpointURI, Resource owner)
          throws ResourceInitializationException {
    this(endpointURI, null, owner, null);
  }

  public VinciAnalysisEngineServiceStub(String endpointURI, Integer timeout, Resource owner,
          Parameter[] parameters) throws ResourceInitializationException {
    mOwner = owner;

    // open Vinci connection
    try {
      VinciContext vctx = new VinciContext(InetAddress.getLocalHost().getCanonicalHostName(), 0);
      // Override vinci default VNS settings
      String vnsHost = null;
      String vnsPort = null; 
      String getMetaDataTimeout = null; 
      if (parameters != null) {
         vnsHost = 
          VinciBinaryAnalysisEngineServiceStub.getParameterValueFor("VNS_HOST", parameters); 
         vnsPort = VinciBinaryAnalysisEngineServiceStub.getParameterValueFor("VNS_PORT",
                parameters);
         getMetaDataTimeout = VinciBinaryAnalysisEngineServiceStub.getParameterValueFor("GetMetaDataTimeout", parameters);
      }
      if (vnsHost == null) {
        vnsHost = System.getProperty("VNS_HOST");
        if (vnsHost == null)
          vnsHost = Constants.DEFAULT_VNS_HOST;
      }
      if (vnsPort == null) {
        vnsPort = System.getProperty("VNS_PORT");
        if (vnsPort == null)
          vnsPort = "9000";
      }
      vctx.setVNSHost(vnsHost);
      vctx.setVNSPort(Integer.parseInt(vnsPort));
      
      // Override socket keepAlive setting
      vctx.setSocketKeepAliveEnabled(isSocketKeepAliveEnabled());

      if (debug) {
        System.out.println("Establishing connnection to " + endpointURI + " using VNS_HOST:"
                + vctx.getVNSHost() + " and VNS_PORT=" + vctx.getVNSPort());
      }
        
      // establish connection to service
      mVinciClient = new VinciClient(endpointURI, AFrame.getAFrameFactory(), vctx);
      
      //store timeout for use in later RPC calls
      if (timeout != null) {
        mTimeout = timeout.intValue();
      } else {
       mTimeout = mVinciClient.getSocketTimeout(); //default
      }
      if (getMetaDataTimeout != null) {
        mGetMetaDataTimeout = Integer.parseInt(getMetaDataTimeout);
      } else {
        mGetMetaDataTimeout = mVinciClient.getSocketTimeout(); //default
      }
      
      if (debug) {
        System.out.println("Success");
      }
    } catch (Exception e) {
      throw new ResourceInitializationException(e);
    }
  }


  /**
   * @see ResourceServiceStub#callGetMetaData()
   */
  public ResourceMetaData callGetMetaData() throws ResourceServiceException {
    try {
      // create Vinci Frame
      VinciFrame queryFrame = new VinciFrame();
      // Add Vinci Command, so that the receiving service knows what to do
      queryFrame.fadd("vinci:COMMAND", "GetMeta");
      // Send the request to the service and wait for response

      if (debug) {
        System.out.println("Calling GetMeta");
      }

      VinciFrame resultFrame = mVinciClient.rpc(queryFrame, mGetMetaDataTimeout);

      if (debug) {
        System.out.println("Success");
      }

      // Extract the data from Vinci Response frame
      // System.out.println(resultFrame.toXML()); //DEBUG

      // Remove things from the result frame that are not the MetaData objects we expect.
      // In the future other things may go in here.
      int i = 0;
      while (i < resultFrame.getKeyValuePairCount()) {
        String key = resultFrame.getKeyValuePair(i).getKey();
        if (key.length() < 8 || !key.substring(key.length() - 8).equalsIgnoreCase("metadata")) {
          resultFrame.fdrop(key);
        } else {
          i++;
        }
      }

      // Parse the XML into the ProcessingResourceMetaData object
      SaxDeserializer saxDeser = UIMAFramework.getXMLParser().newSaxDeserializer();

      VinciSaxParser vinciSaxParser = new VinciSaxParser();
      vinciSaxParser.setContentHandler(saxDeser);
      vinciSaxParser.parse(resultFrame);
      ProcessingResourceMetaData metadata = (ProcessingResourceMetaData) saxDeser.getObject();

      return metadata;
    } catch (Exception e) {
      throw new ResourceServiceException(e);
    }
  }

  /**
   * @see AnalysisEngineServiceStub#callProcess(CAS)
   */
  public void callProcess(CAS aCAS) throws ResourceServiceException {
    doProcess(aCAS);
  }

  /**
   * @see CasObjectProcessor#processCas(CAS)
   */
  public void callProcessCas(CAS aCAS) throws ResourceServiceException {
    doProcess(aCAS);
  }

  /**
   * The actual process call.
   */
  private void doProcess(CAS aCAS) throws ResourceServiceException {
    try {
      aCAS = ((CASImpl) aCAS).getBaseCAS();

      // create CASTransportable ... always send the base CAS
      final CASTransportable query = new CASTransportable(aCAS, null, mOwner.getUimaContext(), true);
      query.setCommand("Annotate");

      mVinciClient.sendAndReceive(query, new TransportableFactory() {
        public Transportable makeTransportable() {
          // query.ignoreResponse = true; // TESTING
          return query;
        }
      }, mTimeout);

      // if service reply included the time taken to do the analysis,
      // add that to the AnalysisEngineManagement MBean
      int annotationTime = query.getExtraDataFrame().fgetInt("TAE:AnnotationTime");
      if (annotationTime > 0) {
        AnalysisEngineManagementImpl mbean = (AnalysisEngineManagementImpl) mOwner
                .getUimaContextAdmin().getManagementInterface();
        mbean.reportAnalysisTime(annotationTime);
      }
    } catch (Exception e) {
      throw new ResourceServiceException(e);
    }
  }

  /**
   * @see ResourceServiceStub#destroy()
   */
  public void destroy() {
    mVinciClient.close();
  }

  /**
   * @see CasObjectProcessor#batchProcessComplete(org.apache.uima.util.ProcessTrace)
   */
  public void callBatchProcessComplete() throws ResourceServiceException {
    try {
      // create Vinci Frame ( Data Cargo)
      AFrame queryFrame = new AFrame();
      // Add Vinci Command, so that the receiving service knows what to do
      queryFrame.fadd("vinci:COMMAND", Constants.BATCH_PROCESS_COMPLETE);

      mVinciClient.send(queryFrame); // oneway call
    } catch (Exception e) {
      throw new ResourceServiceException(e);
    }
  }

  /**
   * @see CasObjectProcessor#collectionProcessComplete(org.apache.uima.util.ProcessTrace)
   */
  public void callCollectionProcessComplete() throws ResourceServiceException {
    try {
      // create Vinci Frame ( Data Cargo)
      AFrame queryFrame = new AFrame();
      // Add Vinci Command, so that the receiving service knows what to do
      queryFrame.fadd("vinci:COMMAND", Constants.COLLECTION_PROCESS_COMPLETE);

      // make RPC call (no return val)
      mVinciClient.rpc(queryFrame, mTimeout);
    } catch (Exception e) {
      throw new ResourceServiceException(e);
    }
  }

  /**
   * @see CasObjectProcessor#isReadOnly()
   */
  public boolean callIsReadOnly() throws ResourceServiceException {
    try {
      // create Vinci Frame ( Data Cargo)
      AFrame queryFrame = new AFrame();
      // Add Vinci Command, so that the receiving service knows what to do
      queryFrame.fadd("vinci:COMMAND", Constants.IS_READONLY);

      // make RPC call
      VinciFrame resultFrame = mVinciClient.rpc(queryFrame);
      boolean result = resultFrame.fgetBoolean("Result");
      return result;
    } catch (Exception e) {
      throw new ResourceServiceException(e);
    }
  }

  /**
   * @see CasObjectProcessor#isStateless()
   */
  public boolean callIsStateless() throws ResourceServiceException {
    try {
      // create Vinci Frame ( Data Cargo)
      AFrame queryFrame = new AFrame();
      // Add Vinci Command, so that the receiving service knows what to do
      queryFrame.fadd("vinci:COMMAND", Constants.IS_STATELESS);

      // make RPC call
      VinciFrame resultFrame = mVinciClient.rpc(queryFrame);
      boolean result = resultFrame.fgetBoolean("Result");
      return result;
    } catch (Exception e) {
      throw new ResourceServiceException(e);
    }
  }

  public List callGetSupportedXCasVersions() throws ResourceServiceException {
    try {
      // create Vinci Frame ( Data Cargo)
      AFrame queryFrame = new AFrame();
      // Add Vinci Command, so that the receiving service knows what to do
      queryFrame.fadd("vinci:COMMAND", Constants.GET_SUPPORTED_XCAS_VERSIONS);

      // make RPC call
      VinciFrame resultFrame = mVinciClient.rpc(queryFrame);
      String result = resultFrame.fgetString("Result");
      if (result != null) {
        String[] versions = result.split("\\s+");
        return Collections.unmodifiableList(Arrays.asList(versions));
      }
      else {
        return SUPPORT_XCAS_V1;
      }
    } catch (Exception e) {
      throw new ResourceServiceException(e);
    }
  }
  /**
   * Gets whether socket keepAlive is enabled, by consulting the
   * PerformanceTuningSettings.  (If no setting specified, defaults
   * to true.)
   * @return if socketKeepAlive is enabled
   */
  protected boolean isSocketKeepAliveEnabled() {
    if (mOwner instanceof AnalysisEngine) {
      Properties settings = ((AnalysisEngine)mOwner).getPerformanceTuningSettings();
      if (settings != null) {
        String enabledStr = (String)settings.get(UIMAFramework.SOCKET_KEEPALIVE_ENABLED);
        return !"false".equalsIgnoreCase(enabledStr);
      }
    }
    return true;
  }
}
