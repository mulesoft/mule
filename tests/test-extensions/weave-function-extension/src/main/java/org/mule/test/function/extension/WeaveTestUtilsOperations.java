/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.function.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;

public class WeaveTestUtilsOperations {

  @OutputResolver(output = AnyTypeResolver.class)
  @MediaType(value = ANY, strict = false)
  public Object evaluate(@Content Object payload) {
    return payload;
  }
}
