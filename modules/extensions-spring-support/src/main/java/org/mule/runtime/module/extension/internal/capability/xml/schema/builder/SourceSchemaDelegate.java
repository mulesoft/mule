/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.capitalize;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.MULE_ABSTRACT_MESSAGE_SOURCE;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.MULE_ABSTRACT_MESSAGE_SOURCE_TYPE;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.TYPE_SUFFIX;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.Element;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExplicitGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.ExtensionType;
import org.mule.runtime.module.extension.internal.capability.xml.schema.model.TopLevelElement;

import java.util.List;

import javax.xml.namespace.QName;

/**
 * Builder delegation class to generate a XSD schema that describes a {@link SourceModel}
 *
 * @since 4.0.0
 */
class SourceSchemaDelegate extends ExecutableTypeSchemaDelegate {

  SourceSchemaDelegate(SchemaBuilder builder) {
    super(builder);
  }

  void registerMessageSource(SourceModel sourceModel, DslElementSyntax dslSyntax) {
    String typeName = capitalize(sourceModel.getName()) + TYPE_SUFFIX;
    registerSourceElement(sourceModel, typeName, dslSyntax);
    registerSourceType(typeName, sourceModel, dslSyntax);
  }

  private void registerSourceElement(SourceModel sourceModel, String typeName, DslElementSyntax dslSyntax) {
    Element element = new TopLevelElement();
    element.setName(dslSyntax.getElementName());
    element.setType(new QName(builder.getSchema().getTargetNamespace(), typeName));
    element.setAnnotation(builder.createDocAnnotation(sourceModel.getDescription()));
    element.setSubstitutionGroup(MULE_ABSTRACT_MESSAGE_SOURCE);
    builder.getSchema().getSimpleTypeOrComplexTypeOrGroup().add(element);
  }

  private void registerSourceType(String name, SourceModel sourceModel, DslElementSyntax dslSyntax) {
    final ExtensionType sourceType = createExecutableType(name, MULE_ABSTRACT_MESSAGE_SOURCE_TYPE, dslSyntax);
    initialiseSequence(sourceType);
    ExplicitGroup sequence = sourceType.getSequence();
    builder.addInfrastructureParameters(sourceType, sourceModel, sequence);

    List<ParameterGroupModel> inlineGroupedParameters = getInlineGroups(sourceModel);
    sourceModel.getErrorCallback().ifPresent(cb -> inlineGroupedParameters.addAll(getInlineGroups(cb)));
    sourceModel.getSuccessCallback().ifPresent(cb -> inlineGroupedParameters.addAll(getInlineGroups(cb)));

    List<ParameterModel> flatParameters = sourceModel.getAllParameterModels().stream()
        .filter(p -> inlineGroupedParameters.stream().noneMatch(g -> g.getParameterModels().contains(p)))
        .collect(toList());

    registerParameters(sourceType, flatParameters);
    inlineGroupedParameters.forEach(g -> builder.addInlineParameterGroup(g, sourceType.getSequence()));

  }

  private List<ParameterGroupModel> getInlineGroups(ParameterizedModel model) {
    return model.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .collect(toList());
  }
}
