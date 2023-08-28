/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.extension.test.extension.reconnection.metadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

public class RetryPolicyOutputResolver implements OutputTypeResolver {

  @Override
  public MetadataType getOutputType(MetadataContext context, Object key) throws MetadataResolvingException, ConnectionException {
    return context.getTypeBuilder().objectType().build();
  }

  @Override
  public String getCategoryName() {
    return "retryPolicy";
  }
}
