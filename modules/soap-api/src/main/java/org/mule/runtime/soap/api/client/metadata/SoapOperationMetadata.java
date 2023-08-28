/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.soap.api.client.metadata;

import org.mule.metadata.api.model.MetadataType;

/**
 * Represents the metadata of a SOAP Operation, carries a body type, a headers type and an attachments type.
 *
 * @since 4.0
 */
public interface SoapOperationMetadata {

  /**
   * @return the {@link MetadataType} of the body.
   */
  MetadataType getBodyType();

  /**
   * @return the {@link MetadataType} of the headers.
   */
  MetadataType getHeadersType();

  /**
   * @return the {@link MetadataType} of the attachments.
   */
  MetadataType getAttachmentsType();
}
