/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.metadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;

public class DbBulkInputMetadataResolver extends DbInputMetadataResolver {

  @Override
  public String getCategoryName() {
    return "DbCategory";
  }

  @Override
  public String getResolverName() {
    return "BulkInput";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String query)
      throws MetadataResolvingException, ConnectionException {

    MetadataType queryInputMetadata = super.getInputMetadata(context, query);
    return (queryInputMetadata instanceof NullType)
        ? queryInputMetadata
        : typeBuilder.arrayType().of(queryInputMetadata).build();
  }

}
