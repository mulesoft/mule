/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.launcher.ApplicationDescriptorFactory;
import org.mule.runtime.module.launcher.domain.DomainManager;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptor;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptorFactory;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginRepository;

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

    public TestApplicationFactory(ArtifactClassLoaderFactory applicationClassLoaderFactory, DomainManager domainManager)
    {
        super(applicationClassLoaderFactory, new ApplicationDescriptorFactory(new ApplicationPluginDescriptorFactory(new DefaultArtifactClassLoaderFilterFactory()), new TestEmptyApplicationPluginRepository()), new DefaultApplicationPluginFactory(new ApplicationPluginClassLoaderFactory()), domainManager, new TestEmptyApplicationPluginRepository());
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


    private static class TestEmptyApplicationPluginRepository implements ApplicationPluginRepository
    {
        @Override
        public List<ApplicationPluginDescriptor> getContainerApplicationPluginDescriptors()
        {
            return Collections.emptyList();
        }
    }
}
