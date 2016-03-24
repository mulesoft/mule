/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.descriptor;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.module.artifact.classloader.ClassLoaderFilter;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicy;

import java.io.File;

public class ArtifactDescriptor
{

    private String name;
    private File rootFolder;
    private ClassLoaderLookupPolicy classLoaderLookupPolicy = ClassLoaderLookupPolicy.NULL_LOOKUP_POLICY;
    private ClassLoaderFilter classLoaderFilter = ArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public File getRootFolder()
    {
        return rootFolder;
    }

    public void setRootFolder(File rootFolder)
    {
        if (rootFolder == null)
        {
            throw new IllegalArgumentException("Root folder cannot be null");
        }

        this.rootFolder = rootFolder;
    }

    public ClassLoaderLookupPolicy getClassLoaderLookupPolicy()
    {
        return classLoaderLookupPolicy;
    }

    public void setClassLoaderLookupPolicy(ClassLoaderLookupPolicy classLoaderLookupPolicy)
    {
        checkArgument(classLoaderLookupPolicy != null, "Classloader lookup policy must be non null");
        this.classLoaderLookupPolicy = classLoaderLookupPolicy;
    }

    public ClassLoaderFilter getClassLoaderFilter()
    {
        return classLoaderFilter;
    }

    public void setClassLoaderFilter(ClassLoaderFilter classLoaderFilter)
    {
        this.classLoaderFilter = classLoaderFilter;
    }
}
