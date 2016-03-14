/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import org.mule.module.artifact.descriptor.ArtifactDescriptor;

import java.util.Set;

/**
 * Filters classes and resources using a {@link ArtifactDescriptor} describing
 * exported/blocked names.
 * <p>
 * An exact blocked/exported name match has precedence over a prefix match
 * on a blocked/exported prefix. This enables to export classes or
 * subpackages from a blocked package.
 * </p>
 */
public class ArtifactClassLoaderFilter implements ClassLoaderFilter
{

    private final ArtifactDescriptor descriptor;

    public ArtifactClassLoaderFilter(ArtifactDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    @Override
    public boolean accepts(String name)
    {
        return !isBlockedClass(name) && isExportedClass(name) || !isBlockedPrefix(name) && isExportedPrefix(name);
    }

    private boolean isBlockedPrefix(String name)
    {
        return hasListedPrefix(name, descriptor.getBlockedPrefixNames());
    }

    private boolean isBlockedClass(String name)
    {
        return descriptor.getBlockedPrefixNames().contains(name);
    }

    private boolean isExportedClass(String name)
    {
        return descriptor.getExportedPrefixNames().contains(name);
    }

    private boolean isExportedPrefix(String name)
    {
        return hasListedPrefix(name, descriptor.getExportedPrefixNames());
    }

    private boolean hasListedPrefix(String name, Set<String> classes)
    {
        for (String exported : classes)
        {
            if (name.startsWith(exported))
            {
                return true;
            }
        }

        return false;
    }
}
