/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.extension.api.connection.ConnectionProvider;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.module.extension.internal.AbstractInterceptableContractTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.util.TestTimeSupplier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.MockitoAnnotations;

@SmallTest
@RunWith(Parameterized.class)
public class LifecycleAwareConfigurationTestCase extends AbstractInterceptableContractTestCase<LifecycleAwareConfigurationInstance>
{

    private static final String NAME = "name";

    @Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                {"With provider", mock(ConnectionProvider.class, withSettings().extraInterfaces(Lifecycle.class, MuleContextAware.class))},
                {"Without provider", null}}
        );
    }

    private ConfigurationModel configurationModel = mock(ConfigurationModel.class);

    private Lifecycle value = mock(Lifecycle.class);

    private String name;
    private Optional<ConnectionProvider> connectionProvider;

    public LifecycleAwareConfigurationTestCase(String name, ConnectionProvider connectionProvider)
    {
        this.name = name;
        this.connectionProvider = Optional.ofNullable(connectionProvider);
    }

    private TestTimeSupplier timeSupplier = new TestTimeSupplier(System.currentTimeMillis());

    @Before
    @Override
    public void before() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        super.before();
        muleContext.getRegistry().registerObject(MuleProperties.OBJECT_TIME_SUPPLIER, timeSupplier);
    }

    @Override
    protected LifecycleAwareConfigurationInstance createInterceptable()
    {
        if (connectionProvider.isPresent())
        {
            reset(connectionProvider.get());
        }
        return new LifecycleAwareConfigurationInstance(NAME, configurationModel, value, getInterceptors(), connectionProvider);
    }

    @Test
    public void valueInjected() throws Exception
    {
        interceptable.initialise();
        verify(injector).inject(value);
        if (connectionProvider.isPresent())
        {
            verify(injector).inject(connectionProvider.get());
        }
        else
        {
            verify(injector, never()).inject(any(ConnectionProvider.class));
        }
    }

    @Test
    public void valueInitialised() throws Exception
    {
        interceptable.initialise();
        verify((Initialisable) value).initialise();
        if (connectionProvider.isPresent())
        {
            verify((Initialisable) connectionProvider.get()).initialise();
        }
    }

    @Test
    public void valueStarted() throws Exception
    {
        interceptable.start();
        verify((Startable) value).start();
        if (connectionProvider.isPresent())
        {
            verify((Startable) connectionProvider.get()).start();
        }
    }

    @Test
    public void valueStopped() throws Exception
    {
        interceptable.stop();
        verify((Stoppable) value).stop();
        if (connectionProvider.isPresent())
        {
            verify((Stoppable) connectionProvider.get()).stop();
        }

    }

    @Test
    public void valueDisposed() throws Exception
    {
        interceptable.dispose();
        verify((Disposable) value).dispose();
        if (connectionProvider.isPresent())
        {
            verify((Disposable) connectionProvider.get()).dispose();
        }
    }

    @Test
    public void getName()
    {
        assertThat(interceptable.getName(), is(NAME));
    }

    @Test
    public void getModel()
    {
        assertThat(interceptable.getModel(), is(sameInstance(configurationModel)));
    }

    @Test
    public void getValue()
    {
        assertThat(interceptable.getValue(), is(sameInstance(value)));
    }

    @Test(expected = IllegalStateException.class)
    public void getStatsBeforeInit()
    {
        interceptable.getStatistics();
    }

    @Test
    public void getStatistics() throws Exception
    {
        interceptable.initialise();
        assertThat(interceptable.getStatistics(), is(notNullValue()));
    }
}
