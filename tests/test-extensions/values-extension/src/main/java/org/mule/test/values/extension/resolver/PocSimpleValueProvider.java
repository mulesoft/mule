/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import java.io.InputStream;
import java.util.Set;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;

public class PocSimpleValueProvider implements ValueProvider {

  @Parameter
  private InputStream complexParam;

  @Override
  public Set<Value> resolve() {
    return ValueBuilder.getValuesFor(IOUtils.toString(complexParam));
  }

  @Override
  public String getId() {
    return "Poc Simple value provider";
  }
}
