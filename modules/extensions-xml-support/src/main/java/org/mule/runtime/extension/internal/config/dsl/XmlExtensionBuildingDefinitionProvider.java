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
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildMapConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair.newBuilder;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromMapEntryType;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.PrivateOperationsModelProperty;
import org.mule.runtime.config.internal.factories.ModuleOperationMessageProcessorChainFactoryBean;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.core.privileged.processor.AnnotatedProcessor;
import org.mule.runtime.dsl.api.component.AttributeDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.property.XmlExtensionModelProperty;
import org.mule.runtime.module.extension.internal.config.ExtensionBuildingDefinitionProvider;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

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
      extensionModel.getModelProperty(PrivateOperationsModelProperty.class)
          .ifPresent(privateOperations -> privateOperations.getOperationNames()
              .forEach(privateOperationName -> privateOperations.getOperationModel(privateOperationName)
                  .ifPresent(opModel -> addModuleOperationChainParser(tnsDefinitionBuilder, TNS_PREFIX,
                                                                      dslSyntaxResolver,
                                                                      extensionModel, opModel))));

      new IdempotentExtensionWalker() {

        @Override
        public void onConfiguration(ConfigurationModel model) {
          System.out.println(" -> config: " + extensionModel.getName() + "." + model.getName());

          // addModuleOperationChainParser(model.getName());
          // parseWith(new ConfigurationDefinitionParser(definitionBuilder, extensionModel, model, dslSyntaxResolver,
          // parsingContext));
        }

        @Override
        public void onOperation(OperationModel model) {
          // final Optional<OperationComponentModelModelProperty> modelProperty =
          // model.getModelProperty(OperationComponentModelModelProperty.class);
          // final List<ComponentModel> innerComponents = modelProperty.get().getBodyComponentModel().getInnerComponents();

          addModuleOperationChainParser(definitionBuilder,
                                        xmlDslModel.getPrefix(),
                                        dslSyntaxResolver,
                                        extensionModel, model);
        }
      }.walk(extensionModel);
    }
  }

  private void addModuleOperationChainParser(final Builder baseDefinition, String namespacePrefix,
                                             DslSyntaxResolver dslSyntaxResolver,
                                             ExtensionModel extensionModel, OperationModel operationModel) {
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

    definitions.add(baseDefinition.withIdentifier(dslSyntaxResolver.resolve(operationModel).getElementName())
        .withTypeDefinition(fromType(AnnotatedProcessor.class))
        .withObjectFactoryType(ModuleOperationMessageProcessorChainFactoryBean.class)
        .withSetterParameterDefinition("parameters",
                                       AttributeDefinition.Builder.fromMultipleDefinitions(paramsDefinitions
                                           .toArray(new KeyAttributeDefinitionPair[paramsDefinitions.size()]))
                                           .build())
        .withSetterParameterDefinition("properties", fromChildMapConfiguration(String.class, String.class)
            .withWrapperIdentifier("module-operation-properties").build())
        // .withSetterParameterDefinition("parameters", fromChildMapConfiguration(String.class, String.class)
        // .withWrapperIdentifier("module-operation-parameters").build())
        .withSetterParameterDefinition("extensionModel", fromFixedValue(extensionModel).build())
        .withSetterParameterDefinition("operationModel", fromFixedValue(operationModel).build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(Processor.class).build())
        .asPrototype().build());

    final Builder baseCoreDefinition = new Builder().withNamespace(CORE_PREFIX);

    definitions.add(baseCoreDefinition.withIdentifier("module-operation-properties")
        .withTypeDefinition(fromType(TreeMap.class)).build());
    definitions.add(baseCoreDefinition
        .withIdentifier("module-operation-property-entry")
        .withTypeDefinition(fromMapEntryType(String.class, String.class))
        .build());
    definitions.add(baseCoreDefinition.withIdentifier("module-operation-parameters")
        .withTypeDefinition(fromType(TreeMap.class)).build());
    definitions.add(baseCoreDefinition.withIdentifier("module-operation-parameter-entry")
        .withTypeDefinition(fromMapEntryType(String.class, String.class))
        .build());
  }

  @Override
  public void setExtensionModels(Set<ExtensionModel> extensionModels) {
    this.extensions = extensionModels;
  }

}
