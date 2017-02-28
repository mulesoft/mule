/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import org.mule.extension.http.internal.HttpMetadataKey;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * Groups parameters which control how the operation output is generated
 *
 * @since 4.0
 */
public final class OutputSettings {

  @Parameter
  @Optional(defaultValue = "ANY")
  @MetadataKeyId
  @Placement(tab = ADVANCED_TAB)
  private HttpMetadataKey outputType;

  public HttpMetadataKey getOutputType() {
    return outputType;
  }
}
