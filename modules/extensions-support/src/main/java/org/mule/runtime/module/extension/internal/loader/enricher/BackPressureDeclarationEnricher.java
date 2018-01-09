/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.BACK_PRESSURE_STRATEGY_PARAMETER_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.BACK_PRESSURE_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.extension.api.annotation.source.BackPressure;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.source.BackPressureMode;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.property.BackPressureStrategyModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;

import java.util.Optional;

/**
 * Adds a parameter for back pressure on message sources that apply
 *
 * @since 1.1
 */
public class BackPressureDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    final ExtensionDeclaration extensionDeclaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    new IdempotentDeclarationWalker() {

      @Override
      protected void onSource(SourceDeclaration sourceDeclaration) {
        BackPressureStrategyModelProperty property;
        Optional<ExtensionTypeDescriptorModelProperty> extensionTypeDescriptorModelProperty =
            sourceDeclaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class);
        if (extensionTypeDescriptorModelProperty.isPresent()) {
          Type sourceType = extensionTypeDescriptorModelProperty.get().getType();

          property = sourceType.getAnnotation(BackPressure.class)
              .map(BackPressureStrategyModelProperty::of)
              .orElseGet(BackPressureStrategyModelProperty::getDefault);

          sourceDeclaration.addModelProperty(property);

          if (property.getSupportedModes().size() > 1) {
            addBackPressureParameter(extensionDeclaration, sourceDeclaration, property);
          }
        }
      }
    }.walk(extensionDeclaration);
  }

  private void addBackPressureParameter(ExtensionDeclaration extensionDeclaration,
                                        SourceDeclaration sourceDeclaration,
                                        BackPressureStrategyModelProperty property) {

    ParameterDeclaration parameter = new ParameterDeclaration(BACK_PRESSURE_STRATEGY_PARAMETER_NAME);
    parameter.setDescription(BACK_PRESSURE_STRATEGY_PARAMETER_DESCRIPTION);
    parameter.setRequired(false);
    parameter.setDefaultValue(property.getDefaultMode());
    parameter.setExpressionSupport(NOT_SUPPORTED);
    parameter.setLayoutModel(LayoutModel.builder().tabName(ADVANCED_TAB).build());

    MetadataType type = BaseTypeBuilder.create(JAVA).stringType()
        .id(format("%s-%s-backPressureStrategy", extensionDeclaration.getName(), sourceDeclaration.getName()))
        .with(new EnumAnnotation<>(property.getSupportedModes().stream().map(BackPressureMode::name).toArray(String[]::new)))
        .build();

    parameter.setType(type, false);

    sourceDeclaration.getParameterGroup(DEFAULT_GROUP_NAME).addParameter(parameter);
  }

}
