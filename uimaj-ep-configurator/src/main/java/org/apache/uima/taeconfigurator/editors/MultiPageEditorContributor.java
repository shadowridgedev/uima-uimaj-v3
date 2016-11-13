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

package org.apache.uima.taeconfigurator.editors;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.IDEActionFactory;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.apache.uima.taeconfigurator.Messages;
import org.apache.uima.taeconfigurator.PreferencePage;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.xml.XMLEditor;

// TODO: Auto-generated Javadoc
// import org.eclipse.jdt.launching.IVMRunner;

// import org.apache.uima.jcas.jcasgen.Prefs;

/**
 * Manages the installation/deinstallation of global actions for multi-page editors. Responsible for
 * the redirection of global actions to the active editor. Multi-page contributor replaces the
 * contributors for the individual editors in the multi-page editor.
 */
public class MultiPageEditorContributor extends MultiPageEditorActionBarContributor {
  
  /** The active editor part. */
  private IEditorPart activeEditorPart;

  /** The auto J cas action. */
  Action autoJCasAction;
  
  /** The limit J cas gen to project. */
  Action limitJCasGenToProject;

  /** The qualified types action. */
  Action qualifiedTypesAction;

  /** The run J cas gen action. */
  Action runJCasGenAction;

  /**
   * Creates a multi-page contributor.
   */
  public MultiPageEditorContributor() {
    super();
    createActions();
  }

  /**
   * Returns the action registed with the given text editor.
   *
   * @param editor the editor
   * @param actionID the action ID
   * @return IAction or null if editor is null.
   */
  protected IAction getAction(MultiPageEditorPart editor, String actionID) {
    ITextEditor txtEditor = ((MultiPageEditor) editor).getSourcePageEditor();
    return (txtEditor == null ? null : txtEditor.getAction(actionID));
  }

  /**
   * Gets the action 1.
   *
   * @param editor the editor
   * @param actionID the action ID
   * @return the action 1
   */
  protected IAction getAction1(ITextEditor editor, String actionID) {
    return (editor == null ? null : editor.getAction(actionID));
  }

