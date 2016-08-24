/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.util;

import static org.junit.Assert.fail;

import java.util.function.Function;

import org.mule.runtime.api.message.Error;

public class TestFunctions {

  public static Function<Error, Void> failIfErrorFunction() {
    return (error -> {
      fail("error expected");
      return null;
    });
  }

}
