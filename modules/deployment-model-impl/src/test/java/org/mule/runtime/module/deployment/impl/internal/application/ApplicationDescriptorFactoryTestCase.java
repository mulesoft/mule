/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.util.Optional.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppFolder;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.DEFAULT_CONFIGURATION_RESOURCE;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.deployment.impl.internal.artifact.DeployableArtifactDescriptorFactoryTestCase;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.Test;

public class ApplicationDescriptorFactoryTestCase
    extends DeployableArtifactDescriptorFactoryTestCase<ApplicationDescriptor, ApplicationFileBuilder> {

  @Override
  protected ApplicationDescriptor createArtifactDescriptor(String appPath) throws URISyntaxException {
    final ApplicationDescriptorFactory applicationDescriptorFactory = createApplicationDescriptorFactory();
    return applicationDescriptorFactory.create(getArtifact(appPath), empty());
  }

  @Override
  protected File getArtifact(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

  @Override
  protected ApplicationFileBuilder createArtifactFileBuilder() {
    return new ApplicationFileBuilder(ARTIFACT_NAME);
  }

  @Override
  protected String getArtifactRootFolder() {
    return "apps";
  }

  @Override
  protected String getDefaultConfigurationResourceLocation() {
    return DEFAULT_CONFIGURATION_RESOURCE;
  }

  @Override
  protected File getArtifactFolder() {
    return getAppFolder(ARTIFACT_NAME);
  }

  @Override
  protected ApplicationDescriptor createArtifactDescriptor() {
    final ApplicationDescriptorFactory applicationDescriptorFactory = createApplicationDescriptorFactory();

    return applicationDescriptorFactory.create(getArtifactFolder(), empty());
  }

  private ApplicationDescriptorFactory createApplicationDescriptorFactory() {
    return new ApplicationDescriptorFactory(new ArtifactPluginDescriptorLoader(new ArtifactPluginDescriptorFactory()),
                                            createDescriptorLoaderRepository(), ArtifactDescriptorValidatorBuilder.builder());
  }

  @Test
  public void getLogConfigFileFromFullFilePath() {
    MuleApplicationModel deployableModel = mock(MuleApplicationModel.class);
    File logConfigFile = Paths.get("custom-log4j2.xml").toAbsolutePath().toFile();
    when(deployableModel.getLogConfigFile()).thenReturn(logConfigFile.getAbsolutePath());

    File resolvedFile = createApplicationDescriptorFactory().getLogConfigFile(deployableModel);
    assertThat(resolvedFile.toPath(), is(logConfigFile.toPath()));
  }


}
