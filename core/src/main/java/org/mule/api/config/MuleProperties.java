/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.config;


/**
 * <code>MuleProperties</code> is a set of constants pertaining to Mule properties.
 */
public class MuleProperties
{
    /**
     * The prefix for any Mule-specific properties set on an event
     */
    public static final String PROPERTY_PREFIX = "MULE_";

    // End System properties

    /********************************************************************************
     * MuleEvent Level properties
     *******************************************************************************/
    public static final String MULE_EVENT_PROPERTY = PROPERTY_PREFIX + "EVENT";
    public static final String MULE_EVENT_TIMEOUT_PROPERTY = PROPERTY_PREFIX + "EVENT_TIMEOUT";
    public static final String MULE_METHOD_PROPERTY = "method";

    // Deprecated. 'method' is now used consistently for all transports
    // public static final String MULE_METHOD_PROPERTY = PROPERTY_PREFIX + "SERVICE_METHOD";
    public static final String MULE_IGNORE_METHOD_PROPERTY = PROPERTY_PREFIX + "IGNORE_METHOD";
    public static final String MULE_ENDPOINT_PROPERTY = PROPERTY_PREFIX + "ENDPOINT";
    public static final String MULE_ORIGINATING_ENDPOINT_PROPERTY = PROPERTY_PREFIX + "ORIGINATING_ENDPOINT";
    public static final String MULE_ERROR_CODE_PROPERTY = PROPERTY_PREFIX + "ERROR_CODE";
    public static final String MULE_REPLY_TO_PROPERTY = PROPERTY_PREFIX + "REPLYTO";
    /**
     * Prevents processing of the ReplyTo property by the Service. This is useful
     * if you're component routed the message somewhere else which processed the
     * ReplyTo.
     */
    public static final String MULE_REPLY_TO_STOP_PROPERTY = PROPERTY_PREFIX + "REPLYTO_STOP";
    public static final String MULE_USER_PROPERTY = PROPERTY_PREFIX + "USER";
    public static final String MULE_ENCODING_PROPERTY = PROPERTY_PREFIX + "ENCODING";
    public static final String MULE_REPLY_TO_REQUESTOR_PROPERTY = PROPERTY_PREFIX + "REPLYTO_REQUESTOR";
    public static final String MULE_SESSION_ID_PROPERTY = PROPERTY_PREFIX + "SESSION_ID";
    public static final String MULE_SESSION_PROPERTY = PROPERTY_PREFIX + "SESSION";
    public static final String MULE_MESSAGE_ID_PROPERTY = PROPERTY_PREFIX + "MESSAGE_ID";
    public static final String MULE_CORRELATION_ID_PROPERTY = PROPERTY_PREFIX + "CORRELATION_ID";
    public static final String MULE_CORRELATION_GROUP_SIZE_PROPERTY = PROPERTY_PREFIX + "CORRELATION_GROUP_SIZE";
    public static final String MULE_CORRELATION_SEQUENCE_PROPERTY = PROPERTY_PREFIX + "CORRELATION_SEQUENCE";
    public static final String MULE_REMOTE_SYNC_PROPERTY = PROPERTY_PREFIX + "REMOTE_SYNC";
    public static final String MULE_REMOTE_CLIENT_ADDRESS = PROPERTY_PREFIX + "REMOTE_CLIENT_ADDRESS";
    public static final String MULE_SOAP_METHOD = PROPERTY_PREFIX + "SOAP_METHOD";
    public static final String MULE_JMS_SESSION = PROPERTY_PREFIX + "JMS_SESSION";
    public static final String MULE_MANAGEMENT_CONTEXT_PROPERTY = PROPERTY_PREFIX + "MANAGEMENT_CONTEXT";
    // End MuleEvent Level properties

    /********************************************************************************
     * Generic Service descriptor properties
     *******************************************************************************/
    public static final String SERVICE_FINDER = "service.finder";
    
    /********************************************************************************
     * Model Service descriptor properties
     *******************************************************************************/
    public static final String MODEL_CLASS = "model";

