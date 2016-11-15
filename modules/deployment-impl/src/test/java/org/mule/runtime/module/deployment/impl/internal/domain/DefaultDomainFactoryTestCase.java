/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainFolder;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.DEFAULT_DEPLOY_PROPERTIES_RESOURCE;
import static org.mule.runtime.deployment.model.api.domain.Domain.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.module.deployment.impl.internal.application.PropertiesDescriptorParser.PROPERTY_REDEPLOYMENT_ENABLED;
import static org.mule.runtime.module.reboot.MuleContainerBootstrapUtils.MULE_DOMAIN_FOLDER;

import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.internal.domain.AbstractDomainTestCase;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainFactory;
import org.mule.runtime.module.service.ServiceRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.junit.Test;

public class DefaultDomainFactoryTestCase extends AbstractDomainTestCase {

  private final ArtifactClassLoaderManager artifactClassLoaderManager = mock(ArtifactClassLoaderManager.class);
  private final ServiceRepository serviceRepository = mock(ServiceRepository.class);
  private final DomainFactory domainFactory = new DefaultDomainFactory(
                                                                       new DomainClassLoaderFactory(getClass().getClassLoader()),
                                                                       new DefaultDomainManager(), containerClassLoader, null,
                                                                       serviceRepository);

  public DefaultDomainFactoryTestCase() throws IOException {}

  @Test
  public void createDefaultDomain() throws IOException {
    createDomainDir(MULE_DOMAIN_FOLDER, DEFAULT_DOMAIN_NAME);

    createAndVerifyDomain(DEFAULT_DOMAIN_NAME, true);
  }

  @Test
  public void createCustomDomain() throws IOException {
    String domainName = "custom-domain";
    createDomainDir(MULE_DOMAIN_FOLDER, domainName);

    createAndVerifyDomain(DEFAULT_DOMAIN_NAME, true);
  }

  @Test
  public void createCustomDomainWithProperties() throws IOException {
    String domainName = "custom-domain-with-props";
    createDomainDir(MULE_DOMAIN_FOLDER, domainName);
    createDeployPropertiesFile(domainName);

    createAndVerifyDomain(domainName, false);
  }

  private void createAndVerifyDomain(String name, boolean redeployment)
      throws IOException {
    Domain domain = domainFactory.createArtifact(new File(name));
    assertThat(domain.getArtifactName(), is(name));
    assertThat(domain.getDescriptor().getName(), is(name));
    assertThat(domain.getDescriptor().isRedeploymentEnabled(), is(redeployment));
  }

  private void createDeployPropertiesFile(String domainName) throws FileNotFoundException, UnsupportedEncodingException {
    File properties = new File(getDomainFolder(domainName), DEFAULT_DEPLOY_PROPERTIES_RESOURCE);
    PrintWriter writer = new PrintWriter(properties, "UTF8");
    writer.println(PROPERTY_REDEPLOYMENT_ENABLED + "=false");
    writer.close();
  }
}
