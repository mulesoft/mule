/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.model.response;

import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.metadata.extension.resolver.TestMetadataInputPersonResolver;

import java.io.InputStream;

public class SuccessResponse {

  @Parameter
  @TypeResolver(TestMetadataInputPersonResolver.class)
  private InputStream response;

  @Parameter
  private Integer code;

  public InputStream getResponse() {
    return response;
  }

  public Integer getCode() {
    return code;
  }
}
