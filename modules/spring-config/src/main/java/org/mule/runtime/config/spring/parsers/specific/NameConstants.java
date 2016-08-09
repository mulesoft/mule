/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import javax.xml.namespace.QName;

/**
 * Utils to share name constants between definition parser
 *
 * @since 4.0
 */
public class NameConstants {

  public static final String MULE_NAMESPACE = "http://www.mulesoft.org/schema/mule/core";
  public static final String MULE_PREFIX = "mule";
  public static final String MULE_EXTENSION_NAMESPACE = "http://www.mulesoft.org/schema/mule/extension";
  public static final String MULE_EXTENSION_PREFIX = "extension";

  public static final QName MULE_ABSTRACT_MESSAGE_SOURCE_TYPE =
      new QName(MULE_NAMESPACE, "abstractMessageSourceType", MULE_PREFIX);
  public static final QName MULE_EXTENSION_CONNECTION_PROVIDER_TYPE =
      new QName(MULE_EXTENSION_NAMESPACE, "abstractConnectionProviderType", MULE_EXTENSION_PREFIX);
}
