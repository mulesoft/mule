/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.parameter.resolver.extension.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

@MediaType(TEXT_PLAIN)
public class SomeSource extends Source<String, Object> {

  @Parameter
  public static ParameterResolver<String> someString;

  @Parameter
  public static Literal<String> literalString;

  @org.mule.sdk.api.annotation.param.Config
  public static ParameterResolverExtension extension;

  @Override
  public void onStart(SourceCallback<String, Object> sourceCallback) throws MuleException {}

  @Override
  public void onStop() {

  }
}
