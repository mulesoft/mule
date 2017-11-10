/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import static java.util.Collections.singleton;
import static org.mule.runtime.extension.api.values.ValueBuilder.newValue;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.test.values.extension.source.SourceMustNotStart;

import java.util.Set;

public class TrueFalseValueProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return singleton(newValue(SourceMustNotStart.isStarted ? "TRUE" : "FALSE").build());
  }
}
