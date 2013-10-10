/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.jndi;

import org.mule.api.MuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

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
