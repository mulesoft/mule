/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.domain;

import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.service.ServiceRepository;

import java.io.File;
import java.io.IOException;

public class TestDomainFactory extends DefaultDomainFactory {

  private boolean failOnStop;
  private boolean failOnDispose;

  public TestDomainFactory(DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory,
                           ArtifactClassLoader containerClassLoader, ClassLoaderRepository classLoaderRepository,
                           ServiceRepository serviceRepository) {
    super(domainClassLoaderFactory, new DefaultDomainManager(), containerClassLoader, classLoaderRepository, serviceRepository);
  }

  @Override
  public Domain createArtifact(File artifactLocation) throws IOException {
    TestDomainWrapper testDomainWrapper = new TestDomainWrapper(super.createArtifact(artifactLocation));
    if (this.failOnStop) {
      testDomainWrapper.setFailOnStop();
    }
    if (this.failOnDispose) {
      testDomainWrapper.setFailOnDispose();
    }
    return testDomainWrapper;
  }

  public void setFailOnStopApplication() {
    failOnStop = true;
  }

  public void setFailOnDisposeApplication() {
    failOnDispose = true;
  }

}
