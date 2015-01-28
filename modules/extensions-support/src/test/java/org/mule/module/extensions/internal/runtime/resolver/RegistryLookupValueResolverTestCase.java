/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.extensions.internal.util.ExtensionsTestUtils.HELLO_WORLD;
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
public class RegistryLookupValueResolverTestCase extends AbstractMuleTestCase
{

    private static final String KEY = "key";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleEvent event;

    private ValueResolver resolver;

    @Before
    public void before() throws Exception
    {
        when(event.getMuleContext().getRegistry().get(KEY)).thenReturn(HELLO_WORLD);
        resolver = new RegistryLookupValueResolver(KEY);
    }

    @Test
    public void cache() throws Exception
    {
        Object value = resolver.resolve(event);
        assertThat((String) value, is(HELLO_WORLD));
        verify(event.getMuleContext().getRegistry()).get(KEY);
    }

    @Test
    public void isDynamic()
    {
        assertThat(resolver.isDynamic(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullKey()
    {
        new RegistryLookupValueResolver(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankKey()
    {
        new RegistryLookupValueResolver("");
    }
}
