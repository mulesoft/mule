/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;

/**
 * {@link InputTypeResolver} implementation to resolve metadata for the message headers of a particular operation.
 *
 * @since 4.0
 */
public class SoapHeadersTypeResolver extends AbstractSoapOperationMetadataResolver implements InputTypeResolver<String> {

  @Override
  public String getResolverName() {
    return "SoapHeadersTypeResolver";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    return getSoapMetadata(context, operationName).getHeadersType();
  }
}
