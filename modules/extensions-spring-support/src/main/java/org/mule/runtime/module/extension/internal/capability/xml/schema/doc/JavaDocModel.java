/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.doc;

import java.util.Map;

/**
 * Model representing a JavaDoc
 *
 * @since 4.0
 */
public class JavaDocModel {

  private String body;
  private Map<String, String> parameters;

  public JavaDocModel(String body, Map<String, String> parameters) {
    this.body = body;
    this.parameters = parameters;
  }

  public String getBody() {
    return body;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }
}
