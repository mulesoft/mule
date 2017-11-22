/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExclusiveOptionals
public class VeganArguments {

  @Parameter
  @Optional
  private String argument1;

  @Parameter
  @Optional
  private String argument2;

  @Parameter
  @Optional
  private String argument3;

  public boolean hasAtLeastOneArgument() {
    return argument1 != null || argument2 != null || argument3 != null;
  }

  public String getArgument1() {
    return argument1;
  }

  public String getArgument2() {
    return argument2;
  }

  public String getArgument3() {
    return argument3;
  }

  @Override
  public String toString() {
    return Stream.of(argument1, argument2, argument3)
        .filter(a -> a != null)
        .collect(Collectors.joining(","));
  }
}
