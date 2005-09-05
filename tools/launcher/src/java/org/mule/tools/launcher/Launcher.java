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
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;


/**
 * This is a launcher for Mule.  This class has been adapted from the Ant Launcher
 */
public class Launcher {
    /**
     * Mule home directory
     */
    public static final String MULE_HOME_PROPERTY = "org.mule.home";

    /**
     * The Mule Library Directory property
     */
    public static final String MULE_LIB_DIR_PROPERTY = "org.mule.lib.dir";

    public static final String MAIN_CLASS_PROPERTY = "org.mule.main.class";
    /**
     * The startup class that is to be run
     */
    public static final String MAIN_CLASS = "org.mule.MuleServer";

    private static final String JAVA_CLASS_PATH = "java.class.path";

    /**
     * Entry point for starting command line Mule
     *
     * @param args commandline arguments
     */
    public static void main(String[] args) {
        try {
            Launcher launcher = new Launcher();
            launcher.run(args);
        } catch (LauncherException e) {
            System.err.println(e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
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
                = new StringTokenizer(path, System.getProperty("path.separator"));
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
        String muleHomeProperty = System.getProperty(MULE_HOME_PROPERTY);
        File muleHome = null;

        File sourceJar = Locator.getClassSource(getClass());
        File jarDir = sourceJar.getParentFile();

        if (muleHomeProperty != null) {
            muleHome = new File(muleHomeProperty);
        }

        if (muleHome == null || !muleHome.exists()) {
            muleHome = jarDir.getParentFile();
            System.setProperty(MULE_HOME_PROPERTY, muleHome.getAbsolutePath());
        }

        if (!muleHome.exists()) {
            throw new LauncherException("Mule home is set incorrectly or ant could not be located");
        }

        List libPaths = new ArrayList();
        String cpString = null;
        List argList = new ArrayList();
        String[] newArgs;

        boolean noClassPath = false;

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
        File muleLibOptDir = null;
        String muleLibDirProperty = System.getProperty(MULE_LIB_DIR_PROPERTY);
        if (muleLibDirProperty != null) {
            muleLibDir = new File(muleLibDirProperty);
        }

        if ((muleLibDir == null) || !muleLibDir.exists()) {
            muleLibDir = new File(muleHome + File.separator + "lib");
            System.setProperty(MULE_LIB_DIR_PROPERTY, muleLibDir.getAbsolutePath());
        }

        if ((muleLibDir == null) || !muleLibDir.exists()) {
            muleLibDir = jarDir;
            System.setProperty(MULE_LIB_DIR_PROPERTY, muleLibDir.getAbsolutePath());
        }

        URL[] systemJars = Locator.getLocationURLs(muleLibDir);

        muleLibOptDir = new File(muleLibDir.getAbsolutePath() + File.separator + "opt");

        URL[] muleOptJars = muleLibOptDir.exists() ? Locator.getLocationURLs(muleLibOptDir) :  new URL[0];

        int numJars = libJars.length + muleOptJars.length + systemJars.length;
        if (toolsJar != null) {
            numJars++;
        }
        URL[] jars = new URL[numJars];
        System.arraycopy(libJars, 0, jars, 0, libJars.length);
        System.arraycopy(muleOptJars, 0, jars, libJars.length, muleOptJars.length);
        System.arraycopy(systemJars, 0, jars, muleOptJars.length + libJars.length,
            systemJars.length);

        if (toolsJar != null) {
            jars[jars.length - 1] = toolsJar.toURL();
        }

        // now update the class.path property
        StringBuffer baseClassPath
                = new StringBuffer(System.getProperty(JAVA_CLASS_PATH));
        if (baseClassPath.charAt(baseClassPath.length() - 1)
                == File.pathSeparatorChar) {
            baseClassPath.setLength(baseClassPath.length() - 1);
        }

        for (int i = 0; i < jars.length; ++i) {
            baseClassPath.append(File.pathSeparatorChar);
            baseClassPath.append(Locator.fromURI(jars[i].toString()));
        }

        System.setProperty(JAVA_CLASS_PATH, baseClassPath.toString());

        URLClassLoader loader = new URLClassLoader(jars);
        Thread.currentThread().setContextClassLoader(loader);
        String tempMain = System.getProperty(MAIN_CLASS_PROPERTY, MAIN_CLASS);
        Class mainClass = null;
        try {
            mainClass = loader.loadClass(tempMain);
        } catch (Exception ex) {
            System.err.println("Incompatible version of org.mule.tools.laucher detected");
            File mainJar = Locator.getClassSource(mainClass);
            System.err.println("Location of this class is: " + mainJar);
        }
        Method main = null;
        try {
            main = mainClass.getMethod("main", new Class[]{String[].class});
        } catch (NoSuchMethodException e) {
            System.err.println("There is no main(String[] args) method on launch class: " + mainClass);
            e.printStackTrace(System.err);
        } catch (SecurityException e) {
            System.err.println("A Security Exception blocked access to the main method of class: " + mainClass);
            e.printStackTrace(System.err);
        }
        try {
            main.invoke(mainClass, new Object[]{newArgs});
        } catch (Exception e) {
            System.err.println("Failed to invoke class: " + mainClass);
            e.printStackTrace(System.err);
        }
    }
}
