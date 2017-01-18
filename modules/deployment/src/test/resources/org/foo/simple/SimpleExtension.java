/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.hello;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.service.test.FooService;

import javax.inject.Inject;

@Extension(
    name = "Simple",
    description = "Extension for testing purposes")
@Operations({SimpleOperation.class})
public class SimpleExtension {

  public SimpleExtension() {}

  public String getMessage() {
    return "Simplicity is better!!";
  }
}
