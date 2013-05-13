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

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.admin.CASMgr;

public class Serialization {

  public static CASSerializer serializeCAS(CAS cas) {
    CASSerializer ser = new CASSerializer();
    ser.addCAS((CASImpl) cas);
    return ser;
  }

  public static CASSerializer serializeNoMetaData(CAS cas) {
    CASSerializer ser = new CASSerializer();
    ser.addNoMetaData((CASImpl) cas);
    return ser;
  }

  public static CASMgrSerializer serializeCASMgr(CASMgr casMgr) {
    CASMgrSerializer ser = new CASMgrSerializer();
    ser.addTypeSystem((TypeSystemImpl) casMgr.getCAS().getTypeSystem());
    ser.addIndexRepository((FSIndexRepositoryImpl) ((CASImpl) casMgr.getCAS())
            .getBaseIndexRepository());
    return ser;
  }

  public static CASCompleteSerializer serializeCASComplete(CASMgr casMgr) {
    return new CASCompleteSerializer((CASImpl) casMgr);
  }

  public static void deserializeCASComplete(CASCompleteSerializer casCompSer, CASMgr casMgr) {
    ((CASImpl) casMgr).reinit(casCompSer);
  }

  public static CASMgr createCASMgr(CASMgrSerializer ser) {
    return new CASImpl(ser);
  }

  // public static CASMgr createCASMgr(CASMgrSerializer ser) {
  // return new CASImpl(ser);
  // }

  public static CAS createCAS(CASMgr casMgr, CASSerializer casSer) {
    ((CASImpl) casMgr).reinit(casSer);
    return ((CASImpl) casMgr).getCurrentView();
  }

  public static void serializeCAS(CAS cas, OutputStream ostream) {
    CASSerializer ser = new CASSerializer();
    ser.addCAS((CASImpl) cas, ostream);
  }

  public static SerialFormat deserializeCAS(CAS cas, InputStream istream) {
    return ((CASImpl) cas).reinit(istream);
  }

  /**
   * Serializes CAS data added or modified after the tracking Marker was created and writes it
   * to the output stream in Delta CAS format
   * @param cas
   * @param ostream
   * @param mark
   */
  public static void serializeCAS(CAS cas, OutputStream ostream, Marker mark) {
  	if (!mark.isValid() ) {
  	  throw new CASRuntimeException(CASRuntimeException.INVALID_MARKER);
  	}
  	CASSerializer ser = new CASSerializer();
  	ser.addCAS((CASImpl) cas, ostream, mark);
  }

}
