/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata.body;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;
import org.mule.extension.ws.internal.metadata.NodeElementResolver;
import org.mule.extension.ws.internal.metadata.TypeResolverDelegate;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.wsdl.BindingOperation;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.extensions.soap.SOAPBody;

/**
 * Handles the dynamic {@link MetadataType} resolution for the SOAP Body parts of a web service operation.
 * <p>
 * This is the base class for both INPUT and OUTPUT body resolution, the {@link TypeResolverDelegate} is in charge
 * to get the information to introspect the soap parts from.
 *
 * @since 4.0
 */
abstract class BodyElementResolver extends NodeElementResolver {

  BodyElementResolver(TypeResolverDelegate delegate) {
    super(delegate);
  }

  @Override
  public MetadataType getMetadata(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    WsdlIntrospecter introspecter = getConnection(context).getWsdlIntrospecter();
    Operation operation = introspecter.getOperation(operationName);
    BindingOperation bindingOperation = introspecter.getBindingOperation(operationName);
    XmlTypeLoader loader = new XmlTypeLoader(introspecter.getSchemas());
    Part body = getBodyPart(operation, bindingOperation);
    return buildPartMetadataType(loader, body);
  }

  private Part getBodyPart(Operation operation, BindingOperation bindingOperation)
      throws MetadataResolvingException {
    String name = operation.getName();
    Message message = delegate.getMessage(operation);
    Map parts = message.getParts();
    if (parts == null || parts.isEmpty()) {
      throw new MetadataResolvingException(format("No message parts found for operation [%s]", name), UNKNOWN);
    }
    if (parts.size() == 1) {
      return (Part) parts.get(parts.keySet().toArray()[0]);
    }

    String bodyPartName = getBodyPartName(bindingOperation)
        .orElseThrow(() -> new MetadataResolvingException(format("No body element found for operation [%s]", name), UNKNOWN));

    return (Part) parts.get(bodyPartName);
  }

  @SuppressWarnings("unchecked")
  private Optional<String> getBodyPartName(BindingOperation bindingOperation)
      throws MetadataResolvingException {
    List elements = delegate.getBindingType(bindingOperation).getExtensibilityElements();
    if (elements != null) {
      //TODO: MULE-10796 - what about other type of SOAP body out there? (e.g.: SOAP12Body)
      Optional<SOAPBody> body = elements.stream().filter(e -> e instanceof SOAPBody).findFirst();
      if (body.isPresent()) {
        List bodyParts = body.get().getParts();
        if (!bodyParts.isEmpty()) {
          if (bodyParts.size() > 1) {
            throw new MetadataResolvingException("Multipart body operations are not supported", INVALID_CONFIGURATION);
          }
          return ofNullable((String) bodyParts.get(0));
        }
      }
    }
    return empty();
  }
}
