/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.construct.Flow;
import org.mule.tck.functional.EventCallback;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class SoapActionFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();

    @Override
    protected String getConfigFile()
    {
        return "soap-action-config.xml";
    }

    @Test
    public void operationWithSoapActionVersion11() throws Exception
    {
        assertSoapAction("operationWithSoapActionVersion11", "TestOperationWithSoapAction", null);
    }

    @Test
    public void operationWithNoSoapActionVersion11() throws Exception
    {
        assertSoapAction("operationWithNoSoapActionVersion11", "", null);
    }

    @Test
    public void operationWithSoapActionVersion12() throws Exception
    {
        assertSoapAction("operationWithSoapActionVersion12", null, "TestOperationWithSoapAction");
    }

    @Test
    public void operationWithNoSoapActionVersion12() throws Exception
    {
        assertSoapAction("operationWithNoSoapActionVersion12", null, null);
    }


    private void assertSoapAction(String flowName, final String expectedSoapActionHeader,
                                  final String expectedActionInContentType) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent event = getTestEvent("<test/>");

        getFunctionalTestComponent("server").setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                String soapAction = context.getMessage().getInboundProperty("SOAPAction");
                String contentType = context.getMessage().getInboundProperty("Content-Type");

                String actionInContentType = extractAction(contentType);

                assertMatchesQuoted(expectedSoapActionHeader, soapAction);
                assertMatchesQuoted(expectedActionInContentType, actionInContentType);
            }
        });

        flow.process(event);
    }

    private String extractAction(String contentType)
    {
        Pattern pattern = Pattern.compile("action=(.*?);");
        Matcher matcher = pattern.matcher(contentType);

        if (!matcher.find())
        {
            return null;
        }

        return matcher.group(1);
    }

    private void assertMatchesQuoted(String expected, String value)
    {
        if (expected == null)
        {
            errorCollector.checkThat(value, nullValue());
        }
        else
        {
            errorCollector.checkThat(value, equalTo(String.format("\"%s\"", expected)));
        }
    }
}
