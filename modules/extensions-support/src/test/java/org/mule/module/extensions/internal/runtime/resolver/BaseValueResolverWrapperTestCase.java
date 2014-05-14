/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.module.extensions.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public abstract class BaseValueResolverWrapperTestCase extends AbstractMuleTestCase
{

    protected BaseValueResolverWrapper resolver;

    @Mock
    protected MuleContext muleContext;

    @Mock
    protected ValueResolver delegate;

    @Mock
    protected MuleEvent event;

    @Before
    public final void before() throws Exception
    {
        resolver = createResolver(delegate);
        resolver.setMuleContext(muleContext);
        doBefore();
    }

    protected void doBefore() throws Exception
    {
    }

    protected abstract BaseValueResolverWrapper createResolver(ValueResolver delegate);

    @Test
    public void initialise() throws Exception
    {
        ValueResolver delegate = getDelegateImplementing(MuleContextAware.class, Initialisable.class);
        resolver = createResolver(delegate);
        resolver.setMuleContext(muleContext);
        resolver.initialise();

        ExtensionsTestUtils.verifyInitialisation(delegate, muleContext);
    }

    @Test
    public void start() throws Exception
    {
        ValueResolver delegate = getDelegateImplementing(Startable.class);
        resolver = createResolver(delegate);

        resolver.start();
        verify((Startable) delegate).start();
    }

    @Test
    public void stop() throws Exception
    {
        ValueResolver delegate = getDelegateImplementing(Stoppable.class);
        resolver = createResolver(delegate);

        resolver.stop();
        verify((Stoppable) delegate).stop();
    }

    @Test
    public void dispose() throws Exception
    {
        ValueResolver delegate = getDelegateImplementing(Disposable.class);
        resolver = createResolver(delegate);

        resolver.dispose();
        verify((Disposable) delegate).dispose();
    }

    private ValueResolver getDelegateImplementing(Class<?>... interfaces)
    {
        return mock(ValueResolver.class, withSettings().extraInterfaces(interfaces));
    }

}
