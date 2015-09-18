/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import org.mule.DefaultMuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.extension.introspection.ConfigurationModel;
import org.mule.api.extension.runtime.Interceptor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.util.TestTimeSupplier;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class LifecycleAwareConfigurationTestCase extends AbstractMuleContextTestCase
{

    private static final String NAME = "name";

    @Mock
    private ConfigurationModel configurationModel;

    @Mock
    private Lifecycle value;

    @Mock(extraInterfaces = Lifecycle.class)
    private Interceptor interceptor1;

    @Mock(extraInterfaces = Lifecycle.class)
    private Interceptor interceptor2;

    private TestTimeSupplier timeSupplier = new TestTimeSupplier(System.currentTimeMillis());
    private LifecycleAwareConfigurationInstance<Object> configuration;

    @Before
    public void before() throws Exception
    {
        muleContext.getRegistry().registerObject(MuleProperties.OBJECT_TIME_SUPPLIER, timeSupplier);
        configuration = new LifecycleAwareConfigurationInstance<>(NAME, configurationModel, value, Arrays.asList(interceptor1, interceptor2));
        muleContext.getInjector().inject(configuration);
        ((DefaultMuleContext) muleContext).setInjector(spy(muleContext.getInjector()));
    }

    @Test
    public void initialise() throws Exception
    {
        configuration.initialise();
        verify(value).initialise();
        verify((Initialisable) interceptor1).initialise();
        verify((Initialisable) interceptor2).initialise();
    }

    @Test
    public void iocPropagated() throws Exception
    {
        configuration.initialise();
        verify(muleContext.getInjector()).inject(value);
        verify(muleContext.getInjector()).inject(interceptor1);
        verify(muleContext.getInjector()).inject(interceptor2);
    }

    @Test
    public void start() throws Exception
    {
        configuration.start();
        verify(value).start();
        verify((Startable) interceptor1).start();
        verify((Startable) interceptor2).start();
    }

    @Test
    public void stop() throws Exception
    {
        configuration.stop();
        verify(value).stop();
        verify((Stoppable) interceptor1).stop();
        verify((Stoppable) interceptor2).stop();
    }

    @Test
    public void dispose() throws Exception
    {
        configuration.dispose();
        verify(value).dispose();
        verify((Disposable) interceptor1).dispose();
        verify((Disposable) interceptor2).dispose();
    }

    @Test
    public void getName()
    {
        assertThat(configuration.getName(), is(NAME));
    }

    @Test
    public void getModel()
    {
        assertThat(configuration.getModel(), is(sameInstance(configurationModel)));
    }

    @Test
    public void getValue()
    {
        assertThat(configuration.getValue(), is(sameInstance(value)));
    }

    @Test(expected = IllegalStateException.class)
    public void getStatsBeforeInit()
    {
        configuration.getStatistics();
    }

    @Test
    public void getStatistics() throws Exception
    {
        configuration.initialise();
        assertThat(configuration.getStatistics(), is(notNullValue()));
    }
}
