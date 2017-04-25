/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import static org.mule.services.soap.api.client.metadata.SoapOutputTypeBuilder.ATTACHMENTS_FIELD;
import static org.mule.services.soap.api.client.metadata.SoapOutputTypeBuilder.BODY_FIELD;
import static org.mule.services.soap.api.client.metadata.SoapOutputTypeBuilder.HEADERS_FIELD;
import org.mule.extension.ws.api.SoapMessageBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
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
public class MessageBuilderResolver extends BaseWscResolver implements InputTypeResolver<String> {

  @Override
  public String getResolverName() {
    return "ConsumeInputResolver";
  }

  /**
   * {@inheritDoc}
   * <p>
   * Creates a complex object that represents the {@link SoapMessageBuilder} that contains a body, a Set of headers an a set of
   * attachments. Any component can be represented as a {@link NullType} if there is no required data for the field.
   */
  @Override
  public MetadataType getInputMetadata(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    SoapOperationMetadata metadata = getMetadataResolver(context).getInputMetadata(operationName);
    ObjectTypeBuilder object = context.getTypeBuilder().objectType();
    object.addField().key(HEADERS_FIELD).value(metadata.getHeadersType());
    object.addField().key(BODY_FIELD).value(metadata.getBodyType());
    object.addField().key(ATTACHMENTS_FIELD).value(metadata.getAttachmentsType());
    return object.build();
  }
}
