/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.type.catalog;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.message.api.el.ModuleDefinition.builder;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.FunctionTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.el.ModuleDefinition;
import org.mule.metadata.message.api.el.ModuleDefinition.Builder;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.function.FunctionModel;

import java.util.function.Function;

/**
 * Utility object to create a {@link ModuleDefinition} starting from a {@link ExtensionModel}. The resulting
 * {@link ModuleDefinition} object will have the functions and types from the original {@link ExtensionModel}.
 *
 * @since 4.5.0
 */
public class ExtensionModelToModuleDefinitionTransformer implements Function<ExtensionModel, ModuleDefinition> {

  @Override
  public ModuleDefinition apply(ExtensionModel extensionModel) {
    Builder moduleDefBuilder = builder(getPrefix(extensionModel));
    extensionModel.getFunctionModels().forEach(functionModel -> addFunction(functionModel, moduleDefBuilder));
    extensionModel.getTypes().forEach(type -> addType(type, moduleDefBuilder));
    return moduleDefBuilder.build();
  }

  private static String getPrefix(ExtensionModel extensionModel) {
    return extensionModel.getXmlDslModel().getPrefix();
  }

  private static void addType(MetadataType type, Builder moduleDefBuilder) {
    moduleDefBuilder.addType(type);
  }

  private static void addFunction(FunctionModel functionModel, Builder moduleDefBuilder) {
    FunctionTypeBuilder functionTypeBuilder = new BaseTypeBuilder(JAVA).functionType();
    functionModel.getAllParameterModels().forEach(parameterModel -> {
      if (parameterModel.isRequired()) {
        functionTypeBuilder.addOptionalParameterOf(parameterModel.getName(), parameterModel.getType());
      } else {
        functionTypeBuilder.addParameterOf(parameterModel.getName(), parameterModel.getType());
      }
    });

    functionTypeBuilder.returnType(functionModel.getOutput().getType());
    moduleDefBuilder.addElement(functionModel.getName(), functionTypeBuilder.build());
  }
}
