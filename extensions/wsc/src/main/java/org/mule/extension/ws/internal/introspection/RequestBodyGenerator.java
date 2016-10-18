/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.introspection;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import org.mule.extension.ws.internal.WscConnection;
import org.mule.extension.ws.api.exception.WscException;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.xml.XmlTypeLoader;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.wsdl.BindingOperation;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap12.SOAP12Body;
import javax.xml.namespace.QName;

/**
 * Enables the construction of request bodies for web service operations that don't require input parameters.
 *
 * @since 4.0
 */
public class RequestBodyGenerator {

  /**
   * SOAP request mask for operations without input parameters
   */
  private static final String NO_PARAMS_SOAP_BODY_CALL_MASK = "<ns:%s xmlns:ns=\"%s\"/>";

  /**
   * Generates a request body for an operation that don't require input parameters, if the required XML in the body is
   * just one constant element.
   *
   * @param connection the connection with the web service
   * @param operation  the name of the operation which body needs to be created.
   */
  public String generateRequest(WscConnection connection, String operation) {

    BindingOperation bindingOperation = connection.getWsdlIntrospecter().getBindingOperation(operation);
    List<String> soapBodyParts = getSoapBodyParts(bindingOperation);
    Message message = bindingOperation.getOperation().getInput().getMessage();
    Optional<Part> part = getSinglePart(soapBodyParts, message);

    // Checks that the message has a single part with at least one element defined.
    if (part.isPresent() && part.get().getElementName() != null) {
      XmlTypeLoader loader = new XmlTypeLoader(connection.getWsdlIntrospecter().getSchemas());
      Optional<MetadataType> loadedType = loader.load(part.get().getElementName().toString());
      if (loadedType.isPresent()) {
        MetadataType metadataType = loadedType.get();
        if (metadataType instanceof ObjectType) {
          ObjectType type = (ObjectType) ((ObjectType) metadataType).getFields().iterator().next().getValue();
          if (type.getFields().isEmpty()) {
            QName element = part.get().getElementName();
            return format(NO_PARAMS_SOAP_BODY_CALL_MASK, element.getLocalPart(), element.getNamespaceURI());
          }
        }
      }
    }
    throw new WscException(format("No payload was provided for the operation [%s] and a default one cannot be built", operation));
  }

  /**
   * Finds the part of the input message that must be used in the SOAP body, if the operation requires only one part.
   *
   * @param soapBodyParts the body parts discovered in the binding type
   * @param inputMessage  the input {@link Message} of the operation.
   */
  private Optional<Part> getSinglePart(List<String> soapBodyParts, Message inputMessage) {
    if (soapBodyParts.isEmpty()) {
      Map parts = inputMessage.getParts();
      if (parts.size() == 1) {
        return Optional.ofNullable((Part) parts.values().iterator().next());
      }
    } else {
      if (soapBodyParts.size() == 1) {
        String partName = soapBodyParts.get(0);
        return Optional.ofNullable(inputMessage.getPart(partName));
      }
    }
    return Optional.empty();
  }

  /**
   * Retrieves the list of SOAP body parts of a binding operation if defined.
   *
   * @param bindingOperation the binding operation that we want to get the SOAP body parts from.
   */
  public static List<String> getSoapBodyParts(BindingOperation bindingOperation) {
    List elements = bindingOperation.getBindingInput().getExtensibilityElements();
    List result = null;
    for (Object element : elements) {
      if (element instanceof SOAPBody) {
        result = ((SOAPBody) element).getParts();
        break;
      }
      if (element instanceof SOAP12Body) {
        result = ((SOAP12Body) element).getParts();
        break;
      }
    }
    return result != null ? result : emptyList();
  }
}
