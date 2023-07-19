/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.multiple.config;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class TestDocumentedParameterGroup {

  /**
   * Group parameter 1
   */
  @Parameter
  private String value1;

  /**
   * Group parameter 2
   */
  @Parameter
  private String value2;

  /**
   * Param with alias
   */
  @Parameter
  @Alias("alias-param")
  private String aliasedParam;
}
