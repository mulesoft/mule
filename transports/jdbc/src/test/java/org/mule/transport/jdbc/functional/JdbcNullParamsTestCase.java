/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transport.NullPayload;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class JdbcNullParamsTestCase extends AbstractJdbcFunctionalTestCase
{
    public JdbcNullParamsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setPopulateTestData(false);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.FLOW, "jdbc-null-params.xml"}
        });
    }

    @Test
    public void testJdbcNullParams() throws Exception
    {
        MuleClient client = muleContext.getClient();

        //check that db is still empty
        MuleMessage reply = client.request("jdbc://getTest", 1000);
        assertTrue(reply.getPayload() instanceof Collection);
        assertTrue(((Collection<?>)reply.getPayload()).isEmpty());

        //execute the write query by sending a message on the jdbc://writeTest
        //the message is a nullpayload since we are not taking any params from any object
        //No other params will be sent to this endpoint
        client.send("jdbc://writeTest", new DefaultMuleMessage(NullPayload.getInstance(), muleContext));

        //get the data which was written by the previous statement and test it
        reply = client.request("jdbc://getTest", 1000);

        assertNotNull(reply);
        assertTrue(reply.getPayload() instanceof Collection);
        Collection<?> result = (Collection<?>)reply.getPayload();
        assertEquals(1, result.size());

        Map<?, ?> res = (Map<?, ?>)result.iterator().next();

        //check that id is equal to the one set originally and all others are null
        Integer id = (Integer)res.get("ID");
        assertEquals(1, id.intValue());
        assertNull(res.get("TYPE"));
        assertNull(res.get("DATA"));
        assertNull(res.get("RESULT"));
    }
}
