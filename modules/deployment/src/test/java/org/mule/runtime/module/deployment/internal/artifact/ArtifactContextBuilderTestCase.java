/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.artifact;

import static java.lang.Thread.currentThread;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.module.deployment.internal.artifact.ArtifactContextBuilder.CLASS_LOADER_REPOSITORY_CANNOT_BE_NULL;
import static org.mule.runtime.module.deployment.internal.artifact.ArtifactContextBuilder.CLASS_LOADER_REPOSITORY_WAS_NOT_SET;
import static org.mule.runtime.module.deployment.internal.artifact.ArtifactContextBuilder.EXECUTION_CLASSLOADER_WAS_NOT_SET;
import static org.mule.runtime.module.deployment.internal.artifact.ArtifactContextBuilder.INSTALLATION_DIRECTORY_MUST_BE_A_DIRECTORY;
import static org.mule.runtime.module.deployment.internal.artifact.ArtifactContextBuilder.MULE_CONTEXT_ARTIFACT_PROPERTIES_CANNOT_BE_NULL;
import static org.mule.runtime.module.deployment.internal.artifact.ArtifactContextBuilder.ONLY_APPLICATIONS_ARE_ALLOWED_TO_HAVE_A_PARENT_CONTEXT;
import static org.mule.runtime.module.deployment.internal.artifact.ArtifactContextBuilder.SERVICE_REPOSITORY_CANNOT_BE_NULL;
import static org.mule.runtime.module.deployment.internal.artifact.ArtifactContextBuilder.newBuilder;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class ArtifactContextBuilderTestCase extends AbstractMuleTestCase {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void emptyBuilder() throws Exception {
    MuleContext muleContext =
        newBuilder(new TestServicesConfigurationBuilder()).setExecutionClassloader(currentThread().getContextClassLoader())
            .setClassLoaderRepository(mock(ClassLoaderRepository.class)).build().getMuleContext();
    assertThat(muleContext, notNullValue());
    assertThat(muleContext.isInitialised(), is(true));
    muleContext.start();
    assertThat(muleContext.isStarted(), is(true));
  }

  @Test
  public void buildWithoutClassloader() throws Exception {
    expectedException.expectMessage(EXECUTION_CLASSLOADER_WAS_NOT_SET);
    newBuilder().build();
  }

  @Test
  public void setNullArtifactProperties() throws Exception {
    expectedException.expectMessage(MULE_CONTEXT_ARTIFACT_PROPERTIES_CANNOT_BE_NULL);
    newBuilder().setArtifactProperties(null);
  }

  @Test
  public void setNullClassLoaderRepository() throws Exception {
    expectedException.expectMessage(CLASS_LOADER_REPOSITORY_CANNOT_BE_NULL);
    newBuilder().setClassLoaderRepository(null);
  }

  @Test
  public void buildWithoutClassloaderRepository() throws Exception {
    expectedException.expectMessage(CLASS_LOADER_REPOSITORY_WAS_NOT_SET);
    newBuilder().setExecutionClassloader(Thread.currentThread().getContextClassLoader()).build();
  }

  @Test
  public void setRegularFileInstallationLocation() throws Exception {
    expectedException.expectMessage(INSTALLATION_DIRECTORY_MUST_BE_A_DIRECTORY);
    newBuilder().setArtifactInstallationDirectory(temporaryFolder.newFile());
  }

  @Test
  public void buildUsingDomainAndParentContext() throws Exception {
    expectedException.expectMessage(ONLY_APPLICATIONS_ARE_ALLOWED_TO_HAVE_A_PARENT_CONTEXT);
    newBuilder().setArtifactType(DOMAIN)
        .setExecutionClassloader(Thread.currentThread().getContextClassLoader()).setParentContext(mock(MuleContext.class))
        .setClassLoaderRepository(mock(ClassLoaderRepository.class))
        .build();
  }

  @Test
  public void setNullServiceRepository() throws Exception {
    expectedException.expectMessage(SERVICE_REPOSITORY_CANNOT_BE_NULL);
    newBuilder().setServiceRepository(null);
  }
}
