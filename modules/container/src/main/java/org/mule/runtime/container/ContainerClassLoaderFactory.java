/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container;

import org.mule.runtime.container.internal.ContainerClassLoaderFilterFactory;
import org.mule.runtime.container.internal.FilteringContainerClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleClassLoaderLookupPolicy;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
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
            "java", "javax", "org.apache.xerces", "org.mule.mvel2",
            "org.apache.logging.log4j", "org.slf4j", "org.apache.commons.logging", "org.apache.log4j",
            "org.dom4j", "org.w3c.dom", "com.sun", "sun", "org.springframework"
    );

    /**
     * Creates the classLoader to represent the Mule container.
     *
     * @param parentClassLoader parent classLoader. Can be null.
     * @return a non null {@link ArtifactClassLoader} containing container code that can be used as
     * parent classloader for other mule artifacts.
     */
    public ArtifactClassLoader createContainerClassLoader(final ClassLoader parentClassLoader)
    {
        final Set<String> parentOnlyPackages = new HashSet<>(BOOT_PACKAGES);
        parentOnlyPackages.addAll(SYSTEM_PACKAGES);
        final MuleClassLoaderLookupPolicy containerLookupPolicy = new MuleClassLoaderLookupPolicy(Collections.emptyMap(), parentOnlyPackages);
        final ArtifactClassLoader containerClassLoader = new MuleArtifactClassLoader("mule", new URL[0], parentClassLoader, containerLookupPolicy)
        {
            @Override
            public URL findResource(String name)
            {
                // Container classLoader is just an adapter, must find resources on the parent
                return parentClassLoader.getResource(name);
            }

            @Override
            public Enumeration<URL> findResources(String name) throws IOException
            {
                // Container classLoader is just an adapter, must find resources on the parent
                return parentClassLoader.getResources(name);
            }
        };

        return createContainerFilteringClassLoader(containerClassLoader);
    }

    private FilteringArtifactClassLoader createContainerFilteringClassLoader(ArtifactClassLoader containerClassLoader)
    {
        return new FilteringContainerClassLoader(containerClassLoader, new ContainerClassLoaderFilterFactory().create(BOOT_PACKAGES));
    }
}
