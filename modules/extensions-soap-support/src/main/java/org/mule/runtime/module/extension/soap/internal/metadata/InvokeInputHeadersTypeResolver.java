/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.metadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.extension.api.soap.WebServiceTypeKey;
import org.mule.runtime.soap.api.client.SoapClient;
import org.mule.runtime.soap.api.client.metadata.SoapOperationMetadata;

/**
 * Resolves the metadata for the input headers for the soap connect invoke operation.
 *
 * @since 4.0
 */
public final class InvokeInputHeadersTypeResolver extends BaseInvokeResolver implements InputTypeResolver<WebServiceTypeKey> {

  /**
   * {@inheritDoc}
   */
  @Override
  public String getResolverName() {
    return "InvokeInputHeaders";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataType getInputMetadata(MetadataContext context, WebServiceTypeKey key)
      throws MetadataResolvingException, ConnectionException {
    SoapClient client = getClient(context, key);
    SoapOperationMetadata metadata = client.getMetadataResolver().getInputMetadata(key.getOperation());
    return metadata.getHeadersType();
  }
}
