/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.HELLO_WORLD;
import org.mule.api.MuleEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CachingValueResolverWrapperTestCase extends AbstractMuleTestCase
{
    @Mock
    private ValueResolver delegate;

    @Mock
    private MuleEvent event;

    private BaseValueResolverWrapper resolver;

    @Before
    public void before() throws Exception
    {
        when(delegate.resolve(event))
                .thenReturn(HELLO_WORLD)
                .thenReturn(null);
        resolver = new CachingValueResolverWrapper(delegate);
    }

    @Test
    public void cache() throws Exception
    {
        for (int i = 0; i < 10; i++)
        {
            assertThat((String) resolver.resolve(event), is(HELLO_WORLD));
        }

        verify(delegate, times(1)).resolve(event);
    }

    @Test
    public void isDynamic()
    {
        assertThat(resolver.isDynamic(), is(false));
    }

}
