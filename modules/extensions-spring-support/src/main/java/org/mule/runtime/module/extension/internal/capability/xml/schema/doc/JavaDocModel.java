/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
