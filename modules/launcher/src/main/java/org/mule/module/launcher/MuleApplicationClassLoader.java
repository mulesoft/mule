/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.module.launcher.application.ApplicationClassLoader;
import org.mule.module.launcher.nativelib.NativeLibraryFinder;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

public class MuleApplicationClassLoader extends MuleArtifactClassLoader implements ApplicationClassLoader
{

    private NativeLibraryFinder nativeLibraryFinder;

    public MuleApplicationClassLoader(String appName, ClassLoader parentCl, NativeLibraryFinder nativeLibraryFinder, List<URL> urls)
    {
        this(appName, parentCl, Collections.<String>emptySet(), nativeLibraryFinder, urls);
    }

    public MuleApplicationClassLoader(String appName, ClassLoader parentCl, Set<String> loaderOverrides, NativeLibraryFinder nativeLibraryFinder, List<URL> urls)
    {
        super(appName, urls.toArray(new URL[0]), parentCl, loaderOverrides);

        this.nativeLibraryFinder = nativeLibraryFinder;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        return super.findClass(name);
    }

    @Override
    public URL getResource(String name)
    {
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException
    {
        return super.getResources(name);
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]@%s", getClass().getName(),
                             getArtifactName(),
                             Integer.toHexString(System.identityHashCode(this)));
    }

    @Override
    protected String findLibrary(String name)
    {
        String libraryPath = super.findLibrary(name);

        libraryPath = nativeLibraryFinder.findLibrary(name, libraryPath);

        return libraryPath;
    }

    @Override
    protected String[] getLocalResourceLocations()
    {
        return new String[] {MuleFoldersUtil.getAppClassesFolder(getArtifactName()).getAbsolutePath()};
    }
}
