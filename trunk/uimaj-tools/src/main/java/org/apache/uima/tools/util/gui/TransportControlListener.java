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


/**
 * The listener interface for receiving transportControl events.
 * The class that is interested in processing a transportControl
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addTransportControlListener</code> method. When
 * the transportControl event occurs, that object's appropriate
 * method is invoked.
 *
 */
public interface TransportControlListener {
  
  /**
   * Control started.
   */
  public void controlStarted();

  /**
   * Control paused.
   */
  public void controlPaused();

  /**
   * Control resumed.
   */
  public void controlResumed(); // Following pause.

  /**
   * Control stopped.
   */
  public void controlStopped();
}
