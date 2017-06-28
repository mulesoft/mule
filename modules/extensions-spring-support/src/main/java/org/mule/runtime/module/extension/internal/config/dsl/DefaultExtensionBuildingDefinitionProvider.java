/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionXmlNamespaceInfo.EXTENSION_NAMESPACE;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.config.spring.dsl.model.extension.xml.XmlExtensionModelProperty;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.module.extension.internal.config.ExtensionBuildingDefinitionProvider;
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
import org.mule.runtime.module.extension.internal.loader.java.property.CraftedExtensionModelProperty;
import org.mule.runtime.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import com.google.common.collect.ImmutableList;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * A generic {@link ComponentBuildingDefinitionProvider} which provides definitions capable of handling all extensions registered
 * on the {@link ExtensionManager}.
 * <p>
 * It also provides static definitions for the config elements in the {@link ExtensionXmlNamespaceInfo#EXTENSION_NAMESPACE}
 * namespace, which are used for cross extension configuration
 *
 * @since 4.0
 */
public class DefaultExtensionBuildingDefinitionProvider implements ExtensionBuildingDefinitionProvider {

  private final List<ComponentBuildingDefinition> definitions = new LinkedList<>();

  private Set<ExtensionModel> extensions;

  public void setExtensions(Set<ExtensionModel> extensions) {
    this.extensions = extensions;
  }

  /**
   * Gets a hold on a {@link ExtensionManager} instance and generates the definitions.
   *
   * @throws java.lang.IllegalStateException if no extension manager could be found
   */
  @Override
  public void init() {
    checkState(extensions != null, "extensions cannot be null");
    extensions.stream()
        .filter(this::shouldRegisterExtensionParser)
        .forEach(this::registerExtensionParsers);
  }

  /**
   * Taking an {@link ExtensionModel}, it will indicate whether it must register a {@link ComponentBuildingDefinitionProvider} or
   * not.
   *
   * @param extensionModel to introspect
   * @return true if a parser must be registered, false otherwise.
   */
  private boolean shouldRegisterExtensionParser(ExtensionModel extensionModel) {
    return !extensionModel.getModelProperty(XmlExtensionModelProperty.class).isPresent()
        && !extensionModel.getModelProperty(CraftedExtensionModelProperty.class).isPresent();
  }

  /**
   * Returns the {@link ComponentBuildingDefinition}s for all the extensions plus for the elements in the
   * {@link ExtensionXmlNamespaceInfo#EXTENSION_NAMESPACE}
   */
  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    Builder baseDefinition =
        new Builder().withNamespace(EXTENSION_NAMESPACE);
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
        .withConstructorParameterDefinition(
                                            fromSimpleParameter("timeUnit", value -> TimeUnit.valueOf((String) value)).build())
        .build());

    definitions.add(baseDefinition.copy().withIdentifier("dynamic-config-policy")
        .withTypeDefinition(fromType(DynamicConfigPolicy.class))
        .withObjectFactoryType(DynamicConfigPolicyObjectFactory.class)
        .withSetterParameterDefinition("expirationPolicy", fromChildConfiguration(ExpirationPolicy.class).build())
        .build());

    definitions.add(baseDefinition.copy().withIdentifier("expiration-policy").withTypeDefinition(fromType(ExpirationPolicy.class))
        .withObjectFactoryType(ExpirationPolicyObjectFactory.class)
        .withSetterParameterDefinition("maxIdleTime", fromSimpleParameter("maxIdleTime").build())
        .withSetterParameterDefinition("timeUnit",
                                       fromSimpleParameter("timeUnit", value -> TimeUnit.valueOf((String) value))
                                           .build())
        .build());

    return definitions;
  }

  private void registerExtensionParsers(ExtensionModel extensionModel) {

    XmlDslModel xmlDslModel = extensionModel.getXmlDslModel();

    final ExtensionParsingContext parsingContext = createParsingContext(extensionModel);
    final Builder definitionBuilder =
        new Builder().withNamespace(xmlDslModel.getPrefix());
    final DslSyntaxResolver dslSyntaxResolver =
        DslSyntaxResolver.getDefault(extensionModel, DslResolvingContext.getDefault(extensions));

    final ClassLoader extensionClassLoader = getClassLoader(extensionModel);
    withContextClassLoader(extensionClassLoader, () -> {
      new IdempotentExtensionWalker() {

        @Override
        public void onConfiguration(ConfigurationModel model) {
          parseWith(new ConfigurationDefinitionParser(definitionBuilder, extensionModel, model, dslSyntaxResolver,
                                                      parsingContext));
        }

        @Override
        public void onOperation(OperationModel model) {
          parseWith(new OperationDefinitionParser(definitionBuilder, extensionModel,
                                                  model, dslSyntaxResolver, parsingContext));
        }

        @Override
        public void onConnectionProvider(ConnectionProviderModel model) {
          parseWith(new ConnectionProviderDefinitionParser(definitionBuilder, model, extensionModel, dslSyntaxResolver,
                                                           parsingContext));
        }

        @Override
        public void onSource(SourceModel model) {
          parseWith(new SourceDefinitionParser(definitionBuilder, extensionModel, model, dslSyntaxResolver,
                                               parsingContext));
        }

        @Override
        protected void onParameter(ParameterGroupModel groupModel, ParameterModel model) {
          registerTopLevelParameter(model.getType(), definitionBuilder, extensionClassLoader, dslSyntaxResolver, parsingContext);
        }


      }.walk(extensionModel);

      registerExportedTypesTopLevelParsers(extensionModel, definitionBuilder, extensionClassLoader, dslSyntaxResolver,
                                           parsingContext);

      registerSubTypes(definitionBuilder, extensionClassLoader, dslSyntaxResolver, parsingContext);
    });
  }

  private void registerSubTypes(MetadataType type, Builder definitionBuilder,
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
        if (objectType.isOpen()) {
          objectType.getOpenRestriction().get().accept(this);
        } else {
          parsingContext.getSubTypes(objectType)
              .forEach(subtype -> registerTopLevelParameter(subtype, definitionBuilder, extensionClassLoader, dslSyntaxResolver,
                                                            parsingContext));
        }
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
    Optional<DslElementSyntax> dslElement = dslSyntaxResolver.resolve(parameterType);
    if (!dslElement.isPresent() ||
        parsingContext.isRegistered(dslElement.get().getElementName(), dslElement.get().getPrefix())) {
      return;
    }

    parameterType.accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        DslElementSyntax pojoDsl = dslElement.get();
        if (pojoDsl.supportsTopLevelDeclaration() || (pojoDsl.supportsChildDeclaration() && pojoDsl.isWrapped()) ||
            parsingContext.getAllSubTypes().contains(objectType)) {

          parseWith(new ObjectTypeParameterParser(definitionBuilder, objectType, extensionClassLoader, dslSyntaxResolver,
                                                  parsingContext));
        }

        registerSubTypes(objectType, definitionBuilder, extensionClassLoader, dslSyntaxResolver, parsingContext);
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        registerTopLevelParameter(arrayType.getType(), definitionBuilder.copy(), extensionClassLoader, dslSyntaxResolver,
                                  parsingContext);
      }

      @Override
      public void visitUnion(UnionType unionType) {
        unionType.getTypes().forEach(type -> type.accept(this));
      }

    });
  }

  private void registerExportedTypesTopLevelParsers(ExtensionModel extensionModel,
                                                    Builder definitionBuilder,
                                                    ClassLoader extensionClassLoader, DslSyntaxResolver dslSyntaxResolver,
                                                    ExtensionParsingContext parsingContext) {
    registerTopLevelParameters(extensionModel.getTypes().stream(),
                               definitionBuilder,
                               extensionClassLoader,
                               dslSyntaxResolver,
                               parsingContext);
  }

  private void registerSubTypes(Builder definitionBuilder,
                                ClassLoader extensionClassLoader, DslSyntaxResolver dslSyntaxResolver,
                                ExtensionParsingContext parsingContext) {


    ImmutableList<MetadataType> mappedTypes = new ImmutableList.Builder<MetadataType>()
        .addAll(parsingContext.getAllSubTypes())
        .addAll(parsingContext.getAllBaseTypes())
        .build();

    registerTopLevelParameters(mappedTypes.stream(), definitionBuilder, extensionClassLoader,
                               dslSyntaxResolver,
                               parsingContext);
  }

  private void registerTopLevelParameters(Stream<? extends MetadataType> parameters, Builder definitionBuilder,
                                          ClassLoader extensionClassLoader, DslSyntaxResolver dslSyntaxResolver,
                                          ExtensionParsingContext parsingContext) {

    parameters.filter(IntrospectionUtils::isInstantiable)
        .forEach(subType -> registerTopLevelParameter(subType,
                                                      definitionBuilder,
                                                      extensionClassLoader,
                                                      dslSyntaxResolver,
                                                      parsingContext));

  }

  private ExtensionParsingContext createParsingContext(ExtensionModel extensionModel) {
    return new ExtensionParsingContext(extensionModel);
  }

  @Override
  public void setExtensionModels(Set<ExtensionModel> extensionModels) {
    this.extensions = extensionModels;
  }
}
