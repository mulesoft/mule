/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata.header;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mule.metadata.api.model.MetadataFormat.XML;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;
import org.mule.extension.ws.internal.metadata.NodeElementResolver;
import org.mule.extension.ws.internal.metadata.TypeResolverDelegate;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectFieldTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;

import java.util.List;

import javax.wsdl.BindingOperation;
import javax.wsdl.Message;
import javax.wsdl.extensions.ElementExtensible;
import javax.wsdl.extensions.soap.SOAPHeader;

/**
 * Handles the dynamic {@link MetadataType} resolution for the SOAP Headers of a web service operation.
 * <p>
 * This is the base class for both INPUT and OUTPUT Headers resolution, the {@link TypeResolverDelegate} is in charge
 * to get the information to introspect the input or output soap headers from.
 *
 * @since 4.0
 */
abstract class HeadersElementResolver extends NodeElementResolver {

  HeadersElementResolver(TypeResolverDelegate delegate) {
    super(delegate);
  }

  @Override
  public MetadataType getMetadata(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    WsdlIntrospecter introspecter = getConnection(context).getWsdlIntrospecter();
    BindingOperation bindingOperation = introspecter.getBindingOperation(operationName);
    ElementExtensible bindingType = delegate.getBindingType(bindingOperation);
    List<SOAPHeader> headers = getHeaders(bindingType);
    if (!headers.isEmpty()) {
      Message message = delegate.getMessage(introspecter.getOperation(operationName));
      XmlTypeLoader loader = new XmlTypeLoader(introspecter.getSchemas());
      return buildHeaderType(headers, message, loader);
    }
    return getNullType();
  }

  private MetadataType buildHeaderType(List<SOAPHeader> headers, Message message, XmlTypeLoader loader)
      throws MetadataResolvingException {
    ObjectTypeBuilder<?> typeBuilder = BaseTypeBuilder.create(XML).objectType();
    for (SOAPHeader header : headers) {
      ObjectFieldTypeBuilder<?> field = typeBuilder.addField();
      String partName = header.getPart();
      field.key(partName);
      field.value(buildPartMetadataType(loader, message.getPart(partName)));
      field.build();
    }
    return typeBuilder.build();
  }

  private List<SOAPHeader> getHeaders(ElementExtensible bindingType) {
    List extensible = bindingType.getExtensibilityElements();
    if (extensible != null) {
      return (List<SOAPHeader>) extensible.stream().filter(e -> e instanceof SOAPHeader).collect(toList());
    }
    return emptyList();
  }
}
