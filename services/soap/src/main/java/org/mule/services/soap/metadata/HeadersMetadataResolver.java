/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.metadata;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.builder.ObjectFieldTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.services.soap.introspection.WsdlIntrospecter;

import java.util.List;

import javax.wsdl.BindingOperation;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.wsdl.extensions.ElementExtensible;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap12.SOAP12Header;

/**
 * Handles the dynamic {@link MetadataType} resolution for the SOAP Headers of a web service operation.
 *
 * @since 4.0
 */
final class HeadersMetadataResolver extends NodeMetadataResolver {

  HeadersMetadataResolver(WsdlIntrospecter introspecter, TypeLoader loader) {
    super(introspecter, loader);
  }

  @Override
  public MetadataType getMetadata(String operation, TypeIntrospecterDelegate delegate) throws MetadataResolvingException {
    BindingOperation bindingOperation = introspecter.getBindingOperation(operation);
    ElementExtensible bindingType = delegate.getBindingType(bindingOperation);
    List<SoapHeaderAdapter> headers = getHeaderParts(bindingType);
    if (!headers.isEmpty()) {
      Message message = delegate.getMessage(introspecter.getOperation(operation));
      return buildHeaderType(headers, message);
    }
    return nullType;
  }

  private MetadataType buildHeaderType(List<SoapHeaderAdapter> headers, Message message)
      throws MetadataResolvingException {
    ObjectTypeBuilder objectType = typeBuilder.objectType();
    for (SoapHeaderAdapter header : headers) {
      ObjectFieldTypeBuilder field = objectType.addField();
      String headerPart = header.getPart();
      Part part = message.getPart(headerPart);
      if (part != null) {
        field.key(headerPart).value(buildPartMetadataType(part));
      } else {
        Message headerMessage = introspecter.getMessage(header.getMessage());
        field.key(headerPart).value(buildPartMetadataType(headerMessage.getPart(headerPart)));
      }
    }
    return objectType.build();
  }

  private List<SoapHeaderAdapter> getHeaderParts(ElementExtensible bindingType) {
    List extensible = bindingType.getExtensibilityElements();
    if (extensible != null) {
      return (List<SoapHeaderAdapter>) extensible.stream()
          .filter(e -> e instanceof SOAPHeader || e instanceof SOAP12Header)
          .map(e -> e instanceof SOAPHeader ? new SoapHeaderAdapter((SOAPHeader) e) : new SoapHeaderAdapter((SOAP12Header) e))
          .collect(toList());
    }
    return emptyList();
  }
}
