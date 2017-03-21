/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.capitalize;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.MULE_ABSTRACT_OPERATOR;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.MULE_ABSTRACT_OPERATOR_TYPE;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.TYPE_SUFFIX;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Element;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ExtendingOperationModelProperty;

import java.util.List;

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
    ExtensionType operationType = createExecutableType(name, MULE_ABSTRACT_OPERATOR_TYPE, dslSyntax);
    initialiseSequence(operationType);
    ExplicitGroup sequence = operationType.getSequence();
    builder.addInfrastructureParameters(operationType, operationModel, sequence);

    List<ParameterModel> inlineGroupedParameters = operationModel.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .flatMap(g -> g.getParameterModels().stream())
        .collect(toList());

    List<ParameterModel> flatParameters = operationModel.getAllParameterModels().stream()
        .filter(p -> !inlineGroupedParameters.contains(p))
        .collect(toList());

    registerParameters(operationType, flatParameters);

    operationModel.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .forEach(g -> {
          initialiseSequence(operationType);
          builder.addInlineParameterGroup(g, operationType.getSequence());
        });
  }

  private QName getOperationSubstitutionGroup(OperationModel operationModel) {
    Reference<QName> substitutionGroup = new Reference<>(MULE_ABSTRACT_OPERATOR);
    operationModel.getModelProperty(ExtendingOperationModelProperty.class)
        .ifPresent(property -> substitutionGroup.set(getSubstitutionGroup(property.getType())));

    return substitutionGroup.get();
  }
}
