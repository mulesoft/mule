/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata.body;

import org.mule.extension.ws.internal.metadata.OutputTypeResolverDelegate;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

/**
 * Output metadata resolver implementation of {@link BodyElementResolver}.
 *
 * @since 4.0
 */
public final class OutputBodyResolver extends BodyElementResolver implements OutputTypeResolver<String> {

  public OutputBodyResolver() {
    super(new OutputTypeResolverDelegate());
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    return getMetadata(context, operationName);
  }
}
