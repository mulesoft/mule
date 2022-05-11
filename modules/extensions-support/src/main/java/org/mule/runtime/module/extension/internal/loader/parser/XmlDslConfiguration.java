/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.parser;

/**
 * Contains the configuration of the schema attributes
 */
public class XmlDslConfiguration {

  String prefix;
  String namespace;

  public XmlDslConfiguration(String prefix, String namespace) {
    this.prefix = prefix;
    this.namespace = namespace;
  }

  public String getPrefix() {
    return prefix;
  }

  public String getNamespace() {
    return namespace;
  }
}
