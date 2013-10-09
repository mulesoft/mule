/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jaas;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.SystemUtils;

public class JaasAuthenticationWithNtLoginModule extends AbstractJaasFunctionalTestCase
{
    public JaasAuthenticationWithNtLoginModule(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "mule-conf-with-NTLoginModule.xml"}

        });
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
        assertTrue(m.getPayload() instanceof String);
        assertTrue(m.getPayloadAsString().equals("Test Received"));
    }
}
