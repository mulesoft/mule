/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.issues;

import static org.junit.Assert.assertThat;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import java.util.HashMap;
import org.hamcrest.core.IsNull;
import org.junit.Rule;
import org.junit.Test;




public class Mule5415TestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/issues/mule-5415-config.xml";
    }

    @Test
    public void testFirstRequestDoesntFail() throws Exception
    {
        MuleClient client = muleContext.getClient();
        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("Content-Type","application/x-www-form-urlencoded");
        MuleMessage message = client.send(String.format("http://localhost:%s?param1=1&param2=3", port1.getNumber()), "message", properties);
        assertThat(message.getExceptionPayload(), IsNull.<Object>nullValue());
    }


}
