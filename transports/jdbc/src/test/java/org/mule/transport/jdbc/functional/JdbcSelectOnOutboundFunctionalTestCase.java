/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class JdbcSelectOnOutboundFunctionalTestCase extends AbstractJdbcFunctionalTestCase
{
    public JdbcSelectOnOutboundFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, AbstractJdbcFunctionalTestCase.getConfig() + ",jdbc-select-outbound-service.xml"},
            {ConfigVariant.FLOW, AbstractJdbcFunctionalTestCase.getConfig() + ",jdbc-select-outbound-flow.xml"}
        });
    }      

    @Test
    public void testSelectOnOutbound() throws Exception
    {
        doSelectOnOutbound("vm://jdbc.test");
    }

    @Test
    public void testSelectOnOutboundByExpression() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MyMessage payload = new MyMessage(2);
        MuleMessage reply = client.send("vm://terra", new DefaultMuleMessage(payload, muleContext));
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof List);
        List resultList = (List)reply.getPayload();
        assertTrue(resultList.size() == 1);
        assertTrue(resultList.get(0) instanceof Map);
        Map resultMap = (Map) resultList.get(0);
        assertEquals(new Integer(2), resultMap.get("TYPE"));
        assertEquals(TEST_VALUES[1], resultMap.get("DATA"));
    }

    @Test
    public void testChain2SelectAlwaysBegin() throws Exception 
    { 
        doSelectOnOutbound("vm://chain.always.begin"); 
    } 

    @Test
    public void testChain2SelectBeginOrJoin() throws Exception 
    { 
        doSelectOnOutbound("vm://chain.begin.or.join"); 
    }
    
    private void doSelectOnOutbound(String endpoint) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send(endpoint, new Object(), null);
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof List);
        List resultList = (List) reply.getPayload();
        assertTrue(resultList.size() == 1);
        assertTrue(resultList.get(0) instanceof Map);
        Map resultMap = (Map) resultList.get(0);
        assertEquals(new Integer(1), resultMap.get("TYPE"));
        assertEquals(TEST_VALUES[0], resultMap.get("DATA"));
    }
    
    public static class MyMessage implements Serializable
    {
        public MyMessage(int type)
        {
            this.type = type;
        }

        private int type;

        public int getType()
        {
            return type;
        }

        public void setType(int type)
        {
            this.type = type;
        }
    }
}
