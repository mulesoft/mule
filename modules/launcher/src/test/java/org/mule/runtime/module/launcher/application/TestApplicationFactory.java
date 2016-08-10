/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.launcher.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.module.launcher.ApplicationDescriptorFactory;
import org.mule.runtime.module.launcher.domain.DomainManager;
import org.mule.runtime.module.launcher.domain.DomainRepository;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginRepository;
import org.mule.runtime.module.launcher.service.ServiceRepository;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Creates a {@link DefaultApplicationFactory} that returns {@link TestApplicationWrapper}
 * instances in order to simulate errors on application deployment phases.
 */
public class TestApplicationFactory extends DefaultApplicationFactory
{

    private boolean failOnStopApplication;
    private boolean failOnDisposeApplication;

    public TestApplicationFactory(ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory, ApplicationDescriptorFactory applicationDescriptorFactory, ArtifactPluginRepository artifactPluginRepository, DomainRepository domainRepository, ServiceRepository serviceRepository)
    {
        super(applicationClassLoaderBuilderFactory, applicationDescriptorFactory, artifactPluginRepository, domainRepository, serviceRepository);
    }

    public static TestApplicationFactory createTestApplicationFactory(MuleApplicationClassLoaderFactory applicationClassLoaderFactory, DomainManager domainManager, ServiceRepository serviceRepository)
    {
        DefaultArtifactClassLoaderFilterFactory classLoaderFilterFactory = new DefaultArtifactClassLoaderFilterFactory();
        ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory = new ArtifactPluginDescriptorFactory(classLoaderFilterFactory);
        ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader = new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory);
        TestEmptyApplicationPluginRepository applicationPluginRepository = new TestEmptyApplicationPluginRepository();
        ApplicationDescriptorFactory applicationDescriptorFactory = new ApplicationDescriptorFactory(artifactPluginDescriptorLoader, applicationPluginRepository);
        ArtifactPluginClassLoaderFactory artifactPluginClassLoaderFactory = new ArtifactPluginClassLoaderFactory();
        DefaultArtifactPluginFactory applicationPluginFactory = new DefaultArtifactPluginFactory(artifactPluginClassLoaderFactory);
        ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory = new ApplicationClassLoaderBuilderFactory(applicationClassLoaderFactory, applicationPluginRepository, applicationPluginFactory);
        return new TestApplicationFactory(applicationClassLoaderBuilderFactory, applicationDescriptorFactory, applicationPluginRepository, domainManager, serviceRepository);
    }

    @Override
    public Application createArtifact(String appName) throws IOException
    {
        Application app = super.createArtifact(appName);

        TestApplicationWrapper testApplicationWrapper = new TestApplicationWrapper(app);
        testApplicationWrapper.setFailOnDisposeApplication(failOnDisposeApplication);
        testApplicationWrapper.setFailOnStopApplication(failOnStopApplication);

        return testApplicationWrapper;
    }

    public void setFailOnDisposeApplication(boolean failOnDisposeApplication)
    {
        this.failOnDisposeApplication = failOnDisposeApplication;
    }

    public void setFailOnStopApplication(boolean failOnStopApplication)
    {
        this.failOnStopApplication = failOnStopApplication;
    }


    private static class TestEmptyApplicationPluginRepository implements ArtifactPluginRepository
    {
        public List<ArtifactPluginDescriptor> getContainerArtifactPluginDescriptors()
        {
            return Collections.emptyList();
        }
    }
}
