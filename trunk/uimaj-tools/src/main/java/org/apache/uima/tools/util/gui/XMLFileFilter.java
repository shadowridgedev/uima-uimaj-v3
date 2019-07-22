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

package org.apache.uima.tools.util.gui;

import java.io.File;

import javax.swing.filechooser.FileFilter;


/**
 * File filter to accept only xml files (.xml extension)
 */
public class XMLFileFilter extends FileFilter {
  
  /** The Constant XML. */
  private static final String XML = "xml";

  /* (non-Javadoc)
   * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
   */
  @Override
  public boolean accept(File file) {
    if (file.isDirectory())
      return true;
    else {
      String extension = getExtension(file);
      if (extension != null)
        return (extension.equals(XML));
      else
        return false;
    }
  }

  /* (non-Javadoc)
   * @see javax.swing.filechooser.FileFilter#getDescription()
   */
  @Override
  public String getDescription() {
    return ".xml";
  }

  /**
   * Gets the extension.
   *
   * @param f the f
   * @return the extension
   */
  private String getExtension(File f) {
    String ext = null;
    String s = f.getName();
    int i = s.lastIndexOf('.');

    if (i > 0 && i < s.length() - 1)
      ext = s.substring(i + 1).toLowerCase();

    return ext;
  }
}
