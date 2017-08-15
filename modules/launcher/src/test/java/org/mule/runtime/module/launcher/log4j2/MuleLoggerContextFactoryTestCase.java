/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.util.ClassUtils.getResource;

import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MuleLoggerContextFactoryTestCase extends AbstractMuleTestCase {

  private static final File CONFIG_LOCATION = new File("my/local/log4j2.xml");

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RegionClassLoader classLoader;
  private ApplicationDescriptor artifactDescriptor;

  @Before
  public void before() throws Exception {
    when(classLoader.getArtifactId()).thenReturn(getClass().getName());
    when(classLoader.findLocalResource("log4j2.xml")).thenReturn(CONFIG_LOCATION.toURI().toURL());
    artifactDescriptor = mock(ApplicationDescriptor.class);
    when(classLoader.getArtifactDescriptor()).thenReturn(artifactDescriptor);
  }

  @Test
  public void externalConf() throws IOException, URISyntaxException {
    File customLogConfig = new File(getResource("log4j2-test-custom.xml", getClass()).toURI());
    assertThat(customLogConfig.exists(), is(true));
    when(artifactDescriptor.getLogConfigFile()).thenReturn(customLogConfig);
    final MuleLoggerContextFactory loggerCtxFactory = mockLoggerContextFactory();

    final LoggerContext ctx = loggerCtxFactory.build(classLoader, mock(ArtifactAwareContextSelector.class));
    assertThat(ctx.getConfigLocation(), equalTo(customLogConfig.toURI()));
  }

  @Test
  public void externalConfInvalid() throws IOException {
    File customLogConfig = new File("src/test/resources/log4j2-test-custom-invalid.xml");
    assertThat(customLogConfig.exists(), is(false));
    when(artifactDescriptor.getLogConfigFile()).thenReturn(customLogConfig);
    final MuleLoggerContextFactory loggerCtxFactory = mockLoggerContextFactory();

    final LoggerContext ctx = loggerCtxFactory.build(classLoader, mock(ArtifactAwareContextSelector.class));
    assertThat(ctx.getConfigLocation(), equalTo(CONFIG_LOCATION.toURI()));
  }

  protected MuleLoggerContextFactory mockLoggerContextFactory() throws IOException {
    return spy(new MuleLoggerContextFactory());
  }
}
