/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.bridge;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * Extension for testing purposes
 */
@Extension(name = "Bridge")
@Operations({JavaBridgeMethodOperation.class})
public class JavaBridgeMethodExtension implements GenericHello<String> {

  @Parameter
  private String message;

  public JavaBridgeMethodExtension() {}

  public String getMessage() {
    return message;
  }
}
