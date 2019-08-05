/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
