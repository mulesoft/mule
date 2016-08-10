/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.collections.CollectionUtils.find;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.Preconditions.checkState;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.launcher.application.ArtifactPlugin;
import org.mule.runtime.module.launcher.application.ArtifactPluginFactory;
import org.mule.runtime.module.launcher.application.CompositeArtifactClassLoader;
import org.mule.runtime.module.launcher.application.FilePackageDiscoverer;
import org.mule.runtime.module.launcher.application.PackageDiscoverer;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginRepository;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for all artifacts class loader filters.
 *
 * @param <T> the type of the filer.
 * @since 4.0
 */
public abstract class AbstractArtifactClassLoaderBuilder<T extends AbstractArtifactClassLoaderBuilder>
{

    private static final String SHARED_LIB_ARTIFACT_NAME = "sharedLibs";

    private final DeployableArtifactClassLoaderFactory artifactClassLoaderFactory;
    private final PackageDiscoverer packageDiscoverer = new FilePackageDiscoverer();
    private final ArtifactPluginRepository artifactPluginRepository;
    private final ArtifactPluginFactory artifactPluginFactory;
    private Set<ArtifactPluginDescriptor> artifactPluginDescriptors = new HashSet<>();
    private File pluginsSharedLibFolder;
    private String artifactId = UUID.getUUID();
    private ArtifactDescriptor artifactDescriptor;
    private ArtifactClassLoader parentClassLoader;
    private List<ArtifactClassLoader> artifactPluginClassLoaders = new ArrayList<>();

    /**
     * Creates an {@link AbstractArtifactClassLoaderBuilder}.
     *
     * @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes. Must be not null.
     * @param artifactPluginRepository   repository of plugins contained by the runtime. Must be not null.
     * @param artifactPluginFactory      factory for creating artifact plugins. Must be not null.
     */
    public AbstractArtifactClassLoaderBuilder(DeployableArtifactClassLoaderFactory artifactClassLoaderFactory,
                                              ArtifactPluginRepository artifactPluginRepository,
                                              ArtifactPluginFactory artifactPluginFactory)
    {
        checkArgument(artifactClassLoaderFactory != null, "artifact class loader factory cannot be null");
        checkArgument(artifactPluginRepository != null, "artifact plugin repository cannot be null");
        checkArgument(artifactPluginFactory != null, "artifact plugin factory cannot be null");
        this.artifactClassLoaderFactory = artifactClassLoaderFactory;
        this.artifactPluginRepository = artifactPluginRepository;
        this.artifactPluginFactory = artifactPluginFactory;
    }

    /**
     * Implementation must redefine this method and it should provide the root class loader which
     * is going to be used as parent class loader for every other class loader created by this builder.
     *
     * @return the root class loader for all other class loaders
     */
    abstract ArtifactClassLoader getParentClassLoader();

    /**
     * @param artifactId unique identifier for this artifact. For instance, for Applications, it can be the app name. Must be not null.
     * @return the builder
     */
    public T setArtifactId(String artifactId)
    {
        checkArgument(artifactId != null, "artifact id cannot be null");
        this.artifactId = artifactId;
        return (T) this;
    }

    /**
     * @param pluginsSharedLibFolder folder in which libraries shared by the plugins are located
     * @return the builder
     */
    public T setPluginsSharedLibFolder(File pluginsSharedLibFolder)
    {
        checkArgument(pluginsSharedLibFolder != null, "plugins shared lib folder cannot be null");
        this.pluginsSharedLibFolder = pluginsSharedLibFolder;
        return (T) this;
    }

    /**
     * @param artifactPluginDescriptors set of plugins descriptors that will be used by the application.
     * @return the builder
     */
    public T addArtifactPluginDescriptors(ArtifactPluginDescriptor... artifactPluginDescriptors)
    {
        checkArgument(artifactPluginDescriptors != null, "artifact plugin descriptors cannot be null");
        this.artifactPluginDescriptors.addAll(asList(artifactPluginDescriptors));
        return (T) this;
    }

    /**
     * @param artifactDescriptor the descriptor of the artifact for which the class loader is going to be created.
     * @return the builder
     */
    public T setArtifactDescriptor(ArtifactDescriptor artifactDescriptor)
    {
        this.artifactDescriptor = artifactDescriptor;
        return (T) this;
    }

    /**
     * Creates a new {@code ArtifactClassLoader} using the provided configuration. It will create
     * the proper class loader hierarchy and filters the artifact resources and plugins classes and
     * resources are resolve correctly.
     *
     * @return a {@code ArtifactClassLoader} created from the provided configuration.
     * @throws IOException exception cause when it was not possible to access the file provided as dependencies
     */
    public ArtifactClassLoader build() throws IOException
    {
        checkState(artifactDescriptor != null, "artifact descriptor cannot be null");
        parentClassLoader = getParentClassLoader();
        checkState(parentClassLoader != null, "parent class loader cannot be null");

        parentClassLoader = getSharedLibClassLoader(parentClassLoader);

        List<ArtifactPluginDescriptor> effectiveArtifactPluginDescriptors = createContainerApplicationPlugins();
        effectiveArtifactPluginDescriptors.addAll(artifactPluginDescriptors);

        if (!effectiveArtifactPluginDescriptors.isEmpty())
        {
            parentClassLoader = createCompositePluginClassLoader(parentClassLoader, effectiveArtifactPluginDescriptors);
        }
        return artifactClassLoaderFactory.create(parentClassLoader, artifactDescriptor, artifactPluginClassLoaders);
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
                final String msg = format("Failed to deploy artifact [%s], plugin [%s] is already bundled within the container and cannot be included in artifact", artifactId, appPluginDescriptor.getName());
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
            artifactPluginClassLoaders.add(artifactPlugin.getArtifactClassLoader());

            final FilteringArtifactClassLoader filteringPluginClassLoader = new FilteringArtifactClassLoader(artifactPlugin.getArtifactClassLoader(), artifactPlugin.getDescriptor().getClassLoaderFilter());
            classLoaders.add(filteringPluginClassLoader);
        }
        return new CompositeArtifactClassLoader("appPlugins", parent.getClassLoader(), classLoaders, parent.getClassLoaderLookupPolicy());
    }

}
