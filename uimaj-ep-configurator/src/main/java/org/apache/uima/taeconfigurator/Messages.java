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

package org.apache.uima.taeconfigurator;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

// TODO: Auto-generated Javadoc
/**
 * The Class Messages.
 */
public class Messages {
  
  /** The Constant BUNDLE_NAME. */
  private static final String BUNDLE_NAME = "org.apache.uima.taeconfigurator.messages";//$NON-NLS-1$

  /** The Constant RESOURCE_BUNDLE. */
  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

  /**
   * Instantiates a new messages.
   */
  private Messages() {
  }

  /**
   * Gets the string.
   *
   * @param key the key
   * @return the string
   */
  public static String getString(String key) {
    try {
      return RESOURCE_BUNDLE.getString(key);
    } catch (MissingResourceException e) {
      return '!' + key + '!';
    }
  }

  /**
   * Gets the formatted string.
   *
   * @param key the key
   * @param args the args
   * @return the formatted string
   */
  public static String getFormattedString(String key, String[] args) {
    return MessageFormat.format(RESOURCE_BUNDLE.getString(key), args);
  }

}
