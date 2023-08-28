/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.soap.internal.metadata;

import static org.mule.runtime.extension.api.soap.metadata.SoapOutputTypeBuilder.buildOutputType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.extension.api.soap.WebServiceTypeKey;
import org.mule.runtime.soap.api.client.SoapClient;
import org.mule.runtime.soap.api.client.metadata.SoapOperationMetadata;

/**
 * Resolves the output metadata for the soap connect invoke operation
 *
 * @since 4.0
 */
public final class InvokeOutputTypeResolver extends BaseInvokeResolver implements OutputTypeResolver<WebServiceTypeKey> {

  /**
   * {@inheritDoc}
   */
  @Override
  public String getResolverName() {
    return "InvokeOutput";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataType getOutputType(MetadataContext context, WebServiceTypeKey key)
      throws MetadataResolvingException, ConnectionException {
    SoapClient client = getClient(context, key);
    SoapOperationMetadata metadata = client.getMetadataResolver().getOutputMetadata(key.getOperation());
    return buildOutputType(metadata.getBodyType(), metadata.getHeadersType(), metadata.getAttachmentsType(),
                           context.getTypeBuilder());
  }
}
