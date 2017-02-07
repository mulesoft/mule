/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.infra;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Hashtable;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.junit.Test;
import org.mule.extensions.jms.api.connection.factory.jndi.SimpleJndiNameResolver;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;

public class JmsJndiReconnectionTestCase {

  private static final String RESOLVED_NAME = "resolvedName";
  private static final String NAME = "name";

  @Test
  public void whenJndiResolverAndCommunicationExceptionContextIsRecreated() throws NamingException, MuleException {
    Context context = mock(Context.class);
    when(context.lookup(NAME)).thenThrow(new CommunicationException()).thenReturn(RESOLVED_NAME);

    verifyNumberOfJndiLookupTries(context, 2);
  }

  @Test
  public void whenJndiResolverAndNoCommunicationExceptionContextIsNotRecreated() throws NamingException, MuleException {
    Context context = mock(Context.class);
    when(context.lookup(NAME)).thenReturn(RESOLVED_NAME);

    verifyNumberOfJndiLookupTries(context, 1);
  }

  private void verifyNumberOfJndiLookupTries(Context context, int numberOfTries) throws NamingException, InitialisationException {
    InitialContextFactory jndiContextFactory = mock(InitialContextFactory.class);
    when(jndiContextFactory.getInitialContext(any(Hashtable.class))).thenReturn(context);

    SimpleJndiNameResolver jndiNameResolver = new SimpleJndiNameResolver();
    jndiNameResolver.setContextFactory(jndiContextFactory);
    jndiNameResolver.setJndiInitialFactory("initialFactory");
    jndiNameResolver.initialise();

    assertThat(RESOLVED_NAME, equalTo(jndiNameResolver.lookup(NAME)));

    verify(context, times(numberOfTries)).lookup(NAME);
  }
}
