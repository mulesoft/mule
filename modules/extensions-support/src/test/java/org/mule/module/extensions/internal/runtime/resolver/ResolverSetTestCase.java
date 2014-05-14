/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.module.extensions.internal.util.ExtensionsTestUtils.getParameter;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.extensions.introspection.Parameter;
import org.mule.module.extensions.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ResolverSetTestCase extends AbstractMuleTestCase
{

    private static final String NAME = "MG";
    private static final int AGE = 31;

    private ResolverSet set;
    private Map<Parameter, ValueResolver> mapping;

    @Mock
    private MuleEvent event;

    @Mock
    private MuleContext muleContext;

    @Before
    public void before() throws Exception
    {
        mapping = new LinkedHashMap<>();
        mapping.put(getParameter("myName", String.class), getResolver(NAME));
        mapping.put(getParameter("age", Integer.class), getResolver(AGE));

        set = buildSet(mapping);
    }

    @Test
    public void resolve() throws Exception
    {
        ResolverSetResult result = set.resolve(event);
        assertResult(result, mapping);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNullParameter() throws Exception
    {
        set.add(null, getResolver(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNullresolver() throws Exception
    {
        set.add(getParameter("blah", String.class), null);
    }

    @Test
    public void initialise() throws Exception
    {
        set.setMuleContext(muleContext);
        set.initialise();
        ExtensionsTestUtils.verifyAllInitialised(mapping.values(), muleContext);
    }

    @Test
    public void start() throws Exception
    {
        set.start();
        ExtensionsTestUtils.verifyAllStarted(mapping.values());
    }

    @Test
    public void stop() throws Exception
    {
        set.stop();
        ExtensionsTestUtils.verifyAllStopped(mapping.values());
    }

    @Test
    public void dispose() throws Exception
    {
        set.dispose();
        ExtensionsTestUtils.verifyAllDisposed(mapping.values());
    }

    @Test
    public void isNotDynamic()
    {
        assertThat(set.isDynamic(), is(false));
    }

    @Test
    public void isDynamic() throws Exception
    {
        ValueResolver resolver = getResolver(null);
        when(resolver.isDynamic()).thenReturn(true);

        set.add(getParameter("whatever", String.class), resolver);
        assertThat(set.isDynamic(), is(true));
    }

    private void assertResult(ResolverSetResult result, Map<Parameter, ValueResolver> mapping) throws Exception
    {
        assertThat(result, is(notNullValue()));
        for (Map.Entry<Parameter, ValueResolver> entry : mapping.entrySet())
        {
            Object value = result.get(entry.getKey());
            assertThat(value, is(entry.getValue().resolve(event)));
        }
    }

    private ResolverSet buildSet(Map<Parameter, ValueResolver> mapping)
    {
        ResolverSet set = new ResolverSet();
        for (Map.Entry<Parameter, ValueResolver> entry : mapping.entrySet())
        {
            set.add(entry.getKey(), entry.getValue());
        }

        return set;
    }

    private ValueResolver getResolver(Object value) throws Exception
    {
        return ExtensionsTestUtils.getResolver(value, event, false, MuleContextAware.class, Lifecycle.class);
    }
}
