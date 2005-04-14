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
package org.mule.config.i18n;

/**
 * <code>CoreMessageConstants</code> contians contants for all Mule core exception messages
 * and other string.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface CoreMessageConstants
{
    public static final int VERSION_INFO_NOT_SET = 1;
    public static final int SERVER_STARTED_AT_X = 2;
    public static final int SERVER_SHUTDOWN_AT_X = 3;
    public static final int AGENTS_RUNNING = 4;
    public static final int NOT_SET = 5;
    public static final int VERSION = 6;
    public static final int SHUTDOWN_NORMALLY_ON_X = 7;
    public static final int SERVER_WAS_UP_FOR_X = 8;
    public static final int CONFIG_NOT_FOUND_USAGE = 9;
    public static final int FATAL_ERROR_WHILE_RUNNING = 10;
    public static final int ROOT_STACK_TRACE = 11;
    public static final int EXCEPTION_STACK_IS = 12;
    public static final int FOR_MORE_INFO_EXCEPTION = 13;
    public static final int FOR_MORE_INFO = 14;
    public static final int JAVADOC_REF = 15;
    public static final int DOC_REF = 16;
    public static final int SEE_EXCEPTION_BELOW = 17;
    public static final int MESSAGE_IS_OF_TYPE_X = 18;
    public static final int MESSAGE_DETAILS = 19;
    public static final int FATAL_ERROR_SHUTDOWN = 20;
    public static final int NORMAL_SHUTDOWN = 21;
    public static final int NONE = 22;

    public static final int FAILED_TO_ROUTER_VIA_ENDPOINT = 30;
    public static final int NO_COMPONENT_FOR_LOCAL_REFERENCE = 31;
    public static final int CANT_START_DISPOSED_CONNECTOR = 32;
    public static final int CONNECTOR_CAUSED_ERROR = 33;
    public static final int ENDPOINT_NULL_FOR_LISTENER = 34;
    public static final int LISTENER_ALREADY_REGISTERED = 35;
    public static final int CONNECTOR_NOT_STARTED = 36;
    public static final int OBJECT_X_ALREADY_INITIALSIED = 37;
    public static final int COMPONENT_CAUSED_ERROR_IS_X = 38;
    public static final int OBJECT_CAUSED_ERROR_IS_X = 39;
    public static final int X_FAILED_TO_INITIALISE = 40;
    public static final int FAILED_TO_STOP_X = 41;
    public static final int FAILED_TO_START_X = 42;
    public static final int PROXY_POOL_TIMED_OUT = 43;
    public static final int FAILED_TO_GET_POOLED_OBJECT = 44;
    public static final int X_IS_NULL = 45;
    public static final int COMPONENT_X_NOT_REGISTERED = 46;
    public static final int FAILED_TO_REGISTER_X_ON_ENDPOINT_X = 47;
    public static final int FAILED_TO_UNREGISTER_X_ON_ENDPOINT_X = 48;
    public static final int FAILED_TO_PAUSE_X = 49;
    public static final int FAILED_TO_RESUME_X = 50;
    public static final int ENPOINT_X_IS_MALFORMED = 51;
    public static final int TRANSFORM_FAILED_BEFORE_FILTER = 52;
    public static final int TRANSFORM_X_UNEXPECTED_TYPE_X = 53;
    public static final int TRANSFORM_X_UNSUPORTED_TYPE_X_ENDPOINT_X = 54;
    public static final int TRANSFORM_FAILED_FROM_X = 55;
    public static final int ENCRYPT_STRATEGY_NOT_SET = 56;
    public static final int FAILED_LOAD_X_TRANSFORMER_X = 57;
    public static final int FAILED_LOAD_X = 58;
    public static final int MESSAGE_X_NOT_SUPPORTED_BY_ADAPTER_X = 59;
    public static final int TOO_MANY_ENTRY_POINTS_ON_X = 60;
    public static final int CANT_SET_PROP_X_ON_X_OF_TYPE_X = 61;
    public static final int NO_SESSION_FOR_COMPONENT_X = 62;
    public static final int NO_ENDPOINT_X_FOR_COMPONENT_X = 63;
    public static final int NO_COMPONENT_FOR_ENDPOINT = 64;
    public static final int FAILED_TO_CREATE_X = 65;
    public static final int NO_CORRELATION_ID = 66;
    public static final int FAILED_TO_DISPOSE_X = 67;
    public static final int FAILED_TO_INVOKE_X = 68;
    public static final int CANT_READ_PAYLOAD_AS_BYTES_TYPE_IS_X = 69;
    public static final int CANT_READ_PAYLOAD_AS_STRING_TYPE_IS_X = 70;
    public static final int CLASS_X_NOT_FOUND = 71;
    public static final int COMPONENT_X_ROUTING_FAILED_ON_ENDPOINT_X = 72;
    public static final int CANT_INSTANCIATE_FINDER_X = 73;
    public static final int FAILED_TO_CREATE_X_WITH_X = 74;
    public static final int X_NOT_SET_IN_SERVICE_X = 75;
    public static final int OBJECT_NOT_FOUND_X = 76;
    public static final int TX_MARKED_FOR_ROLLBACK = 77;
    public static final int TX_CANT_BIND_TO_NULL_KEY = 78;
    public static final int TX_CANT_BIND_NULL_RESOURCE = 79;
    public static final int TX_SINGLE_RESOURCE_ONLY = 80;
    public static final int NO_CURRENT_EVENT_FOR_TRANSFORMER = 81;
    public static final int X_NOT_REGISTERED_WITH_MANAGER = 82;
    public static final int FAILED_TO_SET_PROPERTIES_ON_X = 83;
    public static final int FAILED_TO_CREATE_CONNECTOR_FROM_URI_X = 84;
    public static final int INITIALISATION_FAILURE_X = 85;
    public static final int FAILED_TO_INITIALISE_INTERCEPTORS_ON_X = 86;
    public static final int FAILED_TO_ENDPOINT_FROM_LOCATION_X = 87;
    public static final int MANAGER_ALREADY_STARTED = 88;
    public static final int NO_ENDPOINTS_FOR_ROUTER = 89;
    public static final int RESPONSE_TIMED_OUT_X_WAITING_FOR_ID_X = 90;
    public static final int NO_LOCAL_IMPL_X_SET_ON_DESCRIPTOR_X = 91;
    public static final int CANT_INSTANCIATE_NON_CONTAINER_REF_X = 92;
    public static final int FAILED_TO_RECEIVE_OVER_X_TIMEOUT_X = 93;
    public static final int FAILED_TO_WRITE_X_TO_STORE_X = 94;
    public static final int FAILED_TO_READ_FROM_STORE_X = 95;
    public static final int TX_CANT_START_X_TRANSACTION = 96;
    public static final int TX_COMMIT_FAILED = 97;
    public static final int TX_ROLLBACK_FAILED = 98;
    public static final int TX_CANT_READ_STATE = 99;
    public static final int TX_RESOURCE_ALREADY_LISTED_FOR_KEY_X = 100;
    public static final int NO_OUTBOUND_ROUTER_SET_ON_X = 101;
    public static final int CANT_SET_PROPERTY_X_ON_X = 102;
    public static final int TX_AVAILABLE_BUT_ACTION_IS_X = 103;
    public static final int TX_NOT_AVAILABLE_BUT_ACTION_IS_X = 104;
    public static final int NO_CATCH_ALL_ENDPOINT_SET = 105;
    public static final int INTERRUPTED_QUEUING_EVENT_FOR_X = 106;
    public static final int TX_CANT_UNBIND = 107;
    public static final int TX_CANT_BIND_ALREADY_BOUND = 108;
    public static final int METHOD_X_NOT_FOUND_ON_X = 109;
    public static final int TRANSFORM_FAILED_FROM_X_TO_X = 110;
    public static final int EVENT_PROPERTY_X_NOT_SET_CANT_PROCESS_REQUEST = 111;
    public static final int CRYPTO_FAILURE = 112;
    public static final int JMX_CREATE_OR_LOCATE_SHOULD_BE_SET = 113;
    public static final int JMX_CANT_LOCATE_CREATE_SERVER = 114;
    public static final int SCHEME_X_NOT_COMPATIBLE_WITH_CONNECTOR_X = 115;
    public static final int NO_ENTRY_POINT_FOUND_ON_X = 116;
    public static final int AUTH_NO_SECURITY_PROVIDER_X = 117;
    public static final int NO_RECEIVER_X_FOR_ENDPOINT_X = 118;
    public static final int TX_SET_AUTO_COMMIT_FAILED = 119;
    public static final int TX_CAN_ONLY_BIND_TO_X_TYPE_RESOURCES = 120;
    public static final int JNDI_RESOURCE_X_NOT_FOUND = 121;
    public static final int CANT_LOAD_X_FROM_CLASSPATH_FILE = 122;
    public static final int FAILED_TO_CONFIGURE_CONTAINER = 123;
    public static final int FAILED_TO_READ_PAYLOAD = 124;
    public static final int MESSAGE_NOT_X_IT_IS_TYPE_X_CHECK_TRANSFORMER_ON_X = 125;
    public static final int ENDPOINT_X_NOT_FOUND = 126;
    public static final int EVENT_PROCIESSING_FAILED_FOR_X = 127;
    public static final int FAILED_TO_DISPATCH_TO_REPLYTO_X = 128;
    public static final int ROUTING_ERROR = 129;
    public static final int CRYPTO_STRATEGY_IS = 130;
    public static final int AUTH_TYPE_NOT_RECOGNISED = 131;
    public static final int AUTH_SECURITY_MANAGER_NOT_SET = 132;
    public static final int AUTH_SET_TO_X_BUT_NO_CONTEXT = 133;
    public static final int AUTH_DENIED_ON_ENDPOINT_X = 134;
    public static final int AUTH_FAILED_FOR_USER_X = 135;
    public static final int AUTH_ENDPOINT_TYPE_FOR_FILTER_MUST_BE_X_BUT_IS_X = 136;
    public static final int AUTH_NO_PROVIDER_REGISTERED_FOR = 137;
    public static final int AUTH_REALM_MUST_SET_ON_FILTER = 138;
    public static final int FAILED_TO_PARSE_CONFIG_RESOURCE_X = 139;
    public static final int TX_MANAGER_ALREADY_SET = 140;
    public static final int COULD_NOT_RECOVER_CONTIANER_CONFIG = 141;
    public static final int ONLY_SINGLE_RESOURCE_CAN_BE_SPECIFIED = 142;
    public static final int ONLY_CUSTOM_EVENTS_CAN_BE_FIRED = 143;
    public static final int FAILED_TO_CREATE_MANAGER_INSTANCE_X = 144;
    public static final int FAILED_TO_CLONE_X = 145;
    public static final int EXCEPTION_ON_CONNECTOR_X_NO_EXCEPTION_LISTENER = 146;
    public static final int UNIQUE_ID_NOT_SUPPORTED_BY_ADAPTER_X = 147;
    public static final int FAILED_TO_PERSIST_EVENT_X = 148;
    public static final int FILE_X_DOES_NTO_EXIST = 149;
    public static final int SERVER_EVENT_MANAGER_NOT_ENABLED = 150;
    public static final int FAILED_TO_SCHEDULE_WORK = 151;
    public static final int AUTH_NO_CREDENTIALS = 152;
    public static final int X_IS_DISPOSED = 153;
    public static final int X_IS_INVALID = 154;
    public static final int CONTAINER_X_ALREADY_REGISTERED = 155;
    public static final int CONNECTOR_WITH_PROTOCOL_X_NOT_REGISTERED = 156;
    public static final int X_IS_NOT_SUPPORTED_TYPE_X_IT_IS_TYPE_X = 157;
    public static final int PROPERTY_TEMPLATE_MALFORMED_X = 158;
    public static final int MANAGER_IS_ALREADY_CONFIGURED = 159;

}
