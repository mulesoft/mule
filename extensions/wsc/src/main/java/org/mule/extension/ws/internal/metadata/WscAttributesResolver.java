/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import org.mule.extension.ws.internal.ConsumeOperation;
import org.mule.extension.ws.internal.metadata.header.OutputHeadersResolver;
import org.mule.metadata.api.builder.DictionaryTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;

/**
 * {@link AttributesTypeResolver} implementation for the {@link ConsumeOperation}.
 *
 * Builds an object type with a generic dictionary where all the protocol headers are located and a more specific
 * soap headers element with the specific outbound SOAP headers returned by the operation.
 *
 * @since 4.0
 */
public final class WscAttributesResolver extends BaseWscResolver implements AttributesTypeResolver<String> {

  private static final OutputHeadersResolver outputHeadersResolver = new OutputHeadersResolver();

  @Override
  public MetadataType getAttributesType(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    MetadataType soapHeadersType = outputHeadersResolver.getOutputType(context, operationName);
    ObjectTypeBuilder<?> attributes = context.getTypeBuilder().objectType();
    attributes.addField().key("soapHeaders").value(soapHeadersType);
    DictionaryTypeBuilder<?> protocolHeaders = attributes.addField().key("protocolHeaders").value().dictionaryType();
    protocolHeaders.ofKey().stringType();
    protocolHeaders.ofValue().stringType();

    return attributes.build();
  }
}
