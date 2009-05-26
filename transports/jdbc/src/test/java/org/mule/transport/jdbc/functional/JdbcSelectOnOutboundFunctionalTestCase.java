/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class JdbcSelectOnOutboundFunctionalTestCase extends AbstractJdbcFunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return super.getConfigResources() + ",jdbc-select-outbound.xml";
    }

    public void testSelectOnOutbound() throws Exception
    {
        doSelectOnOutbound("vm://jdbc.test");
    }

    public void testSelectOnOutboundByExpression() throws Exception
    {
        MuleClient client = new MuleClient();
        MyMessage payload = new MyMessage(2);
        MuleMessage reply = client.send("vm://terra", new DefaultMuleMessage(payload));
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof List);
        List resultList = (List)reply.getPayload();
        assertTrue(resultList.size() == 1);
        assertTrue(resultList.get(0) instanceof Map);
        Map resultMap = (Map) resultList.get(0);
        assertEquals(new Integer(2), resultMap.get("TYPE"));
        assertEquals(TEST_VALUES[1], resultMap.get("DATA"));
    }

    public void testChain2SelectAlwaysBegin() throws Exception 
    { 
        doSelectOnOutbound("vm://chain.always.begin"); 
    } 

    public void testChain2SelectBeginOrJoin() throws Exception 
    { 
        doSelectOnOutbound("vm://chain.begin.or.join"); 
    }
    
    private void doSelectOnOutbound(String endpoint) throws Exception
    {
        MuleClient client = new MuleClient();
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
