/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.metadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

/**
 * An {@link OutputTypeResolver} for JMS operations
 *
 * @since 4.0
 */
public class JmsOutputResolver implements OutputTypeResolver {

  @Override
  public String getCategoryName() {
    return "JMSMetadata";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Object key)
      throws MetadataResolvingException, ConnectionException {
    return context.getTypeBuilder().anyType().build();
  }
}
