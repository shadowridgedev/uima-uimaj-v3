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

package org.apache.uima.tools.cvd;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.uima.internal.util.CommandLineParser;
import org.apache.uima.resource.RelativePathResolver;
import org.apache.uima.tools.images.Images;

// TODO: Auto-generated Javadoc
/**
 * The main class for the CAS Visual Debugger.
 * 
 * 
 */
public class CVD {

  /** The Constant MAN_PATH_PROPERTY. */
  public static final String MAN_PATH_PROPERTY = "uima.tools.cvd.manpath";

  /** The Constant TEXT_FILE_PARAM. */
  private static final String TEXT_FILE_PARAM = "-text";

  /** The Constant DESC_FILE_PARAM. */
  private static final String DESC_FILE_PARAM = "-desc";

  /** The Constant EXECUTE_SWITCH. */
  private static final String EXECUTE_SWITCH = "-exec";

  /** The Constant DATA_PATH_PARAM. */
  private static final String DATA_PATH_PARAM = "-datapath";

  /** The Constant INI_FILE_PARAM. */
  private static final String INI_FILE_PARAM = "-ini";

  /** The Constant LOOK_AND_FEEL_PARAM. */
  private static final String LOOK_AND_FEEL_PARAM = "-lookandfeel";
  
  /** The Constant XMI_FILE_PARAM. */
  private static final String XMI_FILE_PARAM = "-xmi"; 

  /**
   * Instantiates a new cvd.
   */
  private CVD() {
    super();
  }

  /**
   * Creates the main frame.
   *
   * @return the main frame
   */
  public static MainFrame createMainFrame() {
    return createMainFrame(null);
  }

  /**
   * Creates the main frame.
   *
   * @param iniFile the ini file
   * @return the main frame
   */
  public static MainFrame createMainFrame(File iniFile) {
    final MainFrame frame = new MainFrame(iniFile);
    // Set icon.
    ImageIcon icon = Images.getImageIcon(Images.MICROSCOPE);
    if (icon != null) {
      frame.setIconImage(icon.getImage());
    }
    try {
      javax.swing.SwingUtilities.invokeAndWait(new Runnable() {

        @Override
        public void run() {
          frame.pack();
          frame.setVisible(true);
        }
      });
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return frame;
  }

  /**
   * Creates the cmd line parser.
   *
   * @return the command line parser
   */
  private static final CommandLineParser createCmdLineParser() {
    CommandLineParser parser = new CommandLineParser();
    parser.addParameter(TEXT_FILE_PARAM, true);
    parser.addParameter(DESC_FILE_PARAM, true);
    parser.addParameter(DATA_PATH_PARAM, true);
    parser.addParameter(LOOK_AND_FEEL_PARAM, true);
    parser.addParameter(EXECUTE_SWITCH);
    parser.addParameter(XMI_FILE_PARAM, true); 
    parser.addParameter(INI_FILE_PARAM, true);
    return parser;
  }

  /**
   * Prints the usage.
   */
  private static final void printUsage() {
    System.out
        .println("Usage: java org.apache.uima.cvd.CVD [-text <TextFile>] [-desc <XmlDescriptor>] [-datapath <DataPath>] [-exec]");
    System.out.println("Additional optional parameters:");
    System.out.println("  -lookandfeel <LookAndFeelClassName>");
  }

  /**
   * Check cmd line syntax.
   *
   * @param clp the clp
   * @return true, if successful
   */
  private static final boolean checkCmdLineSyntax(CommandLineParser clp) {
    if (clp.getRestArgs().length > 0) {
      System.err.println("Error parsing CVD command line: unknown argument(s):");
      String[] args = clp.getRestArgs();
      for (int i = 0; i < args.length; i++) {
        System.err.print(" ");
        System.err.print(args[i]);
      }
      System.err.println();
      return false;
    }
    if (clp.isInArgsList(EXECUTE_SWITCH) && !clp.isInArgsList(DESC_FILE_PARAM)) {
      System.err.println("Error parsing CVD command line: -exec switch requires -desc parameter.");
      return false;
    }
    if (clp.isInArgsList(XMI_FILE_PARAM) && !clp.isInArgsList(DESC_FILE_PARAM)) {
      System.err.println("Error parsing CVD command line: -xmi switch requires -desc parameter.");
      return false;
    }
    return true;
  }

  /**
   * The main method.
   *
   * @param args the arguments
   */
  public static void main(String[] args) {
    try {
      CommandLineParser clp = createCmdLineParser();
      clp.parseCmdLine(args);
      if (!checkCmdLineSyntax(clp)) {
        printUsage();
        System.exit(2);
      }
      String lookAndFeel = null;
      if (clp.isInArgsList(LOOK_AND_FEEL_PARAM)) {
        lookAndFeel = clp.getParamArgument(LOOK_AND_FEEL_PARAM);
        try {
          UIManager.setLookAndFeel(lookAndFeel);
        } catch (UnsupportedLookAndFeelException e) {
          System.err.println(e.getMessage());
        }
      }
      File iniFile = null;
      if (clp.isInArgsList(INI_FILE_PARAM)) {
        String iniFileName = clp.getParamArgument(INI_FILE_PARAM);
        iniFile = new File(iniFileName);
      }
      MainFrame frame = createMainFrame(iniFile);
      if (clp.isInArgsList(TEXT_FILE_PARAM)) {
        frame.loadTextFile(new File(clp.getParamArgument(TEXT_FILE_PARAM)));
      }
      if (clp.isInArgsList(DATA_PATH_PARAM)) {
        frame.setDataPath(clp.getParamArgument(DATA_PATH_PARAM));
      } else {
        String dataProp = System.getProperty(RelativePathResolver.UIMA_DATAPATH_PROP);
        if (dataProp != null) {
          frame.setDataPath(dataProp);
        }
      }
      if (clp.isInArgsList(DESC_FILE_PARAM)) {
        frame.loadAEDescriptor(new File(clp.getParamArgument(DESC_FILE_PARAM)));
      }
      if (clp.isInArgsList(TEXT_FILE_PARAM)) {
	frame.loadTextFile(new File(clp.getParamArgument(TEXT_FILE_PARAM)));
      } else if (clp.isInArgsList(XMI_FILE_PARAM)) {
	frame.loadXmiFile(new File(clp.getParamArgument(XMI_FILE_PARAM)));
      }
      if (clp.isInArgsList(EXECUTE_SWITCH)) {
        frame.runAE(true);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

}
