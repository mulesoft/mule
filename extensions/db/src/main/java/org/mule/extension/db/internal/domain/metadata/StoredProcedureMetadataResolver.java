/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.metadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;

public class StoredProcedureMetadataResolver extends BaseDbMetadataResolver implements MetadataOutputResolver<String> {

  @Override
  public String getCategoryName() {
    return "StoredProcedureDbCategory";
  }

  @Override
  public MetadataType getOutputMetadata(MetadataContext context, String query)
      throws MetadataResolvingException, ConnectionException {

    typeBuilder = context.getTypeBuilder();
    return typeBuilder.dictionaryType()
        .ofKey(typeBuilder.stringType())
        .ofValue(typeBuilder.anyType())
        .build();
  }
}
