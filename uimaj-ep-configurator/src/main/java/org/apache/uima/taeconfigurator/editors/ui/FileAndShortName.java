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

package org.apache.uima.taeconfigurator.editors.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

// TODO: Auto-generated Javadoc
/**
 * The Class FileAndShortName.
 */
public class FileAndShortName {
  
  /** The file name. */
  public String fileName;

  /** The short name. */
  public String shortName;

  /**
   * Instantiates a new file and short name.
   *
   * @param o the o
   */
  public FileAndShortName(Object o) {

    if (o instanceof IFile) {
      IFile file = (IFile) o;
      fileName = file.getLocation().toString();
      shortName = file.getName();
    } else if (o instanceof String) {
        fileName = (String)o;
        int lastSlash = fileName.lastIndexOf('/');
        shortName = (lastSlash >= 0) ? fileName.substring(lastSlash + 1) : fileName;
      } else {
      IPath path = (IPath) o;
      fileName = path.toString();
      shortName = path.toFile().getName();
    }
  }
}
