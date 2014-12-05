/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.log4j2;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.module.launcher.application.CompositeApplicationClassLoader;
import org.mule.module.launcher.artifact.ShutdownListener;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;

import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ArtifactAwareContextSelectorTestCase extends AbstractMuleTestCase
{
    private static final File CONFIG_LOCATION = new File("my/local/log4j2.xml");
    private ArtifactAwareContextSelector selector;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CompositeApplicationClassLoader classLoader;

    @Before
    public void before() throws Exception
    {
        selector = new ArtifactAwareContextSelector();
        when(classLoader.getArtifactName()).thenReturn(getClass().getName());
        when(classLoader.findLocalResource("log4j2.xml")).thenReturn(CONFIG_LOCATION.toURI().toURL());
    }

    @Test
    public void classLoaderToContext()
    {
        MuleLoggerContext context = (MuleLoggerContext) selector.getContext(EMPTY, classLoader, true);
        assertThat(context, is(selector.getContext(EMPTY, classLoader, true)));

        classLoader = mock(CompositeApplicationClassLoader.class, RETURNS_DEEP_STUBS);
        when(classLoader.getArtifactName()).thenReturn(getClass().getName());
        assertThat(context, not(sameInstance(selector.getContext(EMPTY, classLoader, true))));
    }

    @Test
    public void shutdownListener()
    {
        MuleLoggerContext context = (MuleLoggerContext) selector.getContext("", classLoader, true);

        ArgumentCaptor<ShutdownListener> captor = ArgumentCaptor.forClass(ShutdownListener.class);
        verify(classLoader).addShutdownListener(captor.capture());
        ShutdownListener listener = captor.getValue();
        assertThat(listener, notNullValue());

        assertThat(context, is(selector.getContext(EMPTY, classLoader, true)));
        listener.execute();

        assertThat(context, not(selector.getContext("", classLoader, true)));
    }

    @Test
    public void returnsMuleLoggerContext()
    {
        LoggerContext ctx = selector.getContext("", classLoader, true);
        assertThat(ctx, instanceOf(MuleLoggerContext.class));
        assertConfigurationLocation(ctx);
    }

    private void assertConfigurationLocation(LoggerContext ctx)
    {
        assertThat(ctx.getConfigLocation(), equalTo(CONFIG_LOCATION.toURI()));
    }
}
