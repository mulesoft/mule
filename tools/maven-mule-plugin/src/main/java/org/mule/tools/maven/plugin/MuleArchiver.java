/*
 * $Id:  $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.maven.plugin;

import java.io.File;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Creates the structure and archive for a Mule Application
 */
public class MuleArchiver extends ZipArchiver
{

    public final static String LIB_LOCATION = "lib" + File.separator;
    public final static String CLASSES_LOCATION = "classes" + File.separator;
    public final static String ROOT_LOCATION = "";

    public void addResources(final File directoryName) throws ArchiverException
    {
        addDirectory(directoryName, ROOT_LOCATION, null, addDefaultExcludes(null));
    }

    public void addLib(final File file) throws ArchiverException
    {
        addFile(file, LIB_LOCATION + file.getName());
    }

    public void addLibs(final File directoryName, final String[] includes, final String[] excludes) throws ArchiverException
    {
        addDirectory(directoryName, LIB_LOCATION, includes, addDefaultExcludes(excludes));
    }

    /**
     * add files under /classes
     */
    public void addClasses(File directoryName, String[] includes, String[] excludes)
            throws ArchiverException
    {
        addDirectory(directoryName, CLASSES_LOCATION, includes, addDefaultExcludes(excludes));
    }

    private String[] addDefaultExcludes(String[] excludes)
    {
        if(excludes==null || excludes.length==0)
        {
            return DirectoryScanner.DEFAULTEXCLUDES;
        }
        else
        {
            String[] newExcludes = new String[excludes.length + DirectoryScanner.DEFAULTEXCLUDES.length];

            System.arraycopy(DirectoryScanner.DEFAULTEXCLUDES, 0, newExcludes, 0, DirectoryScanner.DEFAULTEXCLUDES.length);
            System.arraycopy(excludes, 0, newExcludes, DirectoryScanner.DEFAULTEXCLUDES.length, excludes.length);

            return newExcludes;
        }
    }

}