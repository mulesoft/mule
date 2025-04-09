/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.config.dsl;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair.newBuilder;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.extension.internal.ast.MacroExpansionModuleModel.DEFAULT_GLOBAL_ELEMENTS;
import static org.mule.runtime.extension.internal.ast.MacroExpansionModuleModel.TNS_PREFIX;

import static java.util.Collections.emptySet;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.visitor.BasicTypeMetadataVisitor;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.EnrichedErrorMapping;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.core.privileged.processor.AnnotatedProcessor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.property.XmlExtensionModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.extension.internal.ast.property.PrivateOperationsModelProperty;
import org.mule.runtime.extension.internal.factories.ModuleOperationMessageProcessorFactoryBean;
import org.mule.runtime.module.extension.internal.config.ExtensionBuildingDefinitionProvider;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * A {@link ComponentBuildingDefinitionProvider} which makes all definitions of extensions done with the XML SDK be declared by
 * the same definitions of the XML SDK.
 *
 * @since 4.3
 */
public class XmlExtensionBuildingDefinitionProvider implements ExtensionBuildingDefinitionProvider {

  private static final String MESSAGE_PROCESSORS = "messageProcessors";

  private final List<ComponentBuildingDefinition> definitions = new LinkedList<>();

  private Set<ExtensionModel> extensions = emptySet();
  private DslResolvingContext dslResolvingContext;
  private Function<ExtensionModel, Optional<DslSyntaxResolver>> dslSyntaxResolverLookup;

