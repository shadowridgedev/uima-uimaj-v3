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

package org.apache.uima.resource.metadata.impl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLizable;

/**
 * Reference implementation of {@link TypeSystemDescription}.
 * 
 * 
 */
public class TypeSystemDescription_impl extends MetaDataObject_impl implements
        TypeSystemDescription {

  static final long serialVersionUID = -3372766232454730201L;

  private String mName;

  private String mVersion;

  private String mDescription;

  private String mVendor;

  private Import[] mImports = new Import[0];

  /** Descriptions of all Types in this type system. */
  private TypeDescription[] mTypes = new TypeDescription[0];

  /**
   * Creates a new TypeSystemDescription_impl.
   */
  public TypeSystemDescription_impl() {
  }

  /**
   * @see ResourceMetaData#getName()
   */
  public String getName() {
    return mName;
  }

  /**
   * @see ResourceMetaData#setName(String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * @see ResourceMetaData#getVersion()
   */
  public String getVersion() {
    return mVersion;
  }

  /**
   * @see ResourceMetaData#setVersion(String)
   */
  public void setVersion(String aVersion) {
    mVersion = aVersion;
  }

  /**
   * @see ResourceMetaData#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see ResourceMetaData#setDescription(String)
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see ResourceMetaData#getVendor()
   */
  public String getVendor() {
    return mVendor;
  }

  /**
   * @see ResourceMetaData#setVendor(String)
   */
  public void setVendor(String aVendor) {
    mVendor = aVendor;
  }

  /**
   * @see TypeSystemDescription#getImports()
   */
  public Import[] getImports() {
    return mImports;
  }

  /**
   * @see TypeSystemDescription#setImports(Import[])
   */
  public void setImports(Import[] aImports) {
    if (aImports == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aImports", "setImports" });
    }
    mImports = aImports;
  }

  /**
   * @see TypeSystemDescription#getTypes()
   */
  public TypeDescription[] getTypes() {
    return mTypes;
  }

  /**
   * @see TypeSystemDescription#setTypes(TypeDescription[])
   */
  public void setTypes(TypeDescription[] aTypes) {
    if (aTypes == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aTypes", "setTypes" });
    }
    mTypes = aTypes;
  }

  /**
   * @see TypeSystemDescription#addType(String, String, String)
   */
  public TypeDescription addType(String aTypeName, String aDescription, String aSupertypeName) {
    // create new type description
    TypeDescription newType = new TypeDescription_impl(aTypeName, aDescription, aSupertypeName);

    // add to array
    TypeDescription[] types = getTypes();
    if (types == null) {
      setTypes(new TypeDescription[] { newType });
    } else {
      TypeDescription[] newArray = new TypeDescription[types.length + 1];
      System.arraycopy(types, 0, newArray, 0, types.length);
      newArray[types.length] = newType;
      setTypes(newArray);
    }

    return newType;
  }

  /**
   * @see TypeSystemDescription#getType(java.lang.String)
   */
  public TypeDescription getType(String aTypeName) {
    for (int i = 0; i < mTypes.length; i++) {
      if (aTypeName.equals(mTypes[i].getName()))
        return mTypes[i];
    }
    return null;
  }

  /**
   * @see TypeSystemDescription#resolveImports()
   */
  public void resolveImports() throws InvalidXMLException {
    resolveImports(new TreeSet<String>(), UIMAFramework.newDefaultResourceManager());
  }

  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException {
    resolveImports(new TreeSet<String>(), aResourceManager);
  }

  public void resolveImports(Collection<String> aAlreadyImportedTypeSystemURLs,
          ResourceManager aResourceManager) throws InvalidXMLException {
    // add our own URL, if known, to the collection of already imported URLs
    if (getSourceUrl() != null) {
      aAlreadyImportedTypeSystemURLs.add(getSourceUrl().toString());
    }

    List<TypeDescription> importedTypes = new ArrayList<TypeDescription>();
    Import[] imports = getImports();
    for (int i = 0; i < imports.length; i++) {
      // make sure Import's relative path base is set, to allow for users who create
      // new import objects
      if (imports[i] instanceof Import_impl) {
        ((Import_impl) imports[i]).setSourceUrlIfNull(this.getSourceUrl());
      }
      URL url = imports[i].findAbsoluteUrl(aResourceManager);
      if (!aAlreadyImportedTypeSystemURLs.contains(url.toString())) {
        aAlreadyImportedTypeSystemURLs.add(url.toString());
        try {
          resolveImport(url, aAlreadyImportedTypeSystemURLs, importedTypes, aResourceManager);
        } catch (IOException e) {
          throw new InvalidXMLException(InvalidXMLException.IMPORT_FAILED_COULD_NOT_READ_FROM_URL,
                  new Object[] { url, imports[i].getSourceUrlString() }, e);
        }
      }
    }
    // update this object
    TypeDescription[] existingTypes = this.getTypes();
    if (existingTypes == null) {
      existingTypes = new TypeDescription[0];
    }
    TypeDescription[] newTypes = new TypeDescription[existingTypes.length + importedTypes.size()];
    System.arraycopy(existingTypes, 0, newTypes, 0, existingTypes.length);
    for (int i = 0; i < importedTypes.size(); i++) {
      newTypes[existingTypes.length + i] = (TypeDescription) importedTypes.get(i);
    }
    this.setTypes(newTypes);

    // clear imports
    this.setImports(new Import[0]);
  }

  private void resolveImport(URL aURL, Collection<String> aAlreadyImportedTypeSystemURLs,
          Collection<TypeDescription> aResults, ResourceManager aResourceManager) throws InvalidXMLException,
          IOException {
    //check the import cache
    TypeSystemDescription desc;    
    String urlString = aURL.toString();
    XMLizable cachedObject = aResourceManager.getImportCache().get(urlString);
    if (cachedObject instanceof TypeSystemDescription) {
      desc = (TypeSystemDescription)cachedObject;
    } else {   
      XMLInputSource input;
      input = new XMLInputSource(aURL);
      desc = UIMAFramework.getXMLParser().parseTypeSystemDescription(input);
      desc.resolveImports(aAlreadyImportedTypeSystemURLs, aResourceManager);
      aResourceManager.getImportCache().put(urlString, desc);
    }
    aResults.addAll(Arrays.asList(desc.getTypes()));
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("typeSystemDescription",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name", true),
              new PropertyXmlInfo("description", true), new PropertyXmlInfo("version", true),
              new PropertyXmlInfo("vendor", true), new PropertyXmlInfo("imports", true),
              new PropertyXmlInfo("types", true) });
}
