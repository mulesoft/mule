/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.domain;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getDomainFolder;
import static org.mule.runtime.module.launcher.descriptor.DeployableArtifactDescriptor.DEFAULT_DEPLOY_PROPERTIES_RESOURCE;
import static org.mule.runtime.module.launcher.descriptor.PropertiesDescriptorParser.PROPERTY_REDEPLOYMENT_ENABLED;
import static org.mule.runtime.module.launcher.domain.Domain.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.module.reboot.MuleContainerBootstrapUtils.MULE_DOMAIN_FOLDER;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Test;

public class DefaultDomainFactoryTestCase extends AbstractDomainTestCase {

  private DomainFactory domainFactory = new DefaultDomainFactory(new DomainClassLoaderFactory(getClass().getClassLoader()),
                                                                 new DefaultDomainManager(), containerClassLoader);

  public DefaultDomainFactoryTestCase() throws IOException {}

  @Test
  public void createDefaultDomain() throws IOException {
    createDomainDir(MULE_DOMAIN_FOLDER, DEFAULT_DOMAIN_NAME);

    createAndVerifyDomain(DEFAULT_DOMAIN_NAME, true, is(empty()));
  }

  @Test
  public void createCustomDomain() throws IOException {
    String domainName = "custom-domain";
    createDomainDir(MULE_DOMAIN_FOLDER, domainName);

    createAndVerifyDomain(DEFAULT_DOMAIN_NAME, true, is(empty()));
  }

  @Test
  public void createCustomDomainWithProperties() throws IOException {
    String domainName = "custom-domain-with-props";
    createDomainDir(MULE_DOMAIN_FOLDER, domainName);
    createDeployPropertiesFile(domainName);

    createAndVerifyDomain(domainName, false, containsInAnyOrder("org.mycom.MyClass", "org.yourcom"));
  }

  private void createAndVerifyDomain(String name, boolean redeployment, Matcher<? super Set<String>> loaderOverridesMatcher)
      throws IOException {
    Domain domain = domainFactory.createArtifact(name);
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
