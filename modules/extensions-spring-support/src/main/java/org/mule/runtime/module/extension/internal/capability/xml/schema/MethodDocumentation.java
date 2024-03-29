/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;

import java.util.Map;

/**
 * A summary object to group a method's javadoc combined with the ones of its parameters.
 *
 * @since 3.7.0
 */
public final class MethodDocumentation {

  private final String summary;
  private final Map<String, String> parameters;

  MethodDocumentation(String summary, Map<String, String> parameters) {
    this.summary = summary;
    this.parameters = parameters;
  }

  /**
   * the method's javadoc block without tags
   */
  public String getSummary() {
    return summary;
  }

  /**
   * A non {@code null} {@link java.util.Map} with each parameter's comments
   */
  public Map<String, String> getParameters() {
    return parameters;
  }
}
