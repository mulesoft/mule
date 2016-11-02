/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import static java.lang.String.format;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import org.mule.extension.ws.internal.introspection.TypeIntrospecterDelegate;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;

import javax.wsdl.Part;

/**
 * Base class for metadata resolvers that resolve dynamic metadata of XML node elements.
 *
 * @since 4.0
 */
public abstract class NodeElementResolver extends BaseWscResolver {

  final TypeIntrospecterDelegate delegate;

  protected NodeElementResolver(TypeIntrospecterDelegate delegate) {
    this.delegate = delegate;
  }

  protected abstract MetadataType getMetadata(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException;

  MetadataType buildPartMetadataType(TypeLoader loader, Part part) throws MetadataResolvingException {
    if (part.getElementName() != null) {
      String partName = part.getElementName().toString();
      return loader.load(partName)
          .orElseThrow(() -> new MetadataResolvingException(format("Could not load part element name [%s]", partName), UNKNOWN));
    }
    throw new MetadataResolvingException("Trying to resolve metadata for a nameless part, probably the provided WSDL is invalid",
                                         INVALID_CONFIGURATION);
  }
}
