/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.declaration.fluent.HasParametersDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.connectivity.api.platform.schema.extension.ExcludeFromConnectivitySchemaModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;

import java.util.LinkedList;
import java.util.List;

public final class ParameterModelsLoaderDelegate {


  public List<ParameterDeclarer> declare(HasParametersDeclarer component, List<ParameterGroupModelParser> groupParsers) {

    final List<ParameterDeclarer> declarerList = new LinkedList<>();
    groupParsers.forEach(group -> {
      ParameterGroupDeclarer groupDeclarer;

      if (DEFAULT_GROUP_NAME.equals(group.getName())) {
        groupDeclarer = component.onDefaultParameterGroup();
      } else {
        groupDeclarer = component.onParameterGroup(group.getName())
            .withDslInlineRepresentation(group.showsInDsl());

        group.getDisplayModel().ifPresent(groupDeclarer::withDisplayModel);
        group.getLayoutModel().ifPresent(groupDeclarer::withLayout);
        group.getAdditionalModelProperties().forEach(groupDeclarer::withModelProperty);
        group.getExclusiveOptionals().ifPresent(descriptor -> groupDeclarer
            .withExclusiveOptionals(descriptor.getExclusiveOptionals(), descriptor.isOneRequired()));
        groupDeclarer.getDeclaration().setDescription(group.getDescription());
        group.getLayoutModel().ifPresent(groupDeclarer::withLayout);
      }

      group.getParameterParsers().forEach(extensionParameter -> {
        ParameterDeclarer parameter;
        if (extensionParameter.isRequired()) {
          parameter = groupDeclarer.withRequiredParameter(extensionParameter.getName());
        } else {
          parameter = groupDeclarer.withOptionalParameter(extensionParameter.getName())
              .defaultingTo(extensionParameter.getDefaultValue());
        }

        final MetadataType metadataType = extensionParameter.getType();
        parameter.ofType(metadataType)
            .describedAs(extensionParameter.getDescription())
            .withAllowedStereotypes(extensionParameter.getAllowedStereotypes())
            .withRole(extensionParameter.getRole())
            .withExpressionSupport(extensionParameter.getExpressionSupport());

        if (extensionParameter.isComponentId()) {
          parameter.asComponentId();
        }

        if (extensionParameter.isConfigOverride()) {
          parameter.asConfigOverride();
        }

        if (extensionParameter.isExcludedFromConnectivitySchema()) {
          parameter.withModelProperty(new ExcludeFromConnectivitySchemaModelProperty());
        }

        extensionParameter.getLayoutModel().ifPresent(parameter::withLayout);
        extensionParameter.getDslConfiguration().ifPresent(parameter::withDsl);
        extensionParameter.getDeprecationModel().ifPresent(parameter::withDeprecation);
        extensionParameter.getDisplayModel().ifPresent(parameter::withDisplayModel);
        extensionParameter.getAdditionalModelProperties().forEach(parameter::withModelProperty);
        declarerList.add(parameter);
      });

      if (declarerList.stream().noneMatch(p -> p.getDeclaration().isComponentId())) {
        declarerList.stream()
            .filter(p -> p.getDeclaration().getName().equals("name")
                && p.getDeclaration().isRequired()
                && p.getDeclaration().getExpressionSupport() == NOT_SUPPORTED
                && p.getDeclaration().getType() instanceof StringType
                && p.getDeclaration().getAllowedStereotypeModels().isEmpty())
            .forEach(p -> p.asComponentId());
      }
    });

    return declarerList;
  }
}
