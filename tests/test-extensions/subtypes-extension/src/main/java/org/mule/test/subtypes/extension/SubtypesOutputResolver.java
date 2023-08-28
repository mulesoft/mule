/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

public class SubtypesOutputResolver implements OutputTypeResolver {

  @Override
  public MetadataType getOutputType(MetadataContext context, Object key) throws MetadataResolvingException, ConnectionException {
    return null;
  }

  @Override
  public String getCategoryName() {
    return "SubtypesOutputResolver";
  }
}
