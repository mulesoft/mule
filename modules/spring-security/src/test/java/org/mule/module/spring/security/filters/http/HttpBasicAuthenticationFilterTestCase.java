/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security.filters.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.PropertyScope.OUTBOUND;
import static org.mule.module.http.api.HttpHeaders.Names.AUTHORIZATION;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityManager;
import org.mule.api.security.UnauthorisedException;
import org.mule.PropertyScope;
import org.mule.module.http.internal.filter.HttpBasicAuthenticationFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class HttpBasicAuthenticationFilterTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testAuthenticationHeaderFailure() throws Exception
    {
        MuleEvent oldEvent = RequestContext.getEvent();

        MuleEvent event = this.getTestEvent("a");
        MuleMessage message = event.getMessage();
        message.setProperty(AUTHORIZATION, "Basic a", PropertyScope.INBOUND);
        RequestContext.setEvent(event);

        HttpBasicAuthenticationFilter filter = new HttpBasicAuthenticationFilter();

        SecurityManager manager = mock(SecurityManager.class);
        filter.setSecurityManager(manager);

        doThrow(new UnauthorisedException(null, (MuleEvent) null)).when(manager).authenticate(
            (Authentication) anyObject());

        try
        {
            filter.authenticate(event);
            fail("An UnauthorisedException should be thrown");
        }
        catch (UnauthorisedException e)
        {
            assertNotNull(event.getMessage().getProperty("WWW-Authenticate", OUTBOUND));
            assertEquals("Basic realm=", event.getMessage().getProperty("WWW-Authenticate", OUTBOUND));
            verify(manager);
        }
        RequestContext.setEvent(oldEvent);
    }
}
