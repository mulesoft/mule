/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.util.MapUtils;
import org.mule.runtime.core.util.StringMessageUtils;

import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A default implementation of the resolver uses endpoint's
 * resource info and Java's {@code instanceof} operator to
 * detect JMS topics.
 */
public class DefaultJmsTopicResolver implements JmsTopicResolver
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(DefaultJmsTopicResolver.class);

    /**
     * Connector back-reference.
     */
    private JmsConnector connector;

    /**
     * Create an instance of the resolver.
     * @param connector owning connector
     */
    public DefaultJmsTopicResolver (final JmsConnector connector)
    {
        this.connector = connector;
    }


    /**
     * Getter for property 'connector'.
     *
     * @return Value for property 'connector'.
     */
    public JmsConnector getConnector ()
    {
        return connector;
    }

    /**
     * Will use endpoint's resource info to detect a topic,
     * as in {@code jms://topic:trade.PriceUpdatesTopic}. This
     * method will call {@link #isTopic(org.mule.api.endpoint.ImmutableEndpoint, boolean)}
     * with fallback flag set to <strong>true</false>.
     * <p/>
     * <strong>NOTE:</strong> When using topics, use the '.' (dot) symbol for subcontext separation,
     * as opposed to '/'. Otherwise the resource info may not get properly translated for the
     * topic endpoint due to the way URI's are parsed. 
     * @param endpoint endpoint to test
     * @return true if the endpoint has a topic configuration
     * @see #isTopic(org.mule.api.endpoint.ImmutableEndpoint, boolean) 
     */
    @Override
    public boolean isTopic (ImmutableEndpoint endpoint)
    {
        return isTopic(endpoint, true);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTopic (ImmutableEndpoint endpoint, boolean fallbackToEndpointProperties)
    {
        String resourceInfo = endpoint.getEndpointURI().getResourceInfo();

        boolean topic = JmsConstants.TOPIC_PROPERTY.equalsIgnoreCase(resourceInfo) ||
                endpoint.getEndpointURI().toString().contains(JmsConstants.TOPIC_PROPERTY + ":");
        if (!topic && fallbackToEndpointProperties)
        {
            topic = MapUtils.getBooleanValue(endpoint.getProperties(), JmsConstants.TOPIC_PROPERTY, false);
        }

        return topic;
    }

    /**
     * Will use an {@code instanceof} operator. Keep in mind
     * that may fail for JMS systems implementing both a
     * {@code javax.jms.Topic} and {@code javax.jms.Queue} in
     * a single destination class implementation.
     * @param destination a jms destination to test
     * @return {@code true} if the destination is a topic
     */
    @Override
    public boolean isTopic (Destination destination)
    {
        checkInvariants(destination);

        return destination instanceof Topic;
    }

    /**
     * Perform some sanity checks, will complain in the log.
     * @param destination destination to test
     */
    protected void checkInvariants (final Destination destination)
    {
        if (destination instanceof Topic && destination instanceof Queue
            && connector.getJmsSupport() instanceof Jms102bSupport)
        {
            logger.error(StringMessageUtils.getBoilerPlate(
                    "Destination implements both Queue and Topic "
                    + "while complying with JMS 1.0.2b specification. "
                    + "Please report your application server or JMS vendor name and version "
                    + "to dev<_at_>mule.codehaus.org or http://www.mulesoft.org/jira"));
        }
    }
}
