/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
