/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.capability.xml.schema.model;

import static org.mule.runtime.config.spring.parsers.specific.NameConstants.MULE_EXTENSION_NAMESPACE;
import static org.mule.runtime.config.spring.parsers.specific.NameConstants.MULE_EXTENSION_PREFIX;
import static org.mule.runtime.config.spring.parsers.specific.NameConstants.MULE_NAMESPACE;
import static org.mule.runtime.config.spring.parsers.specific.NameConstants.MULE_PREFIX;

import org.mule.runtime.module.extension.internal.ExtensionProperties;

import javax.xml.namespace.QName;

public final class SchemaConstants
{

    public static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
    public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    public static final String SPRING_FRAMEWORK_NAMESPACE = "http://www.springframework.org/schema/beans";
    public static final String SPRING_FRAMEWORK_SCHEMA_LOCATION = "http://www.springframework.org/schema/beans/spring-beans-3.0.xsd";
    public static final String MULE_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/core/current/mule.xsd";
    public static final String MULE_EXTENSION_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/extension/current/mule-extension.xsd";
    public static final String MULE_TLS_NAMESPACE = "http://www.mulesoft.org/schema/mule/tls";
    public static final String MULE_TLS_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/tls/current/mule-tls.xsd";
    public static final String OPERATION_SUBSTITUTION_GROUP_SUFFIX = "-OperationGroup";
    public static final String GROUP_SUFFIX = "-group";
    public static final String DEFAULT_SCHEMA_LOCATION_MASK = "http://www.mulesoft.org/schema/mule/%s";

    public static final QName MULE_ABSTRACT_EXTENSION = new QName(MULE_NAMESPACE, "abstract-extension", MULE_PREFIX);
    public static final QName MULE_PROPERTY_PLACEHOLDER_TYPE = new QName(MULE_NAMESPACE, "propertyPlaceholderType", MULE_PREFIX);
    public static final QName MULE_ABSTRACT_POOLING_PROFILE_TYPE = new QName(MULE_NAMESPACE, "abstractPoolingProfileType", MULE_PREFIX);
    public static final QName MULE_POOLING_PROFILE_TYPE = new QName(MULE_NAMESPACE, "pooling-profile", MULE_PREFIX);
    public static final QName MULE_ABSTRACT_THREADING_PROFILE_TYPE = new QName(MULE_NAMESPACE, "abstractServiceThreadingProfileType", MULE_PREFIX);
    public static final QName MULE_ABSTRACT_THREADING_PROFILE = new QName(MULE_NAMESPACE, "abstract-service-threading-profile", MULE_PREFIX);
    public static final QName MULE_EXTENSION_CONNECTION_PROVIDER_ELEMENT = new QName(MULE_EXTENSION_NAMESPACE, "abstractConnectionProvider", MULE_EXTENSION_PREFIX);
    public static final QName MULE_EXTENSION_CONNECTION_PROVIDER_TYPE = new QName(MULE_EXTENSION_NAMESPACE, "abstractConnectionProviderType", MULE_EXTENSION_PREFIX);
    public static final QName MULE_EXTENSION_DYNAMIC_CONFIG_POLICY_ELEMENT = new QName(MULE_EXTENSION_NAMESPACE, "dynamic-config-policy", MULE_EXTENSION_PREFIX);
    public static final QName MULE_ABSTRACT_EXTENSION_TYPE = new QName(MULE_NAMESPACE, "abstractExtensionType", MULE_PREFIX);
    public static final QName MULE_ABSTRACT_MESSAGE_PROCESSOR = new QName(MULE_NAMESPACE, "abstract-message-processor", MULE_PREFIX);
    public static final QName MULE_ABSTRACT_MESSAGE_PROCESSOR_TYPE = new QName(MULE_NAMESPACE, "abstractMessageProcessorType", MULE_PREFIX);
    public static final QName MULE_ABSTRACT_MESSAGE_SOURCE = new QName(MULE_NAMESPACE, "abstract-message-source", MULE_PREFIX);
    public static final QName MULE_ABSTRACT_RECONNECTION_STRATEGY = new QName(MULE_NAMESPACE, "abstract-reconnection-strategy", MULE_PREFIX);
    public static final QName MULE_ABSTRACT_RECONNECTION_STRATEGY_TYPE = new QName(MULE_NAMESPACE, "abstractReconnectionStrategyType", MULE_PREFIX);
    public static final QName MULE_MESSAGE_PROCESSOR_TYPE = new QName(MULE_NAMESPACE, "messageProcessorOrMixedContentMessageProcessor", MULE_PREFIX);
    public static final QName TLS_CONTEXT_TYPE = new QName(MULE_TLS_NAMESPACE, "context", "tls");

