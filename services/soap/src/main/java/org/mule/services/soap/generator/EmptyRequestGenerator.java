/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.generator;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.services.soap.api.exception.BadRequestException;
import org.mule.services.soap.api.exception.InvalidWsdlException;
import org.mule.services.soap.introspection.WsdlIntrospecter;
import org.mule.services.soap.util.SoapServiceMetadataTypeUtils;

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
final class EmptyRequestGenerator {

  private static final String REQUIRED_PARAMS_ERROR_MASK =
      "Cannot build default body request for operation [%s]%s, the operation requires input parameters";

  /**
   * SOAP request mask for operations without input parameters
   */
  private static final String NO_PARAMS_SOAP_BODY_CALL_MASK = "<ns:%s xmlns:ns=\"%s\"/>";
  private final WsdlIntrospecter introspecter;
  private final XmlTypeLoader loader;

  public EmptyRequestGenerator(WsdlIntrospecter introspecter, XmlTypeLoader loader) {
    this.introspecter = introspecter;
    this.loader = loader;
  }

  /**
   * Generates a request body for an operation that don't require input parameters, if the required XML in the body is
   * just one constant element.
   */
  String generateRequest(String operation) {

    BindingOperation bindingOperation = introspecter.getBindingOperation(operation);
    Optional<List<String>> soapBodyParts = getSoapBodyParts(bindingOperation);

    if (!soapBodyParts.isPresent()) {
      throw new InvalidWsdlException(format("No SOAP body defined in the WSDL for the specified operation, cannot check if the operation"
          + " requires input parameters. Cannot build a default body request for the specified operation [%s]", operation));
    }

    Message message = bindingOperation.getOperation().getInput().getMessage();
    Optional<Part> part = getSinglePart(soapBodyParts.get(), message);

    if (!part.isPresent()) {
      throw new BadRequestException(
                                    format(REQUIRED_PARAMS_ERROR_MASK, operation,
                                           " there is no single part in the input message"));
    }

    if (part.get().getElementName() == null) {
      throw new BadRequestException(
                                    format(REQUIRED_PARAMS_ERROR_MASK, operation,
                                           " there is one message body part but no does not have an element defined"));
    }

    Part bodyPart = part.get();
    if (isOperationWithRequiredParameters(loader, bodyPart)) {
      // operation has required parameters
      throw new BadRequestException(format(REQUIRED_PARAMS_ERROR_MASK, operation, ""));
    }

    // There is a single part with an element defined and it does not require parameters
    QName element = bodyPart.getElementName();
    return format(NO_PARAMS_SOAP_BODY_CALL_MASK, element.getLocalPart(), element.getNamespaceURI());
  }

  private boolean isOperationWithRequiredParameters(TypeLoader loader, Part part) {
    // Find the body type
    Optional<MetadataType> bodyType = loader.load(part.getElementName().toString());
    if (bodyType.isPresent()) {
      Collection<ObjectFieldType> operationFields = SoapServiceMetadataTypeUtils.getOperationType(bodyType.get()).getFields();
      return !operationFields.isEmpty();
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
   * @param operation the binding operation that we want to get the SOAP body parts from.
   */
  @SuppressWarnings("unchecked")
  private Optional<List<String>> getSoapBodyParts(BindingOperation operation) {
    List elements = operation.getBindingInput().getExtensibilityElements();
    return elements.stream()
        .filter(e -> e instanceof SOAPBody || e instanceof SOAP12Body)
        .map(e -> e instanceof SOAPBody ? ((SOAPBody) e).getParts() : ((SOAP12Body) e).getParts())
        .map(parts -> parts == null ? emptyList() : parts)
        .findFirst();
  }
}
