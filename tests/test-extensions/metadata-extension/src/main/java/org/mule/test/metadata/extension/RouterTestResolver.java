/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;

public class RouterTestResolver implements OutputTypeResolver<String> {

  @Override
  public String getCategoryName() {
    return "router";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, String key) throws MetadataResolvingException, ConnectionException {
    if (!context.getInnerRoutesOutputType().containsKey("metaroute")) {
      throw new MetadataResolvingException("Invalid Chain output.", INVALID_CONFIGURATION);
    }
    return context.getInnerRoutesOutputType().get("metaroute").get();
  }
}
