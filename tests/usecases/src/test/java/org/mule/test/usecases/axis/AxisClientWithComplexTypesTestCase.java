/* 
* $Header$
* $Revision$
* $Date$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.test.usecases.axis;

import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.providers.soap.NamedParameter;
import org.mule.providers.soap.SoapMethod;
import org.mule.tck.NamedTestCase;
import org.mule.umo.UMOMessage;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import java.util.HashMap;
import java.util.Map;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisClientWithComplexTypesTestCase extends NamedTestCase
 {
    private Trade trade = null;
    private String uri = "axis:http://localhost:8081/services/BackOfficeImplBindingImplUMO?method=submitTrade";

    protected void setUp() throws Exception
    {
        if (MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("org/mule/test/usecases/axis/axis-client-test.xml");

        trade = new Trade();
        trade.setAccountID(11);
        trade.setCusip("33");
        trade.setCurrency(22);
        trade.setTradeID(22);
        trade.setTransaction(11);

    }

    public void testSendComplexDOCLIT() throws Exception {

        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put("style", "Document");
        props.put("use", "Literal");

        SubmitTrade submittrade = new SubmitTrade();
		submittrade.setArg0(trade);

        //We need to name the parameters weh using Doc/Lit
        SoapMethod method = new SoapMethod(new QName("submitTrade"), SubmitTradeResponse.class);
        method.addNamedParameter(new NamedParameter(new QName("submitTrade"), NamedParameter.createQName("Object"), ParameterMode.IN));
        props.put(MuleProperties.MULE_SOAP_METHOD, method);
        UMOMessage result = client.send(uri, submittrade, props);
        assertNotNull(result);
        SubmitTradeResponse response = (SubmitTradeResponse)result.getPayload();
        assertEquals("RECEIVED", response.get_return().getStatus());

    }

    public void testSendComplexRPCENC() throws Exception {
        MuleClient client = new MuleClient();

        UMOMessage result = client.send(uri, trade, null);
        assertNotNull(result);
        TradeStatus status = (TradeStatus)result.getPayload();
        assertEquals("RECEIVED", status.getStatus());
    }
}
