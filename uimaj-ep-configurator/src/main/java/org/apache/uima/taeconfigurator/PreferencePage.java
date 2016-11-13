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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

// TODO: Auto-generated Javadoc
/**
 * The Class PreferencePage.
 */
public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
  
  /** The Constant P_JCAS. */
  public static final String P_JCAS = "org.apache.uima.cde.autojcasgen";
  
  /** The Constant P_JCAS_LIMIT_TO_PROJECT_SCOPE. */
  public static final String P_JCAS_LIMIT_TO_PROJECT_SCOPE = "org.apache.uima.cde.jcasgen.limittoprojectscope";

  /** The Constant P_SHOW_FULLY_QUALIFIED_NAMES. */
  public static final String P_SHOW_FULLY_QUALIFIED_NAMES = "org.apache.uima.cde.qualifiedtypes";

  /** The Constant P_XML_TAB_SPACES. */
  public static final String P_XML_TAB_SPACES = "org.apache.uima.cde.xmlIndentAmount";

  /** The Constant P_VNS_HOST. */
  public static final String P_VNS_HOST = "org.apache.uima.cde.vnsHost";

  /** The Constant P_VNS_PORT. */
  public static final String P_VNS_PORT = "org.apache.uima.cde.vnsPort";

  /**
   * Instantiates a new preference page.
   */
  public PreferencePage() {
    super(GRID);
    setPreferenceStore(TAEConfiguratorPlugin.getDefault().getPreferenceStore());
    setDescription("UIMA Component Descriptor Editor Preferences");
    initializeDefaults();
  }

  /**
   * Sets the default values of the preferences.
   */
  private void initializeDefaults() {
    IPreferenceStore store = getPreferenceStore();
    store.setDefault(P_JCAS, false);
    store.setDefault(P_SHOW_FULLY_QUALIFIED_NAMES, true);
    store.setDefault(P_XML_TAB_SPACES, 2);
    store.setDefault(P_VNS_HOST, "localhost");
    store.setDefault(P_VNS_PORT, "9000");
    // store.setDefault(P_DATA_PATH, "");
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
   */
  @Override
  public void createFieldEditors() {
    addField(new BooleanFieldEditor(P_JCAS, "&Automatically run JCasGen when Types change",
            getFieldEditorParent()));

    addField(new BooleanFieldEditor(P_JCAS_LIMIT_TO_PROJECT_SCOPE, "&Limit JCasGen to types defined in this project",
        getFieldEditorParent()));

    addField(new BooleanFieldEditor(P_SHOW_FULLY_QUALIFIED_NAMES, "&Show fully qualified names",
            getFieldEditorParent()));

    addField(new IntegerFieldEditor(P_XML_TAB_SPACES, "&XML indentation", getFieldEditorParent()));

    addField(new StringFieldEditor(P_VNS_HOST, "&Vinci Name Service Host IP address",
            getFieldEditorParent()));

    addField(new StringFieldEditor(P_VNS_PORT, "Vinci NameService &Port number",
            getFieldEditorParent()));
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  @Override
  public void init(IWorkbench workbench) {
  }
}
