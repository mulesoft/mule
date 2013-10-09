/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.config;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test configuration of security filters
 */
public class SecurityFilterTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
