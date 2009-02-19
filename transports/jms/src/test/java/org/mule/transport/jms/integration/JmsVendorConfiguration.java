/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

import java.util.Map;

import javax.jms.Connection;

/**
 * Abstracts all the Jms Vendor specific configuration.
 */
public interface JmsVendorConfiguration
{
    /**
     * Create a connection factory for the Jms profiver being tested
     * 
     * @param topic whether to use a topic or queue connection factory, for 1.1
     *            implementations this proerty can be ignored
     * @param xa whether to create an XA connection factory
     * @return a new connection factory
     */
    public abstract Connection getConnection(boolean topic, boolean xa) throws Exception;

    public String getInboundEndpoint();

    public String getOutboundEndpoint();

    public String getMiddleEndpoint();

    public String getTopicBroadcastEndpoint();

    public String getInboundDestinationName();

    public String getOutboundDestinationName();

    public String getMiddleDestinationName();

    public String getBroadcastDestinationName();

    /**
     * Timeout used when checking that a message is NOT present
     * 
     * @return
     */
    public long getSmallTimeout();

    /**
     * The timeout used when waiting for a message to arrive
     * 
     * @return
     */
    public long getTimeout();

    public String getProtocol();

    public String getProviderName();

    /**
     * These properties will get loaded into the registry. Good for adding property
     * placeholders
     * 
     * @return
     */
    public Map getProperties();
}
