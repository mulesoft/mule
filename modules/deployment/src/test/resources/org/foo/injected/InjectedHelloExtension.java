/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
