/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

/*
* Copyright  2003-2005 The Apache Software Foundation
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*
*/
package org.mule.tools.launcher;

import java.io.File;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;


/**
 * This is a launcher for Mule.  This class has been adapted from the Ant Launcher
 */
public class Launcher {

    public static final String MULE_LAUNCHER_PROPERTIES = "mule-launcher.properties";

    /**
     * Mule home directory
     */
    public static final String MULE_HOME_PROPERTY = "org.mule.home";

    /**
     * Defines the specific location of the mule-launcher.properties file.
     * If this is not set the property file will be looked for on the classpath and in
     * the current directory
     */
    public static final String MULE_LAUNCHER_PROPERTIES_PROPERTY = "org.mule.launcher.properties";
    /**
     * The Mule Library Directory property
     * Under this directory two other directories will be searched for -
     * opt - any optional jars in the Mule distribution
     * patch - Adirectory containing any patch jars for the Mule distribution.  This will
     * always be empty in a new distribution, but jars can be placed in here and will be loaded
     * at the top of the classpath
     */
    public static final String MULE_LIB_DIR_PROPERTY = "org.mule.lib.dir";

    /**
     * A custom directory containing additional jars
     */
    public static final String MULE_CUSTOM_LIB_DIR_PROPERTY = "org.mule.custom.lib.dir";

    /**
     * The main class to invoke. default is org.mule.MuleServer
     */
    public static final String MAIN_CLASS_PROPERTY = "org.mule.main.class";

    /**
     * Whether to output logging in the launcher
     */
    public static final String DEBUG_PROPERTY = "org.mule.debug";


    private static final String JAVA_CLASS_PATH = "java.class.path";
    /**
     * The startup class that is to be run
     */
    public static final String MAIN_CLASS = "org.mule.MuleServer";


    private static Properties properties = new Properties();

    private boolean debug = false;
    /**
     * Entry point for starting command line Mule
     *
     * @param args commandline arguments
     */
    public static void main(String[] args) {
        InputStream is=null;

        //Try loading properties from a specific location
        String propsFile = System.getProperty(MULE_LAUNCHER_PROPERTIES_PROPERTY, null);
        if(propsFile!=null) {
            File file = new File(propsFile);
            try {
                is = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                logError("Failed to load " + propsFile, e);
                System.exit(1);
            }
        }
        File file = new File(MULE_LAUNCHER_PROPERTIES);
        if(file.exists()) {
            try {
                is = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                //ignore
            }
        }

        if(is==null) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(MULE_LAUNCHER_PROPERTIES);
        }
        if(is!=null) {
            try {
                properties.load(is);
                System.out.println("Loaded Mule launcher from " + Thread.currentThread().getContextClassLoader().getResource(MULE_LAUNCHER_PROPERTIES));
            } catch (Throwable e) {
                logError("Failed to load " + MULE_LAUNCHER_PROPERTIES, e);
            }
        }

