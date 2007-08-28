/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl;

import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DirectoryManager</code> is responsible for exposing the directories used
 * for deploying mule artifacts
 */
public class Directories
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public static final String TEMP_DIR = "temp";
    public static final String BUNDLES_DIR = "bundles";
    //TODO: do we need stages similar to JBI?
    public static final String INSTALL_DIR = "install";
    public static final String DEPLOY_DIR = "deploy";
    public static final String PROCESSED_DIR = "processed";
    public static final String WORKSPACE_DIR = "workspace";

    private static int counter;

    private File rootDir;

    public Directories(File rootDir)
    {
        this.rootDir = rootDir;
        createDirectory(rootDir);
    }

    public static synchronized File getNewTempDir(File rootDir)
    {
        while (true)
        {
            String s = Integer.toHexString(++counter);
            while (s.length() < 8)
            {
                s = "0" + s;
            }
            File f = FileUtils.newFile(rootDir, File.separator + TEMP_DIR + File.separator + s);
            if (!f.exists())
            {
                return f;
            }
        }
    }

    public File getBundleInstallDir(String name)
    {
        return FileUtils.newFile(rootDir, BUNDLES_DIR + File.separator + validateString(name));
    }

    public File getAutoInstallDir()
    {
        return FileUtils.newFile(rootDir, INSTALL_DIR);
    }

    public File getAutoInstallProcessedDir()
    {
        return FileUtils.newFile(rootDir, INSTALL_DIR + File.separator + PROCESSED_DIR);
    }

    public File getAutoDeployDir()
    {
        return FileUtils.newFile(rootDir, DEPLOY_DIR);
    }

    public File getAutoDeployProcessedDir()
    {
        return FileUtils.newFile(rootDir, DEPLOY_DIR + File.separator + PROCESSED_DIR);
    }

    public void deleteMarkedDirectories()
    {
        deleteMarkedDirectories(rootDir);
    }

    public void deleteMarkedDirectories(File rootDir)
    {
        if (rootDir != null && rootDir.isDirectory())
        {
            if (FileUtils.newFile(rootDir, ".delete").isFile())
            {
                deleteDirectory(rootDir);
            }
            else
            {
                File[] children = rootDir.listFiles();
                for (int i = 0; i < children.length; i++)
                {
                    if (children[i].isDirectory())
                    {
                        deleteMarkedDirectories(children[i]);
                    }
                }
            }
        }
    }

    public void deleteDirectory(String dir)
    {
        deleteDirectory(FileUtils.newFile(dir));
    }

    public void deleteDirectory(File dir)
    {
        FileUtils.deleteTree(dir);
        if (dir.isDirectory())
        {
            try
            {
                FileUtils.newFile(dir, ".delete").createNewFile();
            }
            catch (IOException e)
            {
                logger.warn("Could not mark directory to be deleted", e);
            }
        }
    }

    public void createDirectories() throws IOException
    {
        createDirectories(rootDir);
    }

    public void createDirectories(File rootDir) throws IOException
    {
        createDirectory(rootDir);
        createDirectory(FileUtils.newFile(rootDir, BUNDLES_DIR));
        createDirectory(FileUtils.newFile(rootDir, WORKSPACE_DIR));
        createDirectory(getAutoInstallDir());
        createDirectory(getAutoDeployDir());
        createDirectory(getAutoDeployProcessedDir());
    }

    public void createDirectory(File dir)
    {
        if (dir != null)
        {
            dir.mkdirs();
        }
    }

    private String validateString(String str)
    {
        str = str.replace(':', '_');
        str = str.replace('/', '_');
        str = str.replace('\\', '_');
        return str;
    }

    public File getWorkingDirectory()
    {
        return rootDir;
    }
}
