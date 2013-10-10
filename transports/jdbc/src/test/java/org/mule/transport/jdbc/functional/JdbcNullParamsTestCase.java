/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
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
        MuleClient client = new MuleClient(muleContext);
        
        //check that db is still empty
        MuleMessage reply = client.request("jdbc://getTest", 1000);
        assertTrue(reply.getPayload() instanceof Collection);
        assertTrue(((Collection)reply.getPayload()).isEmpty());
        
        //execute the write query by sending a message on the jdbc://writeTest
        //the message is a nullpayload since we are not taking any params from any object
        //No other params will be sent to this endpoint
        client.send("jdbc://writeTest", new DefaultMuleMessage(NullPayload.getInstance(), muleContext));
        
        //get the data which was written by the previous statement and test it
        reply = client.request("jdbc://getTest", 1000);
        
        assertNotNull(reply);
        assertTrue(reply.getPayload() instanceof Collection);
        Collection result = (Collection)reply.getPayload();
        assertEquals(1, result.size());   
        
        Map res = (Map)result.iterator().next();
        
        //check that id is equal to the one set originally and all others are null
        Integer id = (Integer)res.get("ID");
        assertEquals(1, id.intValue());
        assertNull(res.get("TYPE"));
        assertNull(res.get("DATA"));
        assertNull(res.get("RESULT"));
    }
}
