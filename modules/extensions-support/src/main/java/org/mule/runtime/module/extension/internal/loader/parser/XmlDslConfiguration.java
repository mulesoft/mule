/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
