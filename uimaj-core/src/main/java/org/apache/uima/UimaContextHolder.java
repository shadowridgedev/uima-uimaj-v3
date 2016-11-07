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

package org.apache.uima;

/**
 * This class holds the UimaContext for the current thread, or a parent thread.
 * The getContext method may be used by any plain Java class invoked by an annotator,
 * The POJO must run in the same thread or a child thread of the annotator.
 * 
 * For example a POJO can access the shared External Override Settings with:
 *     String paramValue = UimaContextHolder.getContext().getSetting(paramName);
 */
public class UimaContextHolder {
  
  private static InheritableThreadLocal<UimaContext> threadLocalContext = new InheritableThreadLocal<UimaContext>();
  
  /**
   * Get the UimaContext for this thread
   * 
   * @return      the thread-specific UimaContext
   */
  public static UimaContext getContext() {
    return threadLocalContext.get();
  }
  
  /**
   * Sets the UimaContext for the current thread.
   * <p>
   * NOTE - Should be used only by the UIMA Framework.
   * 
   * @param uimaContext -
   */
  public static void setContext(UimaContext uimaContext) {
    threadLocalContext.set(uimaContext);;
  }
  
  /**
   * Clears the UimaContext entry for the current thread
   * <p>
   * NOTE - Should be used only by the UIMA Framework.
   */
  public static void clearContext() {
    threadLocalContext.set(null);
  }
}
