/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.security.CryptoFailureException;
import org.mule.api.security.EncryptionStrategyNotFoundException;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.processor.SecurityFilterMessageProcessor;
import org.mule.security.AbstractAuthenticationFilter;
import org.mule.security.filters.MuleEncryptionEndpointSecurityFilter;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.List;

import org.junit.Test;

/**
 * Test configuration of security filters
 */
public class SecurityFilterTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/config/security-filter-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        EndpointBuilder epb = muleContext.getRegistry().lookupEndpointBuilder("testEndpoint1");
        assertNotNull(epb);
        InboundEndpoint iep = epb.buildInboundEndpoint();
        List<MessageProcessor> mps =iep.getMessageProcessors();
        int count = 0;
        SecurityFilterMessageProcessor securityMp = null;
        for (MessageProcessor mp : mps)
        {
            if (mp instanceof SecurityFilterMessageProcessor)
            {
                count++;
                securityMp = (SecurityFilterMessageProcessor) mp;
            }
        }
        assertEquals(1, count);
        assertEquals(CustomSecurityFilter.class, securityMp.getFilter().getClass());

        epb = muleContext.getRegistry().lookupEndpointBuilder("testEndpoint2");
        assertNotNull(epb);
        iep = epb.buildInboundEndpoint();
        mps =iep.getMessageProcessors();
        count = 0;
        securityMp = null;
        for (MessageProcessor mp : mps)
        {
            if (mp instanceof SecurityFilterMessageProcessor)
            {
                count++;
                securityMp = (SecurityFilterMessageProcessor) mp;
            }
        }
        assertEquals(1, count);
        assertEquals(MuleEncryptionEndpointSecurityFilter.class, securityMp.getFilter().getClass());
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
