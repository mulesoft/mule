/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.jndi;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.junit.Test;
import org.mockito.Mockito;

public class CachedJndiNameResolverTestCase extends AbstractMuleTestCase
{

    private static final String RESOLVED_NAME = "resolvedName";
    private static final String NAME = "name";

    @Test
    public void testResolvesWithCache() throws NamingException, MuleException
    {

        Context context = mock(Context.class);
        when(context.lookup(NAME)).thenReturn(RESOLVED_NAME);

        InitialContextFactory jndiContextFactory = mock(InitialContextFactory.class);
        when(jndiContextFactory.getInitialContext(any(Hashtable.class))).thenReturn(context);

        CachedJndiNameResolver jndiNameResolver = new CachedJndiNameResolver();
        jndiNameResolver.setContextFactory(jndiContextFactory);
        jndiNameResolver.setJndiInitialFactory("initialFactory");
        jndiNameResolver.initialise();

        // First lookup should use the context, second should use the cache
        assertEquals(RESOLVED_NAME, jndiNameResolver.lookup(NAME));
        assertEquals(RESOLVED_NAME, jndiNameResolver.lookup(NAME));

        Mockito.verify(context, times(1)).lookup(NAME);
    }
}
