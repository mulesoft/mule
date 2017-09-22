/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import static com.google.common.base.Optional.fromNullable;

import org.mule.module.launcher.domain.DefaultDomainFactory;
import org.mule.module.launcher.domain.MuleDomainClassLoaderRepository;

import java.io.IOException;
import java.util.Properties;

import com.google.common.base.Optional;

/**
 * Creates a {@link ApplicationFactory} that returns {@link TestApplicationWrapper}
 * instances in order to simulate errors on application deployment phases.
 */
public class TestApplicationFactory extends DefaultApplicationFactory
{

    private boolean failOnStopApplication;
    private boolean failOnDisposeApplication;

    public TestApplicationFactory(ApplicationClassLoaderFactory applicationClassLoaderFactory)
    {
        super(applicationClassLoaderFactory, new DefaultDomainFactory(new MuleDomainClassLoaderRepository()));
    }

    @Override
    public Application createArtifact(String artifactName) throws IOException
    {
        return createArtifact(artifactName, fromNullable(new Properties()));
    }
    
    @Override
    public Application createArtifact(String appName, Optional<Properties> deploymentProperties) throws IOException
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
}
