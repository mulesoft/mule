/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class HttpRequestTargetWithComplexVariable extends FunctionalTestCase
{

    @Rule
    public DynamicPort port1 = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-request-target-with-complex-variable.xml";
    }

    @Test
    public void test() throws Exception
    {
        final MuleEvent testEvent = getTestEvent(TEST_MESSAGE);
        testEvent.getMessage().setProperty("urls", Arrays.asList(getUrlForPort(port1)), PropertyScope.INBOUND);
        final MuleEvent response = ((Flow) getFlowConstruct("test-http-response-targetFlow")).process(testEvent);
        assertThat(response, notNullValue());
        assertThat(response.getMessage(), notNullValue());
        assertEquals("dummy", response.getMessage().getPayloadAsString());
    }

    private String getUrlForPort(DynamicPort port)
    {
        return String.format("http://localhost:%s/test", port.getNumber());
    }
}
