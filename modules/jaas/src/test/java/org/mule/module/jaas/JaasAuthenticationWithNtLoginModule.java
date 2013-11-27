/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jaas;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.util.SystemUtils;

import java.util.Map;

import org.junit.Test;

public class JaasAuthenticationWithNtLoginModule extends AbstractJaasFunctionalTestCase
{

    @Override
    protected String getConfigFile()
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
        Map<String, Object> props = createMessagePropertiesWithCredentials("Marie.Rizzo", "dragon");
        MuleMessage m = muleContext.getClient().send("vm://test", "Test", props);

        assertNotNull(m);
        assertTrue(m.getPayloadAsString().equals("Test Received"));
    }
}
