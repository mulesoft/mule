/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactionalxa;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.sdk.api.metadata.MetadataContext;
import org.mule.sdk.api.metadata.resolving.OutputTypeResolver;

public class TransactionalMetadataResolver implements OutputTypeResolver {

  @Override
  public String getCategoryName() {
    return "TransactionalResolver";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Object key) throws MetadataResolvingException, ConnectionException {
    return context.getTypeBuilder().anyType().build();
  }
}
