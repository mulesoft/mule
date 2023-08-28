/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.injected;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Extension for testing purposes
 */
@Extension(name = "Hello")
@Operations({InjectedHelloOperation.class})
public class InjectedHelloExtension {

  @Parameter
  private String message;

  @Inject
  @Named("plugin.echotest")
  private Object echoTest;


  public String getMessage() {
    return message + echoTest.toString();
  }
}
