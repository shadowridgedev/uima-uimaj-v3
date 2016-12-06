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

package org.apache.uima.tools.jcasgen;

import java.io.File;
import java.io.IOException;


/**
 * The Interface IMerge.
 */
public interface IMerge {
  
  /**
   * Do merge.
   *
   * @param jg the jg
   * @param progressMonitor the progress monitor
   * @param sourceContents the source contents
   * @param targetContainer the target container
   * @param targetPath the target path
   * @param targetClassName the target class name
   * @param targetFile the target file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void doMerge(Jg jg, IProgressMonitor progressMonitor, String sourceContents,
          String targetContainer, String targetPath, String targetClassName, File targetFile)
          throws IOException;
}
