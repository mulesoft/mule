/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class InitialLifecycleValueResolverTestCase extends BaseValueResolverWrapperTestCase
{

    @Mock(extraInterfaces = {MuleContextAware.class, Initialisable.class, Startable.class})
    private Object resolvedValue;

    @Override
    protected void doBefore() throws Exception
    {
        when(delegate.resolve(event)).thenReturn(resolvedValue);
    }

    @Override
    protected BaseValueResolverWrapper createResolver(ValueResolver delegate)
    {
        return new InitialLifecycleValueResolver(delegate, muleContext);
    }

    @Test
    public void resolve() throws Exception
    {
        assertThat(resolver.resolve(event), is(resolvedValue));
        InOrder order = inOrder(resolvedValue);
        order.verify((MuleContextAware) resolvedValue).setMuleContext(muleContext);
        order.verify((Initialisable) resolvedValue).initialise();
        order.verify(((Startable) resolvedValue)).start();
    }

    @Test
    public void isDynamic()
    {
        verifyDinamic(true);
    }

    @Test
    public void isNotDynamic()
    {
        verifyDinamic(false);
    }

    private void verifyDinamic(boolean dynamic)
    {
        when(delegate.isDynamic()).thenReturn(dynamic);
        assertThat(resolver.isDynamic(), is(dynamic));
    }

}
