/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.runtime.module.artifact.classloader.EnumerationAdapter;
import org.mule.runtime.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleClassLoaderLookupPolicy;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates the classLoader for the Mule container.
 * <p/>
 * This classLoader must be used as the parent classLoader for any other Mule artifact
 * depending only on the container.
 */
public class ContainerClassLoaderFactory
{

    //TODO(pablo.kraan): MULE-9524: Add a way to configure system and boot packages used on class loading lookup
    /**
     * System package define all the prefixes that must be loaded only from the container
     * classLoader, but then are filtered depending on what is part of the exposed API.
     */
    public static final Set<String> SYSTEM_PACKAGES = ImmutableSet.of(
            "org.mule.runtime", "com.mulesoft.mule.runtime"
    );

    /**
     * Boot packages define all the prefixes that must be loaded from the container
     * classLoader without being filtered
     */
    public static final Set<String> BOOT_PACKAGES = ImmutableSet.of(
            "java",
            "javax.accessibility",
            "javax.activation",
            "javax.activity",
            "javax.annotation",
            "javax.crypto",
            "javax.imageio",
            "javax.jws",
            "javax.lang.model",
            "javax.management",
            "javax.naming",
            "javax.net",
            "javax.print",
            "javax.rmi",
            "javax.script",
            "javax.security",
            "javax.smartcardio",
            "javax.sound",
            "javax.sql",
            "javax.swing",
            "javax.tools",
            "javax.transaction",
            "javax.resource",
            "javax.xml",
            //Java EE
            "javax.jms",
            "javax.servlet",
            "javax.ws",
            "javax.mail",
            "javax.inject",
            "org.xml.sax", "org.apache.xerces",
            "org.apache.logging.log4j", "org.slf4j", "org.apache.commons.logging", "org.apache.log4j",
            "org.dom4j", "org.w3c.dom",
            "com.sun", "sun",
            "org.springframework",
            "org.mule.mvel2"
    );

    private ModuleDiscoverer moduleDiscoverer = new ClasspathModuleDiscoverer(this.getClass().getClassLoader());

    /**
     * Creates the classLoader to represent the Mule container.
     *
     * @param parentClassLoader parent classLoader. Can be null.
     * @return a non null {@link ArtifactClassLoader} containing container code that can be used as
     * parent classloader for other mule artifacts.
     */
    public ArtifactClassLoader createContainerClassLoader(final ClassLoader parentClassLoader)
    {
        final List<MuleModule> muleModules = moduleDiscoverer.discover();
        final ClassLoaderLookupPolicy containerLookupPolicy = getContainerClassLoaderLookupPolicy(muleModules);
        return createArtifactClassLoader(parentClassLoader, muleModules, containerLookupPolicy);
    }

    /**
     * Creates the container lookup policy to be used by child class loaders.
     *
     * @param muleModules list of modules that would be used to register in the filter based of the class loader.
     * @return a non null {@link ClassLoaderLookupPolicy} that contains the lookup policies for boot, system packages.
     * plus exported packages by the given list of {@link MuleModule}.
     */
    protected ClassLoaderLookupPolicy getContainerClassLoaderLookupPolicy(List<MuleModule> muleModules)
    {
        final Set<String> parentOnlyPackages = new HashSet<>(getBootPackages());
        parentOnlyPackages.addAll(SYSTEM_PACKAGES);

        final Map<String, ClassLoaderLookupStrategy> lookupStrategies = buildClassLoaderLookupStrategy(muleModules);
        return new MuleClassLoaderLookupPolicy(lookupStrategies, parentOnlyPackages);
    }

    /**
     * Creates an {@link ArtifactClassLoader} that always resolves resources by delegating to the parentClassLoader.
     *
     * @param parentClassLoader the parent {@link ClassLoader} for the container
     * @param muleModules the list of {@link MuleModule}s to be used for defining the filter
     * @param containerLookupPolicy the {@link ClassLoaderLookupPolicy} to be used by the container
     * @return a {@link ArtifactClassLoader} to be used in a {@link FilteringContainerClassLoader}
     */
    protected ArtifactClassLoader createArtifactClassLoader(final ClassLoader parentClassLoader, List<MuleModule> muleModules, final ClassLoaderLookupPolicy containerLookupPolicy)
    {
        final ArtifactClassLoader containerClassLoader = new MuleArtifactClassLoader("mule", new URL[0], parentClassLoader, containerLookupPolicy)
        {

             @Override
            public URL findResource(String name)
            {
                // Container classLoader is just an adapter, it does not owns any resource
                return null;
            }

            @Override
            public Enumeration<URL> findResources(String name) throws IOException
            {
                // Container classLoader is just an adapter, it does not owns any resource
                return new EnumerationAdapter<>(Collections.emptyList());
            }
        };

        return createContainerFilteringClassLoader(muleModules, containerClassLoader);
    }

    public void setModuleDiscoverer(ModuleDiscoverer moduleDiscoverer)
    {
        this.moduleDiscoverer = moduleDiscoverer;
    }

    /**
     * Creates a {@link Map<String, ClassLoaderLookupStrategy>} with PARENT_ONLY strategy for the packages
     * exported by the mule modules.
     *
     * @param muleModules to be used for colleting the exported packages
     * @return a {@link Map<String, ClassLoaderLookupStrategy>} for the exported packages as PARENT_ONLY
     */
    private Map<String, ClassLoaderLookupStrategy> buildClassLoaderLookupStrategy(List<MuleModule> muleModules)
    {
        final Map<String, ClassLoaderLookupStrategy> result = new HashMap<>();
        for (MuleModule muleModule : muleModules)
        {
            for (String exportedPackage : muleModule.getExportedPackages())
            {
                result.put(exportedPackage, ClassLoaderLookupStrategy.PARENT_ONLY);
            }
        }

        return result;
    }

    /**
     * Creates a {@link FilteringArtifactClassLoader} to filter the {@link ArtifactClassLoader} containerClassLoader given based on {@link List<MuleModule>} of muleModules.
     *
     * @param muleModules the list of {@link MuleModule}s to be used for defining the filter
     * @param containerClassLoader the {@link ArtifactClassLoader} for the container that will be used to delegate by the {@link FilteringContainerClassLoader}
     * @return a {@link FilteringContainerClassLoader} that would be the one used as the parent of plugins and applications {@link ArtifactClassLoader}
     */
    protected FilteringArtifactClassLoader createContainerFilteringClassLoader(List<MuleModule> muleModules, ArtifactClassLoader containerClassLoader)
    {
        return new FilteringContainerClassLoader(containerClassLoader, new ContainerClassLoaderFilterFactory().create(getBootPackages(), muleModules));
    }

    /**
     * @return a {@link Set} of packages that define all the prefixes that must be loaded from the container
     * classLoader without being filtered
     */
    protected Set<String> getBootPackages()
    {
        return BOOT_PACKAGES;
    }

}
