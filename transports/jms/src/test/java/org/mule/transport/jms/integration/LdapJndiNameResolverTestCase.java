/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.mule.api.MuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.jms.jndi.LdapJndiNameResolver;

import java.util.Hashtable;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class LdapJndiNameResolverTestCase extends AbstractMuleTestCase
{
    private static final String RESOLVED_NAME = "resolvedName";
    private static final String NAME = "name";

    @Test
    public void whenLdapJndiResolverAndCommunicationExceptionContextIsRecreated() throws NamingException, MuleException
    {
        Context context = mock(Context.class);
        when(context.lookup(NAME)).thenThrow(new CommunicationException()).thenReturn(RESOLVED_NAME);

        InitialContextFactory jndiContextFactory = mock(InitialContextFactory.class);
        when(jndiContextFactory.getInitialContext(any(Hashtable.class))).thenReturn(context);

        LdapJndiNameResolver jndiNameResolver = new LdapJndiNameResolver();
        jndiNameResolver.setContextFactory(jndiContextFactory);
        jndiNameResolver.setJndiInitialFactory("initialFactory");
        jndiNameResolver.initialise();

        assertEquals(RESOLVED_NAME, jndiNameResolver.lookup(NAME));

        Mockito.verify(context, times(2)).lookup(NAME);
    }

}