        try {
            Launcher launcher = new Launcher();
            launcher.run(args);
        } catch (Throwable e) {
            logError(e.getMessage(), e);
        }
    }

    public Launcher() {
        debug = Boolean.valueOf(getProperty(DEBUG_PROPERTY, "false")).booleanValue();
    }

    /**
     * Add a CLASSPATH or -lib to lib path urls.
     *
     * @param path        the classpath or lib path to add to the libPathULRLs
     * @param getJars     if true and a path is a directory, add the jars in
     *                    the directory to the path urls
     * @param libPathURLs the list of paths to add to
     */
    private void addPath(String path, boolean getJars, List libPathURLs)
            throws MalformedURLException {
        StringTokenizer myTokenizer
                = new StringTokenizer(path, getProperty("path.separator"));
        while (myTokenizer.hasMoreElements()) {
            String elementName = myTokenizer.nextToken();
            File element = new File(elementName);
            if (elementName.indexOf("%") != -1 && !element.exists()) {
                continue;
            }
            if (getJars && element.isDirectory()) {
                // add any jars in the directory
                URL[] dirURLs = Locator.getLocationURLs(element);
                for (int j = 0; j < dirURLs.length; ++j) {
                    libPathURLs.add(dirURLs[j]);
                }
            }

            libPathURLs.add(element.toURL());
        }
    }

    /**
     * Run the launcher to launch Mule
     *
     * @param args the command line arguments
     * @throws MalformedURLException if the URLs required for the classloader
     *                               cannot be created.
     */
    private void run(String[] args) throws LauncherException, MalformedURLException {
        String muleHomeProperty = getProperty(MULE_HOME_PROPERTY);
        File muleHome = null;

        File sourceJar = Locator.getClassSource(getClass());
        File jarDir = sourceJar.getParentFile();

        if (muleHomeProperty != null) {
            muleHome = new File(muleHomeProperty);
        }

        if (muleHome == null || !muleHome.exists()) {
            muleHome = jarDir.getParentFile();
            System.setProperty(MULE_HOME_PROPERTY, muleHome.getAbsolutePath());
            if(debug) System.out.println("Set " + MULE_HOME_PROPERTY + " to " + muleHome.getAbsolutePath());
        }

        if (!muleHome.exists()) {
            throw new LauncherException("Mule home is set incorrectly or ant could not be located");
        }

        List libPaths = new ArrayList();
        String cpString = null;
        List argList = new ArrayList();
        String[] newArgs;

        boolean noClassPath = false;

        if(debug) {
            for (int i = 0; i < args.length; ++i) {
                System.out.println("Arg " + i + "=" + args[i]);
            }
        }
        
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-lib")) {
                if (i == args.length - 1) {
                    throw new LauncherException("The -lib argument must be followed by a library location");
                }
                libPaths.add(args[++i]);
            } else if (args[i].equals("-cp")) {
                if (i == args.length - 1) {
                    throw new LauncherException("The -cp argument must be followed by a classpath expression");
                }
                if (cpString != null) {
                    throw new LauncherException("The -cp argument must not be repeated");
                }
                cpString = args[++i];
            } else if (args[i].equals("--noclasspath") || args[i].equals("-noclasspath")) {
                noClassPath = true;
            } else {
                argList.add(args[i]);
            }
        }

        //decide whether to copy the existing arg set, or
        //build a new one from the list of all args excluding the special
        //operations that only we handle
        if (argList.size() == args.length) {
            newArgs = args;
        } else {
            newArgs = (String[]) argList.toArray(new String[argList.size()]);
        }

        List libPathURLs = new ArrayList();

        if (cpString != null && !noClassPath) {
            addPath(cpString, false, libPathURLs);
        }

        for (Iterator i = libPaths.iterator(); i.hasNext();) {
            String libPath = (String) i.next();
            addPath(libPath, true, libPathURLs);
        }

        URL[] libJars = (URL[]) libPathURLs.toArray(new URL[libPathURLs.size()]);

        // Now try and find JAVA_HOME
        File toolsJar = Locator.getToolsJar();

        // determine Mule library directory for system jars: use property
        // or default using location of mule-launcher.jar
        File muleLibDir = null;
        File muleCustomLibDir = null;
        File muleLibOptDir = null;
        File muleLibPatchDir = null;
        String muleLibDirProperty = getProperty(MULE_LIB_DIR_PROPERTY);
        if (muleLibDirProperty != null) {
            muleLibDir = new File(muleLibDirProperty);
        }

        if ((muleLibDir == null) || !muleLibDir.exists()) {
            muleLibDir = new File(muleHome + File.separator + "lib");
            System.setProperty(MULE_LIB_DIR_PROPERTY, muleLibDir.getAbsolutePath());
            if(debug) System.out.println("Set " + MULE_LIB_DIR_PROPERTY + " to " + muleLibDir.getAbsolutePath());
        }

        if ((muleLibDir == null) || !muleLibDir.exists()) {
            muleLibDir = jarDir;
            System.setProperty(MULE_LIB_DIR_PROPERTY, muleLibDir.getAbsolutePath());
            if(debug) System.out.println("Set " + MULE_LIB_DIR_PROPERTY + " to " + muleLibDir.getAbsolutePath());
        }

        String muleCustomLibDirProperty = getProperty(MULE_CUSTOM_LIB_DIR_PROPERTY);
        if (muleCustomLibDirProperty != null) {
            muleCustomLibDir = new File(muleCustomLibDirProperty);
        }

        URL[] systemJars = Locator.getLocationURLs(muleLibDir);

        //Optional libraries
        muleLibOptDir = new File(muleLibDir.getAbsolutePath() + File.separator + "opt");
        URL[] muleOptJars = muleLibOptDir.exists() ? Locator.getLocationURLs(muleLibOptDir) :  new URL[0];

        //Patch libraries
        muleLibPatchDir = new File(muleLibDir.getAbsolutePath() + File.separator + "patch");
        URL[] mulePatchJars = muleLibOptDir.exists() ? Locator.getLocationURLs(muleLibPatchDir) :  new URL[0];

        URL[] muleCustomLibJars = (muleCustomLibDir!=null && muleCustomLibDir.exists() ? Locator.getLocationURLs(muleCustomLibDir) :  new URL[0]);

        int numJars = mulePatchJars.length + libJars.length + muleOptJars.length + systemJars.length + muleCustomLibJars.length;
        if (toolsJar != null) {
            numJars++;
        }
        URL[] jars = new URL[numJars];
        System.arraycopy(mulePatchJars, 0, jars, 0, mulePatchJars.length);
        System.arraycopy(libJars, 0, jars, mulePatchJars.length, libJars.length);
        System.arraycopy(muleOptJars, 0, jars, mulePatchJars.length + libJars.length, muleOptJars.length);
        System.arraycopy(systemJars, 0, jars, mulePatchJars.length + libJars.length + muleOptJars.length, systemJars.length);
        System.arraycopy(muleCustomLibJars, 0, jars, mulePatchJars.length + libJars.length + muleOptJars.length + systemJars.length, muleCustomLibJars.length);

        if (toolsJar != null) {
            jars[jars.length - 1] = toolsJar.toURL();
        }

        // now update the class.path property
        StringBuffer baseClassPath
                = new StringBuffer(getProperty(JAVA_CLASS_PATH));
        if (baseClassPath.charAt(baseClassPath.length() - 1)
                == File.pathSeparatorChar) {
            baseClassPath.setLength(baseClassPath.length() - 1);
        }

        String url;
        for (int i = 0; i < jars.length; ++i) {
            baseClassPath.append(File.pathSeparatorChar);
            url = Locator.fromURI(jars[i].toString());
            baseClassPath.append(url);
            if(debug) {
                System.out.println("Added: " + url);
            }

        }

        System.setProperty(JAVA_CLASS_PATH, baseClassPath.toString());

        URLClassLoader loader = new URLClassLoader(jars);
        Thread.currentThread().setContextClassLoader(loader);
        String tempMain = getProperty(MAIN_CLASS_PROPERTY, MAIN_CLASS);

        if(debug) System.out.println("Using Main class: " + tempMain);

        Class mainClass = null;
        try {
            mainClass = loader.loadClass(tempMain);
        } catch (Exception e) {
            File mainJar = Locator.getClassSource(mainClass);
            logError("Incompatible version of org.mule.tools.laucher detected. Location of this class is: " + mainJar, e);
        }
        Method main = null;
        try {
            main = mainClass.getMethod("main", new Class[]{String[].class});
        } catch (NoSuchMethodException e) {
            logError("There is no main(String[] args) method on launch class: " + mainClass, e);
        } catch (SecurityException e) {
            logError("A Security Exception blocked access to the main method of class: " + mainClass, e);
        }
        try {
            main.invoke(mainClass, new Object[]{newArgs});
        } catch (Exception e) {
            logError("Failed to invoke class: " + mainClass, e);

        }
    }

    protected static void logError(String msg, Throwable t) {
        System.err.println(msg);
        t.printStackTrace(System.err);
    }

    protected String getProperty(String key) {
        String value = System.getProperty(key);
        if(value==null) {
            value = properties.getProperty(key);
        }
        return value;
    }

    protected String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        if(value==null) value = defaultValue;
        return value;
    }
}
