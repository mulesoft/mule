/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.introspection;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.metadata.internal.utils.MetadataTypeUtils.hasExposedFields;
import static org.mule.metadata.internal.utils.MetadataTypeUtils.isObjectType;
import org.mule.extension.ws.api.exception.WscException;
import org.mule.extension.ws.internal.WscConnection;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.xml.XmlTypeLoader;

import java.util.Collection;
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

  private static final String REQUIRED_PARAMS_ERROR_MASK =
      "Cannot build default body request for operation [%s]%s, the operation requires input parameters";

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
    Optional<List<String>> soapBodyParts = getSoapBodyParts(bindingOperation);

    if (!soapBodyParts.isPresent()) {
      throw new WscException("No SOAP body defined in the WSDL for the specified operation, cannot check if the operation "
          + "requires input parameters.");
    }

    Message message = bindingOperation.getOperation().getInput().getMessage();
    Optional<Part> part = getSinglePart(soapBodyParts.get(), message);

    if (!part.isPresent()) {
      throw new WscException(
                             format(REQUIRED_PARAMS_ERROR_MASK, operation, " there is no single part in the input message"));
    }

    if (part.get().getElementName() == null) {
      throw new WscException(
                             format(REQUIRED_PARAMS_ERROR_MASK, operation,
                                    " there is one message body part but no does not have an element defined"));
    }

    Part bodyPart = part.get();
    if (isOperationWithNoParameters(connection, bodyPart)) {
      // operation has required parameters
      throw new WscException(format(REQUIRED_PARAMS_ERROR_MASK, operation, ""));
    }

    // There is a single part with an element defined and it does not require parameters
    QName element = bodyPart.getElementName();
    return format(NO_PARAMS_SOAP_BODY_CALL_MASK, element.getLocalPart(), element.getNamespaceURI());
  }

  private boolean isOperationWithNoParameters(WscConnection connection, Part part) {
    XmlTypeLoader loader = new XmlTypeLoader(connection.getWsdlIntrospecter().getSchemas());
    String bodyPartQName = part.getElementName().toString();

    // Find the body type
    Optional<MetadataType> bodyType = loader.load(bodyPartQName);
    if (bodyType.isPresent()) {
      if (isObjectType(bodyType.get())) {
        Collection<ObjectFieldType> bodyFields = ((ObjectType) bodyType.get()).getFields();
        // Contains only one field which represents de operation
        ObjectType operationType = (ObjectType) bodyFields.iterator().next().getValue();
        // Check if the operation type has
        return hasExposedFields(operationType);
      }
    }
    return false;
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
        return ofNullable((Part) parts.values().iterator().next());
      }
    } else {
      if (soapBodyParts.size() == 1) {
        String partName = soapBodyParts.get(0);
        return ofNullable(inputMessage.getPart(partName));
      }
    }
    return empty();
  }

  /**
   * Retrieves the list of SOAP body parts of a binding operation if defined.
   *
   * @param bindingOperation the binding operation that we want to get the SOAP body parts from.
   */
  @SuppressWarnings("unchecked")
  private static Optional<List<String>> getSoapBodyParts(BindingOperation bindingOperation) {
    List elements = bindingOperation.getBindingInput().getExtensibilityElements();
    return elements.stream()
        .filter(e -> e instanceof SOAPBody || e instanceof SOAP12Body)
        .map(e -> e instanceof SOAPBody ? ((SOAPBody) e).getParts() : ((SOAP12Body) e).getParts())
        .map(parts -> parts == null ? emptyList() : parts)
        .findFirst();
  }
}
