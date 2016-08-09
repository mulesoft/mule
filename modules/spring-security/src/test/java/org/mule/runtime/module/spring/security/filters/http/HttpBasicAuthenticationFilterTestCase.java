/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security.filters.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.AUTHORIZATION;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.module.http.internal.filter.HttpBasicAuthenticationFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class HttpBasicAuthenticationFilterTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testAuthenticationHeaderFailure() throws Exception {
    MuleEvent oldEvent = RequestContext.getEvent();

    MuleEvent event = getTestEvent(MuleMessage.builder().payload("a").addInboundProperty(AUTHORIZATION, "Basic a").build());
    RequestContext.setEvent(event);

    HttpBasicAuthenticationFilter filter = new HttpBasicAuthenticationFilter();

    SecurityManager manager = mock(SecurityManager.class);
    filter.setSecurityManager(manager);

    doThrow(new UnauthorisedException(null, (MuleEvent) event)).when(manager).authenticate(anyObject());

    try {
      filter.authenticate(event);
      fail("An UnauthorisedException should be thrown");
    } catch (UnauthorisedException e) {
      assertNotNull(event.getMessage().getOutboundProperty("WWW-Authenticate"));
      assertEquals("Basic realm=", event.getMessage().getOutboundProperty("WWW-Authenticate"));
      verify(manager);
    }
    RequestContext.setEvent(oldEvent);
  }
}
