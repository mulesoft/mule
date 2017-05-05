/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import org.mule.extension.ws.api.SoapMessageBuilder;
import org.mule.metadata.api.model.NullType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.services.soap.api.client.metadata.SoapOperationMetadata;

/**
 * {@link InputTypeResolver} implementation to resolve metadata for an input message of a particular operation.
 *
 * @since 4.0
 */
abstract class AbstractSoapOperationMetadataResolver extends BaseWscResolver {

  /**
   * {@inheritDoc}
   * <p>
   * Creates a complex object that represents the {@link SoapMessageBuilder} that contains a body, a Set of headers an a set of
   * attachments. Any component can be represented as a {@link NullType} if there is no required data for the field.
   */
  public SoapOperationMetadata getSoapMetadata(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    // TODO MULE-12405: Use MetadataCache for the resolved SoapOperationMetadata
    return getMetadataResolver(context).getInputMetadata(operationName);
  }
}
