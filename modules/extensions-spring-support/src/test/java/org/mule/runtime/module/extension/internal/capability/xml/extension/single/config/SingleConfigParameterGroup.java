/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.single.config;

import org.mule.runtime.extension.api.annotation.param.Parameter;

public class SingleConfigParameterGroup {


  /**
   * First Description
   */
  @Parameter
  private String first;

  /**
   * Second Description
   */
  @Parameter
  private String second;
}
