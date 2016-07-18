/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.runtime.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.launcher.ApplicationDescriptorFactory;
import org.mule.runtime.module.launcher.DeploymentException;
import org.mule.runtime.module.launcher.DeploymentListener;
import org.mule.runtime.module.launcher.artifact.ArtifactFactory;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.domain.Domain;
import org.mule.runtime.module.launcher.domain.DomainRepository;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptor;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginRepository;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates default mule applications
 */
public class DefaultApplicationFactory implements ArtifactFactory<Application>
{

    private final ArtifactClassLoaderFactory applicationClassLoaderFactory;
    private final ApplicationDescriptorFactory applicationDescriptorFactory;
    private final ApplicationPluginFactory applicationPluginFactory;
    private final DomainRepository domainRepository;
    private final ApplicationPluginRepository applicationPluginRepository;
    protected DeploymentListener deploymentListener;
    private PackageDiscoverer packageDiscoverer = new FilePackageDiscoverer();

    public DefaultApplicationFactory(ArtifactClassLoaderFactory<ApplicationDescriptor> applicationClassLoaderFactory, ApplicationDescriptorFactory applicationDescriptorFactory, ApplicationPluginFactory applicationPluginFactory, DomainRepository domainRepository, ApplicationPluginRepository applicationPluginRepository)
    {
        checkArgument(applicationClassLoaderFactory != null, "Application classLoader factory cannot be null");
        checkArgument(applicationDescriptorFactory != null, "Application descriptor factory cannot be null");
        checkArgument(applicationPluginFactory != null, "Application plugin factory cannot be null");
        checkArgument(domainRepository != null, "Domain repository cannot be null");
        checkArgument(applicationPluginRepository != null, "Application plugin repository cannot be null");

        this.applicationClassLoaderFactory = applicationClassLoaderFactory;
        this.applicationDescriptorFactory = applicationDescriptorFactory;
        this.applicationPluginFactory = applicationPluginFactory;
        this.domainRepository = domainRepository;
        this.applicationPluginRepository = applicationPluginRepository;

    }

    public void setDeploymentListener(DeploymentListener deploymentListener)
    {
        this.deploymentListener = deploymentListener;
    }

    public Application createArtifact(String appName) throws IOException
    {
        if (appName.contains(" "))
        {
            throw new IllegalArgumentException("Mule application name may not contain spaces: " + appName);
        }

        final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();
        final ApplicationDescriptor descriptor = applicationDescriptorFactory.create(new File(appsDir, appName));

        return createAppFrom(descriptor);
    }

    @Override
    public File getArtifactDir()
    {
        return MuleContainerBootstrapUtils.getMuleAppsDir();
    }

    private static final String SHARED_LIB_ARTIFACT_NAME = "sharedLibs";

    protected Application createAppFrom(ApplicationDescriptor descriptor) throws IOException
    {
        Domain domain = domainRepository.getDomain(descriptor.getDomain());
        if (domain == null)
        {
            throw new DeploymentException(CoreMessages.createStaticMessage(format("Domain '%s' has to be deployed in order to deploy Application '%s'", descriptor.getDomain(), descriptor.getName())));
        }

        ArtifactClassLoader parent = domain.getArtifactClassLoader();

        parent = getSharedLibClassLoader(descriptor, parent);

        final List<ApplicationPlugin> applicationPlugins = createContainerApplicationPlugins(parent, descriptor);
        applicationPlugins.addAll(createApplicationPlugins(parent, descriptor.getPlugins()));

        if (!applicationPlugins.isEmpty())
        {
          parent = createCompositePluginClassLoader(parent, applicationPlugins);
        }

        final ArtifactClassLoader deploymentClassLoader = applicationClassLoaderFactory.create(parent, descriptor);
        DefaultMuleApplication delegate = new DefaultMuleApplication(descriptor, deploymentClassLoader, applicationPlugins, domainRepository);

        if (deploymentListener != null)
        {
            delegate.setDeploymentListener(deploymentListener);
        }

        return new ApplicationWrapper(delegate);
    }

    private ArtifactClassLoader getSharedLibClassLoader(ApplicationDescriptor descriptor, ArtifactClassLoader parent)
    {
        Map<String, ClassLoaderLookupStrategy> lookupStrategies = emptyMap();
        URL[] pluginLibs = descriptor.getSharedPluginLibs();
        if (pluginLibs != null && pluginLibs.length != 0)
        {
            lookupStrategies = getLookStrategiesFrom(pluginLibs);
        }
        ClassLoaderLookupPolicy lookupPolicy = parent.getClassLoaderLookupPolicy().extend(lookupStrategies);

        return new MuleArtifactClassLoader(SHARED_LIB_ARTIFACT_NAME, pluginLibs, parent.getClassLoader(), lookupPolicy);
    }

    private ArtifactClassLoader createCompositePluginClassLoader(ArtifactClassLoader parent, List<ApplicationPlugin> applicationPlugins)
    {
        List<ArtifactClassLoader> classLoaders = new LinkedList<>();

        // Adds parent classloader first to use parent-first lookup approach
        classLoaders.add(parent);

        for (ApplicationPlugin plugin : applicationPlugins)
        {
            final FilteringArtifactClassLoader filteringPluginClassLoader = new FilteringArtifactClassLoader(plugin.getArtifactClassLoader(), plugin.getDescriptor().getClassLoaderFilter());

            classLoaders.add(filteringPluginClassLoader);
        }

        return new CompositeArtifactClassLoader("appPlugins", parent.getClassLoader(), classLoaders, parent.getClassLoaderLookupPolicy());
    }

    private List<ApplicationPlugin> createContainerApplicationPlugins(ArtifactClassLoader parentClassLoader, ApplicationDescriptor appDescriptor)
    {
        final List<ApplicationPlugin> plugins = new LinkedList<>();

        for (ApplicationPluginDescriptor appPluginDescriptor : applicationPluginRepository.getContainerApplicationPluginDescriptors())
        {
            if (appDescriptor.containsApplicationPluginDescriptor(appPluginDescriptor))
            {
                final String msg = String.format("Failed to deploy artifact [%s], plugin [%s] is already bundled within the container and cannot be included in artifact", appDescriptor.getName(), appPluginDescriptor.getName());
                throw new DeploymentException(createStaticMessage(msg));
            }

            plugins.add(applicationPluginFactory.create(appPluginDescriptor, parentClassLoader));
        }

        return plugins;
    }

    private List<ApplicationPlugin> createApplicationPlugins(ArtifactClassLoader parentClassLoader, Set<ApplicationPluginDescriptor> pluginDescriptors)
    {
        return pluginDescriptors.stream().map(descriptor -> applicationPluginFactory.create(descriptor, parentClassLoader)).collect(Collectors.toCollection(() -> new LinkedList<>()));
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
}
