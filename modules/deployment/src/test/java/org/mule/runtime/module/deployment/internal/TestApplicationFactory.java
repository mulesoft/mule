/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginClassLoaderFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.artifact.DefaultDependenciesProvider;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.classloader.TrackingArtifactClassLoaderFactory;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.application.TestApplicationWrapper;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.service.ServiceRepository;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Creates a {@link DefaultApplicationFactory} that returns {@link TestApplicationWrapper} instances in order to simulate errors
 * on application deployment phases.
 */
public class TestApplicationFactory extends DefaultApplicationFactory {

  private boolean failOnStopApplication;
  private boolean failOnDisposeApplication;

  public TestApplicationFactory(ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory,
                                ApplicationDescriptorFactory applicationDescriptorFactory,
                                ArtifactPluginRepository artifactPluginRepository, DomainRepository domainRepository,
                                ServiceRepository serviceRepository, ClassLoaderRepository classLoaderRepository) {
    super(applicationClassLoaderBuilderFactory, applicationDescriptorFactory, artifactPluginRepository, domainRepository,
          serviceRepository, classLoaderRepository);
  }

  public static TestApplicationFactory createTestApplicationFactory(MuleApplicationClassLoaderFactory applicationClassLoaderFactory,
                                                                    DomainManager domainManager,
                                                                    ServiceRepository serviceRepository) {
    ArtifactClassLoaderFilterFactory classLoaderFilterFactory = new ArtifactClassLoaderFilterFactory();
    ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory =
        new ArtifactPluginDescriptorFactory();
    ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader =
        new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory);
    TestEmptyApplicationPluginRepository applicationPluginRepository = new TestEmptyApplicationPluginRepository();
    ApplicationDescriptorFactory applicationDescriptorFactory =
        new ApplicationDescriptorFactory(artifactPluginDescriptorLoader, applicationPluginRepository);
    final DefaultClassLoaderManager artifactClassLoaderManager = new DefaultClassLoaderManager();
    ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory =
        new ApplicationClassLoaderBuilderFactory(applicationClassLoaderFactory, applicationPluginRepository,
                                                 new TrackingArtifactClassLoaderFactory<>(artifactClassLoaderManager,
                                                                                          new ArtifactPluginClassLoaderFactory()),
                                                 artifactPluginDescriptorFactory, new DefaultDependenciesProvider());
    return new TestApplicationFactory(applicationClassLoaderBuilderFactory, applicationDescriptorFactory,
                                      applicationPluginRepository, domainManager, serviceRepository, artifactClassLoaderManager);
  }

  @Override
  public Application createArtifact(File appLocation) throws IOException {
    Application app = super.createArtifact(appLocation);

    TestApplicationWrapper testApplicationWrapper = new TestApplicationWrapper(app);
    testApplicationWrapper.setFailOnDisposeApplication(failOnDisposeApplication);
    testApplicationWrapper.setFailOnStopApplication(failOnStopApplication);

    return testApplicationWrapper;
  }

  public void setFailOnDisposeApplication(boolean failOnDisposeApplication) {
    this.failOnDisposeApplication = failOnDisposeApplication;
  }

  public void setFailOnStopApplication(boolean failOnStopApplication) {
    this.failOnStopApplication = failOnStopApplication;
  }


  private static class TestEmptyApplicationPluginRepository implements ArtifactPluginRepository {

    public List<ArtifactPluginDescriptor> getContainerArtifactPluginDescriptors() {
      return Collections.emptyList();
    }
  }
}
