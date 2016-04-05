/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import static java.util.Collections.emptyMap;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.mule.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import org.mule.module.artifact.classloader.ArtifactClassLoader;
import org.mule.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.module.launcher.MuleApplicationClassLoader;
import org.mule.module.launcher.MuleFoldersUtil;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.domain.DomainClassLoaderRepository;
import org.mule.module.launcher.nativelib.NativeLibraryFinderFactory;
import org.mule.module.launcher.plugin.ApplicationPluginDescriptor;
import org.mule.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link MuleApplicationClassLoader} instances based on the
 * application descriptor.
 */
public class MuleApplicationClassLoaderFactory implements ArtifactClassLoaderFactory<ApplicationDescriptor>
{

    public static final String CLASS_EXTENSION = ".class";
    private static final String SHARED_LIB_ARTIFACT_NAME = "sharedLibs";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DomainClassLoaderRepository domainClassLoaderRepository;
    private final NativeLibraryFinderFactory nativeLibraryFinderFactory;

    private PackageDiscoverer packageDiscoverer = new FilePackageDiscoverer();

    public MuleApplicationClassLoaderFactory(DomainClassLoaderRepository domainClassLoaderRepository, NativeLibraryFinderFactory nativeLibraryFinderFactory)
    {
        this.domainClassLoaderRepository = domainClassLoaderRepository;
        this.nativeLibraryFinderFactory = nativeLibraryFinderFactory;
    }

    public void setPackageDiscoverer(PackageDiscoverer packageDiscoverer)
    {
        this.packageDiscoverer = packageDiscoverer;
    }

    @Override
    public ArtifactClassLoader create(ApplicationDescriptor descriptor)
    {
        List<URL> urls = getApplicationResourceUrls(descriptor);
        final String domain = descriptor.getDomain();
        ArtifactClassLoader parent;
        if (domain == null)
        {
            parent = domainClassLoaderRepository.getDefaultDomainClassLoader();
        }
        else
        {
            parent = domainClassLoaderRepository.getDomainClassLoader(domain);
        }

        final Set<ApplicationPluginDescriptor> plugins = descriptor.getPlugins();
        ClassLoaderLookupPolicy lookupPolicy = parent.getClassLoaderLookupPolicy();

        if (!plugins.isEmpty())
        {
            // Re-assigns parent classloader if there are shared plugin libraries
            final Map<String, ClassLoaderLookupStrategy> lookupStrategies;
            URL[] pluginLibs = descriptor.getSharedPluginLibs();
            if (pluginLibs != null && pluginLibs.length != 0)
            {
                lookupStrategies = getLookStrategiesFrom(pluginLibs);
                lookupPolicy = lookupPolicy.extend(lookupStrategies);
                parent = new MuleArtifactClassLoader(SHARED_LIB_ARTIFACT_NAME, pluginLibs, parent.getClassLoader(), lookupPolicy);
            }

            // Defines a new parent classLoader from plugins
            parent = createPluginsClassLoader(parent, plugins);
        }
        else
        {
            // Cannot share the same lookup policy across different applications in the domain
           lookupPolicy = lookupPolicy.extend(emptyMap());
        }

        return new MuleApplicationClassLoader(descriptor.getName(), parent.getClassLoader(), nativeLibraryFinderFactory.create(descriptor.getName()), urls, lookupPolicy);
    }

    private List<URL> getApplicationResourceUrls(ApplicationDescriptor descriptor)
    {
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

        return urls;
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
            Collection<File> jars = listFiles(dir, new String[] {"jar"}, false);

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

    private Map<String, ClassLoaderLookupStrategy> getLookStrategiesFrom(URL[] libraries)
    {
        final Map<String, ClassLoaderLookupStrategy> result = new HashMap<>();

        for (URL library : libraries)
        {
            Set<String> packages = packageDiscoverer.findPackages(library);
            for (String packageName : packages)
            {
                result.put(packageName, PARENT_FIRST);
            }
        }

        return result;
    }

    private ArtifactClassLoader createPluginsClassLoader(ArtifactClassLoader parent, Set<ApplicationPluginDescriptor> plugins)
    {
        List<ClassLoader> classLoaders = new LinkedList<>();

        // Adds parent classloader first to use parent-first lookup approach
        classLoaders.add(parent.getClassLoader());

        for (ApplicationPluginDescriptor descriptor : plugins)
        {
            URL[] urls = new URL[descriptor.getRuntimeLibs().length + 1];
            urls[0] = descriptor.getRuntimeClassesDir();
            System.arraycopy(descriptor.getRuntimeLibs(), 0, urls, 1, descriptor.getRuntimeLibs().length);

            final MuleArtifactClassLoader pluginClassLoader = new MuleArtifactClassLoader(descriptor.getName(), urls, parent.getClassLoader(), parent.getClassLoaderLookupPolicy());

            classLoaders.add(new FilteringArtifactClassLoader(descriptor.getName(), pluginClassLoader, descriptor.getClassLoaderFilter()));
        }

        return new CompositeArtifactClassLoader("appPlugins", parent.getClassLoader(), classLoaders, parent.getClassLoaderLookupPolicy());
    }
}
