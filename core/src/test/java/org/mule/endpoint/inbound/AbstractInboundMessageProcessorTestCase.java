/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.inbound;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.context.notification.SecurityNotificationListener;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.concurrent.Latch;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import edu.emory.mathcs.backport.java.util.Collections;

public abstract class AbstractInboundMessageProcessorTestCase extends AbstractMuleTestCase
{
    protected static final String TEST_URI = "test://myTestUri";

    @Override
    protected void configureMuleContext(MuleContextBuilder builder)
    {
        super.configureMuleContext(builder);

        // Configure EndpointMessageNotificationListener for notifications test
        ServerNotificationManager notificationManager = new ServerNotificationManager();
        notificationManager.addInterfaceToType(EndpointMessageNotificationListener.class,
            EndpointMessageNotification.class);
        notificationManager.addInterfaceToType(SecurityNotificationListener.class, SecurityNotification.class);

        builder.setNotificationManager(notificationManager);
    }

    protected InboundEndpoint createTestInboundEndpoint(Transformer transformer,
                                                        Transformer responseTransformer)
        throws EndpointException, InitialisationException
    {
        return createTestInboundEndpoint(null, null, transformer, responseTransformer, true, null);
    }

    protected InboundEndpoint createTestInboundEndpoint(Filter filter,
                                                        EndpointSecurityFilter securityFilter,
                                                        boolean sync,
                                                        TransactionConfig txConfig)
        throws InitialisationException, EndpointException
    {
        return createTestInboundEndpoint(filter, securityFilter, null, null, sync, txConfig);
    }

    protected InboundEndpoint createTestInboundEndpoint(Filter filter,
                                                        EndpointSecurityFilter securityFilter,
                                                        Transformer transformer,
                                                        Transformer responseTransformer,
                                                        boolean sync,
                                                        TransactionConfig txConfig)
        throws EndpointException, InitialisationException
    {
        EndpointURIEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(TEST_URI, muleContext);
        endpointBuilder.setFilter(filter);
        endpointBuilder.setSecurityFilter(securityFilter);
        if (transformer != null)
        {
            endpointBuilder.setTransformers(Collections.singletonList(transformer));
        }
        if (responseTransformer != null)
        {
            endpointBuilder.setResponseTransformers(Collections.singletonList(responseTransformer));
        }
        endpointBuilder.setSynchronous(sync);
        endpointBuilder.setTransactionConfig(txConfig);
        InboundEndpoint endpoint = endpointBuilder.buildInboundEndpoint();
        return endpoint;
    }

    protected MuleEvent createTestInboundEvent(InboundEndpoint endpoint, boolean sync) throws Exception
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "value1");
        return new DefaultMuleEvent(new DefaultMuleMessage(TEST_MESSAGE, props, muleContext), endpoint,
            getTestSession(getTestService(), muleContext), sync);
    }

    static class TestFilter implements Filter
    {
        boolean accept;

        public TestFilter(boolean accept)
        {
            this.accept = accept;
        }

        public boolean accept(MuleMessage message)
        {
            return accept;
        }
    }

    static class TestSecurityNotificationListener<SecurityNotification>
        implements SecurityNotificationListener<org.mule.context.notification.SecurityNotification>
    {
        org.mule.context.notification.SecurityNotification securityNotification;
        Latch latch = new Latch();

        public void onNotification(org.mule.context.notification.SecurityNotification notification)
        {
            securityNotification = notification;
            latch.countDown();
        }
    }

    static class TestListener implements MessageProcessor
    {
        MuleEvent sensedEvent;

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            sensedEvent = event;
            return event;
        }
    }

    static class TestEndpointMessageNotificationListener<EndpointMessageNotification>
        implements
        EndpointMessageNotificationListener<org.mule.context.notification.EndpointMessageNotification>
    {
        org.mule.context.notification.EndpointMessageNotification messageNotification;
        Latch latch = new Latch();

        public void onNotification(org.mule.context.notification.EndpointMessageNotification notification)
        {
            messageNotification = notification;
            latch.countDown();
        }
    }

}
