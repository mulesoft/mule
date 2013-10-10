/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.axis;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.soap.axis.AxisConnector;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AxisClientWithComplexTypesTestCase extends FunctionalTestCase
{
    
    private Trade trade = null;
    private String uri = null;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
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
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        props.put(AxisConnector.STYLE, "Document");
        props.put(AxisConnector.USE, "Literal");

        SubmitTrade submittrade = new SubmitTrade();
        submittrade.setArg0(trade);

        // We need to name the parameters weh using Doc/Lit
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
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send(uri, trade, null);
        assertNotNull(result);
        TradeStatus status = (TradeStatus)result.getPayload();
        assertEquals("RECEIVED", status.getStatus());
    }

}