  /*
   * (non-JavaDoc) Method declared in AbstractMultiPageEditorActionBarContributor.
   */

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.MultiPageEditorActionBarContributor#setActiveEditor(org.eclipse.ui.IEditorPart)
   */
  @Override
  public void setActiveEditor(IEditorPart part) {
    if (activeEditorPart == part)
      return;

    if (null == part)
      return;
    activeEditorPart = part;

    IActionBars actionBars = getActionBars();
    if (actionBars != null) {

      MultiPageEditorPart editor = (MultiPageEditorPart) part;

      actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), getAction(editor,
              ITextEditorActionConstants.DELETE));
      actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), getAction(editor,
              ITextEditorActionConstants.UNDO));
      actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), getAction(editor,
              ITextEditorActionConstants.REDO));
      actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), getAction(editor,
              ITextEditorActionConstants.CUT));
      actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), getAction(editor,
              ITextEditorActionConstants.COPY));
      actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), getAction(editor,
              ITextEditorActionConstants.PASTE));
      actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), getAction(editor,
              ITextEditorActionConstants.SELECT_ALL));
      actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), getAction(editor,
              ITextEditorActionConstants.FIND));
      actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(), getAction(editor,
              IDEActionFactory.BOOKMARK.getId()));
      actionBars.updateActionBars();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.part.MultiPageEditorActionBarContributor#setActivePage(org.eclipse.ui.IEditorPart)
   */
  @Override
  public void setActivePage(IEditorPart part) {

    IActionBars actionBars = getActionBars();
    if (actionBars != null) {

      ITextEditor textEditor = (part instanceof XMLEditor) ? (ITextEditor) part : null;

      actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), getAction1(textEditor,
              ITextEditorActionConstants.DELETE));
      actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), getAction1(textEditor,
              ITextEditorActionConstants.UNDO));
      actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), getAction1(textEditor,
              ITextEditorActionConstants.REDO));
      actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), getAction1(textEditor,
              ITextEditorActionConstants.CUT));
      actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), getAction1(textEditor,
              ITextEditorActionConstants.COPY));
      actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), getAction1(textEditor,
              ITextEditorActionConstants.PASTE));
      actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), getAction1(textEditor,
              ITextEditorActionConstants.SELECT_ALL));
      actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), getAction1(textEditor,
              ITextEditorActionConstants.FIND));
      actionBars.setGlobalActionHandler(IDEActionFactory.BOOKMARK.getId(), getAction1(textEditor,
              IDEActionFactory.BOOKMARK.getId()));
      actionBars.updateActionBars();
    }
  }

  /**
   * Creates the actions.
   */
  private void createActions() {

    autoJCasAction = new Action() {
      // The run action is simply to toggle the setting in the prefs page, and
      //   to update the checked status to correspond to that
      @Override
      public void run() {
        TAEConfiguratorPlugin plugin = TAEConfiguratorPlugin.getDefault();
        Preferences prefs = plugin.getPluginPreferences();
        boolean bAutoJCasGen = !prefs.getBoolean(PreferencePage.P_JCAS); 
        autoJCasAction.setChecked(bAutoJCasGen);
        prefs.setValue(PreferencePage.P_JCAS, bAutoJCasGen); 
      }
    };
    
    limitJCasGenToProject = new Action () {
      // The run action is simply to toggle the setting in the prefs page, and
      //   to update the checked status to correspond to that
      @Override
      public void run() {
        TAEConfiguratorPlugin plugin = TAEConfiguratorPlugin.getDefault();
        Preferences prefs = plugin.getPluginPreferences();
        boolean bJCasLimit = !prefs.getBoolean(PreferencePage.P_JCAS_LIMIT_TO_PROJECT_SCOPE); 
        limitJCasGenToProject.setChecked(bJCasLimit);
        prefs.setValue(PreferencePage.P_JCAS_LIMIT_TO_PROJECT_SCOPE, bJCasLimit); 
      }
    };

    runJCasGenAction = new Action() {
      @Override
      public void run() {
        ((MultiPageEditor) activeEditorPart).doJCasGenChkSrc(null); // don't know how to get
        // progress monitor
      }
    };

    qualifiedTypesAction = new Action() {
      @Override
      public void run() {
        TAEConfiguratorPlugin plugin = TAEConfiguratorPlugin.getDefault();
        Preferences prefs = plugin.getPluginPreferences();
        boolean bFullyQualifiedTypeNames = !prefs
                .getBoolean(PreferencePage.P_SHOW_FULLY_QUALIFIED_NAMES); 
        qualifiedTypesAction.setChecked(bFullyQualifiedTypeNames);
        prefs.setValue(PreferencePage.P_SHOW_FULLY_QUALIFIED_NAMES, bFullyQualifiedTypeNames); 

        // mark all pages as stale for all editors, since this is a global setting
        IWorkbenchPage[] pages = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages();
        for (int i = 0; i < pages.length; i++) {
          IWorkbenchPart[] editors = pages[i].getEditors();
          for (int j = 0; j < editors.length; j++) {
            if (editors[j] != null && editors[j] instanceof MultiPageEditor) {
              ((MultiPageEditor) editors[j]).markAllPagesStale();
            }
          }
        }

      }
    };

    autoJCasAction.setText(Messages.getString("MultiPageEditorContributor.autoGenJCas")); //$NON-NLS-1$
    autoJCasAction.setChecked(getAutoJCasGen()); 
    
    limitJCasGenToProject.setText(Messages.getString("MultiPageEditorContributor.limitJCasGenToProjectScope"));
    limitJCasGenToProject.setChecked(getLimitJCasGenToProjectScope());

    qualifiedTypesAction.setText(Messages.getString("MultiPageEditorContributor.showFullNames")); //$NON-NLS-1$
    qualifiedTypesAction.setChecked(getUseQualifiedTypes()); 

    runJCasGenAction.setText("Run JCasGen");
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(org.eclipse.jface.action.IMenuManager)
   */
  @Override
  public void contributeToMenu(IMenuManager manager) {

    IMenuManager menu = new MenuManager("&UIMA"); //$NON-NLS-1$
    manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
    menu.add(runJCasGenAction);
    IMenuManager settingsMenu = new MenuManager("Settings"); //$NON-NLS-1$
    menu.add(settingsMenu);
    settingsMenu.add(autoJCasAction);
    settingsMenu.add(qualifiedTypesAction);
    settingsMenu.add(limitJCasGenToProject);
  }

  /**
   * Gets the auto J cas gen.
   *
   * @return the auto J cas gen
   */
  public static boolean getAutoJCasGen() {
    return getUimaPrefBoolean(PreferencePage.P_JCAS, false); // Jira UIMA-1177
  }

  /**
   * Gets the use qualified types.
   *
   * @return the use qualified types
   */
  public static boolean getUseQualifiedTypes() {
    return getUimaPrefBoolean(PreferencePage.P_SHOW_FULLY_QUALIFIED_NAMES, true);
  }
  
  /**
   * Gets the limit J cas gen to project scope.
   *
   * @return the limit J cas gen to project scope
   */
  public static boolean getLimitJCasGenToProjectScope() {
    return getUimaPrefBoolean(PreferencePage.P_JCAS_LIMIT_TO_PROJECT_SCOPE, false);
  }

  /**
   * Gets the XM lindent.
   *
   * @return the XM lindent
   */
  public static int getXMLindent() {
    return getUimaPrefInt(PreferencePage.P_XML_TAB_SPACES, 2);
  }

  /**
   * Gets the CDE vns host.
   *
   * @return the CDE vns host
   */
  public static String getCDEVnsHost() {
    return getUimaPrefString(PreferencePage.P_VNS_HOST, "localhost");
  }

  /**
   * Gets the CDE vns port.
   *
   * @return the CDE vns port
   */
  public static String getCDEVnsPort() {
    return getUimaPrefString(PreferencePage.P_VNS_PORT, "9000");
  }

  /**
   * Sets the vns host.
   *
   * @param v the new vns host
   */
  public static void setVnsHost(String v) {
    System.setProperty("VNS_HOST", v);
  }

  /**
   * Sets the vns port.
   *
   * @param v the new vns port
   */
  public static void setVnsPort(String v) {
    System.setProperty("VNS_PORT", v);
  }

  /**
   * Sets the vns host.
   */
  public static void setVnsHost() {
    setVnsHost(getCDEVnsHost());
  }

  /**
   * Sets the vns port.
   */
  public static void setVnsPort() {
    setVnsPort(getCDEVnsPort());
  }

  /**
   * Gets the uima pref string.
   *
   * @param key the key
   * @param defaultValue the default value
   * @return the uima pref string
   */
  private static String getUimaPrefString(String key, String defaultValue) {
    TAEConfiguratorPlugin plugin = TAEConfiguratorPlugin.getDefault();
    Preferences prefs = plugin.getPluginPreferences();
    boolean isDefault = prefs.isDefault(key);
    if (isDefault)
      prefs.setDefault(key, defaultValue);
    return prefs.getString(key);
  }

  /**
   * Gets the uima pref boolean.
   *
   * @param key the key
   * @param defaultValue the default value
   * @return the uima pref boolean
   */
  private static boolean getUimaPrefBoolean(String key, boolean defaultValue) {
    TAEConfiguratorPlugin plugin = TAEConfiguratorPlugin.getDefault();
    Preferences prefs = plugin.getPluginPreferences();
    boolean isDefault = prefs.isDefault(key);
    if (isDefault)
      prefs.setDefault(key, defaultValue);
    return prefs.getBoolean(key);
  }

  /**
   * Gets the uima pref int.
   *
   * @param key the key
   * @param defaultValue the default value
   * @return the uima pref int
   */
  private static int getUimaPrefInt(String key, int defaultValue) {
    TAEConfiguratorPlugin plugin = TAEConfiguratorPlugin.getDefault();
    Preferences prefs = plugin.getPluginPreferences();
    boolean isDefault = prefs.isDefault(key);
    if (isDefault)
      prefs.setDefault(key, defaultValue);
    return prefs.getInt(key);
  }

}
