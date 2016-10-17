/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.introspection;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import org.mule.extension.ws.api.WscConnection;
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
 * Creates the request body for an operation of a web service when no parameters are required.
 */
public class RequestBodyGenerator {

  private static final String NO_PARAMS_SOAP_BODY_CALL_MASK = "<ns:%s xmlns:ns=\"%s\"/>";

  /**
   * Checks if the operation requires input parameters (if the XML required in the body is just one constant element). If so, the
   * body with this XML will be returned in order to send it in every request instead of the payload.
   */
  public String generateRequest(WscConnection connection, String operation) {

    BindingOperation bindingOperation = connection.getWsdlIntrospecter().getBindingOperation(operation);
    List<String> soapBodyParts = getSoapBodyParts(bindingOperation);
    Message message = bindingOperation.getOperation().getInput().getMessage();
    Part part = getSinglePart(soapBodyParts, message);

    // Checks that the message has a single part with at least one element defined.
    if (part != null && part.getElementName() != null) {
      XmlTypeLoader loader = new XmlTypeLoader(connection.getWsdlIntrospecter().getSchemas());
      Optional<MetadataType> loadedType = loader.load(part.getElementName().toString());
      if (loadedType.isPresent()) {
        MetadataType metadataType = loadedType.get();
        if (metadataType instanceof ObjectType) {
          ObjectType type = (ObjectType) ((ObjectType) metadataType).getFields().iterator().next().getValue();
          if (type.getFields().isEmpty()) {
            QName element = part.getElementName();
            return format(NO_PARAMS_SOAP_BODY_CALL_MASK, element.getLocalPart(), element.getNamespaceURI());
          }
        }
      }
    }
    throw new WscException(format("No payload was provided for the operation [%s] and a default one cannot be built", operation));
  }

  /**
   * Finds the part of the input message that must be used in the SOAP body, if the operation requires only one part. Otherwise
   * returns null.
   */
  private Part getSinglePart(List<String> soapBodyParts, Message inputMessage) {
    if (soapBodyParts.isEmpty()) {
      Map parts = inputMessage.getParts();
      if (parts.size() == 1) {
        return (Part) parts.values().iterator().next();
      }
    } else {
      if (soapBodyParts.size() == 1) {
        String partName = soapBodyParts.get(0);
        return inputMessage.getPart(partName);
      }
    }

    return null;
  }

  /**
   * Retrieves the list of SOAP body parts of a binding operation, or null if there is no SOAP body defined.
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
