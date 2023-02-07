/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.addSemanticTerms;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.declaration.fluent.HasParametersDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.extension.api.property.ExcludeFromConnectivitySchemaModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ParameterModelsLoaderDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(ParameterModelsLoaderDelegate.class);

  private final Supplier<StereotypeModelLoaderDelegate> stereotypeModelLoader;
  private final Consumer<MetadataType> typeRegisterer;

  public ParameterModelsLoaderDelegate(Supplier<StereotypeModelLoaderDelegate> stereotypeModelLoader,
                                       Consumer<MetadataType> typeRegisterer) {
    this.stereotypeModelLoader = stereotypeModelLoader;
    this.typeRegisterer = typeRegisterer;
  }

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
        groupDeclarer.getDeclaration().setDescription(group.getDescription());
        group.getLayoutModel().ifPresent(groupDeclarer::withLayout);
      }
      group.getExclusiveOptionals().ifPresent(descriptor -> groupDeclarer
          .withExclusiveOptionals(descriptor.getExclusiveOptionals(), descriptor.isOneRequired()));

      group.getParameterParsers().forEach(parameterParser -> {
        ParameterDeclarer parameter;
        if (parameterParser.isRequired()) {
          parameter = groupDeclarer.withRequiredParameter(parameterParser.getName());
        } else {
          parameter = groupDeclarer.withOptionalParameter(parameterParser.getName())
              .defaultingTo(parameterParser.getDefaultValue());
        }

        final MetadataType metadataType = parameterParser.getType();
        parameter.ofType(metadataType)
            .describedAs(parameterParser.getDescription())
            .withRole(parameterParser.getRole())
            .withExpressionSupport(parameterParser.getExpressionSupport());

        typeRegisterer.accept(metadataType);

        if (parameterParser.isComponentId()) {
          parameter.asComponentId();
        }

        if (parameterParser.isConfigOverride()) {
          parameter.asConfigOverride();
        }

        if (parameterParser.isExcludedFromConnectivitySchema()) {
          parameter.withModelProperty(new ExcludeFromConnectivitySchemaModelProperty());
        }

        parameterParser.getLayoutModel().ifPresent(parameter::withLayout);
        parameterParser.getDslConfiguration().ifPresent(parameter::withDsl);
        parameterParser.getDeprecationModel().ifPresent(parameter::withDeprecation);
        parameterParser.getDisplayModel().ifPresent(parameter::withDisplayModel);
        parameterParser.getOAuthParameterModelProperty().ifPresent(parameter::withModelProperty);
        parameterParser.getAdditionalModelProperties().forEach(parameter::withModelProperty);
        parameterParser.getResolvedMinMuleVersion().ifPresent(resolvedMMV -> {
          parameter.withMinMuleVersion(resolvedMMV.getMinMuleVersion());
          LOGGER.debug(resolvedMMV.getReason());
        });

        addSemanticTerms(parameter.getDeclaration(), parameterParser);
        stereotypeModelLoader.get().addStereotypes(parameterParser, parameter);
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
