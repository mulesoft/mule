/* 
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.util;

import org.apache.commons.io.IOUtils;
import org.mule.MuleManager;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <code>FileUtils</code> contains useful methods for dealing with files & directories.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class FileUtils extends org.apache.commons.io.FileUtils
{

    public static File createFile(String filename) throws IOException
    {
        File file = new File(filename);
        if (!file.canWrite()) {
            String dirName = file.getPath();
            int i = dirName.lastIndexOf(File.separator);
            if (i > -1) {
                dirName = dirName.substring(0, i);
                File dir = new File(dirName);
                dir.mkdirs();
            }
            file.createNewFile();
        }
        return file;
    }

    public static String prepareWinFilename(String filename)
    {
        filename = filename.replaceAll("<", "(");
        filename = filename.replaceAll(">", ")");
        filename = filename.replaceAll("[/\\*?|:;]", "-");
        return filename;
    }

    public static File openDirectory(String directory) throws IOException
    {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.isDirectory() || !dir.canRead()) {
            throw new IOException("Directory: " + directory + " exists but isn't a directory");
        }
        return dir;
    }

    /**
     * Reads the incoming String into a file at at the given destination.
     * 
     * @param filename
     *            name and path of the file to create
     * @param data
     *            the contents of the file
     * @return the new file.
     * @throws IOException
     *             If the creating or writing to the file stream fails
     */
    public static File stringToFile(String filename, String data) throws IOException
    {
        return stringToFile(filename, data, false);
    }

    public static synchronized File stringToFile(String filename, String data, boolean append)
            throws IOException
    {
        return stringToFile(filename, data, append, false);
    }

    public static synchronized File stringToFile(String filename,
            String data,
            boolean append,
            boolean newLine) throws IOException
    {
        File f = createFile(filename);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(f, append));
            writer.write(data);
            if (newLine) {
                writer.newLine();
            }
        }
        finally {
            if (writer != null) {
                writer.close();
            }
        }
        return f;
    }

    public static File loadFile(String filename) throws IOException
    {
        File file = new File(filename);
        if (file.canRead()) {
            return file;
        }
        else {
            throw new IOException("File: " + filename + " can not be read");
        }
    }

    /**
     * Load a given resource. Trying broader class loaders each time.
     * 
     * @param resourceName
     *            The name of the resource to load
     * @param callingClass
     *            The Class object of the calling object
     */
    public static URL getResource(final String resourceName, final Class callingClass)
    {
        URL url = ClassUtils.getResource(resourceName, callingClass);

        if (url == null) {
            url = (URL)AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    File f = new File(resourceName);
                    if (f.exists()) {
                        try {
                            return f.toURL();
                        }
                        catch (MalformedURLException e) {
                            return null;
                        }
                    }
                    return null;
                }
            });
        }
        return url;
    }

    public static String loadResourceAsString(String resourceName, Class callingClass)
            throws IOException
    {
        return loadResourceAsString(resourceName, callingClass, MuleManager.getConfiguration()
                .getEncoding());
    }

    public static String loadResourceAsString(String resourceName, Class callingClass, String encoding)
            throws IOException
    {
        URL url = getResource(resourceName, callingClass);
        if (url != null) {
            resourceName = url.getFile();
        }

        return FileUtils.readFileToString(new File(resourceName), encoding);
    }

    public static InputStream loadResource(String resourceName, Class callingClass) throws IOException
    {
        URL url = getResource(resourceName, callingClass);
        InputStream resource = null;
        if (url == null) {
            File f = new File(resourceName);
            if (f.exists()) {
                resource = new FileInputStream(f);
            }
        }
        else {
            resource = url.openStream();
        }
        return resource;
    }

    public static String getResourcePath(String resourceName, Class callingClass) throws IOException
    {
        return getResourcePath(resourceName, callingClass, MuleManager.getConfiguration().getEncoding());
    }

    public static String getResourcePath(String resourceName, Class callingClass, String encoding)
            throws IOException
    {
        if (resourceName == null) {
            return null;
        }
        URL url = getResource(resourceName, callingClass);
        String resource = null;
        if (url == null) {
            File f = new File(resourceName);
            if (f.exists()) {
                resource = f.getAbsolutePath();
            }
        }
        else {
            resource = URLDecoder.decode(url.toExternalForm(), encoding);
        }
        if (resource != null) {
            if (resource.startsWith("file:/")) {
                resource = resource.substring(6);
            }
            if (!resource.startsWith(File.separator)) {
                resource = File.separator + resource;
            }
        }

        return resource;
    }

    public static boolean deleteTree(File dir)
    {
        if (dir == null || !dir.exists()) {
            return true;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    if (!deleteTree(files[i])) {
                        return false;
                    }
                }
                else {
                    if (!files[i].delete()) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }

    /**
     * Unzip the specified archive to the given directory
     */
    public static void unzip(File archive, File directory) throws IOException
    {
        ZipFile zip = null;

        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new IOException("Directory is not a directory: " + directory);
            }
        }
        else {
            if (!directory.mkdirs()) {
                throw new IOException("Could not create directory: " + directory);
            }
        }
        try {
            zip = new ZipFile(archive);
            for (Enumeration entries = zip.entries(); entries.hasMoreElements();) {
                ZipEntry entry = (ZipEntry)entries.nextElement();
                File f = new File(directory, entry.getName());
                if (entry.isDirectory()) {
                    if (!f.mkdirs()) {
                        throw new IOException("Could not create directory: " + f);
                    }
                }
                else {
                    if (zip != null) {
                        InputStream is = zip.getInputStream(entry);
                        OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
                        IOUtils.copy(is, os);
                        IOUtils.closeQuietly(is);
                        IOUtils.closeQuietly(os);
                    }
                }
            }
        }
        finally {
            if (zip != null) {
                zip.close();
            }
        }
    }

}
