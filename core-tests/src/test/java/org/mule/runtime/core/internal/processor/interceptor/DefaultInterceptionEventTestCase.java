/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static java.util.Optional.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.internal.interception.DefaultInterceptionEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class DefaultInterceptionEventTestCase extends AbstractMuleTestCase {

  @Test
  public void correlationIdAutogenerated() throws MuleException {
    final InternalEvent event =
        InternalEvent.builder(create("id", "serverId", TEST_CONNECTOR_LOCATION, null, empty()))
            .message(of(TEST_PAYLOAD)).build();

    final DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);

    assertThat(interceptionEvent.getCorrelationId(), is("id"));
  }

  @Test
  public void correlationIdGiven() throws MuleException {
    final InternalEvent event =
        InternalEvent.builder(create("id", "serverId", TEST_CONNECTOR_LOCATION, "corr1", empty()))
            .message(of(TEST_PAYLOAD)).build();

    final DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);

    assertThat(interceptionEvent.getCorrelationId(), is("corr1"));
  }

  @Test
  public void correlationIdLegacy() throws MuleException {
    final InternalEvent event =
        InternalEvent.builder(create("id", "serverId", TEST_CONNECTOR_LOCATION, null, empty()))
            .correlationId("corr1").message(of(TEST_PAYLOAD)).build();

    final DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);

    assertThat(interceptionEvent.getCorrelationId(), is("corr1"));
  }

  @Test
  public void addSession() throws MuleException {
    final InternalEvent event = this.<InternalEvent.Builder>getEventBuilder().message(of(TEST_PAYLOAD)).build();
    final DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);

    final MuleSession session = mock(MuleSession.class);
    interceptionEvent.session(session);

    assertThat(((PrivilegedEvent) interceptionEvent.resolve()).getSession(), sameInstance(session));
  }

  @Test
  public void changeSession() throws MuleException {
    final InternalEvent event =
        this.<InternalEvent.Builder>getEventBuilder().message(of(TEST_PAYLOAD)).session(mock(MuleSession.class)).build();
    final DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);

    final MuleSession session = mock(MuleSession.class);
    interceptionEvent.session(session);

    assertThat(((PrivilegedEvent) interceptionEvent.resolve()).getSession(), sameInstance(session));
  }

  @Test
  public void updateSession() throws MuleException {
    final InternalEvent event = this.<InternalEvent.Builder>getEventBuilder().message(of(TEST_PAYLOAD)).build();
    final DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);

    final MuleSession session = ((PrivilegedEvent) event).getSession();
    session.setProperty("myKey", "myValue");

    interceptionEvent.session(session);

    assertThat(((PrivilegedEvent) interceptionEvent.resolve()).getSession().getProperty("myKey"), is("myValue"));
  }
}
