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

package org.apache.uima.adapter.soap.axis11;

import java.io.File;

import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.DeserializerImpl;
import org.apache.axis.message.SOAPHandler;
import org.apache.uima.UIMAFramework;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLizable;
import org.apache.uima.util.impl.SaxDeserializer_impl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.LocatorImpl;

// TODO: Auto-generated Javadoc
/**
 * An Axis deserializer for any {@link XMLizable} object.
 * <p>
 * This class works only under Axis v1.1.
 */
public class XmlDeserializer_Axis11 extends DeserializerImpl {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 6687245569965318456L;

  /** The m deser. */
  private SaxDeserializer_impl mDeser;

  /** The m nesting. */
  private int mNesting;

  /**
   * Instantiates a new xml deserializer axis 11.
   */
  public XmlDeserializer_Axis11() {
    try {
      mNesting = 0;
      mDeser = new SaxDeserializer_impl(UIMAFramework.getXMLParser(), null, null,
              new XMLParser.ParsingOptions(false));
      LocatorImpl loc = new LocatorImpl();
      loc.setSystemId(new File(System.getProperty("user.dir")).toURL().toString());
      mDeser.setDocumentLocator(loc);
      mDeser.startDocument();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Characters.
   *
   * @param ch the ch
   * @param start the start
   * @param length the length
   * @throws SAXException the SAX exception
   * @see org.xml.sax.ContentHandler#characters(char[], int, int)
   */
  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    // System.out.println("AxisResourceServiceDeserializer::characters(" + new
    // String(ch,start,length) + ")");
    mDeser.characters(ch, start, length);
  }

  /**
   * On end element.
   *
   * @param arg0 the arg 0
   * @param arg1 the arg 1
   * @param arg2 the arg 2
   * @throws SAXException the SAX exception
   * @see org.apache.axis.encoding.Deserializer#onEndElement(java.lang.String, java.lang.String,
   *      org.apache.axis.encoding.DeserializationContext)
   */
  @Override
  public void onEndElement(String arg0, String arg1, DeserializationContext arg2)
          throws SAXException {
    // System.out.println("AxisResourceServiceDeserializer::onEndElement(" + arg0 + "," + arg1);
    mNesting--;

    if (mNesting > 0) {
      mDeser.endElement(arg0, arg1, arg1);
    }
  }

  /**
   * This method is invoked when an element start tag is encountered.
   *
   * @param namespace          is the namespace of the element
   * @param localName          is the name of the element
   * @param prefix          is the element's prefix
   * @param attributes          are the attributes on the element...used to get the type
   * @param context          is the DeserializationContext
   * @return the SOAP handler
   * @throws SAXException the SAX exception
   */
  @Override
  public SOAPHandler onStartChild(String namespace, String localName, String prefix,
          Attributes attributes, DeserializationContext context) throws SAXException {
    return this;
  }

  /**
   * On start element.
   *
   * @param arg0 the arg 0
   * @param arg1 the arg 1
   * @param arg2 the arg 2
   * @param arg3 the arg 3
   * @param arg4 the arg 4
   * @throws SAXException the SAX exception
   * @see org.apache.axis.message.SOAPHandler#onStartChild(String, String, String, Attributes, DeserializationContext)
   */
  @Override
  public void onStartElement(String arg0, String arg1, String arg2, Attributes arg3,
          DeserializationContext arg4) throws SAXException {
    // System.out.println("AxisResourceServiceDeserializer::onStartElement(" + arg0 + "," + arg1 +
    // "," + arg2 + ")");

    // don't process the topmost element - it is a SOAP "multiRef" element and
    // we don't care about that
    if (mNesting > 0) {
      mDeser.startElement(arg0, arg1, arg1, arg3);
    }
    mNesting++;
  }

  /**
   * Value complete.
   *
   * @throws SAXException the SAX exception
   * @see org.apache.axis.encoding.Deserializer#valueComplete()
   */
  @Override
  public void valueComplete() throws SAXException {
    // System.out.println("AxisResourceServiceDeserializer::valueComplete");
    if (mNesting == 0) {
      // System.out.println("Building value");
      mDeser.endDocument();
      try {
        Object val = mDeser.getObject();
        // System.out.println("Value is: " + val);
        // value = val;
        setValue(val);
      } catch (InvalidXMLException e) {
        e.printStackTrace();
        setValue(null);
      }

      // call superclass to register the deserialized object
      super.valueComplete();
    }
  }
}
