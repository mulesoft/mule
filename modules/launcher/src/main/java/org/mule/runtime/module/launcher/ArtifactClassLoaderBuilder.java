/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.collections.CollectionUtils.find;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.Preconditions.checkState;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.reboot.MuleContainerBootstrapUtils.getMuleTmpDir;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.runtime.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.launcher.application.ArtifactPlugin;
import org.mule.runtime.module.launcher.application.ArtifactPluginFactory;
import org.mule.runtime.module.launcher.application.CompositeArtifactClassLoader;
import org.mule.runtime.module.launcher.application.FilePackageDiscoverer;
import org.mule.runtime.module.launcher.application.PackageDiscoverer;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginRepository;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link ArtifactClassLoader} builder for class loaders used by mule artifacts such as domains or applications.
 *
 * Allows to construct a classloader when using a set of artifact plugins and takes into account default plugins
 * provided by the runtime and the shared libraries configured for the plugins.
 *
 * @since 4.0
 */
public class ArtifactClassLoaderBuilder
{
    private static final String SHARED_LIB_ARTIFACT_NAME = "sharedLibs";

    private final ArtifactClassLoaderFactory applicationClassLoaderFactory;
    private final PackageDiscoverer packageDiscoverer = new FilePackageDiscoverer();
    private final ArtifactPluginRepository artifactPluginRepository;
    private final ArtifactPluginFactory artifactPluginFactory;
    private File[] applicationPluginDependencies = new File[0];
    private Set<ArtifactPluginDescriptor> artifactPluginDescriptors = new HashSet<>();
    private ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;
    private File pluginsSharedLibFolder;
    private String artifactId = UUID.getUUID();
    private ArtifactDescriptor artifactDescriptor;
    private ArtifactClassLoader parentClassLoader;

    public ArtifactClassLoaderBuilder(ArtifactClassLoaderFactory artifactClassLoaderFactory,
                                      ArtifactPluginRepository artifactPluginRepository,
                                      ArtifactPluginFactory artifactPluginFactory,
                                      ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader)
    {
        this.applicationClassLoaderFactory = artifactClassLoaderFactory;
        this.artifactPluginRepository = artifactPluginRepository;
        this.artifactPluginFactory = artifactPluginFactory;
        this.artifactPluginDescriptorLoader = artifactPluginDescriptorLoader;
    }

    /**
     * @param parentClassLoader parent class loader for the artifact class loader.
     * @return the builder
     */
    public ArtifactClassLoaderBuilder setParentClassLoader(ArtifactClassLoader parentClassLoader)
    {
        this.parentClassLoader = parentClassLoader;
        return this;
    }

    /**
     * @param artifactId unique identifier for this artifact. For instance, for Applications, it can be the app name.
     * @return the builder
     */
    public ArtifactClassLoaderBuilder setArtifactId(String artifactId)
    {
        this.artifactId = artifactId;
        return this;
    }

    /**
     * @param pluginsSharedLibFolder folder in which libraries shared by the plugins are located
     * @return the builder
     */
    public ArtifactClassLoaderBuilder setPluginsSharedLibFolder(File pluginsSharedLibFolder)
    {
        this.pluginsSharedLibFolder = pluginsSharedLibFolder;
        return this;
    }

    /**
     * @param plugins set of plugins descriptors that will be used by the application.
     * @return the builder
     */
    public ArtifactClassLoaderBuilder addArtifactPluginDescriptor(ArtifactPluginDescriptor... plugins)
    {
        this.artifactPluginDescriptors.addAll(asList(plugins));
        return this;
    }

    /**
     * @param artifactDescriptor the descriptor of the artifact for which the class loader is going to be created.
     * @return the builder
     */
    public ArtifactClassLoaderBuilder setArtifactDescriptor(ArtifactDescriptor artifactDescriptor)
    {
        this.artifactDescriptor = artifactDescriptor;
        return this;
    }

