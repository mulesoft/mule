/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc.functional;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.providers.NullPayload;
import org.mule.umo.UMOMessage;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class JdbcSelectOnOutboundFunctionalTestCase extends AbstractJdbcFunctionalTestCase
{
    protected String getConfigResources()
    {
        return super.getConfigResources() + ",jdbc-select-outbound.xml";
    }

    public void testSelectOnOutbound() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("vm://jdbc.test", new MuleMessage(NullPayload.getInstance()));
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof List);
        List resultList = (List) reply.getPayload();
        assertTrue(resultList.size() == 1);
        assertTrue(resultList.get(0) instanceof Map);
        Map resultMap = (Map) resultList.get(0);
        assertEquals(new Integer(1), resultMap.get("TYPE"));
        assertEquals(TEST_VALUES[0], resultMap.get("DATA"));
    }

    public void testSelectOnOutboundByPropertyExtractor() throws Exception
    {
        MuleClient client = new MuleClient();
        MyMessage payload = new MyMessage(2);
        UMOMessage reply = client.send("vm://terra", new MuleMessage(payload));
        assertNotNull(reply.getPayload());
        assertTrue(reply.getPayload() instanceof List);
        List resultList = (List) reply.getPayload();
        logger.debug("resultList.size() " + resultList.size());
        assertTrue(resultList.size() == 1);
        assertTrue(resultList.get(0) instanceof Map);
        Map resultMap = (Map) resultList.get(0);
        assertEquals(new Integer(2), resultMap.get("TYPE"));
        assertEquals(TEST_VALUES[1], resultMap.get("DATA"));
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
