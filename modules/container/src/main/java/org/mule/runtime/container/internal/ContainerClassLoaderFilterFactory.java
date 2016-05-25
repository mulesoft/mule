/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.core.util.PropertiesUtils.discoverProperties;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter.EXPORTED_RESOURCE_PACKAGES_PROPERTY;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Creates a {@link ClassLoaderFilter} for the container filter what is exposed to
 * Mule artifacts.
 * <p/>
 * Filter is constructed searching for {code}mule-module.properties{code} files in the classpath
 * and then merging the corresponding packages and resources in a new filter.
 */
public class ContainerClassLoaderFilterFactory
{

    public static final String MODULE_PROPERTIES = "META-INF/mule-module.properties";

    public ClassLoaderFilter create(Set<String> bootPackages)
    {

        Map<String, String> packages = new HashMap<>();
        Set<String> modules = new HashSet<>();

        Set<String> resources = new HashSet<>();
        // Adds default SPI resource folder
        resources.add("/META-INF/services");

        try
        {
            for (Properties muleModule : discoverProperties(MODULE_PROPERTIES))
            {
                final String moduleName = (String) muleModule.get("module.name");
                if (isEmpty(moduleName))
                {
                    throw new IllegalStateException("Mule-module.properties must contain module.name property");
                }

                if (modules.contains(moduleName))
                {
                    throw new IllegalStateException(String.format("Module '%s' was already defined", moduleName));
                }
                modules.add(moduleName);

                final String exportedPackagesProperty = (String) muleModule.get(EXPORTED_CLASS_PACKAGES_PROPERTY);
                if (!isEmpty(exportedPackagesProperty))
                {
                    for (String packageName : exportedPackagesProperty.split(","))
                    {
                        packageName = packageName.trim();
                        if (!isEmpty(packageName))
                        {
                            packages.put(packageName, moduleName);
                        }
                    }
                }

                final String exportedResourcesProperty = (String) muleModule.get(EXPORTED_RESOURCE_PACKAGES_PROPERTY);
                if (!isEmpty(exportedResourcesProperty))
                {
                    for (String resource : exportedResourcesProperty.split(","))
                    {
                        if (!isEmpty(resource.trim()))
                        {
                            if (resource.startsWith("/"))
                            {
                                resource = resource.substring(1);
                            }
                            resources.add(resource);
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Cannot discover mule modules", e);
        }

        final ArtifactClassLoaderFilter artifactClassLoaderFilter = new ArtifactClassLoaderFilter(packages.keySet(), resources);
        return new ContainerClassLoaderFilter(artifactClassLoaderFilter, bootPackages);
    }

    public static class ContainerClassLoaderFilter implements ClassLoaderFilter
    {

        public static final String CLASS_PACKAGE_SPLIT_REGEX = "\\.";
        public static final String RESOURCE_PACKAGE_SPLIT_REGEX = "/";
        private final ClassLoaderFilter moduleClassLoaderFilter;
        private final Set<String> bootPackages;

        public ContainerClassLoaderFilter(ClassLoaderFilter moduleClassLoaderFilter, Set<String> bootPackages)
        {
            this.moduleClassLoaderFilter = moduleClassLoaderFilter;
            this.bootPackages = bootPackages;
        }

        @Override
        public boolean exportsClass(String name)
        {
            boolean exported = moduleClassLoaderFilter.exportsClass(name);

            if (!exported)
            {
                exported = isExportedBooPackage(name, CLASS_PACKAGE_SPLIT_REGEX);
            }
            return exported;
        }

        @Override
        public boolean exportsResource(String name)
        {
            boolean exported = moduleClassLoaderFilter.exportsResource(name);

            if (!exported)
            {
                exported = isExportedBooPackage(name, RESOURCE_PACKAGE_SPLIT_REGEX);
            }

            return exported;
        }

        private boolean isExportedBooPackage(String name, String splitRegex)
        {
            boolean exported = false;
            final String[] splitName = name.split(splitRegex);
            final String[] packages = Arrays.copyOf(splitName, splitName.length - 1);
            String candidatePackage = "";

            for (String currentPackage : packages)
            {
                if (candidatePackage.length() != 0)
                {
                    candidatePackage += ".";
                }
                candidatePackage += currentPackage;

                if (bootPackages.contains(candidatePackage))
                {
                    exported = true;
                    break;
                }

            }
            return exported;
        }
    }

}
