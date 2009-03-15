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
 * Abstracts all the Jms Vendor specific configuration for the Jms integration test suite.
 * An implementation of this class must be created for each Jms Vendor that gets tested.
 *
 * The integration tests use a fixed set of destination names since some Jms vendors require
 * that all destinations are configured beforehand.  The queue configurations that must be
 * made available are:
 *
 * - 'in' Queue
 * - 'middle' Queue
 * - 'middle2' Queue
 * - 'middle3' Queue
 * - 'out' Queue
 * - 'broadcast' Topic
 *
 * Also, there will need to be a {@link javax.jms.QueueConnectionFactory}, {@link javax.jms.TopicConnectionFactory},
 * {@link javax.jms.XAQueueConnectionFactory} and {@link javax.jms.XATopicConnectionFactory} available. These will be
 * used to create JMS {@link javax.jms.Connection} objects using the {@link #getConnection(boolean, boolean)} method of
 * this class.
 *
 * Note that this class defines a single method for {@link #getMiddleDestinationName()} but the {@link AbstractJmsFunctionalTestCase}
 * will made available 'middle' destination references i.e. 'middle2' and 'middle3'.
 *
 * Fore more inforation about the JMS Integration tests see {@link AbstractJmsFunctionalTestCase}
 */
public interface JmsVendorConfiguration
{
    /**
     * Create a connection factory for the Jms profider being tested
     * 
     * @param topic whether to use a topic or queue connection factory, for 1.1
     *            implementations this proerty can be ignored
     * @param xa whether to create an XA connection factory
     * @return a new JMS connection
     */
    public abstract Connection getConnection(boolean topic, boolean xa) throws Exception;

    /**
     * Returns the {@link #getInboundDestinationName()} in the form of an endpoint URI i.e.
     * jms://in
     *
     * @return the Inbound JMS endpoint
     */
    public String getInboundEndpoint();

     /**
     * Returns the {@link #getOutboundDestinationName()} in the form of an endpoint URI i.e.
     * jms://out
      *
     * @return the Outbound JMS endpoint
     */
    public String getOutboundEndpoint();

     /**
     * Returns the {@link #getMiddleDestinationName()} in the form of an endpoint URI i.e.
     * jms://middle
      *
     * @return the middle JMS endpoint
     */
    public String getMiddleEndpoint();

     /**
     * Returns the {@link #getBroadcastDestinationName()} in the form of an endpoint URI i.e.
     * jms://topic:broadcast
      *
     * @return the Broadcast JMS topic endpoint
     */
    public String getTopicBroadcastEndpoint();

    /**
     * The test inbound queue name.  For consistency this should always be 'in'. Note that you need to make
     * sure that this queue is available in the the JMS provider being tested.
     *
     * @return The test inbound destination name
     */
    public String getInboundDestinationName();

    /**
     * The test outbound queue name.  For consistency this should always be 'out'. Note that you need to make
     * sure that this queue is available in the the JMS provider being tested.
     *
     * @return The test outbound destination name
     */
    public String getOutboundDestinationName();

    /**
     * The test middle queue name.  For consistency this should always be 'middle'. This value is used to create
     * multiple middle queues, namely, 'middle', 'middle2', 'middle3'. You need to make
     * sure that these queues are available in the the JMS provider being tested.
     *
     * @return The test middle destination name
     */
    public String getMiddleDestinationName();

    /**
     * The test broadcast topic name.  For consistency this should always be 'broadcast'. Note that you need to make
     * sure that this topic is available in the the JMS provider being tested.
     *
     * @return The test broadcast topic name
     */
    public String getBroadcastDestinationName();

    /**
     * Timeout in milliseconds used when checking that a message is NOT present. This is usually 1000-2000ms.
     * It is customizable so that slow connections i.e. over a wAN can be accounted for.
     * 
     * @return timeout in milliseconds used when checking that a message is NOT present
     */
    public long getSmallTimeout();

    /**
     * The timeout in milliseconds used when waiting for a message to arrive. This is usually 3000-5000ms.
     * However, it is customizable so that slow connections i.e. over a wAN can be accounted for.
     * 
     * @return The timeout used when waiting for a message to arrive
     */
    public long getTimeout();

    /**
     * The protocol used for creating endpoints.  This is usually 'jms' but for specific messaging transports
     * such as WebsphereMQ the protocol will be the protocol of the transport i.e. 'wmq'.
     * @return returns the transport protocol
     */
    public String getProtocol();

    /**
     * A string that identifies the Jms Provider i.e. WebsphereMQ
     * @return the provider name
     */
    public String getProviderName();

    /**
     * These properties will get loaded into the registry. Good for adding property
     * placeholders since provider or environment specific config info can be added as
     * properties here and configuration files can use placeholders for these properties
     * to make the tests portable. For example, if a property defined here was activemq.host=192.168.0.4
     * an XML configuration file could access it using ${activemq.host} instead of putting the host name
     * in the configuration file.
     * It is good practice to prefix the property with a namespace just to ensure there are no conflicts, In
     * this case the namespace is 'activemq'
     * 
     * @return a map of properties that will be made available in the registry
     */
    public Map getProperties();
}
