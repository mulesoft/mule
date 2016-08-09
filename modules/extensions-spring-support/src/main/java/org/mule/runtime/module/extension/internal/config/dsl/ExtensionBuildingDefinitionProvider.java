/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromType;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionXmlNamespaceInfo.EXTENSION_NAMESPACE;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.isInstantiable;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition.Builder;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinitionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.property.ExportModelProperty;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.extension.api.util.SubTypesMappingContainer;
import org.mule.runtime.extension.xml.dsl.api.DslElementSyntax;
import org.mule.runtime.extension.xml.dsl.api.property.XmlModelProperty;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.ExtensionConfig;
import org.mule.runtime.module.extension.internal.config.dsl.config.ConfigurationDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.connection.ConnectionProviderDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.infrastructure.DynamicConfigPolicyObjectFactory;
import org.mule.runtime.module.extension.internal.config.dsl.infrastructure.DynamicConfigurationExpiration;
import org.mule.runtime.module.extension.internal.config.dsl.infrastructure.DynamicConfigurationExpirationObjectFactory;
import org.mule.runtime.module.extension.internal.config.dsl.infrastructure.ExpirationPolicyObjectFactory;
import org.mule.runtime.module.extension.internal.config.dsl.infrastructure.ExtensionConfigObjectFactory;
import org.mule.runtime.module.extension.internal.config.dsl.operation.OperationDefinitionParser;
import org.mule.runtime.module.extension.internal.config.dsl.parameter.ObjectTypeParameterParser;
import org.mule.runtime.module.extension.internal.config.dsl.source.SourceDefinitionParser;
import org.mule.runtime.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.runtime.module.extension.internal.util.IdempotentExtensionWalker;
import org.mule.runtime.module.extension.internal.util.MetadataTypeUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A generic {@link ComponentBuildingDefinitionProvider} which provides definitions capable of handling all extensions registered
 * on the {@link ExtensionManager}.
 * <p>
 * It also provides static definitions for the config elements in the {@link ExtensionXmlNamespaceInfo#EXTENSION_NAMESPACE}
 * namespace, which are used for cross extension configuration
 *
 * @since 4.0
 */
public class ExtensionBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  private final List<ComponentBuildingDefinition> definitions = new LinkedList<>();
  private ExtensionManager extensionManager;
  private MuleContext muleContext;

  /**
   * Gets a hold on a {@link ExtensionManager} instance and generates the definitions.
   *
   * @throws java.lang.IllegalStateException if no extension manager could be found
   */
  @Override
  public void init(MuleContext muleContext) {
    this.muleContext = muleContext;
    extensionManager = muleContext.getExtensionManager();
    if (extensionManager != null) {
      extensionManager.getExtensions().forEach(this::registerExtensionParsers);
    }
  }

  /**
   * Returns the {@link ComponentBuildingDefinition}s for all the extensions plus for the elements in the
   * {@link ExtensionXmlNamespaceInfo#EXTENSION_NAMESPACE}
   */
  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    Builder baseDefinition = new Builder().withNamespace(EXTENSION_NAMESPACE);
    definitions.add(
                    baseDefinition.copy().withIdentifier("extensions-config").withTypeDefinition(fromType(ExtensionConfig.class))
                        .withObjectFactoryType(ExtensionConfigObjectFactory.class)
                        .withSetterParameterDefinition("dynamicConfigurationExpiration",
                                                       fromChildConfiguration(DynamicConfigurationExpiration.class).build())
                        .build());
    definitions.add(baseDefinition.copy().withIdentifier("dynamic-configuration-expiration")
        .withTypeDefinition(fromType(DynamicConfigurationExpiration.class))
        .withObjectFactoryType(DynamicConfigurationExpirationObjectFactory.class)
        .withConstructorParameterDefinition(fromSimpleParameter("frequency").build())
        .withConstructorParameterDefinition(fromSimpleParameter("timeUnit", value -> TimeUnit.valueOf((String) value)).build())
        .build());

    definitions.add(baseDefinition.copy().withIdentifier("dynamic-config-policy")
        .withTypeDefinition(fromType(DynamicConfigPolicy.class)).withObjectFactoryType(DynamicConfigPolicyObjectFactory.class)
        .withSetterParameterDefinition("expirationPolicy", fromChildConfiguration(ExpirationPolicy.class).build()).build());

    definitions.add(baseDefinition.copy().withIdentifier("expiration-policy").withTypeDefinition(fromType(ExpirationPolicy.class))
        .withObjectFactoryType(ExpirationPolicyObjectFactory.class)
        .withSetterParameterDefinition("maxIdleTime", fromSimpleParameter("maxIdleTime").build())
        .withSetterParameterDefinition("timeUnit",
                                       fromSimpleParameter("timeUnit", value -> TimeUnit.valueOf((String) value)).build())
        .build());

    return definitions;
  }

  private void registerExtensionParsers(ExtensionModel extensionModel) {

    Optional<XmlModelProperty> xmlModelProperty = extensionModel.getModelProperty(XmlModelProperty.class);
    if (!xmlModelProperty.isPresent()) {
      return;
    }

    final ExtensionParsingContext parsingContext = new ExtensionParsingContext();
    final Builder definitionBuilder = new Builder().withNamespace(xmlModelProperty.get().getNamespace());
    Optional<SubTypesModelProperty> subTypesProperty = extensionModel.getModelProperty(SubTypesModelProperty.class);
    final SubTypesMappingContainer typeMapping =
        new SubTypesMappingContainer(subTypesProperty.isPresent() ? subTypesProperty.get().getSubTypesMapping() : emptyMap());
    final DslSyntaxResolver dslSyntaxResolver = new DslSyntaxResolver(extensionModel);

    final ClassLoader extensionClassLoader = getClassLoader(extensionModel);
    withContextClassLoader(extensionClassLoader, () -> {
      new IdempotentExtensionWalker() {

        @Override
        public void onConfiguration(ConfigurationModel model) {
          parseWith(new ConfigurationDefinitionParser(definitionBuilder, (RuntimeConfigurationModel) model, dslSyntaxResolver,
                                                      muleContext, parsingContext));
        }

        @Override
        public void onOperation(OperationModel model) {
          parseWith(new OperationDefinitionParser(definitionBuilder, (RuntimeExtensionModel) extensionModel,
                                                  (RuntimeOperationModel) model, dslSyntaxResolver, muleContext, parsingContext));
        }

        @Override
        public void onConnectionProvider(ConnectionProviderModel model) {
          parseWith(new ConnectionProviderDefinitionParser(definitionBuilder, model, dslSyntaxResolver, muleContext,
                                                           parsingContext));
        }

        @Override
        public void onSource(SourceModel model) {
          parseWith(new SourceDefinitionParser(definitionBuilder, (RuntimeExtensionModel) extensionModel,
                                               (RuntimeSourceModel) model, dslSyntaxResolver, muleContext, parsingContext));
        }

        @Override
        public void onParameter(ParameterModel model) {
          registerSubTypes(typeMapping, model.getType(), definitionBuilder, extensionClassLoader, dslSyntaxResolver,
                           parsingContext);
          registerTopLevelParameter(model.getType(), definitionBuilder, extensionClassLoader, dslSyntaxResolver, parsingContext);
        }


      }.walk(extensionModel);

      registerExportedTypesTopLevelParsers(extensionModel, definitionBuilder, extensionClassLoader, dslSyntaxResolver,
                                           parsingContext);
    });
  }

  private void registerSubTypes(SubTypesMappingContainer typeMapping, MetadataType type, Builder definitionBuilder,
                                ClassLoader extensionClassLoader, DslSyntaxResolver dslSyntaxResolver,
                                ExtensionParsingContext parsingContext) {
    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitUnion(UnionType unionType) {
        unionType.getTypes().forEach(type -> type.accept(this));
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        arrayType.getType().accept(this);
      }

      @Override
      public void visitObject(ObjectType objectType) {
        typeMapping.getSubTypes(objectType)
            .forEach(subtype -> registerTopLevelParameter(subtype, definitionBuilder, extensionClassLoader, dslSyntaxResolver,
                                                          parsingContext));
      }

      @Override
      public void visitDictionary(DictionaryType dictionaryType) {
        dictionaryType.getValueType().accept(this);
      }
    });
  }

  private void parseWith(ExtensionDefinitionParser parser) {
    try {
      definitions.addAll(parser.parse());
    } catch (ConfigurationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private void registerTopLevelParameter(final MetadataType parameterType, Builder definitionBuilder,
                                         ClassLoader extensionClassLoader, DslSyntaxResolver dslSyntaxResolver,
                                         ExtensionParsingContext parsingContext) {
    DslElementSyntax elementDsl = dslSyntaxResolver.resolve(parameterType);
    if (parsingContext.isRegistered(elementDsl.getElementName(), elementDsl.getNamespace())) {
      return;
    }

    parameterType.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        if (isInstantiable(objectType)) {
          parseWith(new ObjectTypeParameterParser(definitionBuilder, objectType, extensionClassLoader, dslSyntaxResolver,
                                                  parsingContext));
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        registerTopLevelParameter(arrayType.getType(), definitionBuilder.copy(), extensionClassLoader, dslSyntaxResolver,
                                  parsingContext);
      }

      @Override
      public void visitDictionary(DictionaryType dictionaryType) {
        MetadataType keyType = dictionaryType.getKeyType();
        keyType.accept(this);
        registerTopLevelParameter(keyType, definitionBuilder.copy(), extensionClassLoader, dslSyntaxResolver, parsingContext);
      }

      @Override
      public void visitUnion(UnionType unionType) {
        unionType.getTypes().forEach(type -> type.accept(this));
      }

    });
  }

  private void registerExportedTypesTopLevelParsers(ExtensionModel extensionModel, Builder definitionBuilder,
                                                    ClassLoader extensionClassLoader, DslSyntaxResolver dslSyntaxResolver,
                                                    ExtensionParsingContext parsingContext) {
    extensionModel.getModelProperty(ExportModelProperty.class).map(ExportModelProperty::getExportedTypes)
        .ifPresent(exportedTypes -> exportedTypes.stream().filter(MetadataTypeUtils::isInstantiable)
            .filter(MetadataTypeUtils::hasExposedFields).forEach(exportedType -> registerTopLevelParameter(exportedType,
                                                                                                           definitionBuilder,
                                                                                                           extensionClassLoader,
                                                                                                           dslSyntaxResolver,
                                                                                                           parsingContext)));
  }
}
