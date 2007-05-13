/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <code>FileUtils</code> contains useful methods for dealing with files &
 * directories.
 */
// @ThreadSafe
public class FileUtils extends org.apache.commons.io.FileUtils
{

    public static final String DEFAULT_ENCODING = "UTF-8";

    public static synchronized void copyStreamToFile(InputStream input, File destination) throws IOException
    {
        if (destination.exists() && !destination.canWrite())
        {
            throw new IOException("Destination file does not exist or is not writeable");
        }

        try
        {
            FileOutputStream output = new FileOutputStream(destination);
            try
            {
                IOUtils.copy(input, output);
            }
            finally
            {
                IOUtils.closeQuietly(output);
            }
        }
        finally
        {
            IOUtils.closeQuietly(input);
        }
    }

    // TODO Document me!
    public static File createFile(String filename) throws IOException
    {
        File file = FileUtils.newFile(filename);
        if (!file.canWrite())
        {
            String dirName = file.getPath();
            int i = dirName.lastIndexOf(File.separator);
            if (i > -1)
            {
                dirName = dirName.substring(0, i);
                File dir = FileUtils.newFile(dirName);
                dir.mkdirs();
            }
            file.createNewFile();
        }
        return file;
    }

    // TODO Document me!
    public static String prepareWinFilename(String filename)
    {
        filename = filename.replaceAll("<", "(");
        filename = filename.replaceAll(">", ")");
        filename = filename.replaceAll("[/\\*?|:;\\]\\[\"]", "-");
        return filename;
    }

    // TODO Document me!
    public static File openDirectory(String directory) throws IOException
    {
        File dir = FileUtils.newFile(directory);
        if (!dir.exists())
        {
            dir.mkdirs();
        }
        if (!dir.isDirectory() || !dir.canRead())
        {
            throw new IOException("Path: " + directory + " exists but isn't a directory");
        }
        return dir;
    }

    /**
     * Reads the incoming String into a file at at the given destination.
     *
     * @param filename name and path of the file to create
     * @param data the contents of the file
     * @return the new file.
     * @throws IOException If the creating or writing to the file stream fails
     */
    public static File stringToFile(String filename, String data) throws IOException
    {
        return stringToFile(filename, data, false);
    }

    // TODO Document me!
    public static synchronized File stringToFile(String filename, String data, boolean append)
        throws IOException
    {
        return stringToFile(filename, data, append, false);
    }

    // TODO Document me!
    public static synchronized File stringToFile(String filename, String data, boolean append, boolean newLine)
        throws IOException
    {
        File f = createFile(filename);
        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(f, append));
            writer.write(data);
            if (newLine)
            {
                writer.newLine();
            }
        }
        finally
        {
            if (writer != null)
            {
                writer.close();
            }
        }
        return f;
    }

    // TODO Document me!
    public static String getResourcePath(String resourceName, Class callingClass) throws IOException
    {
        return getResourcePath(resourceName, callingClass, DEFAULT_ENCODING);
    }

    // TODO Document me!
    public static String getResourcePath(String resourceName, Class callingClass, String encoding)
        throws IOException
    {
        if (resourceName == null)
        {
            // no name
            return null;
        }

        URL url = IOUtils.getResourceAsUrl(resourceName, callingClass);
        if (url == null)
        {
            // not found
            return null;
        }

        String resource = URLDecoder.decode(url.toExternalForm(), encoding);
        if (resource != null)
        {
            if (resource.startsWith("file:/"))
            {
                resource = resource.substring(6);
            }
            if (!resource.startsWith(File.separator))
            {
                resource = File.separator + resource;
            }
        }

        return resource;
    }

    // TODO Document me!
    public static boolean deleteTree(File dir)
    {
        if (dir == null || !dir.exists())
        {
            return true;
        }
        File[] files = dir.listFiles();
        if (files != null)
        {
            for (int i = 0; i < files.length; i++)
            {
                if (files[i].isDirectory())
                {
                    if (!deleteTree(files[i]))
                    {
                        return false;
                    }
                }
                else
                {
                    if (!files[i].delete())
                    {
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

        if (directory.exists())
        {
            if (!directory.isDirectory())
            {
                throw new IOException("Directory is not a directory: " + directory);
            }
        }
        else
        {
            if (!directory.mkdirs())
            {
                throw new IOException("Could not create directory: " + directory);
            }
        }
        try
        {
            zip = new ZipFile(archive);
            for (Enumeration entries = zip.entries(); entries.hasMoreElements();)
            {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                File f = new File(directory, entry.getName());
                if (entry.isDirectory())
                {
                    if (!f.mkdirs())
                    {
                        throw new IOException("Could not create directory: " + f);
                    }
                }
                else
                {
                    InputStream is = zip.getInputStream(entry);
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
                    IOUtils.copy(is, os);
                    IOUtils.closeQuietly(is);
                    IOUtils.closeQuietly(os);
                }
            }
        }
        finally
        {
            if (zip != null)
            {
                zip.close();
            }
        }
    }

    /**
     * Workaround for JDK bug <a href="http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4117557">
     * 4117557</a>. More in-context information at
     * <a href="http://mule.mulesource.org/jira/browse/MULE-1112">MULE-1112</a>
     * <p/>
     * Factory methods correspond to constructors of the <code>java.io.File class</code>.
     * No physical file created in this method.
     *
     * @see File
     *
     */
    public static File newFile(String pathName)
    {
        try
        {
            return new File(pathName).getCanonicalFile();
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(
                    MessageFactory.createStaticMessage("Unable to create a canonical file for " + pathName), 
                    e);
        }
    }

    /**
     * Workaround for JDK bug <a href="http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4117557">
     * 4117557</a>. More in-context information at
     * <a href="http://mule.mulesource.org/jira/browse/MULE-1112">MULE-1112</a>
     * <p/>
     * Factory methods correspond to constructors of the <code>java.io.File class</code>.
     * No physical file created in this method.
     *
     * @see File
     *
     */
    public static File newFile(URI uri)
    {
        try
        {
            return new File(uri).getCanonicalFile();
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(
                    MessageFactory.createStaticMessage("Unable to create a canonical file for " + uri),
                    e);
        }
    }

    /**
     * Workaround for JDK bug <a href="http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4117557">
     * 4117557</a>. More in-context information at
     * <a href="http://mule.mulesource.org/jira/browse/MULE-1112">MULE-1112</a>
     * <p/>
     * Factory methods correspond to constructors of the <code>java.io.File class</code>.
     * No physical file created in this method.
     *
     * @see File
     *
     */
    public static File newFile(File parent, String child)
    {
        try
        {
            return new File(parent, child).getCanonicalFile();
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(
                    MessageFactory.createStaticMessage("Unable to create a canonical file for parent: " 
                        + parent + " and child: " + child),
                    e);
        }
    }

    /**
     * Workaround for JDK bug <a href="http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4117557">
     * 4117557</a>. More in-context information at
     * <a href="http://mule.mulesource.org/jira/browse/MULE-1112">MULE-1112</a>
     * <p/>
     * Factory methods correspond to constructors of the <code>java.io.File class</code>.
     * No physical file created in this method.
     *
     * @see File
     *
     */
    public static File newFile(String parent, String child)
    {
        try
        {
            return new File(parent, child).getCanonicalFile();
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(
                    MessageFactory.createStaticMessage("Unable to create a canonical file for parent: " 
                        + parent + " and child: " + child),
                    e);
        }
    }
}
