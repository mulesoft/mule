/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jaas;

import org.mule.api.EncryptionStrategy;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.security.MuleCredentials;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.SystemUtils;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JaasAuthenticationWithNtLoginModule extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "mule-conf-with-NTLoginModule.xml";
    }

    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        return SystemUtils.IS_OS_UNIX;
    }

    @Test
    public void testCaseAuthentication() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map props = new HashMap();
        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("Marie.Rizzo", "dragon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        MuleMessage m = client.send("vm://test", "Test", props);

        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertTrue(m.getPayloadAsString().equals("Test Received"));
    }

}
