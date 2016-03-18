/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.log4j2;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.mule.module.launcher.application.CompositeApplicationClassLoader;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MuleLoggerContextFactoryTestCase
{
    private static final File CONFIG_LOCATION = new File("my/local/log4j2.xml");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CompositeApplicationClassLoader classLoader;

    @Before
    public void before() throws Exception
    {
        when(classLoader.getArtifactName()).thenReturn(getClass().getName());
        when(classLoader.findLocalResource("log4j2.xml")).thenReturn(CONFIG_LOCATION.toURI().toURL());
    }

    @Test
    public void externalConf() throws IOException
    {
        File customLogConfig = new File("src/test/resources/log4j2-test-custom.xml");
        assertThat(customLogConfig.exists(), is(true));
        final MuleLoggerContextFactory loggerCtxFactory = mockLoggerContextFactory(customLogConfig);

        final LoggerContext ctx = loggerCtxFactory.build(classLoader, mock(ArtifactAwareContextSelector.class));
        assertThat(ctx.getConfigLocation(), equalTo(customLogConfig.toURI()));
    }

    @Test
    public void externalConfInvalid() throws IOException
    {
        File customLogConfig = new File("src/test/resources/log4j2-test-custom-invalid.xml");
        assertThat(customLogConfig.exists(), is(false));
        final MuleLoggerContextFactory loggerCtxFactory = mockLoggerContextFactory(customLogConfig);

        final LoggerContext ctx = loggerCtxFactory.build(classLoader, mock(ArtifactAwareContextSelector.class));
        assertThat(ctx.getConfigLocation(), equalTo(CONFIG_LOCATION.toURI()));
    }

    protected MuleLoggerContextFactory mockLoggerContextFactory(File customLogConfig) throws IOException
    {
        final ApplicationDescriptor appDescriptor = mock(ApplicationDescriptor.class);
        when(appDescriptor.getLogConfigFile()).thenReturn(customLogConfig);

        final MuleLoggerContextFactory loggerCtxFactory = spy(new MuleLoggerContextFactory());
        doReturn(appDescriptor).when(loggerCtxFactory).fetchApplicationDescriptor(any(ArtifactClassLoader.class));
        return loggerCtxFactory;
    }
}
