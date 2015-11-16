/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.mockito.Mockito.verify;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.spyInjector;
import org.mule.api.Injector;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.extension.api.runtime.Interceptor;
import org.mule.module.extension.internal.introspection.AbstractInterceptable;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.google.common.collect.ImmutableList;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractInterceptableContractTestCase<T extends AbstractInterceptable> extends AbstractMuleContextTestCase
{

    @Mock(extraInterfaces = Lifecycle.class)
    private Interceptor interceptor1;

    @Mock(extraInterfaces = Lifecycle.class)
    private Interceptor interceptor2;

    protected Injector injector;
    protected T interceptable;

    @Before
    public void before() throws Exception
    {
        interceptable = createInterceptable();
        muleContext.getInjector().inject(interceptable);

        injector = spyInjector(muleContext);
    }

    protected abstract T createInterceptable();

    @Test
    public void interceptorsInjected() throws Exception
    {
        interceptable.initialise();
        verify(injector).inject(interceptor1);
        verify(injector).inject(interceptor2);
    }

    @Test
    public void interceptorsInitialised() throws Exception
    {
        interceptable.initialise();
        verify((Initialisable) interceptor1).initialise();
        verify((Initialisable) interceptor2).initialise();
    }

    @Test
    public void interceptorsStarted() throws Exception
    {
        interceptable.start();
        verify((Startable) interceptor1).start();
        verify((Startable) interceptor2).start();
    }

    @Test
    public void interceptorsStopped() throws Exception
    {
        interceptable.stop();
        verify((Stoppable) interceptor1).stop();
        verify((Stoppable) interceptor2).stop();
    }

    @Test
    public void interceptorsDisposed() throws Exception
    {
        interceptable.dispose();
        verify((Disposable) interceptor1).dispose();
        verify((Disposable) interceptor2).dispose();
    }

    protected List<Interceptor> getInterceptors()
    {
        return ImmutableList.of(interceptor1, interceptor2);
    }
}
