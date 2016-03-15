/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.mule.module.artifact.classloader.ArtifactClassLoader;
import org.mule.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.module.artifact.classloader.CompositeClassLoader;
import org.mule.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.module.artifact.classloader.GoodCitizenClassLoader;
import org.mule.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.module.launcher.MuleApplicationClassLoader;
import org.mule.module.launcher.MuleFoldersUtil;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.domain.DomainClassLoaderRepository;
import org.mule.module.launcher.nativelib.NativeLibraryFinderFactory;
import org.mule.module.launcher.plugin.ApplicationPluginDescriptor;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link MuleApplicationClassLoader} instances based on the
 * application descriptor.
 */
public class MuleApplicationClassLoaderFactory implements ArtifactClassLoaderFactory<ApplicationDescriptor>
{

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DomainClassLoaderRepository domainClassLoaderRepository;
    private final NativeLibraryFinderFactory nativeLibraryFinderFactory;

    public MuleApplicationClassLoaderFactory(DomainClassLoaderRepository domainClassLoaderRepository, NativeLibraryFinderFactory nativeLibraryFinderFactory)
    {
        this.domainClassLoaderRepository = domainClassLoaderRepository;
        this.nativeLibraryFinderFactory = nativeLibraryFinderFactory;
    }

    @Override
    public ArtifactClassLoader create(ApplicationDescriptor descriptor)
    {
        ClassLoader parent = getParentClassLoader(descriptor);

        List<URL> urls = new LinkedList<>();
        try
        {
            urls.add(MuleFoldersUtil.getAppClassesFolder(descriptor.getName()).toURI().toURL());
            urls.addAll(findJars(descriptor.getName(), MuleFoldersUtil.getAppLibFolder(descriptor.getName()), true));
            urls.addAll(findJars(descriptor.getName(), MuleFoldersUtil.getMulePerAppLibFolder(), true));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to create classloader for application", e);
        }

        return new MuleApplicationClassLoader(descriptor.getName(), parent, descriptor.getLoaderOverrides(), nativeLibraryFinderFactory.create(descriptor.getName()), urls);
    }

    /**
     * Add jars from the supplied directory to the class path
     */
    private List<URL> findJars(String appName, File dir, boolean verbose) throws MalformedURLException
    {
        List<URL> result = new LinkedList<>();

        if (dir.exists() && dir.canRead())
        {
            @SuppressWarnings("unchecked")
            Collection<File> jars = FileUtils.listFiles(dir, new String[]{"jar"}, false);

            if (!jars.isEmpty() && logger.isInfoEnabled())
            {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("[%s] Loading the following jars:%n", appName));
                sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                for (File jar : jars)
                {
                    sb.append(jar.toURI().toURL()).append(SystemUtils.LINE_SEPARATOR);
                }

                sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                if (verbose)
                {
                    logger.info(sb.toString());
                }
                else
                {
                    logger.debug(sb.toString());
                }
            }

            for (File jar : jars)
            {
                result.add(jar.toURI().toURL());
            }
        }

        return result;
    }

    private ClassLoader getParentClassLoader(ApplicationDescriptor descriptor)
    {
        final String domain = descriptor.getDomain();
        ClassLoader parent;
        if (domain == null)
        {
            parent = domainClassLoaderRepository.getDefaultDomainClassLoader().getClassLoader();
        }
        else
        {
            parent = domainClassLoaderRepository.getDomainClassLoader(domain).getClassLoader();
        }
        final Set<ApplicationPluginDescriptor> plugins = descriptor.getPlugins();
        if (!plugins.isEmpty())
        {
            // Re-assigns parent classloader if there are shared plugin libraries
            URL[] pluginLibs = descriptor.getSharedPluginLibs();
            if (pluginLibs != null && pluginLibs.length != 0)
            {
                parent = new GoodCitizenClassLoader(pluginLibs, parent);
            }

            // Defines a new parent classLoader from plugins
            parent = createPluginsClassLoader(parent, plugins);
        }

        return parent;
    }

    private ClassLoader createPluginsClassLoader(ClassLoader parent, Set<ApplicationPluginDescriptor> plugins)
    {
        List<ClassLoader> classLoaders = new LinkedList<>();

        // Adds parent classloader first to use parent-first lookup approach
        classLoaders.add(parent);

        for (ApplicationPluginDescriptor descriptor : plugins)
        {
            URL[] urls = new URL[descriptor.getRuntimeLibs().length + 1];
            urls[0] = descriptor.getRuntimeClassesDir();
            System.arraycopy(descriptor.getRuntimeLibs(), 0, urls, 1, descriptor.getRuntimeLibs().length);

            final MuleArtifactClassLoader pluginClassLoader = new MuleArtifactClassLoader(descriptor.getName(), urls, parent, descriptor.getLoaderOverrides());

            ArtifactClassLoaderFilter filter = new ArtifactClassLoaderFilter(descriptor);
            classLoaders.add(new FilteringArtifactClassLoader(descriptor.getName(), pluginClassLoader, filter));
        }

        return new CompositeClassLoader(parent, classLoaders);
    }
}
