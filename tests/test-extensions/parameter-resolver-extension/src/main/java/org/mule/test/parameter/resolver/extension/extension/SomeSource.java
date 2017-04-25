/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.parameter.resolver.extension.extension;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

public class SomeSource extends Source<String, Attributes> {

  @Parameter
  public static ParameterResolver<String> someString;

  @Parameter
  public static Literal<String> literalString;

  @Config
  public static ParameterResolverExtension extension;

  @Override
  public void onStart(SourceCallback<String, Attributes> sourceCallback) throws MuleException {}

  @Override
  public void onStop() {

  }
}