    public ArtifactClassLoader build() throws IOException
    {
        checkState(artifactDescriptor != null, "artifact descriptor cannot be null");
        checkState(parentClassLoader != null, "parent class loader cannot be null");

        parentClassLoader = getSharedLibClassLoader(parentClassLoader);

        for (File applicationPluginDependency : applicationPluginDependencies)
        {
            ArtifactPluginDescriptor artifactPluginDescriptor = artifactPluginDescriptorLoader.load(applicationPluginDependency, new File(getMuleTmpDir(), UUID.getUUID()));
            artifactPluginDescriptors.add(artifactPluginDescriptor);
        }

        List<ArtifactPluginDescriptor> effectiveArtifactPluginDescriptors = createContainerApplicationPlugins();
        effectiveArtifactPluginDescriptors.addAll(artifactPluginDescriptors);

        if (!effectiveArtifactPluginDescriptors.isEmpty())
        {
            parentClassLoader = createCompositePluginClassLoader(parentClassLoader, effectiveArtifactPluginDescriptors);
        }
        return applicationClassLoaderFactory.create(parentClassLoader, artifactDescriptor);
    }

    private ArtifactClassLoader getSharedLibClassLoader(ArtifactClassLoader parent) throws MalformedURLException
    {
        Set<URL> urls = new HashSet<>();

        if (pluginsSharedLibFolder.exists())
        {
            Collection<File> jars = FileUtils.listFiles(pluginsSharedLibFolder, new String[] {"jar"}, false);

            for (File jar : jars)
            {
                urls.add(jar.toURI().toURL());
            }
        }

        URL[] pluginLibs = urls.toArray(new URL[0]);
        Map<String, ClassLoaderLookupStrategy> lookupStrategies = emptyMap();
        if (pluginLibs != null && pluginLibs.length != 0)
        {
            lookupStrategies = getLookStrategiesFrom(pluginLibs);
        }
        ClassLoaderLookupPolicy lookupPolicy = parent.getClassLoaderLookupPolicy().extend(lookupStrategies);

        return new MuleArtifactClassLoader(SHARED_LIB_ARTIFACT_NAME, pluginLibs, parent.getClassLoader(), lookupPolicy);
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

    private List<ArtifactPluginDescriptor> createContainerApplicationPlugins()
    {
        final List<ArtifactPluginDescriptor> containerPlugins = new LinkedList<>();
        for (ArtifactPluginDescriptor appPluginDescriptor : artifactPluginRepository.getContainerArtifactPluginDescriptors())
        {
            if (containsApplicationPluginDescriptor(appPluginDescriptor))
            {
                final String msg = String.format("Failed to deploy artifact [%s], plugin [%s] is already bundled within the container and cannot be included in artifact", artifactId, appPluginDescriptor.getName());
                throw new DeploymentException(createStaticMessage(msg));
            }

            containerPlugins.add(appPluginDescriptor);
        }
        return containerPlugins;
    }

    /**
     * @param appPluginDescriptor
     * @return true if this application has the given appPluginDescriptor already defined in its artifactPluginDescriptors list.
     */
    private boolean containsApplicationPluginDescriptor(ArtifactPluginDescriptor appPluginDescriptor)
    {
        return find(this.artifactPluginDescriptors, object -> ((ArtifactPluginDescriptor) object).getName().equals(appPluginDescriptor.getName())) != null;
    }

    private ArtifactClassLoader createCompositePluginClassLoader(ArtifactClassLoader parent, List<ArtifactPluginDescriptor> artifactPluginDescriptors)
    {
        List<ArtifactClassLoader> classLoaders = new LinkedList<>();

        // Adds parent classloader first to use parent-first lookup approach
        classLoaders.add(parent);

        for (ArtifactPluginDescriptor artifactPluginDescriptor : artifactPluginDescriptors)
        {
            ArtifactPlugin artifactPlugin = artifactPluginFactory.create(artifactPluginDescriptor, parent);
            final FilteringArtifactClassLoader filteringPluginClassLoader = new FilteringArtifactClassLoader(artifactPlugin.getArtifactClassLoader(), artifactPlugin.getDescriptor().getClassLoaderFilter());

            classLoaders.add(filteringPluginClassLoader);
        }
        return new CompositeArtifactClassLoader("appPlugins", parent.getClassLoader(), classLoaders, parent.getClassLoaderLookupPolicy());
    }


}
