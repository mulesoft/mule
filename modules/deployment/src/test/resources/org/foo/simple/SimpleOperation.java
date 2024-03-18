/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.simple;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Map;

public class SimpleOperation {

  public SimpleOperation() {}

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String printMessage(@Config SimpleExtension config) {
    System.out.println("Simple extension says: " + config.getMessage());
    return config.getMessage();
  }

  @MediaType(TEXT_PLAIN)
  public String fail() throws Exception {
       throw new Exception("Simple Error");
  }

}
