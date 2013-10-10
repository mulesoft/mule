/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.scripting;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GroovyRegistryLookupTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "groovy-registry-lookup-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        muleContext.getRegistry().registerObject("hello", new Hello());
    }

    @Test
    public void testBindingCallout() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://test", "", null);
        assertNotNull(response);
        assertEquals("hello", response.getPayloadAsString());
    }

    public static class Hello
    {
        public String sayHello() 
        {
            return "hello";
        }
    }

}
