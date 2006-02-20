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
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;

/**
 * The Locator is a utility class which is used to find certain items
 * in the environment.
 *
 * This class is brorrowed for the Ant project
 *
 */
public final class Locator {
    /**
     * Not instantiable
     */
    private Locator() {
    }

    /**
     * Find the directory or jar file the class has been loaded from.
     *
     * @param c the class whose location is required.
     * @return the file or jar with the class or null if we cannot
     *         determine the location.
     */
    public static File getClassSource(Class c) {
        String classResource = c.getName().replace('.', '/') + ".class";
        return getResourceSource(c.getClassLoader(), classResource);
    }

    /**
     * Find the directory or jar a given resource has been loaded from.
     *
     * @param c the classloader to be consulted for the source.
     * @param resource the resource whose location is required.
     *
     * @return the file with the resource source or null if
     *         we cannot determine the location.
     */
    public static File getResourceSource(ClassLoader c, String resource) {
        if (c == null) {
            c = Locator.class.getClassLoader();
        }
        URL url = null;
        if (c == null) {
            url = ClassLoader.getSystemResource(resource);
        } else {
            url = c.getResource(resource);
        }
        if (url != null) {
            String u = url.toString();
            if (u.startsWith("jar:file:")) {
                int pling = u.indexOf("!");
                String jarName = u.substring(4, pling);
                return new File(fromURI(jarName));
            } else if (u.startsWith("file:")) {
                int tail = u.indexOf(resource);
                String dirName = u.substring(0, tail);
                return new File(fromURI(dirName));
            }
        }
        return null;
    }

    /**
     * Constructs a file path from a <code>file:</code> URI.
     *
     * <p>Will be an absolute path if the given URI is absolute.</p>
     *
     * <p>Swallows '%' that are not followed by two characters,
     * doesn't deal with non-ASCII characters.</p>
     *
     * @param uri the URI designating a file in the local filesystem.
     * @return the local file system path for the file.
     */
    public static String fromURI(String uri) {
        URL url = null;
        try {
            url = new URL(uri);
        } catch (MalformedURLException emYouEarlEx) {
            // Ignore malformed exception
        }
        if (url == null || !("file".equals(url.getProtocol()))) {
            throw new IllegalArgumentException("Can only handle valid file: URIs");
        }
        StringBuffer buf = new StringBuffer(url.getHost());
        if (buf.length() > 0) {
            buf.insert(0, File.separatorChar).insert(0, File.separatorChar);
        }
        String file = url.getFile();
        int queryPos = file.indexOf('?');
        buf.append((queryPos < 0) ? file : file.substring(0, queryPos));

        uri = buf.toString().replace('/', File.separatorChar);

        if (File.pathSeparatorChar == ';' && uri.startsWith("\\") && uri.length() > 2
            && Character.isLetter(uri.charAt(1)) && uri.lastIndexOf(':') > -1) {
            uri = uri.substring(1);
        }
        String path = decodeUri(uri);
        return path;
    }

    /**
     * Decodes an Uri with % characters.
     * @param uri String with the uri possibly containing % characters.
     * @return The decoded Uri
     */
    private static String decodeUri(String uri) {
        if (uri.indexOf('%') == -1)
        {
            return uri;
        }
        StringBuffer sb = new StringBuffer();
        CharacterIterator iter = new StringCharacterIterator(uri);
        for (char c = iter.first(); c != CharacterIterator.DONE;
             c = iter.next()) {
            if (c == '%') {
                char c1 = iter.next();
                if (c1 != CharacterIterator.DONE) {
                    int i1 = Character.digit(c1, 16);
                    char c2 = iter.next();
                    if (c2 != CharacterIterator.DONE) {
                        int i2 = Character.digit(c2, 16);
                        sb.append((char) ((i1 << 4) + i2));
                    }
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Get the File necessary to load the Sun compiler tools. If the classes
     * are available to this class, then no additional URL is required and
     * null is returned. This may be because the classes are explicitly in the
     * class path or provided by the JVM directly.
     *
     * @return the tools jar as a File if required, null otherwise.
     */
    public static File getToolsJar() {
        // firstly check if the tools jar is already in the classpath
        boolean toolsJarAvailable = false;
        try {
            // just check whether this throws an exception
            Class.forName("com.sun.tools.javac.Main");
            toolsJarAvailable = true;
        } catch (Exception e) {
            try {
                Class.forName("sun.tools.javac.Main");
                toolsJarAvailable = true;
            } catch (Exception e2) {
                // ignore
            }
        }
        if (toolsJarAvailable) {
            return null;
        }
        // couldn't find compiler - try to find tools.jar
        // based on java.home setting
        String javaHome = System.getProperty("java.home");
        if (javaHome.toLowerCase(Locale.US).endsWith(File.separator + "jre")) {
            javaHome = javaHome.substring(0, javaHome.length() - 4);
        }
        File toolsJar = new File(javaHome + "/lib/tools.jar");
        if (!toolsJar.exists()) {
            System.out.println("Unable to locate tools.jar. "
                 + "Expected to find it in " + toolsJar.getPath());
            return null;
        }
        return toolsJar;
    }

    /**
     * Get an array of URLs representing all of the jar files in the
     * given location. If the location is a file, it is returned as the only
     * element of the array. If the location is a directory, it is scanned for
     * jar files.
     *
     * @param location the location to scan for Jars.
     *
     * @return an array of URLs for all jars in the given location.
     *
     * @exception MalformedURLException if the URLs for the jars cannot be
     *            formed.
     */
    public static URL[] getLocationURLs(File location)
         throws MalformedURLException {
        return getLocationURLs(location, new String[]{".jar"});
    }

    /**
     * Get an array of URLs representing all of the files of a given set of
     * extensions in the given location. If the location is a file, it is
     * returned as the only element of the array. If the location is a
     * directory, it is scanned for matching files.
     *
     * @param location the location to scan for files.
     * @param extensions an array of extension that are to match in the
     *        directory search.
     *
     * @return an array of URLs of matching files.
     * @exception MalformedURLException if the URLs for the files cannot be
     *            formed.
     */
    public static URL[] getLocationURLs(File location,
                                        final String[] extensions)
         throws MalformedURLException {
        URL[] urls = new URL[0];

        if (!location.exists()) {
            return urls;
        }
        if (!location.isDirectory()) {
            urls = new URL[1];
            String path = location.getPath();
            for (int i = 0; i < extensions.length; ++i) {
                if (path.toLowerCase().endsWith(extensions[i])) {
                    urls[0] = location.toURL();
                    break;
                }
            }
            return urls;
        }
        File[] matches = location.listFiles(
            new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    for (int i = 0; i < extensions.length; ++i) {
                        if (name.toLowerCase().endsWith(extensions[i])) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        urls = new URL[matches.length];
        for (int i = 0; i < matches.length; ++i) {
            urls[i] = matches[i].toURL();
        }
        return urls;
    }
}
