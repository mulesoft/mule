/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_ABSTRACT_OPERATOR;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.MULE_ABSTRACT_OPERATOR_TYPE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.TYPE_SUFFIX;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Element;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;

import javax.xml.namespace.QName;

/**
 * Builder delegation class to generate a XSD schema that describes an {@link OperationModel}
 *
 * @since 4.0.0
 */
class OperationSchemaDelegate extends ExecutableTypeSchemaDelegate {

  public OperationSchemaDelegate(SchemaBuilder builder) {
    super(builder);
  }

  public void registerOperation(ComponentModel componentModel, DslElementSyntax dslSyntax, boolean hasImplicitConfig) {
    String typeName = capitalize(componentModel.getName()) + TYPE_SUFFIX;
    registerProcessorElement(componentModel, typeName, dslSyntax);
    ExtensionType extensionType = registerOperationType(typeName, componentModel, dslSyntax, hasImplicitConfig);
    registerNestedComponents(extensionType, componentModel.getNestedComponents());
  }

  void registerProcessorElement(ComponentModel componentModel, String typeName, DslElementSyntax dslSyntax) {
    Element element = new TopLevelElement();
    element.setName(dslSyntax.getElementName());
    element.setType(new QName(builder.getSchema().getTargetNamespace(), typeName));
    element.setAnnotation(builder.createDocAnnotation(componentModel.getDescription()));
    element.setSubstitutionGroup(getOperationSubstitutionGroup(componentModel));
    builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(element);
  }

  ExtensionType registerOperationType(String name, ComponentModel operationModel, DslElementSyntax dslSyntax,
                                      boolean hasImplicitConfig) {
    ExtensionType componentType = createExecutableType(name, MULE_ABSTRACT_OPERATOR_TYPE, dslSyntax, hasImplicitConfig);
    initialiseSequence(componentType);
    ExplicitGroup sequence = componentType.getSequence();
    builder.addInfrastructureParameters(componentType, operationModel, sequence);
    operationModel.getParameterGroupModels()
        .forEach(group -> registerParameterGroup(componentType, group));
    return componentType;
  }

  private QName getOperationSubstitutionGroup(ComponentModel componentModel) {
    return componentModel.getStereotype().equals(PROCESSOR)
        ? MULE_ABSTRACT_OPERATOR
        : getSubstitutionGroup(componentModel.getStereotype());
  }
}
