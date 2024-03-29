/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
