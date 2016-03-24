/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import org.mule.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Creates {@link ArtifactClassLoaderFilter} instances
 */
public class ArtifactClassLoaderFilterFactory implements ClassLoaderFilterFactory
{

    @Override
    public ClassLoaderFilter create(String exportedClassPackages, String exportedResourcePackages)
    {
        Set<String> exportedClasses = getPackages(exportedClassPackages);
        Set<String> exportedResources = getPackages(exportedResourcePackages);

        if (exportedClasses.isEmpty() && exportedResources.isEmpty())
        {
            return ArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
        }
        else
        {
            return new ArtifactClassLoaderFilter(exportedClasses, exportedResources);
        }
    }

    private Set<String> getPackages(String exportedPackages)
    {
        Set<String> exported = new HashSet<>();
        if (StringUtils.isNotBlank(exportedPackages))
        {
            final String[] exports = exportedPackages.split(",");
            for (String export : exports)
            {
                export = export.trim();
                if (export.startsWith("/"))
                {
                    export = export.substring(1);
                }
                exported.add(export);
            }
        }

        return exported;
    }
}
