/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

import java.util.Collections;
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

    public static final ArtifactClassLoaderFilter NULL_CLASSLOADER_FILTER = new ArtifactClassLoaderFilter(Collections.EMPTY_SET, Collections.EMPTY_SET);

    public static final String EXPORTED_CLASS_PACKAGES_PROPERTY = "artifact.export.classPackages";
    public static final String EXPORTED_RESOURCE_PACKAGES_PROPERTY = "artifact.export.resourcePackages";

    private static final char PACKAGE_SEPARATOR = '.';
    private static final String EMPTY_PACKAGE = "";
    private static final char RESOURCE_SEPARATOR = '/';

    private final Set<String> exportedClassPackages;
    private final Set<String> exportedResourcePackages;

    /**
     * Creates a new classLoader filter
     *
     * @param exportedClassPackages    class package names to export. Can be empty
     * @param exportedResourcePackages resource package names to export. Can be empty
     */
    public ArtifactClassLoaderFilter(Set<String> exportedClassPackages, Set<String> exportedResourcePackages)
    {
        checkArgument(exportedClassPackages != null, "Exported class packages cannot be null");
        checkArgument(exportedResourcePackages != null, "Exported resource packages cannot be null");

        this.exportedClassPackages = unmodifiableSet(exportedClassPackages);
        this.exportedResourcePackages = unmodifiableSet(exportedResourcePackages);
    }

    @Override
    public boolean exportsClass(String className)
    {
        checkArgument(!StringUtils.isEmpty(className), "Class name cannot be empty");
        final String packageName = getPackageName(className);

        return exportedClassPackages.contains(packageName);
    }

    @Override
    public boolean exportsResource(String name)
    {
        checkArgument(name != null, "Resource name cannot be null");
        final String resourcePackage = getResourceFolder(name);

        return exportedResourcePackages.contains(resourcePackage);
    }

    /**
     * @return exported class packages configured on this filter. Non null.
     */
    public Set<String> getExportedClassPackages()
    {
        return exportedClassPackages;
    }

    private String getResourceFolder(String resourceName)
    {
        String pkgName = "";
        if (resourceName.length() > 0)
        {
            pkgName = (resourceName.charAt(0) == RESOURCE_SEPARATOR) ? resourceName.substring(1) : resourceName;
            pkgName = (pkgName.lastIndexOf(RESOURCE_SEPARATOR) < 0) ? EMPTY_PACKAGE : pkgName.substring(0, pkgName.lastIndexOf(RESOURCE_SEPARATOR));
        }
        return pkgName;
    }

    private String getPackageName(String className)
    {
        return (className.lastIndexOf(PACKAGE_SEPARATOR) < 0) ? EMPTY_PACKAGE : className.substring(0, className.lastIndexOf(PACKAGE_SEPARATOR));
    }

    @Override
    public String toString()
    {
        return reflectionToString(this, MULTI_LINE_STYLE);
    }
}
