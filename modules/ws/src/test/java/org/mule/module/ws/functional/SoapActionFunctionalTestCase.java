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
        assertSoapActionInRequest("operationWithSoapActionVersion11", "TestOperationWithSoapAction");
    }

    @Test
    public void operationWithNoSoapActionVersion11() throws Exception
    {
        assertSoapActionInRequest("operationWithNoSoapActionVersion11", "");
    }

    @Test
    public void operationWithSoapActionVersion12() throws Exception
    {
        assertSoapActionInRequest("operationWithSoapActionVersion12", null);
    }

    @Test
    public void operationWithNoSoapActionVersion12() throws Exception
    {
        assertSoapActionInRequest("operationWithNoSoapActionVersion12", null);
    }


    private void assertSoapActionInRequest(String flowName, final String expectedSoapAction) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MuleEvent event = getTestEvent("<test/>");

        getFunctionalTestComponent("server").setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                String soapAction = context.getMessage().getInboundProperty("SOAPAction");

                if (expectedSoapAction == null)
                {
                    errorCollector.checkThat(soapAction, nullValue());
                }
                else
                {
                    errorCollector.checkThat(soapAction, equalTo(String.format("\"%s\"", expectedSoapAction)));
                }
            }
        });

        flow.process(event);
    }

}
