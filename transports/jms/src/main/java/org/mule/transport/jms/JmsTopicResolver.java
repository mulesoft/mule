/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms;

import org.mule.api.endpoint.ImmutableEndpoint;

import javax.jms.Destination;

/**
 * A strategy interface to detect a {@code javax.jms.Topic} in,
 * possibly, a vendor-specific way.
 */
public interface JmsTopicResolver
{
    /**
     * Use endpoint configuration to detect a topic.
     * @param endpoint endpoint to test
     * @return true if endpoint's config tells it's a topic
     * @see #isTopic(org.mule.api.endpoint.ImmutableEndpoint, boolean)
     */
    boolean isTopic(ImmutableEndpoint endpoint);

    /**
     * Use endpoint configuration to detect a topic. Additionally,
     * specify a fallback mechanism to search in endpoint's properties
     * in case resource info yields {@code false}. In case resource info
     * returned {@code true} no endpoint properties would be consulted.
     * @param endpoint endpoint to test
     * @param fallbackToEndpointProperties  whether to check endpoint's properties if
     *        resource info returned false
     * @return true if endpoint's config tells it's a topic
     */
    boolean isTopic(ImmutableEndpoint endpoint, boolean fallbackToEndpointProperties);

    /**
     * Use any means suitable to detect a topic. This can
     * be as simple as an {@code instanceof} call or utilize
     * reflection and/or vendor API instead. 
     * @param destination a jms destination to test
     * @return {@code true} for topic
     */
    boolean isTopic(Destination destination);
}

