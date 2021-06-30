/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import static org.mule.sdk.api.values.ValueBuilder.getValuesFor;

import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.sdk.api.values.ValueResolvingException;

import java.io.InputStream;
import java.util.Set;

public class WithFourStreamsActingParametersValueProvider implements ValueProvider {

  @Parameter
  private InputStream firstStream;

  @Parameter
  private InputStream secondStream;

  @Parameter
  private InputStream thirdStream;

  @Parameter
  private InputStream fourthStream;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return getValuesFor(
                        IOUtils.toString(firstStream),
                        IOUtils.toString(secondStream),
                        IOUtils.toString(thirdStream),
                        IOUtils.toString(fourthStream));
  }

  @Override
  public String getId() {
    return getClass().getName();
  }
}
