/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.model.ModuleExtensionLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that represents an <operation/> within a <module/>
 * //TODO MULE-10866 : once implemented, this class should go away or refactored as ExtensionModel will be good enough to model an object for a smart connector
 */
public class OperationExtension {

  private String name;
  private List<ParameterExtension> parameters = new ArrayList<>();
  private ComponentModel componentModel;
  private MetadataType outputType;

  /**
   * @param name name of the operation
   * @param componentModel element of the node that's used to look for the MP's defined within the <body/> of the current <operation/>
     */
  public OperationExtension(String name, ComponentModel componentModel) {
    this.name = name;
    this.componentModel = componentModel;
  }

  /**
   * @return the name of the operation
   */
  public String getName() {
    return name;
  }

  /**
   * @return the list of parameters that are mandatory for this operation
   */
  public List<ParameterExtension> getParameters() {
    return parameters;
  }

  /**
   * @param parameters the list of elements that are mandatory for this operation
   */
  public void setParameters(List<ParameterExtension> parameters) {
    this.parameters = parameters;
  }

  /**
   * @param outputType defines the output type. Determines if this operation does write the exit payload or not
   */
  public void setOutputType(MetadataType outputType) {
    this.outputType = outputType;
  }

  /**
   * @return true if the return type is {@link VoidType}, false otherwise
   */
  public boolean returnsVoid() {
    ReturnsVoidTypeVisitor returnsVoidTypeVisitor = new ReturnsVoidTypeVisitor();
    outputType.accept(returnsVoidTypeVisitor);
    return returnsVoidTypeVisitor.returnsVoid;
  }

  /**
   * @return the list of MPs to expand them in the xml of the application
   */
  public List<ComponentModel> getMessageProcessorsComponentModels() {
    return componentModel.getInnerComponents()
        .stream()
        .filter(childComponent -> childComponent.getIdentifier().equals(ModuleExtensionLoader.OPERATION_BODY_IDENTIFIER))
        .findAny().get().getInnerComponents();
  }

  /**
   * visits all the possible types of a given parameter to realize if it's a "void return type", in which case the
   * expanded chain will not modify the structure of the event
   */
  private class ReturnsVoidTypeVisitor extends MetadataTypeVisitor {

    private boolean returnsVoid = false;

    @Override
    public void visitVoid(VoidType voidType) {
      returnsVoid = true;
    }
  }
}
