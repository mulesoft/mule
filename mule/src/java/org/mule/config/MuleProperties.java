/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.config;

/**
 * <code>MuleProperties</code> is a set of constants pertaining to Mule system
 * properties.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface MuleProperties
{
    /**
     * The prefix for any Mule-specific properties set on an event
     */
    public final String PROPERTY_PREFIX = "MULE_";

    /**
     * The prefix for any Mule-specific properties set in the system properties
     */
    public final String SYSTEM_PROPERTY_PREFIX = "org.mule.";

    /***************************************************
     * System properties that can be set as VM arguments
     ***************************************************/

    /** Disable the Admin agent */
    public final String DISABLE_SERVER_CONNECTIONS_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + "disable.server.connections";

    /** Configuration parsing properties */
    public final String XML_VALIDATE_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + "xml.validate";

    /** Path to a Mule Dtd to use */
    public final String XML_DTD_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + "xml.dtd";

    /** Default Ecoding used by the server */
    public final String MULE_ENCODING_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + "encoding";

    /** The operatirng system encoding */
    String MULE_OS_ENCODING_SYSTEM_PROPERTY = "org.mule.osEncoding";

    /** whether a configuration builder should start the server after it has been configured, The default is true*/
    public final String MULE_START_AFTER_CONFIG_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + ".start.after.config";

    // End System properties

    /***************************************************
     * Event Level properties
     ***************************************************/
    public final String MULE_EVENT_PROPERTY = PROPERTY_PREFIX + "EVENT";
    public final String MULE_EVENT_TIMEOUT_PROPERTY = PROPERTY_PREFIX + "EVENT_TIMEOUT";
    public final String MULE_METHOD_PROPERTY = PROPERTY_PREFIX + "SERVICE_METHOD";
    public final String MULE_ENDPOINT_PROPERTY = PROPERTY_PREFIX + "ENDPOINT";
    public final String MULE_ERROR_CODE_PROPERTY = PROPERTY_PREFIX + "ERROR_CODE";
    public final String MULE_REPLY_TO_PROPERTY = PROPERTY_PREFIX + "REPLYTO";
    public final String MULE_USER_PROPERTY = PROPERTY_PREFIX + "USER";
    public final String MULE_REPLY_TO_REQUESTOR_PROPERTY = PROPERTY_PREFIX + "REPLYTO_REQUESTOR";
    public final String MULE_SESSION_ID_PROPERTY = PROPERTY_PREFIX + "SESSION_ID";
    public final String MULE_SESSION_PROPERTY = PROPERTY_PREFIX + "SESSION";
    public final String MULE_MESSAGE_ID_PROPERTY = PROPERTY_PREFIX + "MESSAGE_ID";
    public final String MULE_CORRELATION_ID_PROPERTY = PROPERTY_PREFIX + "CORRELATION_ID";
    public final String MULE_CORRELATION_GROUP_SIZE_PROPERTY = PROPERTY_PREFIX + "CORRELATION_GROUP_SIZE";
    public final String MULE_CORRELATION_SEQUENCE_PROPERTY = PROPERTY_PREFIX + "CORRELATION_SEQUENCE";
    public final String MULE_REMOTE_SYNC_PROPERTY = PROPERTY_PREFIX + "REMOTE_SYNC";
    public final String MULE_SOAP_METHOD = PROPERTY_PREFIX + "SOAP_METHOD";
    //End Event Level properties

    /***************************************************
     * Connector Service descriptor properties
     ***************************************************/
    public final String CONNECTOR_CLASS = "connector";
    public final String CONNECTOR_MESSAGE_RECEIVER_CLASS = "message.receiver";
    public final String CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS = "transacted.message.receiver";
    public final String CONNECTOR_FACTORY = "connector.factory";
    public final String CONNECTOR_DISPATCHER_FACTORY = "dispatcher.factory";
    public final String CONNECTOR_TRANSACTION_FACTORY = "transaction.factory";
    public final String CONNECTOR_MESSAGE_ADAPTER = "message.adapter";
    public final String CONNECTOR_STREAM_MESSAGE_ADAPTER = "stream.message.adapter";
    public final String CONNECTOR_INBOUND_TRANSFORMER = "inbound.transformer";
    public final String CONNECTOR_OUTBOUND_TRANSFORMER = "outbound.transformer";
    public final String CONNECTOR_RESPONSE_TRANSFORMER = "response.transformer";
    public final String CONNECTOR_ENDPOINT_BUILDER = "endpoint.builder";
    public final String CONNECTOR_SERVICE_FINDER = "service.finder";
    public final String CONNECTOR_SERVICE_ERROR = "service.error";
    public final String CONNECTOR_SESSION_HANDLER = "session.handler";
    // End Connector Service descriptor properties
}
