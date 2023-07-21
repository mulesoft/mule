/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.test.values.extension.resolver.WithRequiredParametersValueProvider;

import java.util.List;

@MediaType(TEXT_PLAIN)
public class SourceWithValuesWithRequiredParameters extends AbstractSource {

  @OfValues(WithRequiredParametersValueProvider.class)
  @Parameter
  String channels;

  @Parameter
  String requiredString;

  @Parameter
  boolean requiredBoolean;

  @Parameter
  int requiredInteger;

  @Parameter
  List<String> strings;
}
