/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.websphere;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.transport.jms.JmsConnector;

import java.util.Collections;
import java.util.Map;

/**
 * Websphere-specific JMS connector.
 */
public class WebsphereJmsConnector extends JmsConnector
{
    
    public static final String DEFAULT_XA_RECEIVER_CLASS = WebsphereTransactedJmsMessageReceiver.class.getName();
    
    /** Constructs a new WebsphereJmsConnector. */
    public WebsphereJmsConnector(MuleContext context)
    {
        super(context);
        if (serviceOverrides == null || serviceOverrides.isEmpty())
        {
            Map overrides = Collections.singletonMap(MuleProperties.CONNECTOR_XA_TRANSACTED_MESSAGE_RECEIVER_CLASS,
                DEFAULT_XA_RECEIVER_CLASS);
            setServiceOverrides(overrides);
        }
    }
}
