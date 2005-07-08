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
package org.mule.test.integration.providers.soap;

import org.mule.tck.NamedTestCase;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.MuleManager;
import org.mule.providers.soap.SoapMethod;
import org.mule.providers.soap.NamedParameter;
import org.mule.umo.UMOMessage;
import org.mule.extras.client.MuleClient;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.MuleProperties;
import org.mule.config.builders.MuleXmlConfigurationBuilder;

import javax.xml.rpc.ParameterMode;
import java.util.Map;
import java.util.HashMap;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisNamedParametersTestCase extends AbstractMuleTestCase
{
protected void setUp() throws Exception
    {
        super.setUp();
        if (MuleManager.isInstanciated())
            MuleManager.getInstance().dispose();
        ConfigurationBuilder configBuilder = new MuleXmlConfigurationBuilder();
        configBuilder.configure("org/mule/test/integration/providers/soap/axis-named-param-mule-config.xml");
    }

    public void testNamedParameters() throws Exception
    {
        MuleClient client = new MuleClient();
        //The component itself with throw an exceptin if the parameters in the request
        //soap message are not named
        //UMOMessage result = client.send("axis:http://localhost:38011/mule/mycomponent2?method=echo", "Hello Named", null);
        UMOMessage result = client.send("vm://mycomponent1", "Hello Named", null);
        assertEquals("Hello Named", result.getPayload());
    }

    public void testNamedParametersViaClient() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        //create the soap method passing in the method name and return type
        SoapMethod soapMethod = new SoapMethod("echo", NamedParameter.XSD_STRING);
        //add one or more parameters
        soapMethod.addNamedParameter("value", NamedParameter.XSD_STRING, ParameterMode.IN);
        //set the soap method as a property and pass the properties
        //when making the call
        props.put(MuleProperties.MULE_SOAP_METHOD, soapMethod);

        UMOMessage result = client.send("axis:http://localhost:38011/mule/mycomponent2?method=echo", "Hello Named", props);
        assertEquals("Hello Named", result.getPayload());
    }
}
