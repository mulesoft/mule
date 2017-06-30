/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import static org.mule.runtime.api.values.ValueBuilder.getValuesFor;
import org.mule.runtime.api.values.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValuesProvider;

import java.util.List;
import java.util.Set;

public class WithRequiredParametersValuesProvider implements ValuesProvider {

  @Parameter
  String requiredString;

  @Parameter
  int requiredInteger;

  @Parameter
  boolean requiredBoolean;

  @Parameter
  List<String> strings;

  @Override
  public Set<Value> resolve() {
    return getValuesFor("requiredString:" + requiredString, "requiredInteger:" + requiredInteger,
                        "requiredBoolean:" + requiredBoolean, "strings:" + strings);
  }
}
