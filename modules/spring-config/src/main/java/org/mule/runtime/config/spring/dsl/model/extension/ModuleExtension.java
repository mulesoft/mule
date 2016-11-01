/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension;

import org.mule.runtime.config.spring.dsl.model.ComponentModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that represents a complete extension read from the XML (aka: smart connector, <module>, etc.).
 * //TODO MULE-10866 : once implemented, this class should go away or refactored as ExtensionModel will be good enough to model an object for a smart connector
 */
public class ModuleExtension {

  private final String name;
  private final String namespace;
  private List<ParameterExtension> properties = new ArrayList<>();
  private Map<String, OperationExtension> operations = new HashMap<>();
  private List<ComponentModel> globalElements = new ArrayList<>();

  /**
   * @param name name of the module to look when working in {@link org.mule.runtime.config.spring.dsl.model.ApplicationModel} expanding the macro
   * @param namespace namespace of the module used when doing the lookup when generating XSD
   */
  public ModuleExtension(String name, String namespace) {
    this.name = name;
    this.namespace = namespace;
  }

  /**
   * @return the name of the module taken from the name attribute
   */
  public String getName() {
    return name;
  }

  /**
   * @return the namespace of the module taken from the namespace attribute
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * @return the collection of properties used to generate this module
   */
  public List<ParameterExtension> getProperties() {
    return properties;
  }

  /**
   * @param properties to set the current module (aka: <property name="username" type="string"/>)
   */
  public void setProperties(List<ParameterExtension> properties) {
    this.properties = properties;
  }

  /**
   * @return the map of <operation>s found for this extension, where the key is the name and value is the actual operation
   */
  public Map<String, OperationExtension> getOperations() {
    return operations;
  }

  /**
   * @param operations the map of operations supported by this module
   */
  public void setOperations(Map<String, OperationExtension> operations) {
    this.operations = operations;
  }

  /**
   * @return the elements that represent global elements at config level. For example it will return the
   * "<http:requester-config... />" element or any other that is sibling to a "<operation/>"
   */
  public List<ComponentModel> getGlobalElements() {
    return globalElements;
  }

  /**
   * @param globalElements the list of global elements this module supports
   */
  public void setGlobalElements(List<ComponentModel> globalElements) {
    this.globalElements = globalElements;
  }

  /**
   * @return if this module has at least one <property/> or at least one global element. False otherwise.
   */
  public boolean hasConfig() {
    return (!properties.isEmpty()) || (!globalElements.isEmpty());
  }

  @Override
  public String toString() {
    return "ModuleExtension{" +
        "name='" + name + '\'' +
        ", namespace='" + namespace + '\'' +
        '}';
  }
}
