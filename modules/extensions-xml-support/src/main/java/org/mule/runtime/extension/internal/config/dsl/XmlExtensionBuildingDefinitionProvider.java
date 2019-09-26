/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.config.dsl;

import static java.util.Collections.emptySet;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.TNS_PREFIX;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair.newBuilder;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.PrivateOperationsModelProperty;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.core.privileged.processor.AnnotatedProcessor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.property.XmlExtensionModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.internal.factories.ModuleOperationMessageProcessorChainFactoryBean;
import org.mule.runtime.module.extension.internal.config.ExtensionBuildingDefinitionProvider;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class XmlExtensionBuildingDefinitionProvider implements ExtensionBuildingDefinitionProvider {

  private static final String MESSAGE_PROCESSORS = "messageProcessors";

  private final List<ComponentBuildingDefinition> definitions = new LinkedList<>();

  private Set<ExtensionModel> extensions = emptySet();

  @Override
  public void init() {
    checkState(extensions != null, "extensions cannot be null");
    extensions.stream().forEach(this::registerExtensionParsers);
  }

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    return definitions;
  }

  private void registerExtensionParsers(ExtensionModel extensionModel) {
    XmlDslModel xmlDslModel = extensionModel.getXmlDslModel();

    final Builder definitionBuilder = new Builder().withNamespace(xmlDslModel.getPrefix());
    final Builder tnsDefinitionBuilder = new Builder().withNamespace(TNS_PREFIX);
    final DslSyntaxResolver dslSyntaxResolver =
        DslSyntaxResolver.getDefault(extensionModel, DslResolvingContext.getDefault(extensions));

    if (extensionModel.getModelProperty(CustomBuildingDefinitionProviderModelProperty.class).isPresent()) {
      return;
    }

    if (extensionModel.getModelProperty(XmlExtensionModelProperty.class).isPresent()) {
      // For private operations, register its parser for use from within its same extensions
      extensionModel.getModelProperty(PrivateOperationsModelProperty.class)
          .ifPresent(privateOperations -> privateOperations.getOperationNames()
              .forEach(privateOperationName -> privateOperations.getOperationModel(privateOperationName)
                  .ifPresent(opModel -> addModuleOperationChainParser(tnsDefinitionBuilder, dslSyntaxResolver,
                                                                      extensionModel, opModel))));

      new IdempotentExtensionWalker() {

        // @Override
        // protected void onConnectionProvider(ConnectionProviderModel model) {
        // System.out.println(" -> onConnectionProvider: " + extensionModel.getName() + "." + model.getName());
        //
        // List<KeyAttributeDefinitionPair> paramsDefinitions =
        // processParametersDefinitions(definitionBuilder, dslSyntaxResolver, model);
        //
        // definitions.add(definitionBuilder
        // .withIdentifier(dslSyntaxResolver.resolve(model).getElementName())
        // .withTypeDefinition(fromType(XmlConfiguration.class))
        // // .withConstructorParameterDefinition(fromFixedValue(extensionModel).build())
        // // .withConstructorParameterDefinition(fromFixedValue(model).build())
        // // .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
        // // .withSetterParameterDefinition("expirationPolicy", fromChildConfiguration(ExpirationPolicy.class).build())
        // .withSetterParameterDefinition("parameters",
        // fromMultipleDefinitions(paramsDefinitions
        // .toArray(new KeyAttributeDefinitionPair[paramsDefinitions.size()]))
        // .build())
        // .build());
        // }

        @Override
        public void onConfiguration(ConfigurationModel configurationModel) {
          List<KeyAttributeDefinitionPair> paramsDefinitions =
              processParametersDefinitions(definitionBuilder, dslSyntaxResolver, configurationModel);

          definitions.add(definitionBuilder
              .withIdentifier(dslSyntaxResolver.resolve(configurationModel).getElementName())
              .withTypeDefinition(fromType(ConfigurationProvider.class))
              .withObjectFactoryType(XmlSdkConfigurationProviderFactory.class)
              .withConstructorParameterDefinition(fromFixedValue(extensionModel).build())
              .withConstructorParameterDefinition(fromFixedValue(configurationModel).build())
              .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
              .withSetterParameterDefinition("parameters",
                                             fromMultipleDefinitions(paramsDefinitions
                                                 .toArray(new KeyAttributeDefinitionPair[paramsDefinitions.size()]))
                                                     .build())
              .withSetterParameterDefinition("innerConfigProviders",
                                             fromChildCollectionConfiguration(ConfigurationProvider.class).build())
              .build());
        }

        @Override
        public void onOperation(OperationModel model) {
          // For public operations, register them with the extension namespace for external use, as well as with the `tns`
          // namespace for internal use
          addModuleOperationChainParser(definitionBuilder, dslSyntaxResolver, extensionModel, model);
          addModuleOperationChainParser(tnsDefinitionBuilder, dslSyntaxResolver, extensionModel, model);
        }
      }.walk(extensionModel);
    }
  }

  private void addModuleOperationChainParser(final Builder baseDefinition, DslSyntaxResolver dslSyntaxResolver,
                                             ExtensionModel extensionModel, OperationModel operationModel) {
    List<KeyAttributeDefinitionPair> paramsDefinitions =
        processParametersDefinitions(baseDefinition, dslSyntaxResolver, operationModel);

    definitions.add(baseDefinition.withIdentifier(dslSyntaxResolver.resolve(operationModel).getElementName())
        .withTypeDefinition(fromType(AnnotatedProcessor.class))
        .withObjectFactoryType(ModuleOperationMessageProcessorChainFactoryBean.class)
        .withSetterParameterDefinition("parameters",
                                       fromMultipleDefinitions(paramsDefinitions
                                           .toArray(new KeyAttributeDefinitionPair[paramsDefinitions.size()]))
                                               .build())
        .withSetterParameterDefinition("extensionModel", fromFixedValue(extensionModel).build())
        .withSetterParameterDefinition("operationModel", fromFixedValue(operationModel).build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .asPrototype().build());
  }

  private List<KeyAttributeDefinitionPair> processParametersDefinitions(final Builder baseDefinition,
                                                                        DslSyntaxResolver dslSyntaxResolver,
                                                                        ParameterizedModel operationModel) {
    List<KeyAttributeDefinitionPair> paramsDefinitions = new ArrayList<>();

    final List<ParameterModel> allParameterModels = operationModel.getAllParameterModels();
    for (ParameterModel parameterModel : allParameterModels) {
      if (parameterModel.getDslConfiguration().allowsInlineDefinition()) {
        paramsDefinitions.add(newBuilder()
            .withKey(parameterModel.getName())
            .withAttributeDefinition(fromChildConfiguration(String.class)
                .withIdentifier(dslSyntaxResolver.resolve(parameterModel)
                    .getElementName())
                .withDefaultValue(parameterModel.getDefaultValue()).build())
            .build());

        definitions.add(baseDefinition
            .withIdentifier(dslSyntaxResolver.resolve(parameterModel).getElementName())
            .withTypeDefinition(fromType(String.class))
            .build());
      } else {
        paramsDefinitions.add(newBuilder()
            .withKey(parameterModel.getName())
            .withAttributeDefinition(fromSimpleParameter(dslSyntaxResolver.resolve(parameterModel)
                .getAttributeName())
                    .withDefaultValue(parameterModel.getDefaultValue())
                    .build())
            .build());
      }
    }
    return paramsDefinitions;
  }

  @Override
  public void setExtensionModels(Set<ExtensionModel> extensionModels) {
    this.extensions = extensionModels;
  }

}
