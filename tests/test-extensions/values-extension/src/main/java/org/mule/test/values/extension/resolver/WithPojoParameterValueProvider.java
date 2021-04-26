/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import static org.mule.sdk.api.values.ValueBuilder.getValuesFor;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.sdk.api.values.ValueResolvingException;
import org.mule.test.values.extension.MyPojo;

import java.util.Set;

public class WithPojoParameterValueProvider implements ValueProvider {

  @Parameter
  private MyPojo requiredValue;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return getValuesFor(requiredValue.toString());
  }

  @Override
  public String getId() {
    return getClass().getName();
  }

}
