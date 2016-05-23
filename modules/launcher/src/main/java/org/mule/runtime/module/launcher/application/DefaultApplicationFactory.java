/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.FilenameUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.runtime.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.launcher.ApplicationDescriptorFactory;
import org.mule.runtime.module.launcher.DeploymentException;
import org.mule.runtime.module.launcher.DeploymentListener;
import org.mule.runtime.module.launcher.MuleFoldersUtil;
import org.mule.runtime.module.launcher.artifact.ArtifactFactory;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.domain.DomainRepository;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptor;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptorFactory;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Creates default mule applications
 */
public class DefaultApplicationFactory implements ArtifactFactory<Application>
{

    private final ArtifactClassLoaderFactory applicationClassLoaderFactory;
    private final ApplicationDescriptorFactory applicationDescriptorFactory;
    private final ApplicationPluginDescriptorFactory pluginDescriptorFactory = new ApplicationPluginDescriptorFactory(new ArtifactClassLoaderFilterFactory());
    private final ApplicationPluginFactory applicationPluginFactory = new DefaultApplicationPluginFactory();
    private final DomainRepository domainRepository;
    protected DeploymentListener deploymentListener;
    private PackageDiscoverer packageDiscoverer = new FilePackageDiscoverer();

    public DefaultApplicationFactory(ArtifactClassLoaderFactory<ApplicationDescriptor> applicationClassLoaderFactory, ApplicationDescriptorFactory applicationDescriptorFactory, DomainRepository domainRepository)
    {
        this.applicationClassLoaderFactory = applicationClassLoaderFactory;
        this.applicationDescriptorFactory = applicationDescriptorFactory;
        this.domainRepository = domainRepository;

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
        ArtifactClassLoader parent = domainRepository.getDomain(descriptor.getDomain()).getArtifactClassLoader();

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

    private List<ApplicationPlugin> createContainerApplicationPlugins(ArtifactClassLoader parentClassLoader, ApplicationDescriptor appDescriptor) throws IOException
    {
        final List<ApplicationPlugin> plugins = new LinkedList<>();

        File[] containerPlugins = MuleFoldersUtil.getContainerAppPluginsFolder().listFiles();
        if (containerPlugins != null)
        {
            for (File pluginFolder : MuleFoldersUtil.getContainerAppPluginsFolder().listFiles((FileFilter) new SuffixFileFilter(".zip", IOCase.INSENSITIVE)))
            {
                String pluginName = FilenameUtils.removeExtension(pluginFolder.getName());
                if (appDescriptor.containsApplicationPlugin(pluginName))
                {
                    final String msg = String.format("Failed to deploy artifact [%s], plugin [%s] is already bundled within the container and cannot be included in artifact", appDescriptor.getName(), pluginName);
                    throw new DeploymentException(MessageFactory.createStaticMessage(msg));
                }

                // must unpack as there's no straightforward way for a ClassLoader to use a zip within another jar/zip
                final File tmpDir = new File(MuleContainerBootstrapUtils.getMuleTmpDir(),
                                             appDescriptor.getName() + File.separator + MuleFoldersUtil.PLUGINS_FOLDER + File.separator + pluginName);
                FileUtils.unzip(pluginFolder, tmpDir);
                final ApplicationPluginDescriptor appPluginDescriptor = pluginDescriptorFactory.create(tmpDir);

                plugins.add(applicationPluginFactory.create(appPluginDescriptor, parentClassLoader));
            }
        }

        return plugins;
    }

    private List<ApplicationPlugin> createApplicationPlugins(ArtifactClassLoader parentClassLoader, Set<ApplicationPluginDescriptor> pluginDescriptors)
    {
        final List<ApplicationPlugin> plugins = new LinkedList<>();

        for (ApplicationPluginDescriptor descriptor : pluginDescriptors)
        {
            plugins.add(applicationPluginFactory.create(descriptor, parentClassLoader));
        }

        return plugins;
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
