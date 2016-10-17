/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.introspection;

import static java.lang.String.format;
import static java.util.Collections.emptySet;

import com.ibm.wsdl.extensions.schema.SchemaSerializer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

@SuppressWarnings("unchecked")
public final class WsdlIntrospecter {

  private static final WsdlSchemaCollector schemaCollector = new WsdlSchemaCollector();

  private final Definition definition;
  private final Service service;
  private final Port port;

  public WsdlIntrospecter(String wsdlLocation, String service, String port) {
    this.definition = parseWsdl(wsdlLocation);
    this.service = findService(service);
    this.port = findPort(port);
  }

  public Set<String> getPortNames() {
    if (service != null && service.getPorts() != null) {
      return service.getPorts().keySet();
    }
    return emptySet();
  }

  public List<String> getOperationNames() {
    List<BindingOperation> bindingOperations = (List<BindingOperation>) port.getBinding().getBindingOperations();
    return bindingOperations.stream().map(BindingOperation::getName).collect(Collectors.toList());
  }

  private Service findService(String serviceName) {
    validateBlankString(serviceName, "service name");
    Service service = definition.getService(new QName(definition.getTargetNamespace(), serviceName));
    validateNotNull(service, "The service name [" + serviceName + "] was not found in the current wsdl file.");
    return service;
  }

  private Port findPort(String portName) {
    validateBlankString(portName, "port name");
    Port port = service.getPort(portName.trim());
    validateNotNull(port, "The port name [" + portName + "] was not found in the current wsdl file.");
    return port;
  }

  public Operation getOperation(String operationName) {
    validateBlankString(operationName, "operation name");
    Operation operation = port.getBinding().getPortType().getOperation(operationName, null, null);
    validateNotNull(operation, "The operation name [" + operationName + "] was not found in the current wsdl file.");
    return operation;
  }

  public BindingOperation getBindingOperation(String operationName) {
    validateBlankString(operationName, "operation name");
    BindingOperation operation = port.getBinding().getBindingOperation(operationName, null, null);
    validateNotNull(operation, "The binding operation name [" + operationName + "] was not found in the current wsdl file.");
    return operation;
  }

  public Fault getFault(Operation operation, String faultName) {
    validateBlankString(faultName, "fault name");
    Fault fault = operation.getFault(faultName);
    validateNotNull(fault, "The fault name [" + faultName + "] was not found in the current wsdl file.");
    return fault;
  }

  public Definition getDefinition() {
    return definition;
  }

  public Set<String> getSchemas() {
    return schemaCollector.getSchemas(definition);
  }

  public Service getService() {
    return service;
  }

  public Port getPort() {
    return port;
  }

  /**
   * Given a Wsdl location (either local or remote) it will fetch the definition. If the definition cannot be created, then
   * an exception will be raised
   *
   * @param wsdlLocation path to the desired Wsdl file
   */
  private Definition parseWsdl(final String wsdlLocation) {
    try {
      validateBlankString(wsdlLocation, "Wsdl Location");

      WSDLFactory factory = WSDLFactory.newInstance();
      ExtensionRegistry registry = factory.newPopulatedExtensionRegistry();
      registry.registerSerializer(Types.class,
                                  new QName("http://www.w3.org/2001/XMLSchema", "schema"),
                                  new SchemaSerializer());

      // these will replace whatever may have already been registered
      // in these places, but there's no good way to check what was
      // there before.
      QName header = new QName("http://schemas.xmlsoap.org/wsdl/soap/", "header");
      registry.registerDeserializer(MIMEPart.class,
                                    header,
                                    registry.queryDeserializer(BindingInput.class, header));
      registry.registerSerializer(MIMEPart.class,
                                  header,
                                  registry.querySerializer(BindingInput.class, header));

      // get the original classname of the SOAPHeader
      // implementation that was stored in the registry.
      Class<? extends ExtensibilityElement> clazz = registry.createExtension(BindingInput.class, header).getClass();
      registry.mapExtensionTypes(MIMEPart.class, header, clazz);

      WSDLReader wsdlReader = factory.newWSDLReader();
      wsdlReader.setFeature("javax.wsdl.verbose", false);
      wsdlReader.setFeature("javax.wsdl.importDocuments", true);
      wsdlReader.setExtensionRegistry(registry);

      // TODO: don't delegate
      Definition definition = wsdlReader.readWSDL(wsdlLocation);
      validateNotNull(definition, format("Cannot obtain WSDL definition for file [%s]", wsdlLocation));

      return definition;
    } catch (WSDLException e) {
      //TODO tech debt: we should analyze the type of exception (missing or corrupted file) and thrown better exceptions
      throw new IllegalArgumentException(format("Something went wrong when parsing the wsdl file [%s]", wsdlLocation), e);
    }
  }

  private void validateNotNull(Object paramValue, String errorMessage) {
    if (paramValue == null) {
      throw new IllegalArgumentException(errorMessage);
    }
  }

  private void validateBlankString(String paramValue, String paramName) {
    if (StringUtils.isBlank(paramValue)) {
      throw new IllegalArgumentException("The " + paramName + " can not be blank nor null.");
    }
  }
}
