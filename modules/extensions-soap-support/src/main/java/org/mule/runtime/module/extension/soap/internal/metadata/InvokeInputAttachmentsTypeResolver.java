/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
 * Resolves the metadata for the input attachments for the soap connect invoke operation
 *
 * @since 4.0
 */
public final class InvokeInputAttachmentsTypeResolver extends BaseInvokeResolver implements InputTypeResolver<WebServiceTypeKey> {

  @Override
  public String getResolverName() {
    return "InvokeInputAttachments";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, WebServiceTypeKey key)
      throws MetadataResolvingException, ConnectionException {
    SoapClient client = getClient(context, key);
    SoapOperationMetadata metadata = client.getMetadataResolver().getInputMetadata(key.getOperation());
    return metadata.getAttachmentsType();
  }
}
