/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.core.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.security.AbstractAuthenticationFilter;

import org.junit.Test;

/**
 * Test configuration of security filters
 */
public class SecurityFilterNonBlockingTestCase extends AbstractIntegrationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/config/security-filter-config-nb.xml";
    }

    @Test
    public void securityFilterShouldAllowNonBlocking() throws Exception
    {
        flowRunner("nonBlockingSecurity").withPayload(TEST_MESSAGE).nonBlocking().run();
    }

    /**
     * Custom security filter class that does nothing at all
     */
    public static class CustomSecurityFilter extends AbstractAuthenticationFilter
    {
        @Override
        protected void doInitialise() throws InitialisationException
        {
        }

        @Override
        public void authenticate(MuleEvent event)
                throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
                SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException
        {
            // TODO Auto-generated method stub

        }
    }
}
