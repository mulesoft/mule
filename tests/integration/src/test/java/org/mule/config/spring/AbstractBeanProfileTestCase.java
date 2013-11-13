/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;

public abstract class AbstractBeanProfileTestCase extends FunctionalTestCase
{

    protected String getConfigFile(String profile)
    {
        System.setProperty("spring.profiles.active", profile);
        return "org/mule/test/integration/spring/bean-profiles-config.xml";
    }

    public void profile(String appended) throws MuleException
    {
        org.mule.api.client.MuleClient client = muleContext.getClient();
        client.dispatch("vm://in", "Homero", null);
        MuleMessage response = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull("Response is null", response);
        assertEquals("Homero" + appended, response.getPayload());
    }
}
