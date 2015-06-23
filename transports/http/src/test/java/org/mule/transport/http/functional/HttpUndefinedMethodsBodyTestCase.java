/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.module.http.api.HttpConstants.Methods.DELETE;
import static org.mule.module.http.api.HttpConstants.Methods.GET;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpUndefinedMethodsBodyTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Parameterized.Parameter(0)
    public String method;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {{GET.name()}, {DELETE.name()}});
    }

    @Override
    protected String getConfigFile()
    {
        return "http-undefined-methods-body-config.xml";
    }

    @Test
    public void sendBody() throws Exception
    {
        sendRequestAndAssertMethod(TEST_PAYLOAD, TEST_PAYLOAD);
    }

    @Test
    public void noBody() throws Exception
    {
        sendRequestAndAssertMethod(null, "/");
    }

    private void sendRequestAndAssertMethod(String payload, String expectedContent) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("requestFlow");
        MuleEvent event = getTestEvent(payload);
        event.setFlowVariable("method", method);
        event = flow.process(event);

        assertThat(event.getMessage().<Integer>getInboundProperty(HTTP_STATUS_PROPERTY), is(OK.getStatusCode()));
        assertThat(event.getMessageAsString(), is(expectedContent));
    }

}
