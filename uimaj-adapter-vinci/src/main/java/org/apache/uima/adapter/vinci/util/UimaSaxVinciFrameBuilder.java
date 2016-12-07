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

package org.apache.uima.adapter.vinci.util;

import org.xml.sax.Attributes;

// TODO: Auto-generated Javadoc
/**
 * A specialization of the general <code>SaxVinciFrameBuilder</code> utility for UIMA analysis
 * engine services. Adds two options:
 * <ul>
 * <li>Supress document text</li>
 * <li>Include spanned text as character content of all annotation FSs</li>
 * </ul>
 * 
 * 
 */
public class UimaSaxVinciFrameBuilder extends SaxVinciFrameBuilder {
  
  /**
   * Instantiates a new uima sax vinci frame builder.
   *
   * @param aSupressDocumentText the a supress document text
   * @param aIncludeSpannedTextInAnnotations the a include spanned text in annotations
   * @param aDocText the a doc text
   */
  public UimaSaxVinciFrameBuilder(boolean aSupressDocumentText,
          boolean aIncludeSpannedTextInAnnotations, String aDocText) {
    mSupressDocumentText = aSupressDocumentText;
    mIncludeSpannedTextInAnnotations = aIncludeSpannedTextInAnnotations;
    mDocText = aDocText;
  }

  /** The m supress document text. */
  private boolean mSupressDocumentText;

  /** The m include spanned text in annotations. */
  private boolean mIncludeSpannedTextInAnnotations;

  /** The m doc text. */
  private String mDocText;

  /**
   * Overridden to supress document content and include annotation spans.
   *
   * @param aFrameName the a frame name
   * @param aAttributes the a attributes
   * @param aContentBuf the a content buf
   * @return the leaf content
   * @see SaxVinciFrameBuilder#getLeafContent(String, Attributes, StringBuffer)
   */
  protected String getLeafContent(String aFrameName, Attributes aAttributes,
          StringBuffer aContentBuf) {
    // supress documen text if requested
    if ("uima.tcas.Document".equals(aFrameName)
            || "uima.tcas.DocumentAnnotation".equals(aFrameName)) {
      if (mSupressDocumentText)
        return "";
    } else if (mIncludeSpannedTextInAnnotations && aContentBuf.length() == 0 && mDocText != null) {
      // attempt to extract text fron begin,end feature values
      String begin = aAttributes.getValue("begin");
      String end = aAttributes.getValue("end");
      if (begin != null && end != null) {
        try {
          int b = Integer.parseInt(begin);
          int e = Integer.parseInt(end);
          return mDocText.substring(b, e);
        } catch (Exception e) {
          // just ignore and use default behavior
        }
      }
    }
    // default to superclass behavior
    return super.getLeafContent(aFrameName, aAttributes, aContentBuf);
  }

}
