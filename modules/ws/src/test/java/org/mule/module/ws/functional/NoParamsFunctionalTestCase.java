/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;


import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import org.mule.api.MuleEvent;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;

import org.junit.Test;

public class NoParamsFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "no-params-config.xml";
    }

    @Test
    public void payloadIsIgnoredOperationNoParams() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("noParams");
        MuleEvent event = getTestEvent(TEST_MESSAGE);

        event = flow.process(event);

        String expectedResponse = "<ns:noParamsResponse xmlns:ns=\"http://consumer.ws.module.mule.org/\">" +
                                  "<text>TEST</text></ns:noParamsResponse>";
        assertXMLEqual(expectedResponse, event.getMessage().getPayloadAsString());
    }

    @Test
    public void payloadIsIgnoredOperationNoParamsWithHeaders() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("noParamsWithHeader");
        MuleEvent event = getTestEvent(TEST_MESSAGE);

        String expectedResponse = "<ns2:noParamsWithHeaderResponse xmlns:ns2=\"http://consumer.ws.module.mule.org/\">" +
                                  "<text>HEADER_VALUE</text></ns2:noParamsWithHeaderResponse>";

        String header = "<header xmlns=\"http://consumer.ws.module.mule.org/\">HEADER_VALUE</header>";
        event.getMessage().setProperty("soap.header", header, PropertyScope.OUTBOUND);

        event = flow.process(event);

        assertXMLEqual(expectedResponse, event.getMessage().getPayloadAsString());
    }

}
