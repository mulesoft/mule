/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.weblogic;

import org.mule.api.MuleContext;
import org.mule.transport.jms.JmsConnector;

/**
 * Weblogic-specific JMS connector.
 */
public class WeblogicJmsConnector extends JmsConnector
{
    /** Constructs a new WeblogicJmsConnector. */
    public WeblogicJmsConnector(MuleContext context)
    {
        super(context);
        setTopicResolver(new WeblogicJmsTopicResolver(this));
    }
}
