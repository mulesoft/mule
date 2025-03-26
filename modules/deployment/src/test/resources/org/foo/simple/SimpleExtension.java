/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.simple;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.service.test.api.FooService;

import jakarta.inject.Inject;

/**
 * Extension for testing purposes
 */
@Extension(name = "Simple")
@Operations({SimpleOperation.class})
public class SimpleExtension {

  public SimpleExtension() {}

  public String getMessage() {
    return "Simplicity is better!!";
  }
}
