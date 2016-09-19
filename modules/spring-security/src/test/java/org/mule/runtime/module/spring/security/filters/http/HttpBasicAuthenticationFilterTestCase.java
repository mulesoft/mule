/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security.filters.http;


import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.getCurrentEvent;
import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.setCurrentEvent;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.AUTHORIZATION;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.core.config.i18n.I18nMessage;
import org.mule.runtime.module.http.internal.filter.HttpBasicAuthenticationFilter;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class HttpBasicAuthenticationFilterTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testAuthenticationHeaderFailure() throws Exception {
    Event oldEvent = getCurrentEvent();
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);

    Event event = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.builder().payload("a").addInboundProperty(AUTHORIZATION, "Basic a").build()).build();
    setCurrentEvent(event);

    HttpBasicAuthenticationFilter filter = new HttpBasicAuthenticationFilter();

    SecurityManager manager = mock(SecurityManager.class);
    filter.setSecurityManager(manager);

    doThrow(new UnauthorisedException(mock(I18nMessage.class))).when(manager).authenticate(anyObject());

    try {
      filter.authenticate(event);
      fail("An UnauthorisedException should be thrown");
    } catch (UnauthorisedException e) {
      // assertThat(e.getEvent().getMessage().getOutboundProperty(WWW_AUTHENTICATE), is("Basic realm="));
      verify(manager);
    }
    setCurrentEvent(oldEvent);
  }
}
