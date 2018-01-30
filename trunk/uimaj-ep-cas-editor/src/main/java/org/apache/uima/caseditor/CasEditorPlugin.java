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

package org.apache.uima.caseditor;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * TODO: add javadoc here.
 */
public class CasEditorPlugin extends AbstractUIPlugin {
  
  /** The Constant ID. */
  public static final String ID = "org.apache.uima.caseditor";

  /** The Constant ICONS_PATH. */
  private static final String ICONS_PATH = "icons/";

  /**
   * The shared instance.
   */
  private static CasEditorPlugin sPlugin;

  /**
   * Resource bundle.
   */
  private ResourceBundle mResourceBundle;

  
  /** The show migration dialog. */
  private boolean showMigrationDialog = false;
  
  /**
   * The constructor.
   */
  public CasEditorPlugin() {
    super();

    sPlugin = this;
  }

  /**
   * This method is called upon plug-in activation.
   *
   * @param context the context
   * @throws Exception -
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
  }

  /**
   * This method is called when the plug-in is stopped.
   *
   * @param context the context
   * @throws Exception -
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

    sPlugin = null;
    mResourceBundle = null;
  }

  /**
   * Returns the shared instance.
   *
   * @return the TaePlugin
   */
  public static CasEditorPlugin getDefault() {
    return sPlugin;
  }

  /**
   * Returns the string from the plugin's resource bundle, or 'key' if not found.
   *
   * @param key the key
   * @return resource string
   */
  public static String getResourceString(String key) {
    ResourceBundle bundle = getDefault().getResourceBundle();

    try {
      return bundle != null ? bundle.getString(key) : key;
    } catch (MissingResourceException e) {
      return key;
    }
  }

  /**
   * Returns the plugin's resource bundle.
   *
   * @return the ResourceBbundle or null if missing
   */
  public ResourceBundle getResourceBundle() {
    try {
      if (mResourceBundle == null) {
        mResourceBundle = ResourceBundle.getBundle("Annotator.AnnotatorPluginResources");
      }
    } catch (MissingResourceException x) {
      mResourceBundle = null;
    }

    return mResourceBundle;
  }

  /**
   * Log the throwable.
   *
   * @param t the t
   */
  public static void log(Throwable t) {
    getDefault().getLog().log(new Status(IStatus.ERROR, ID, IStatus.OK, t.getMessage(), t));
  }
  
  /**
   * Log.
   *
   * @param message the message
   * @param t the t
   */
  public static void log(String message, Throwable t) {
    getDefault().getLog().log(new Status(IStatus.ERROR, ID, IStatus.OK, message, t));
  }

  /**
   * Log error.
   *
   * @param message the message
   */
  public static void logError(String message) {
    getDefault().getLog().log(new Status(IStatus.ERROR, ID, message));
  }

  /**
   * Retrieves an image.
   *
   * @param image the image
   * @return the requested image if not available null
   */
  public static ImageDescriptor getTaeImageDescriptor(Images image) {
    return imageDescriptorFromPlugin(ID, ICONS_PATH + image.getPath());
  }
  
  /**
   * Sets the show migration dialog.
   */
  public void setShowMigrationDialog() {
    showMigrationDialog = true;
  }
  
  /**
   * Gets the and clear show migration dialog flag.
   *
   * @return the and clear show migration dialog flag
   */
  public boolean getAndClearShowMigrationDialogFlag() {
    if (showMigrationDialog) {
      showMigrationDialog = false;
      return true;
    }
    
    return false;
  }
  
}
