/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import org.mule.extension.ws.internal.connection.WscConnection;
import org.mule.extension.ws.internal.introspection.TypeIntrospecterDelegate;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectFieldTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;

import java.util.List;

import javax.wsdl.BindingOperation;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.wsdl.extensions.ElementExtensible;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap12.SOAP12Header;

/**
 * Handles the dynamic {@link MetadataType} resolution for the SOAP Headers of a web service operation.
 * <p>
 * This is the base class for both INPUT and OUTPUT Headers resolution, the {@link TypeIntrospecterDelegate} is in charge
 * to get the information to introspect the input or output soap headers from.
 *
 * @since 4.0
 */
final class HeadersElementResolver extends NodeElementResolver {

  HeadersElementResolver(TypeIntrospecterDelegate delegate) {
    super(delegate);
  }

  @Override
  public MetadataType getMetadata(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    WscConnection connection = getConnection(context);
    WsdlIntrospecter introspecter = connection.getWsdlIntrospecter();
    BindingOperation bindingOperation = introspecter.getBindingOperation(operationName);
    ElementExtensible bindingType = delegate.getBindingType(bindingOperation);
    List<SoapHeaderAdapter> headers = getHeaderParts(bindingType);
    if (!headers.isEmpty()) {
      Message message = delegate.getMessage(introspecter.getOperation(operationName));
      return buildHeaderType(context.getTypeBuilder(), connection.getTypeLoader(), headers, introspecter, message);
    }
    return NULL_TYPE;
  }

  private MetadataType buildHeaderType(BaseTypeBuilder builder, TypeLoader loader, List<SoapHeaderAdapter> headers,
                                       WsdlIntrospecter introspecter, Message message)
      throws MetadataResolvingException {
    ObjectTypeBuilder typeBuilder = builder.objectType();
    for (SoapHeaderAdapter header : headers) {
      ObjectFieldTypeBuilder field = typeBuilder.addField();
      String headerPart = header.getPart();
      Part part = message.getPart(headerPart);
      if (part != null) {
        field.key(headerPart).value(buildPartMetadataType(loader, part));
      } else {
        Message headerMessage = introspecter.getMessage(header.getMessage());
        field.key(headerPart).value(buildPartMetadataType(loader, headerMessage.getPart(headerPart)));
      }
    }
    return typeBuilder.build();
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

  @Override
  public String getResolverName() {
    return "HeadersElementResolver";
  }
}
