/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.core.util.FileUtils.unzip;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getServiceFolder;
import static org.mule.runtime.module.launcher.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.module.launcher.service.ServiceDescriptorFactory.SERVICE_PROVIDER_CLASS_NAME;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.module.launcher.builder.ServiceFileBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ServiceDescriptorFactoryTestCase extends AbstractMuleTestCase {

  public static final String SERVICE_NAME = "testService";
  public static final String PROVIDER_CLASS_NAME = "org.foo.FooServiceProvider";
  private final ServiceDescriptorFactory serviceDescriptorFactory = new ServiceDescriptorFactory();
  @Rule
  public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);

  @Test
  public void createServiceDescriptor() throws Exception {
    File servicesFolder = getServicesFolder();
    assertThat(servicesFolder.mkdirs(), is(true));

    final ServiceFileBuilder fooService =
        new ServiceFileBuilder(SERVICE_NAME).configuredWith(SERVICE_PROVIDER_CLASS_NAME, PROVIDER_CLASS_NAME);
    unzip(fooService.getArtifactFile(), getServiceFolder(SERVICE_NAME));

    ServiceDescriptor descriptor = serviceDescriptorFactory.create(getServiceFolder(SERVICE_NAME));
    assertThat(descriptor.getName(), equalTo(SERVICE_NAME));
    assertThat(descriptor.getServiceProviderClassName(), equalTo(PROVIDER_CLASS_NAME));
    assertThat(descriptor.getRootFolder(), equalTo(getServiceFolder(SERVICE_NAME)));
  }
}
