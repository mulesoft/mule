/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.xml;

import static java.lang.String.format;
import static org.mule.runtime.extension.api.util.XmlModelUtils.DEFAULT_SCHEMA_LOCATION_MASK;
import static org.mule.runtime.extension.api.util.XmlModelUtils.MULE_NAMESPACE_SCHEMA_LOCATION;
import static org.mule.runtime.extension.api.util.XmlModelUtils.MULE_PREFIX;

import javax.xml.namespace.QName;

public final class SchemaConstants {

  public static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
  public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
  public static final String SPRING_FRAMEWORK_NAMESPACE = "http://www.springframework.org/schema/beans";
  public static final String SPRING_FRAMEWORK_SCHEMA_LOCATION =
      "http://www.springframework.org/schema/beans/spring-beans-3.0.xsd";
  public static final String MULE_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/core/current/mule.xsd";

  public static final String MULE_EXTENSION_NAMESPACE = format(DEFAULT_SCHEMA_LOCATION_MASK, "extension");
  public static final String MULE_EXTENSION_PREFIX = "extension";
  public static final String MULE_EXTENSION_SCHEMA_LOCATION =
      "http://www.mulesoft.org/schema/mule/extension/current/mule-extension.xsd";

  public static final String MULE_TLS_NAMESPACE = format(DEFAULT_SCHEMA_LOCATION_MASK, "tls");
  public static final String MULE_TLS_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/tls/current/mule-tls.xsd";
  public static final String OPERATION_SUBSTITUTION_GROUP_SUFFIX = "-OperationGroup";
  public static final String GROUP_SUFFIX = "-group";

  public static final QName MULE_ABSTRACT_MESSAGE_SOURCE_TYPE =
      new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "abstractMessageSourceType", MULE_PREFIX);
  public static final QName MULE_ABSTRACT_EXTENSION =
      new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "abstract-extension", MULE_PREFIX);
  public static final QName MULE_PROPERTY_PLACEHOLDER_TYPE =
      new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "propertyPlaceholderType", MULE_PREFIX);
  public static final QName MULE_EXTENSION_CONNECTION_PROVIDER_ELEMENT =
      new QName(MULE_EXTENSION_NAMESPACE, "abstractConnectionProvider", MULE_EXTENSION_PREFIX);
  public static final QName MULE_EXTENSION_CONNECTION_PROVIDER_TYPE =
      new QName(MULE_EXTENSION_NAMESPACE, "abstractConnectionProviderType", MULE_EXTENSION_PREFIX);
  public static final QName MULE_EXTENSION_DYNAMIC_CONFIG_POLICY_ELEMENT =
      new QName(MULE_EXTENSION_NAMESPACE, "dynamic-config-policy", MULE_EXTENSION_PREFIX);
  public static final QName MULE_EXTENSION_OPERATION_TRANSACTIONAL_ACTION_TYPE =
      new QName(MULE_EXTENSION_NAMESPACE, "operationTransactionalActionType", MULE_EXTENSION_PREFIX);
  public static final QName MULE_ABSTRACT_EXTENSION_TYPE =
      new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "abstractExtensionType", MULE_PREFIX);
  public static final QName MULE_ABSTRACT_OPERATOR =
      new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "abstract-operator", MULE_PREFIX);
  public static final QName MULE_ABSTRACT_OPERATOR_TYPE =
      new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "abstractOperatorType", MULE_PREFIX);
  public static final QName MULE_ABSTRACT_MESSAGE_SOURCE =
      new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "abstract-message-source", MULE_PREFIX);
  public static final QName MULE_MESSAGE_PROCESSOR_TYPE =
      new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "messageProcessorOrMixedContentMessageProcessor", MULE_PREFIX);
  public static final QName TLS_CONTEXT_TYPE = new QName(MULE_TLS_NAMESPACE, "context", "tls");

  // TYPES
  public static final QName SUBSTITUTABLE_INT = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "substitutableInt", MULE_PREFIX);
  public static final QName SUBSTITUTABLE_LONG = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "substitutableLong", MULE_PREFIX);
  public static final QName SUBSTITUTABLE_BOOLEAN =
      new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "substitutableBoolean", MULE_PREFIX);
  public static final QName SUBSTITUTABLE_DECIMAL =
      new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "substitutableDecimal", MULE_PREFIX);
  public static final QName SUBSTITUTABLE_DATE_TIME =
      new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "substitutableDateTime", MULE_PREFIX);
  public static final QName SUBSTITUTABLE_NAME = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "substitutableName", MULE_PREFIX);
  public static final QName SUBSTITUTABLE_MAP = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "mapType", MULE_PREFIX);
  public static final QName STRING = new QName(XSD_NAMESPACE, "string", "xs");
  public static final QName EXPRESSION_STRING = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "expressionString", MULE_PREFIX);
  public static final QName EXPRESSION_LONG = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "expressionLong", MULE_PREFIX);
  public static final QName EXPRESSION_BOOLEAN = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "expressionBoolean", MULE_PREFIX);
  public static final QName EXPRESSION_INTEGER = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "expressionInt", MULE_PREFIX);
  public static final QName EXPRESSION_DOUBLE = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "expressionDouble", MULE_PREFIX);
  public static final QName EXPRESSION_DECIMAL = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "expressionDecimal", MULE_PREFIX);
  public static final QName EXPRESSION_LIST = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "expressionList", MULE_PREFIX);
  public static final QName EXPRESSION_MAP = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "expressionMap", MULE_PREFIX);
  public static final QName EXPRESSION_DATE_TIME = new QName(MULE_NAMESPACE_SCHEMA_LOCATION, "expressionDateTime", MULE_PREFIX);

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
  public static final String MAX_ONE = "1";
  public static final String ATTRIBUTE_NAME_NAME = "name";
  public static final String CURRENT_VERSION = "current";

  private SchemaConstants() {}
}
