/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security.filters.http;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityManager;
import org.mule.api.security.UnauthorisedException;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.filters.HttpBasicAuthenticationFilter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class HttpBasicAuthenticationFilterTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testAuthenticationHeaderFailure() throws Exception
    {
        MuleEvent oldEvent = RequestContext.getEvent();

        MuleEvent event = this.getTestEvent("a");
        MuleMessage message = event.getMessage();
        message.setProperty(HttpConstants.HEADER_AUTHORIZATION, "Basic a", PropertyScope.INBOUND);
        RequestContext.setEvent(event);

        HttpBasicAuthenticationFilter filter = new HttpBasicAuthenticationFilter();

        SecurityManager manager = mock(SecurityManager.class);
        filter.setSecurityManager(manager);

        doThrow(new UnauthorisedException(null, (MuleEvent) null)).when(manager).authenticate(
            (Authentication) anyObject());

        try
        {
            filter.authenticateInbound(event);
            fail("An UnauthorisedException should be thrown");
        }
        catch (UnauthorisedException e)
        {
            assertNotNull(event.getMessage().getProperty("WWW-Authenticate"));
            assertEquals("Basic realm=", event.getMessage().getProperty("WWW-Authenticate"));
            verify(manager);
        }
        RequestContext.setEvent(oldEvent);
    }
}
