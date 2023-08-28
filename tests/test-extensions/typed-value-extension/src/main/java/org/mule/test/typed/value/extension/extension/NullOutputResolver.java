/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.typed.value.extension.extension;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

/**
 * Dummy resolver for operations that returns {@link Object}
 */
public class NullOutputResolver implements OutputTypeResolver {

  @Override
  public String getCategoryName() {
    return "typed-value";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Object key) throws MetadataResolvingException, ConnectionException {
    return null;
  }
}
