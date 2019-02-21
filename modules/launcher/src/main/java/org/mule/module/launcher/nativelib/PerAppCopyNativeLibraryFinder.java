/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.nativelib;

import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * * Finds native libraries in an application's lib folder and creates a copy of
 * each found library inside a temporal application folder.
 */
public class PerAppCopyNativeLibraryFinder extends PerAppNativeLibraryFinder
{

    protected Log logger = LogFactory.getLog(getClass());

    private final File perAppNativeLibs;

    public PerAppCopyNativeLibraryFinder(File libDir, File perAppNativeLibs)
    {
        super(libDir);

        this.perAppNativeLibs = perAppNativeLibs;

        if (this.perAppNativeLibs.exists())
        {
            cleanNativeLibs();
        }
        else
        {
            if (!this.perAppNativeLibs.mkdirs())
            {
                throw new IllegalStateException(String.format("Unable to create application '%s' folder", this.perAppNativeLibs.getAbsolutePath()));
            }
        }
    }

    @Override
    public String findLibrary(String name, String parentLibraryPath)
    {
        String libraryPath = parentLibraryPath;

        if (null == libraryPath)
        {
            libraryPath = findLibraryLocally(name);
        }

        if (libraryPath != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Found native library for '%s' on '%s", name, libraryPath));
            }

            final File tempLibrary = copyNativeLibrary(name, libraryPath);
            libraryPath = tempLibrary.getAbsolutePath();

            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Created native library copy for '%s' on '%s", name, libraryPath));
            }
        }
        return libraryPath;
    }

    private void cleanNativeLibs()
    {
        String[] list = perAppNativeLibs.list();

        if (list != null)
        {
            for (String library : list)
            {
                FileUtils.deleteFile(new File(perAppNativeLibs, library));
            }
        }
    }

    private File copyNativeLibrary(String name, String libraryPath)
    {
        final String nativeLibName = System.mapLibraryName(name);
        final File tempLibrary = new File(perAppNativeLibs, nativeLibName + System.currentTimeMillis());

        try
        {
            final File library = new File(libraryPath);
            FileUtils.copyFile(library, tempLibrary);

            return tempLibrary;
        }
        catch (IOException e)
        {
            throw new IllegalStateException(String.format("Unable to generate copy for native library '%s' at '%s'", nativeLibName, tempLibrary.getAbsolutePath()), e);
        }
    }
}