    //TYPES
    public static final QName SUBSTITUTABLE_INT = new QName(MULE_NAMESPACE, "substitutableInt", MULE_PREFIX);
    public static final QName SUBSTITUTABLE_LONG = new QName(MULE_NAMESPACE, "substitutableLong", MULE_PREFIX);
    public static final QName SUBSTITUTABLE_BOOLEAN = new QName(MULE_NAMESPACE, "substitutableBoolean", MULE_PREFIX);
    public static final QName SUBSTITUTABLE_DECIMAL = new QName(MULE_NAMESPACE, "substitutableDecimal", MULE_PREFIX);
    public static final QName SUBSTITUTABLE_DATE_TIME = new QName(MULE_NAMESPACE, "substitutableDateTime", MULE_PREFIX);
    public static final QName SUBSTITUTABLE_NAME = new QName(MULE_NAMESPACE, "substitutableName", MULE_PREFIX);
    public static final QName SUBSTITUTABLE_MAP = new QName(MULE_NAMESPACE, "mapType", MULE_PREFIX);
    public static final QName STRING = new QName(XSD_NAMESPACE, "string", "xs");
    public static final QName EXPRESSION_STRING = new QName(MULE_NAMESPACE, "expressionString", MULE_PREFIX);
    public static final QName EXPRESSION_LONG = new QName(MULE_NAMESPACE, "expressionLong", MULE_PREFIX);
    public static final QName EXPRESSION_BOOLEAN = new QName(MULE_NAMESPACE, "expressionBoolean", MULE_PREFIX);
    public static final QName EXPRESSION_INTEGER = new QName(MULE_NAMESPACE, "expressionInt", MULE_PREFIX);
    public static final QName EXPRESSION_DOUBLE = new QName(MULE_NAMESPACE, "expressionDouble", MULE_PREFIX);
    public static final QName EXPRESSION_DECIMAL = new QName(MULE_NAMESPACE, "expressionDecimal", MULE_PREFIX);
    public static final QName EXPRESSION_OBJECT = new QName(MULE_NAMESPACE, "expressionObject", MULE_PREFIX);
    public static final QName EXPRESSION_LIST = new QName(MULE_NAMESPACE, "expressionList", MULE_PREFIX);
    public static final QName EXPRESSION_MAP = new QName(MULE_NAMESPACE, "expressionMap", MULE_PREFIX);
    public static final QName EXPRESSION_DATE_TIME = new QName(MULE_NAMESPACE, "expressionDateTime", MULE_PREFIX);

    // ATTRIBUTES
    public static final String USE_REQUIRED = "required";
    public static final String USE_OPTIONAL = "optional";
    public static final String CONFIG_ATTRIBUTE = "config-ref";
    public static final String CONFIG_ATTRIBUTE_DESCRIPTION = "Specify which configuration to use for this invocation.";
    public static final String ATTRIBUTE_NAME_VALUE = "value";
    public static final String ATTRIBUTE_NAME_KEY = "key";
    public static final String ENUM_TYPE_SUFFIX = "EnumType";
    public static final String TYPE_SUFFIX = "Type";
    public static final String UNBOUNDED = "unbounded";
    public static final String ATTRIBUTE_NAME_NAME = "name";
    public static final String XSD_EXTENSION = ".xsd";
    public static final String DISABLE_VALIDATION = "disableValidation";
    public static final String CURRENT_VERSION = "current";

    /**
     * Human friendly description for {@link ExtensionProperties#TARGET_ATTRIBUTE}
     */
    public static final String TARGET_ATTRIBUTE_DESCRIPTION = "The name of a variable on which the operation's output will be placed";
}
