/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.metadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.services.soap.api.client.metadata.SoapOperationMetadata;

/**
 * Immutable {@link SoapOperationMetadata} implementation.
 *
 * @since 4.0
 */
class ImmutableSoapOperationMetadata implements SoapOperationMetadata {

  private final MetadataType body;
  private final MetadataType headers;
  private final MetadataType attachments;

  public ImmutableSoapOperationMetadata(MetadataType body, MetadataType headers, MetadataType attachments) {
    this.body = body;
    this.headers = headers;
    this.attachments = attachments;
  }

  @Override
  public MetadataType getBodyType() {
    return body;
  }

  @Override
  public MetadataType getHeadersType() {
    return headers;
  }

  @Override
  public MetadataType getAttachmentsType() {
    return attachments;
  }
}