    /********************************************************************************
     * Transport Service descriptor properties
     *******************************************************************************/
    public static final String CONNECTOR_CLASS = "connector";
    public static final String CONNECTOR_MESSAGE_RECEIVER_CLASS = "message.receiver";
    public static final String CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS = "transacted.message.receiver";
    public static final String CONNECTOR_XA_TRANSACTED_MESSAGE_RECEIVER_CLASS = "xa.transacted.message.receiver";
    public static final String CONNECTOR_DISPATCHER_FACTORY = "dispatcher.factory";
    public static final String CONNECTOR_REQUESTER_FACTORY = "requester.factory";
    public static final String CONNECTOR_TRANSACTION_FACTORY = "transaction.factory";
    public static final String CONNECTOR_MESSAGE_ADAPTER = "message.adapter";
    public static final String CONNECTOR_INBOUND_TRANSFORMER = "inbound.transformer";
    public static final String CONNECTOR_OUTBOUND_TRANSFORMER = "outbound.transformer";
    public static final String CONNECTOR_RESPONSE_TRANSFORMER = "response.transformer";
    public static final String CONNECTOR_ENDPOINT_BUILDER = "endpoint.builder";
    public static final String CONNECTOR_SERVICE_FINDER = "service.finder";
    public static final String CONNECTOR_SERVICE_ERROR = "service.error";
    public static final String CONNECTOR_SESSION_HANDLER = "session.handler";
    // End Connector Service descriptor properties

    public static final String MULE_WORKING_DIRECTORY_PROPERTY = "mule.working.dir";
    public static final String MULE_HOME_DIRECTORY_PROPERTY = "mule.home";

    // Object Name Keys

    public static final String OBJECT_SYSTEM_MODEL = "_muleSystemModel";
    public static final String OBJECT_MULE_CONTEXT_PROCESSOR = "_muleContextProcessor";
    public static final String OBJECT_PROPERTY_PLACEHOLDER_PROCESSOR = "_mulePropertyPlaceholderProcessor";
    public static final String OBJECT_OBJECT_NAME_PROCESSOR = "_muleObjectNameProcessor";
    public static final String OBJECT_LIFECYCLE_MANAGER = "_muleLifecycleManager";
    public static final String OBJECT_SECURITY_MANAGER = "_muleSecurityManager";
    public static final String OBJECT_TRANSACTION_MANAGER = "_muleTransactionManager";
    public static final String OBJECT_QUEUE_MANAGER = "_muleQueueManager";
    public static final String OBJECT_MULE_APPLICATION_PROPERTIES = "_muleProperties";
    public static final String OBJECT_MULE_ENDPOINT_FACTORY = "_muleEndpointFactory";
    public static final String OBJECT_MULE_STREAM_CLOSER_SERVICE = "_muleStreamCloserService";
    public static final String OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP = "_muleSimpleRegistryBootstrap";
    public static final String OBJECT_DEFAULT_THREADING_PROFILE = "_defaultThreadingProfile";
    public static final String OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE = "_defaultMessageDispatcherThreadingProfile";
    public static final String OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE = "_defaultMessageRequesterThreadingProfile";
    public static final String OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE = "_defaultMessageReceiverThreadingProfile";
    public static final String OBJECT_DEFAULT_SERVICE_THREADING_PROFILE = "_defaultServiceThreadingProfile";
    public static final String OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE = "_defaultRetryPolicyTemplate";
    public static final String OBJECT_MULE_CONFIGURATION = "_muleConfiguration";
    public static final String OBJECT_MULE_NAMESPACE_MANAGER = "_muleNamespaceManager";

    // Not currently used as these need to be instance variables of the MuleContext.
    public static final String OBJECT_WORK_MANAGER = "_muleWorkManager";
    public static final String OBJECT_NOTIFICATION_MANAGER = "_muleNotificationManager";

    /**
    * Specifies whether mule should process messages sysnchonously, i.e. that a
    * mule-model can only process one message at a time, or asynchronously. The
    * default value is 'false'.
    */
    public static final String SYNCHRONOUS_PROPERTY = "synchronous";
}
