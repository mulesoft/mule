/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.internal.dsl;

import static java.lang.String.format;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.DEFAULT_NAMESPACE_URI_MASK;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_CONTEXT_ELEMENT_IDENTIFIER;

import javax.xml.namespace.QName;

public final class SchemaConstants {

  public static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
  public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
  public static final String SPRING_FRAMEWORK_NAMESPACE = "http://www.springframework.org/schema/beans";
  public static final String SPRING_FRAMEWORK_SCHEMA_LOCATION =
      "http://www.springframework.org/schema/beans/spring-beans-3.0.xsd";
  public static final String MULE_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/core/current/mule.xsd";
  public static final String EE_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/ee/core/current/mule-ee.xsd";

  public static final String MULE_TLS_NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, "tls");
  public static final String MULE_TLS_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/tls/current/mule-tls.xsd";

  public static final QName MULE_ABSTRACT_MESSAGE_SOURCE_TYPE =
      new QName(CORE_NAMESPACE, "abstractMessageSourceType", CORE_PREFIX);
  public static final QName MULE_ABSTRACT_SHARED_EXTENSION =
      new QName(CORE_NAMESPACE, "abstract-shared-extension", CORE_PREFIX);
  public static final QName MULE_PROPERTY_PLACEHOLDER_TYPE =
      new QName(CORE_NAMESPACE, "propertyPlaceholderType", CORE_PREFIX);
  public static final QName MULE_CONNECTION_PROVIDER_ELEMENT =
      new QName(CORE_NAMESPACE, "abstractConnectionProvider", CORE_PREFIX);
  public static final QName MULE_CONNECTION_PROVIDER_TYPE =
      new QName(CORE_NAMESPACE, "abstractConnectionProviderType", CORE_PREFIX);
  public static final QName MULE_EXPIRATION_POLICY_ELEMENT =
      new QName(CORE_NAMESPACE, "expiration-policy", CORE_PREFIX);
  public static final QName MULE_OPERATION_TRANSACTIONAL_ACTION_TYPE =
      new QName(CORE_NAMESPACE, "operationTransactionalActionType", CORE_PREFIX);
  public static final QName MULE_TRANSACTION_TYPE =
      new QName(CORE_NAMESPACE, "transactionType", CORE_PREFIX);
  public static final QName MULE_ABSTRACT_EXTENSION_TYPE =
      new QName(CORE_NAMESPACE, "abstractExtensionType", CORE_PREFIX);
  public static final QName MULE_ABSTRACT_OPERATOR =
      new QName(CORE_NAMESPACE, "abstract-operator", CORE_PREFIX);
  public static final QName MULE_ABSTRACT_OPERATOR_TYPE =
      new QName(CORE_NAMESPACE, "abstractOperatorType", CORE_PREFIX);
  public static final QName MULE_ABSTRACT_MESSAGE_SOURCE =
      new QName(CORE_NAMESPACE, "abstract-message-source", CORE_PREFIX);
  public static final QName MULE_ABSTRACT_VALIDATOR =
      new QName(CORE_NAMESPACE, "abstract-validator", CORE_PREFIX);
  public static final QName MULE_MESSAGE_PROCESSOR_TYPE =
      new QName(CORE_NAMESPACE, "messageProcessorOrMixedContentMessageProcessor", CORE_PREFIX);
  public static final QName TLS_CONTEXT_TYPE = new QName(MULE_TLS_NAMESPACE, TLS_CONTEXT_ELEMENT_IDENTIFIER, "tls");

  // TYPES
  public static final QName SUBSTITUTABLE_INT = new QName(CORE_NAMESPACE, "substitutableInt", CORE_PREFIX);
  public static final QName SUBSTITUTABLE_LONG = new QName(CORE_NAMESPACE, "substitutableLong", CORE_PREFIX);
  public static final QName SUBSTITUTABLE_BOOLEAN =
      new QName(CORE_NAMESPACE, "substitutableBoolean", CORE_PREFIX);
  public static final QName SUBSTITUTABLE_DECIMAL =
      new QName(CORE_NAMESPACE, "substitutableDecimal", CORE_PREFIX);
  public static final QName SUBSTITUTABLE_DATE_TIME =
      new QName(CORE_NAMESPACE, "substitutableDateTime", CORE_PREFIX);
  public static final QName SUBSTITUTABLE_NAME = new QName(CORE_NAMESPACE, "substitutableName", CORE_PREFIX);
  public static final QName SUBSTITUTABLE_MAP = new QName(CORE_NAMESPACE, "mapType", CORE_PREFIX);
  public static final QName STRING = new QName(XSD_NAMESPACE, "string", "xs");
  public static final QName EXPRESSION_STRING = new QName(CORE_NAMESPACE, "expressionString", CORE_PREFIX);
  public static final QName EXPRESSION_LONG = new QName(CORE_NAMESPACE, "expressionLong", CORE_PREFIX);
  public static final QName EXPRESSION_BOOLEAN = new QName(CORE_NAMESPACE, "expressionBoolean", CORE_PREFIX);
  public static final QName EXPRESSION_INTEGER = new QName(CORE_NAMESPACE, "expressionInt", CORE_PREFIX);
  public static final QName EXPRESSION_DOUBLE = new QName(CORE_NAMESPACE, "expressionDouble", CORE_PREFIX);
  public static final QName EXPRESSION_DECIMAL = new QName(CORE_NAMESPACE, "expressionDecimal", CORE_PREFIX);
  public static final QName EXPRESSION_LIST = new QName(CORE_NAMESPACE, "expressionList", CORE_PREFIX);
  public static final QName EXPRESSION_MAP = new QName(CORE_NAMESPACE, "expressionMap", CORE_PREFIX);
  public static final QName EXPRESSION_DATE_TIME = new QName(CORE_NAMESPACE, "expressionDateTime", CORE_PREFIX);

  // ATTRIBUTES
  public static final String USE_REQUIRED = "required";
  public static final String USE_OPTIONAL = "optional";
  public static final String CONFIG_ATTRIBUTE_DESCRIPTION = "Specify which configuration to use for this invocation.";
  public static final String ENUM_TYPE_SUFFIX = "EnumType";
  public static final String TYPE_SUFFIX = "Type";
  public static final String UNBOUNDED = "unbounded";
  public static final String MAX_ONE = "1";
  public static final String CURRENT_VERSION = "current";

  private SchemaConstants() {}
}
