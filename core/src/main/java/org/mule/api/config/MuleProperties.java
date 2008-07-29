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
public interface MuleProperties
{
    /**
     * The prefix for any Mule-specific properties set on an event
     */
    String PROPERTY_PREFIX = "MULE_";

    // End System properties

    /********************************************************************************
     * MuleEvent Level properties
     *******************************************************************************/
    String MULE_EVENT_PROPERTY = PROPERTY_PREFIX + "EVENT";
    String MULE_EVENT_TIMEOUT_PROPERTY = PROPERTY_PREFIX + "EVENT_TIMEOUT";
    String MULE_METHOD_PROPERTY = "method";

    // Deprecated. 'method' is now used consistently for all transports
    // String MULE_METHOD_PROPERTY = PROPERTY_PREFIX + "SERVICE_METHOD";
    String MULE_IGNORE_METHOD_PROPERTY = PROPERTY_PREFIX + "IGNORE_METHOD";
    String MULE_ENDPOINT_PROPERTY = PROPERTY_PREFIX + "ENDPOINT";
    String MULE_ORIGINATING_ENDPOINT_PROPERTY = PROPERTY_PREFIX + "ORIGINATING_ENDPOINT";
    String MULE_ERROR_CODE_PROPERTY = PROPERTY_PREFIX + "ERROR_CODE";
    String MULE_REPLY_TO_PROPERTY = PROPERTY_PREFIX + "REPLYTO";
    String MULE_USER_PROPERTY = PROPERTY_PREFIX + "USER";
    String MULE_ENCODING_PROPERTY = PROPERTY_PREFIX + "ENCODING";
    String MULE_REPLY_TO_REQUESTOR_PROPERTY = PROPERTY_PREFIX + "REPLYTO_REQUESTOR";
    String MULE_SESSION_ID_PROPERTY = PROPERTY_PREFIX + "SESSION_ID";
    String MULE_SESSION_PROPERTY = PROPERTY_PREFIX + "SESSION";
    String MULE_MESSAGE_ID_PROPERTY = PROPERTY_PREFIX + "MESSAGE_ID";
    String MULE_CORRELATION_ID_PROPERTY = PROPERTY_PREFIX + "CORRELATION_ID";
    String MULE_CORRELATION_GROUP_SIZE_PROPERTY = PROPERTY_PREFIX + "CORRELATION_GROUP_SIZE";
    String MULE_CORRELATION_SEQUENCE_PROPERTY = PROPERTY_PREFIX + "CORRELATION_SEQUENCE";
    String MULE_REMOTE_SYNC_PROPERTY = PROPERTY_PREFIX + "REMOTE_SYNC";
    String MULE_REMOTE_CLIENT_ADDRESS = PROPERTY_PREFIX + "REMOTE_CLIENT_ADDRESS";
    String MULE_SOAP_METHOD = PROPERTY_PREFIX + "SOAP_METHOD";
    String MULE_JMS_SESSION = PROPERTY_PREFIX + "JMS_SESSION";
    String MULE_MANAGEMENT_CONTEXT_PROPERTY = PROPERTY_PREFIX + "MANAGEMENT_CONTEXT";
    // End MuleEvent Level properties

    /********************************************************************************
     * Generic Service descriptor properties
     *******************************************************************************/
    String SERVICE_FINDER = "service.finder";
    
    /********************************************************************************
     * Model Service descriptor properties
     *******************************************************************************/
    String MODEL_CLASS = "model";

    /********************************************************************************
     * Transport Service descriptor properties
     *******************************************************************************/
    String CONNECTOR_CLASS = "connector";
    String CONNECTOR_MESSAGE_RECEIVER_CLASS = "message.receiver";
    String CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS = "transacted.message.receiver";
    String CONNECTOR_XA_TRANSACTED_MESSAGE_RECEIVER_CLASS = "xa.transacted.message.receiver";
    String CONNECTOR_DISPATCHER_FACTORY = "dispatcher.factory";
    String CONNECTOR_REQUESTER_FACTORY = "requester.factory";
    String CONNECTOR_TRANSACTION_FACTORY = "transaction.factory";
    String CONNECTOR_MESSAGE_ADAPTER = "message.adapter";
    String CONNECTOR_INBOUND_TRANSFORMER = "inbound.transformer";
    String CONNECTOR_OUTBOUND_TRANSFORMER = "outbound.transformer";
    String CONNECTOR_RESPONSE_TRANSFORMER = "response.transformer";
    String CONNECTOR_ENDPOINT_BUILDER = "endpoint.builder";
    String CONNECTOR_SERVICE_FINDER = "service.finder";
    String CONNECTOR_SERVICE_ERROR = "service.error";
    String CONNECTOR_SESSION_HANDLER = "session.handler";
    // End Connector Service descriptor properties

    String MULE_WORKING_DIRECTORY_PROPERTY = "mule.working.dir";
    String MULE_HOME_DIRECTORY_PROPERTY = "mule.home";

    // Object Name Keys

    String OBJECT_SYSTEM_MODEL = "_muleSystemModel";
    String OBJECT_MULE_CONTEXT_PROCESSOR = "_muleContextProcessor";
    String OBJECT_PROPERTY_PLACEHOLDER_PROCESSOR = "_mulePropertyPlaceholderProcessor";
    String OBJECT_OBJECT_NAME_PROCESSOR = "_muleObjectNameProcessor";
    String OBJECT_LIFECYCLE_MANAGER = "_muleLifecycleManager";
    String OBJECT_SECURITY_MANAGER = "_muleSecurityManager";
    String OBJECT_TRANSACTION_MANAGER = "_muleTransactionManager";
    String OBJECT_QUEUE_MANAGER = "_muleQueueManager";
    String OBJECT_MULE_APPLICATION_PROPERTIES = "_muleProperties";
    String OBJECT_MULE_ENDPOINT_FACTORY = "_muleEndpointFactory";
    String OBJECT_MULE_STREAM_CLOSER_SERVICE = "_muleStreamCloserService";
    String OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP = "_muleSimpleRegistryBootstrap";
    String OBJECT_DEFAULT_THREADING_PROFILE = "_defaultThreadingProfile";
    String OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE = "_defaultMessageDispatcherThreadingProfile";
    String OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE = "_defaultMessageRequesterThreadingProfile";
    String OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE = "_defaultMessageReceiverThreadingProfile";
    String OBJECT_DEFAULT_COMPONENT_THREADING_PROFILE = "_defaultComponentThreadingProfile";
    String OBJECT_DEFAULT_CONNECTION_STRATEGY = "_defaultConnectionStrategy";
    String OBJECT_MULE_CONFIGURATION = "_muleConfiguration";

    // Not currently used as these need to be instance variables of the MuleContext.
    String OBJECT_WORK_MANAGER = "_muleWorkManager";
    String OBJECT_NOTIFICATION_MANAGER = "_muleNotificationManager";

    /**
    * Specifies whether mule should process messages sysnchonously, i.e. that a
    * mule-model can only process one message at a time, or asynchronously. The
    * default value is 'false'.
    */
    String SYNCHRONOUS_PROPERTY = "synchronous";
}
