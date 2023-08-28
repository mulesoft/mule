/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.simple;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.service.test.api.FooService;

import javax.inject.Inject;

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
