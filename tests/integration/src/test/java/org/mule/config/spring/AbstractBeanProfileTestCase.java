/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

public abstract class AbstractBeanProfileTestCase extends FunctionalTestCase
{

    protected String getConfigResources(String profile)
    {
        System.setProperty("spring.profiles.active", profile);
        return "org/mule/test/integration/spring/bean-profiles-config.xml";
    }

    public void profile(String appended) throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in", "Homero", null);
        MuleMessage response = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull("Response is null", response);
        assertEquals("Homero" + appended, response.getPayload());
    }
}


