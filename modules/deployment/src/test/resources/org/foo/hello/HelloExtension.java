/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.hello;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.service.test.api.FooService;

import javax.inject.Inject;

/**
 * Extension for testing purposes
 */
@Extension(name = "Hello")
@Operations({HelloOperation.class})
public class HelloExtension {

  @Parameter
  private String message;
  @Inject
  private FooService fooService;

  @Inject
  private HelloRegistryBean registryBean;

  public HelloExtension() {}

  public String getMessage() {
    if (registryBean == null) {
      throw new NullPointerException("registryBean is null!");
    }
    return this.fooService.doFoo(this.message);
  }
}
