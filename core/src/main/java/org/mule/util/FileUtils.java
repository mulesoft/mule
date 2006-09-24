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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.mule.MuleManager;

/**
 * <code>FileUtils</code> contains useful methods for dealing with files &
 * directories.
 */
// @Immutable
public class FileUtils extends org.apache.commons.io.FileUtils
{

    // TODO Document me!
    public static File createFile(String filename) throws IOException
    {
        File file = new File(filename);
        if (!file.canWrite())
        {
            String dirName = file.getPath();
            int i = dirName.lastIndexOf(File.separator);
            if (i > -1)
            {
                dirName = dirName.substring(0, i);
                File dir = new File(dirName);
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
        filename = filename.replaceAll("[/\\*?|:;]", "-");
        return filename;
    }

    // TODO Document me!
    public static File openDirectory(String directory) throws IOException
    {
        File dir = new File(directory);
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
        return getResourcePath(resourceName, callingClass, MuleManager.getConfiguration().getEncoding());
    }

    // TODO Document me!
    public static String getResourcePath(String resourceName, Class callingClass, String encoding)
        throws IOException
    {
        if (resourceName == null)
        {
            return null;
        }
        URL url = IOUtils.getResourceAsUrl(resourceName, callingClass);

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
                ZipEntry entry = (ZipEntry)entries.nextElement();
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
                    if (zip != null)
                    {
                        InputStream is = zip.getInputStream(entry);
                        OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
                        IOUtils.copy(is, os);
                        IOUtils.closeQuietly(is);
                        IOUtils.closeQuietly(os);
                    }
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

}
