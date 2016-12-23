/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_ATTRIBUTE;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_OPERATOR;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.MULE_ABSTRACT_OPERATOR_TYPE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.TARGET_ATTRIBUTE_DESCRIPTION;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.TYPE_SUFFIX;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.dsl.DslElementSyntax;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Attribute;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Element;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ExtendingOperationModelProperty;

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

  public void registerOperation(OperationModel operationModel, DslElementSyntax dslSyntax) {
    String typeName = capitalize(operationModel.getName()) + TYPE_SUFFIX;
    registerProcessorElement(operationModel, typeName, dslSyntax);
    registerOperationType(typeName, operationModel, dslSyntax);
  }

  private void registerProcessorElement(OperationModel operationModel, String typeName, DslElementSyntax dslSyntax) {
    Element element = new TopLevelElement();
    element.setName(dslSyntax.getElementName());
    element.setType(new QName(builder.getSchema().getTargetNamespace(), typeName));
    element.setAnnotation(builder.createDocAnnotation(operationModel.getDescription()));
    element.setSubstitutionGroup(getOperationSubstitutionGroup(operationModel));
    builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(element);
  }

  private void registerOperationType(String name, OperationModel operationModel, DslElementSyntax dslSyntax) {
    ExtensionType operationType = registerExecutableType(name, operationModel, MULE_ABSTRACT_OPERATOR_TYPE, dslSyntax);
    addTargetParameter(operationType, operationModel);
  }

  private QName getOperationSubstitutionGroup(OperationModel operationModel) {
    Reference<QName> substitutionGroup = new Reference<>(MULE_ABSTRACT_OPERATOR);
    operationModel.getModelProperty(ExtendingOperationModelProperty.class)
        .ifPresent(property -> substitutionGroup.set(getSubstitutionGroup(property.getType())));

    return substitutionGroup.get();
  }

  private void addTargetParameter(ExtensionType operationType, OperationModel operationModel) {
    if (!isVoid(operationModel)) {
      Attribute attribute = builder.createAttribute(TARGET_ATTRIBUTE, builder.load(String.class), false, NOT_SUPPORTED);
      attribute.setAnnotation(builder.createDocAnnotation(TARGET_ATTRIBUTE_DESCRIPTION));
      operationType.getAttributeOrAttributeGroup().add(attribute);
    }
  }
}
