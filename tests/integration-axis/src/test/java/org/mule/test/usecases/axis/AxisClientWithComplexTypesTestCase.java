/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.axis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.soap.axis.AxisConnector;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class AxisClientWithComplexTypesTestCase extends FunctionalTestCase
{
    private Trade trade = null;
    private String uri = null;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/routing/axis/axis-client-test.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        trade = new Trade();
        trade.setAccountID(11);
        trade.setCusip("33");
        trade.setCurrency(22);
        trade.setTradeID(22);
        trade.setTransaction(11);
        uri = "axis:http://localhost:" + dynamicPort.getNumber() + "/services/BackOfficeImplBindingImplUMO?method=submitTrade";
    }

    @Test
    public void testSendComplexDOCLIT() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AxisConnector.STYLE, "Document");
        props.put(AxisConnector.USE, "Literal");

        SubmitTrade submittrade = new SubmitTrade();
        submittrade.setArg0(trade);

        // We need to name the parameters when using Doc/Lit
        // SoapMethod method = new SoapMethod(new QName("submitTrade"),
        // SubmitTradeResponse.class);
        // method.addNamedParameter(new NamedParameter(new QName("submitTrade"),
        // NamedParameter.createQName("Object"), ParameterMode.IN));
        // props.put(MuleProperties.MULE_SOAP_METHOD, method);
        MuleMessage result = client.send(uri, submittrade, props);
        assertNotNull(result);
        SubmitTradeResponse response = (SubmitTradeResponse)result.getPayload();
        assertEquals("RECEIVED", response.get_return().getStatus());
    }

    @Test
    public void testSendComplexRPCENC() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send(uri, trade, null);
        assertNotNull(result);
        TradeStatus status = (TradeStatus)result.getPayload();
        assertEquals("RECEIVED", status.getStatus());
    }
}