  @Override
  public void init() {
    checkState(extensions != null, "extensions cannot be null");
    extensions.stream()
        .filter(extensionModel -> !extensionModel.getModelProperty(CustomBuildingDefinitionProviderModelProperty.class)
            .isPresent()
            && extensionModel.getModelProperty(XmlExtensionModelProperty.class).isPresent())
        .forEach(this::registerExtensionParsers);
  }

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    return definitions;
  }

  private void registerExtensionParsers(ExtensionModel extensionModel) {
    XmlDslModel xmlDslModel = extensionModel.getXmlDslModel();

    final Builder definitionBuilder = new Builder().withNamespace(xmlDslModel.getPrefix());
    final Builder tnsDefinitionBuilder = new Builder().withNamespace(TNS_PREFIX);
    final DslSyntaxResolver dslSyntaxResolver = dslSyntaxResolverLookup.apply(extensionModel)
        .orElseGet(() -> DslSyntaxResolver.getDefault(extensionModel, dslResolvingContext));

    // For private operations, register its parser for use from within its same extensions
    extensionModel.getModelProperty(PrivateOperationsModelProperty.class)
        .ifPresent(privateOperations -> privateOperations.getOperationNames()
            .forEach(privateOperationName -> privateOperations.getOperationModel(privateOperationName)
                .ifPresent(opModel -> addModuleOperationChainParser(tnsDefinitionBuilder, dslSyntaxResolver,
                                                                    extensionModel, opModel))));

    AtomicBoolean configPresent = new AtomicBoolean(false);

    new IdempotentExtensionWalker() {

      @SuppressWarnings("unchecked")
      @Override
      public void onConfiguration(ConfigurationModel configurationModel) {
        configPresent.set(true);

        List<KeyAttributeDefinitionPair> paramsDefinitions =
            processParametersDefinitions(definitionBuilder, dslSyntaxResolver, configurationModel);

        definitions.add(definitionBuilder
            .withIdentifier(dslSyntaxResolver.resolve(configurationModel).getElementName())
            .withTypeDefinition(fromType(ConfigurationProvider.class))
            .withObjectFactoryType(XmlSdkConfigurationProviderFactory.class)
            .withConstructorParameterDefinition(fromFixedValue(extensionModel).build())
            .withConstructorParameterDefinition(fromFixedValue(configurationModel).build())
            .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
            .withConstructorParameterDefinition(fromReferenceObject(Registry.class).build())
            .withSetterParameterDefinition("parameters",
                                           fromMultipleDefinitions(paramsDefinitions
                                               .toArray(
                                                        new KeyAttributeDefinitionPair[paramsDefinitions.size()]))
                                               .build())
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

    if (!configPresent.get()) {
      definitions.add(definitionBuilder
          .withIdentifier(DEFAULT_GLOBAL_ELEMENTS)
          .withTypeDefinition(fromType(ConfigurationProvider.class))
          .withObjectFactoryType(XmlSdkConfigurationProviderFactory.class)
          .withConstructorParameterDefinition(fromFixedValue(extensionModel).build())
          .withConstructorParameterDefinition(fromFixedValue(null).build())
          .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
          .withConstructorParameterDefinition(fromReferenceObject(Registry.class).build())
          .withSetterParameterDefinition("parameters", fromFixedValue(null).build())
          .build());
    }
  }

  private void addModuleOperationChainParser(final Builder baseDefinition, DslSyntaxResolver dslSyntaxResolver,
                                             ExtensionModel extensionModel, OperationModel operationModel) {
    List<KeyAttributeDefinitionPair> paramsDefinitions =
        processParametersDefinitions(baseDefinition, dslSyntaxResolver, operationModel);

    definitions.add(baseDefinition.withIdentifier(dslSyntaxResolver.resolve(operationModel).getElementName())
        .withTypeDefinition(fromType(AnnotatedProcessor.class))
        .withObjectFactoryType(ModuleOperationMessageProcessorFactoryBean.class)
        .withSetterParameterDefinition("parameters",
                                       fromMultipleDefinitions(paramsDefinitions
                                           .toArray(
                                                    new KeyAttributeDefinitionPair[paramsDefinitions.size()]))
                                           .build())
        .withSetterParameterDefinition("extensionModel", fromFixedValue(extensionModel).build())
        .withSetterParameterDefinition("operationModel", fromFixedValue(operationModel).build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS,
                                       fromChildCollectionConfiguration(Processor.class).build())
        .withSetterParameterDefinition(ERROR_MAPPINGS_PARAMETER_NAME,
                                       fromChildCollectionConfiguration(EnrichedErrorMapping.class).build())
        .asPrototype().build());
  }

  private List<KeyAttributeDefinitionPair> processParametersDefinitions(final Builder baseDefinition,
                                                                        DslSyntaxResolver dslSyntaxResolver,
                                                                        ParameterizedModel operationModel) {
    List<KeyAttributeDefinitionPair> paramsDefinitions = new ArrayList<>();

    final List<ParameterModel> allParameterModels = operationModel.getAllParameterModels();
    for (ParameterModel parameterModel : allParameterModels) {
      if (parameterModel.getDslConfiguration().allowsInlineDefinition()) {
        processInlineParameterDefinition(baseDefinition, dslSyntaxResolver, paramsDefinitions, parameterModel);
      } else {
        paramsDefinitions.add(newBuilder()
            .withKey(parameterModel.getName())
            .withAttributeDefinition(fromSimpleParameter(parameterModel.getName()).build())
            .build());
      }
    }
    return paramsDefinitions;
  }

  @Override
  public void setExtensionModels(Set<ExtensionModel> extensionModels) {
    this.extensions = extensionModels;
  }

  @Override
  public void setDslResolvingContext(DslResolvingContext dslResolvingContext) {
    this.dslResolvingContext = dslResolvingContext;
  }

  @Override
  public void setDslSyntaxResolverLookup(Function<ExtensionModel, Optional<DslSyntaxResolver>> dslSyntaxResolverLookup) {
    this.dslSyntaxResolverLookup = dslSyntaxResolverLookup;
  }

  private void processInlineParameterDefinition(Builder baseDefinition, DslSyntaxResolver dslSyntaxResolver,
                                                List<KeyAttributeDefinitionPair> paramsDefinitions,
                                                ParameterModel parameterModel) {
    Class type = isBehaviour(parameterModel) ? getType(parameterModel.getType()) : String.class;
    DslElementSyntax elementSyntax = dslSyntaxResolver.resolve(parameterModel);

    // Adds the inline parameter definition
    paramsDefinitions.add(newBuilder()
        .withKey(parameterModel.getName())
        .withAttributeDefinition(fromSimpleParameter(parameterModel.getName()).build())
        .build());

    // Adds the parameter definition parser
    definitions.add(baseDefinition
        .withIdentifier(elementSyntax.getElementName())
        .withTypeDefinition(fromType(type)).build());

    if (isBehaviour(parameterModel)) {
      paramsDefinitions.add(newBuilder().withKey(parameterModel.getName())
          .withAttributeDefinition(fromChildConfiguration(type).withIdentifier(elementSyntax.getElementName()).build()).build());

      // For behaviour inline parameters, also add the definition parser associated to its type
      registerComponentBuildingDefinitionType(baseDefinition, parameterModel, elementSyntax);
    }
  }

  private boolean isBehaviour(ParameterModel parameterModel) {
    return parameterModel.getRole() == BEHAVIOUR;
  }

  private void registerComponentBuildingDefinitionType(Builder baseDefinition, ParameterModel parameterModel,
                                                       DslElementSyntax parameterDsl) {
    parameterModel.getType().accept(new MetadataTypeVisitor() {

      @Override
      public void visitArrayType(ArrayType arrayType) {
        Optional<DslElementSyntax> itemDsl = parameterDsl.getGeneric(arrayType.getType());
        if (parameterDsl.supportsChildDeclaration() && itemDsl.isPresent()) {
          arrayType.getType().accept(new BasicTypeMetadataVisitor() {

            @Override
            protected void visitBasicType(MetadataType metadataType) {
              definitions.add(baseDefinition
                  .withIdentifier(itemDsl.get().getElementName())
                  .withNamespace(itemDsl.get().getPrefix())
                  .withTypeDefinition(
                                      fromType(ExtensionMetadataTypeUtils.getType(metadataType).orElse(Object.class)))
                  .build());
            }
          });
        }
      }
    });
  }
}
