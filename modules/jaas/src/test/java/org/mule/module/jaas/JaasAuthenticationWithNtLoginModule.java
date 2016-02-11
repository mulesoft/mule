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
        SecurityHeader securityHeader = createSecurityHeader("Marie.Rizzo", "dragon");
        MuleMessage message = flowRunner("TestUMO").withInboundProperty(securityHeader.getKey(), securityHeader.getValue()).run().getMessage();
        assertNotNull(message);
        assertTrue(getPayloadAsString(message).equals("Test Received"));
    }
}
