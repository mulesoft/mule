/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.requester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.api.transport.PropertyScope.INBOUND;
import static org.mule.module.http.internal.request.grizzly.GrizzlyHttpClient.CUSTOM_MAX_HTTP_PACKET_HEADER_SIZE;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HttpRequesterHeaderSizeTestCase extends FunctionalTestCase
{

    private static final int SIZE_DELTA = 1000;

    private static final Integer HEADER_SIZE = 10000;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public SystemProperty maxHeaderSectionSizeSystemProperty = new SystemProperty(CUSTOM_MAX_HTTP_PACKET_HEADER_SIZE, HEADER_SIZE.toString());

    @Rule
    public SystemProperty exceededContentValue = new SystemProperty("exceededContentValue", randomAlphanumeric(HEADER_SIZE + SIZE_DELTA));

    @Rule
    public SystemProperty notExceededContentValue = new SystemProperty("notExceededContentValue", randomAlphanumeric(HEADER_SIZE - SIZE_DELTA));

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-requester-max-header-size-config.xml";
    }

    @Test
    public void testMaxHeaderSizeExceeded() throws Exception
    {
        expectedException.expect(MessagingException.class);
        try
        {
            runFlow("requestToListenerWithExceededHeaderFlow");
        }
        finally
        {
            expectedException.expectMessage(containsString("Error sending HTTP request"));
        }

    }

    @Test
    public void testMaxHeaderSizeNotExceeded() throws Exception
    {
        MuleEvent event = runFlow("requestToListenerWithNotExceededHeaderFlow");
        String stringContent = event.getMessage().getProperty("content", INBOUND);
        assertThat(stringContent.getBytes().length, is(HEADER_SIZE - SIZE_DELTA));
    }

}
