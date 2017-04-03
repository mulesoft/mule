/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.declaration;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getLocalPart;
import static org.mule.runtime.api.app.declaration.fluent.ElementDeclarer.forExtension;
import static org.mule.runtime.api.app.declaration.fluent.ElementDeclarer.newObjectValue;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.ExtensionConstants.DISABLE_CONNECTION_VALIDATION_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_BYTE_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isInfrastructure;
import static org.mule.runtime.extension.internal.dsl.syntax.DslSyntaxUtils.getId;
import static org.mule.runtime.extension.internal.dsl.syntax.DslSyntaxUtils.isExtensible;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.FLOW_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.NAME_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.POOLING_PROFILE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_FOREVER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_CONTEXT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.api.app.declaration.ComponentElementDeclaration;
import org.mule.runtime.api.app.declaration.ParameterValue;
import org.mule.runtime.api.app.declaration.RouteElementDeclaration;
import org.mule.runtime.api.app.declaration.fluent.ArtifactDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ComponentElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ConfigurationElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ConnectionElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.FlowElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ParameterListValue;
import org.mule.runtime.api.app.declaration.fluent.ParameterObjectValue;
import org.mule.runtime.api.app.declaration.fluent.ParameterSimpleValue;
import org.mule.runtime.api.app.declaration.fluent.ParameterizedBuilder;
import org.mule.runtime.api.app.declaration.fluent.RouteElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.RouterElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ScopeElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.TopLevelParameterDeclarer;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.operation.RouteModel;
import org.mule.runtime.api.meta.model.operation.RouterModel;
import org.mule.runtime.api.meta.model.operation.ScopeModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.config.spring.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.dsl.model.XmlArtifactDeclarationLoader;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.SimpleConfigAttribute;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationServiceRegistry;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.w3c.dom.Document;

/**
 * Default implementation of a {@link XmlArtifactDeclarationLoader}
 *
 * @since 4.0
 */
public class DefaultXmlArtifactDeclarationLoader implements XmlArtifactDeclarationLoader {

  private final DslResolvingContext context;
  private final Map<ExtensionModel, DslSyntaxResolver> resolvers;
  private final Map<String, ExtensionModel> extensionsByNamespace = new HashMap<>();

  public DefaultXmlArtifactDeclarationLoader(DslResolvingContext context) {
    this.context = context;
    this.resolvers = context.getExtensions().stream()
        .collect(toMap(e -> e, e -> DslSyntaxResolver.getDefault(e, context)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ArtifactDeclaration load(String name, InputStream configResource) {

    context.getExtensions().forEach(e -> extensionsByNamespace.put(e.getXmlDslModel().getPrefix(), e));

    ConfigLine configLine = loadArtifactConfig(name, configResource);

    return declareArtifact(configLine);
  }

  private ConfigLine loadArtifactConfig(String name, InputStream resource) {
    checkArgument(resource != null, "The given application was not found as resource");

    Document document = new XmlConfigurationDocumentLoader().loadDocument(empty(), name, resource);

    return new XmlApplicationParser(new XmlApplicationServiceRegistry(new SpiServiceRegistry(), context))
        .parse(document.getDocumentElement())
        .orElseThrow(
                     () -> new MuleRuntimeException(createStaticMessage("Could not load load a Configuration from the given resource")));
  }

  private ArtifactDeclaration declareArtifact(ConfigLine configLine) {
    ArtifactDeclarer artifactDeclarer = ElementDeclarer.newArtifact();
    configLine.getConfigAttributes().values().forEach(a -> artifactDeclarer.withCustomParameter(a.getName(), a.getValue()));
    configLine.getChildren().forEach(line -> declareElement(line, artifactDeclarer));
    return artifactDeclarer.getDeclaration();
  }

  private void declareElement(ConfigLine configLine, ArtifactDeclarer artifactDeclarer) {
    if (configLine.getIdentifier().equals(FLOW_ELEMENT_IDENTIFIER)) {
      declareFlow(configLine, artifactDeclarer);
    } else {
      declareCustomGlobalElement(configLine, artifactDeclarer);
    }
  }

  private void declareCustomGlobalElement(final ConfigLine configLine, final ArtifactDeclarer artifactDeclarer) {
    final ExtensionModel ownerExtension = getExtensionModel(configLine);
    final ElementDeclarer extensionElementsDeclarer = forExtension(ownerExtension.getName());
    final DslSyntaxResolver dsl = resolvers.get(ownerExtension);

    Reference<Boolean> alreadyDeclared = new Reference<>(false);
    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        if (elementDsl.getElementName().equals(configLine.getIdentifier())) {
          ConfigurationElementDeclarer configurationDeclarer = extensionElementsDeclarer.newConfiguration(model.getName());

          configurationDeclarer.withRefName(getDeclaredName(configLine));

          Map<String, SimpleConfigAttribute> attributes = configLine.getConfigAttributes().values().stream()
              .filter(a -> !a.getName().equals(NAME_ATTRIBUTE_NAME))
              .collect(toMap(SimpleConfigAttribute::getName, a -> a));


          List<ConfigLine> configComplexParameters = configLine.getChildren().stream()
              .filter(config -> declareAsConnectionProvider(ownerExtension, model, configurationDeclarer, config))
              .collect(toList());


          declareParameterizedComponent(model, elementDsl, configurationDeclarer, attributes, configComplexParameters);
          artifactDeclarer.withGlobalElement(configurationDeclarer.getDeclaration());
          alreadyDeclared.set(true);
          stop();
        }
      }

      private boolean declareAsConnectionProvider(ExtensionModel ownerExtension,
                                                  ConfigurationModel model, ConfigurationElementDeclarer configurationDeclarer,
                                                  ConfigLine config) {
        Optional<ConnectionProviderModel> connectionProvider = model.getConnectionProviders().stream()
            .filter(cp -> dsl.resolve(cp).getElementName().equals(config.getIdentifier()))
            .findFirst();

        if (!connectionProvider.isPresent()) {
          connectionProvider = ownerExtension.getConnectionProviders().stream()
              .filter(cp -> dsl.resolve(cp).getElementName().equals(config.getIdentifier()))
              .findFirst();
        }

        if (!connectionProvider.isPresent()) {
          return true;
        }

        ConnectionProviderModel providerModel = connectionProvider.get();
        ConnectionElementDeclarer connectionDeclarer = extensionElementsDeclarer.newConnection(providerModel.getName());
        declareParameterizedComponent(providerModel, dsl.resolve(providerModel), connectionDeclarer,
                                      config.getConfigAttributes(), config.getChildren());

        configurationDeclarer.withConnection(connectionDeclarer.getDeclaration());
        return false;
      }

    }.walk(ownerExtension);

    if (!alreadyDeclared.get()) {
      ownerExtension.getTypes().stream()
          .filter(type -> dsl.resolve(type).map(typeDsl -> typeDsl.getElementName().equals(configLine.getIdentifier()))
              .orElse(false))
          .findFirst()
          .ifPresent(type -> {
            String id = getId(type);
            TopLevelParameterDeclarer topLevelParameter = extensionElementsDeclarer.newGlobalParameter(id)
                .withRefName(getDeclaredName(configLine));

            type.accept(getParameterDeclarerVisitor(configLine, dsl.resolve(type).get(),
                                                    value -> topLevelParameter.withValue((ParameterObjectValue) value)));

            artifactDeclarer.withGlobalElement(topLevelParameter.getDeclaration());
          });
    }

  }

  private String getDeclaredName(ConfigLine configLine) {
    return configLine.getConfigAttributes().get(NAME_ATTRIBUTE_NAME).getValue();
  }

  private void declareFlow(ConfigLine configLine, ArtifactDeclarer artifactDeclarer) {
    final FlowElementDeclarer flow = ElementDeclarer.newFlow(getDeclaredName(configLine));
    copyExplicitAttributes(configLine.getConfigAttributes(), flow);

    configLine.getChildren().forEach(line -> {
      final ExtensionModel ownerExtension = getExtensionModel(line);
      final ElementDeclarer extensionElementsDeclarer = forExtension(ownerExtension.getName());
      final DslSyntaxResolver dsl = resolvers.get(ownerExtension);

      getComponentDeclaringWalker(flow::withComponent, line, extensionElementsDeclarer, dsl).walk(ownerExtension);
    });

    artifactDeclarer.withGlobalElement(flow.getDeclaration());
  }

  private ExtensionModel getExtensionModel(ConfigLine line) {

    String namespace = getNamespace(line);
    ExtensionModel extensionModel = extensionsByNamespace.get(namespace);
    if (extensionModel == null) {
      throw new MuleRuntimeException(createStaticMessage("Missing Extension model in the context for namespace [" + namespace
          + "]"));
    }

    return extensionModel;
  }

  private ExtensionWalker getComponentDeclaringWalker(final Consumer<ComponentElementDeclaration> declarationConsumer,
                                                      final ConfigLine line,
                                                      final ElementDeclarer extensionElementsDeclarer,
                                                      final DslSyntaxResolver dsl) {
    return new ExtensionWalker() {

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        declareComponent(model, e -> e.newOperation(model.getName()));
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        declareComponent(model, e -> e.newSource(model.getName()));
      }

      @Override
      protected void onScope(HasOperationModels owner, ScopeModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        if (elementDsl.getElementName().equals(line.getIdentifier())) {
          ScopeElementDeclarer scope = extensionElementsDeclarer.newScope(model.getName());

          if (line.getConfigAttributes().get(CONFIG_ATTRIBUTE_NAME) != null) {
            scope.withConfig(line.getConfigAttributes().get(CONFIG_ATTRIBUTE_NAME).getValue());
          }

          declareParameterizedComponent(model, elementDsl, scope, line.getConfigAttributes(), line.getChildren());

          line.getChildren().forEach(child -> {
            ExtensionModel extensionModel = getExtensionModel(child);
            getComponentDeclaringWalker(scope::withComponent, child, forExtension(extensionModel.getName()), dsl)
                .walk(extensionModel);
          });

          declarationConsumer.accept(scope.getDeclaration());
          stop();
        }
      }

      @Override
      protected void onRouter(HasOperationModels owner, RouterModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);

        if (elementDsl.getElementName().equals(line.getIdentifier())) {
          RouterElementDeclarer router = extensionElementsDeclarer.newRouter(model.getName());

          if (line.getConfigAttributes().get(CONFIG_ATTRIBUTE_NAME) != null) {
            router.withConfig(line.getConfigAttributes().get(CONFIG_ATTRIBUTE_NAME).getValue());
          }

          declareParameterizedComponent(model, elementDsl, router, line.getConfigAttributes(), line.getChildren());

          model.getRouteModels()
              .forEach(routeModel -> declareRoute(routeModel, elementDsl, line, extensionElementsDeclarer, dsl)
                  .ifPresent(router::withRoute));

          declarationConsumer.accept(router.getDeclaration());
          stop();
        }
      }

      private void declareComponent(ComponentModel model,
                                    Function<ElementDeclarer, ComponentElementDeclarer> declarerProvider) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        if (elementDsl.getElementName().equals(line.getIdentifier())) {
          ComponentElementDeclarer declarer = declarerProvider.apply(extensionElementsDeclarer);

          if (line.getConfigAttributes().get(CONFIG_ATTRIBUTE_NAME) != null) {
            declarer.withConfig(line.getConfigAttributes().get(CONFIG_ATTRIBUTE_NAME).getValue());
          }

          declareParameterizedComponent(model, elementDsl, declarer, line.getConfigAttributes(), line.getChildren());
          declarationConsumer.accept((ComponentElementDeclaration) declarer.getDeclaration());
          stop();
        }
      }

    };
  }

  private Optional<RouteElementDeclaration> declareRoute(RouteModel model, DslElementSyntax elementDsl, ConfigLine line,
                                                         ElementDeclarer elementsDeclarer, DslSyntaxResolver dsl) {
    Reference<RouteElementDeclaration> declaration = new Reference<>();
    elementDsl.getChild(model.getName())
        .ifPresent(routeDsl -> line.getChildren().stream()
            .filter(child -> child.getIdentifier().equals(routeDsl.getElementName()))
            .findFirst()
            .ifPresent(routeConfig -> {
              RouteElementDeclarer route = elementsDeclarer.newRoute(model.getName());
              declareParameterizedComponent(model, routeDsl, route,
                                            routeConfig.getConfigAttributes(), routeConfig.getChildren());
              routeConfig.getChildren()
                  .forEach(child -> {
                    ExtensionModel extensionModel = getExtensionModel(child);
                    getComponentDeclaringWalker(route::withComponent, child, forExtension(extensionModel.getName()), dsl)
                        .walk(extensionModel);
                  });

              declaration.set(route.getDeclaration());
            }));

    return Optional.ofNullable(declaration.get());
  }

  private void declareParameterizedComponent(ParameterizedModel model, DslElementSyntax elementDsl,
                                             ParameterizedBuilder<String, ParameterValue, ?> declarer,
                                             Map<String, SimpleConfigAttribute> configAttributes,
                                             List<ConfigLine> children) {
    copyExplicitAttributes(configAttributes, declarer);
    declareChildParameters(model, elementDsl, children, declarer);
  }

  private void declareChildParameters(ParameterizedModel model, DslElementSyntax modelDsl, List<ConfigLine> children,
                                      ParameterizedBuilder<String, ParameterValue, ?> declarer) {

    List<ParameterModel> inlineGroupedParameters = model.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .peek(group -> modelDsl.getChild(group.getName())
            .ifPresent(groupDsl -> children.stream()
                .filter(c -> c.getIdentifier().equals(groupDsl.getElementName()))
                .findFirst()
                .ifPresent(groupConfig -> declareInlineGroup(group, groupDsl, groupConfig, declarer))))
        .flatMap(g -> g.getParameterModels().stream())
        .collect(toList());

    model.getAllParameterModels().stream()
        .filter(param -> !inlineGroupedParameters.contains(param))
        .forEach(param -> modelDsl.getChild(param.getName())
            .ifPresent(paramDsl -> {
              if (isInfrastructure(param)) {
                handleInfrastructure(param, children, declarer);
              } else {
                children.stream()
                    .filter(c -> c.getIdentifier().equals(paramDsl.getElementName()))
                    .findFirst()
                    .ifPresent(paramConfig -> param.getType()
                        .accept(getParameterDeclarerVisitor(paramConfig, paramDsl,
                                                            value -> declarer.withParameter(param.getName(), value))));
              }
            }));
  }

  private void declareInlineGroup(ParameterGroupModel model, DslElementSyntax dsl, ConfigLine config,
                                  ParameterizedBuilder<String, ParameterValue, ?> groupContainer) {

    ParameterObjectValue.Builder builder = ElementDeclarer.newObjectValue();
    copyExplicitAttributes(config.getConfigAttributes(), builder);
    declareComplexParameterValue(model, dsl, config, builder);
    groupContainer.withParameter(model.getName(), builder.build());
  }

  private void declareComplexParameterValue(ParameterGroupModel group, DslElementSyntax groupDsl,
                                            final ConfigLine config, ParameterObjectValue.Builder groupBuilder) {
    group.getParameterModels().stream()
        .filter(parameter -> groupDsl.getChild(parameter.getName())
            .map(dsl -> dsl.getElementName().equals(config.getIdentifier()))
            .orElse(false))
        .findFirst()
        .ifPresent(parameter -> parameter.getType().accept(
                                                           getParameterDeclarerVisitor(config,
                                                                                       groupDsl.getChild(parameter.getName())
                                                                                           .get(),
                                                                                       value -> groupBuilder
                                                                                           .withParameter(parameter.getName(),
                                                                                                          value))));
  }

  private MetadataTypeVisitor getParameterDeclarerVisitor(final ConfigLine config,
                                                          final DslElementSyntax paramDsl,
                                                          final Consumer<ParameterValue> valueConsumer) {
    return new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        if (config.getTextContent() != null) {
          valueConsumer.accept(ParameterSimpleValue.of(config.getTextContent()));
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        ParameterListValue.Builder listBuilder = ElementDeclarer.newListValue();
        config.getChildren()
            .forEach(item -> arrayType.getType().accept(
                                                        getParameterDeclarerVisitor(item,
                                                                                    paramDsl.getGeneric(arrayType.getType())
                                                                                        .get(),
                                                                                    listBuilder::withValue)));

        valueConsumer.accept(listBuilder.build());
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (config.getConfigAttributes().isEmpty() && config.getChildren().isEmpty()) {
          defaultVisit(objectType);
          return;
        }

        ParameterObjectValue.Builder objectValue = ElementDeclarer.newObjectValue();
        if (isMap(objectType)) {
          createMapValue(objectValue, config);
        } else {
          if (paramDsl.isWrapped()) {
            if (config.getChildren().size() == 1) {
              createWrappedObject(objectType, objectValue, config);
            }
          } else {
            createObjectValueFromType(objectType, objectValue, config, paramDsl);
          }
        }
        valueConsumer.accept(objectValue.build());
      }
    };
  }

  private void createMapValue(ParameterObjectValue.Builder objectValue, ConfigLine config) {
    config.getChildren().stream()
        .map(ConfigLine::getConfigAttributes)
        .forEach(entry -> {
          SimpleConfigAttribute entryKey = entry.get(KEY_ATTRIBUTE_NAME);
          SimpleConfigAttribute entryValue = entry.get(VALUE_ATTRIBUTE_NAME);
          if (entryKey != null && entryValue != null) {
            objectValue.withParameter(entryKey.getValue(), entryValue.getValue());
          }
        });
  }

  private void createWrappedObject(ObjectType objectType, ParameterObjectValue.Builder objectValue, ConfigLine config) {
    ConfigLine wrappedConfig = config.getChildren().get(0);
    DslSyntaxResolver wrappedElementResolver = resolvers.get(extensionsByNamespace.get(wrappedConfig.getNamespace()));
    Set<ObjectType> subTypes = context.getTypeCatalog().getSubTypes(objectType);
    if (!subTypes.isEmpty()) {
      subTypes.stream()
          .filter(subType -> wrappedElementResolver.resolve(subType)
              .map(dsl -> dsl.getElementName().equals(wrappedConfig.getIdentifier()))
              .orElse(false))
          .findFirst()
          .ifPresent(subType -> createObjectValueFromType(subType, objectValue, wrappedConfig,
                                                          wrappedElementResolver.resolve(subType).get()));

      // TODO MULE-12002: Revisit DslSyntaxUtils as part of the API
    } else if (isExtensible(objectType)) {
      createObjectValueFromType(objectType, objectValue, wrappedConfig, wrappedElementResolver.resolve(objectType).get());
    }
  }

  private void createObjectValueFromType(ObjectType objectType, ParameterObjectValue.Builder objectValue, ConfigLine config,
                                         DslElementSyntax paramDsl) {

    // TODO MULE-12002: Revisit DslSyntaxUtils as part of the API
    objectValue.ofType(getId(objectType));
    copyExplicitAttributes(config.getConfigAttributes(), objectValue);

    config.getChildren().forEach(fieldConfig -> objectType.getFields().stream()
        .filter(fieldType -> paramDsl.getContainedElement(getLocalPart(fieldType))
            .map(fieldDsl -> fieldDsl.getElementName().equals(fieldConfig.getIdentifier())).orElse(false))
        .findFirst()
        .ifPresent(fieldType -> fieldType.getValue().accept(
                                                            getParameterDeclarerVisitor(fieldConfig,
                                                                                        paramDsl
                                                                                            .getContainedElement(getLocalPart(fieldType))
                                                                                            .get(),
                                                                                        fieldValue -> objectValue
                                                                                            .withParameter(getLocalPart(fieldType),
                                                                                                           fieldValue)))));
  }

  private void handleInfrastructure(final ParameterModel paramModel,
                                    final List<ConfigLine> declaredConfigs,
                                    final ParameterizedBuilder<String, ParameterValue, ?> declarer) {

    switch (paramModel.getName()) {
      case RECONNECTION_STRATEGY_PARAMETER_NAME:

        findAnyMatchingChildById(declaredConfigs, RECONNECT_ELEMENT_IDENTIFIER, RECONNECT_FOREVER_ELEMENT_IDENTIFIER)
            .ifPresent(config -> {
              ParameterObjectValue.Builder reconnection = newObjectValue().ofType(config.getIdentifier());
              copyExplicitAttributes(config.getConfigAttributes(), reconnection);
              declarer.withParameter(RECONNECTION_STRATEGY_PARAMETER_NAME, reconnection.build());
            });
        return;

      case REDELIVERY_POLICY_PARAMETER_NAME:
        findAnyMatchingChildById(declaredConfigs, REDELIVERY_POLICY_ELEMENT_IDENTIFIER)
            .ifPresent(config -> {
              ParameterObjectValue.Builder redelivery = newObjectValue();
              copyExplicitAttributes(config.getConfigAttributes(), redelivery);
              declarer.withParameter(REDELIVERY_POLICY_PARAMETER_NAME, redelivery.build());
            });
        return;

      case POOLING_PROFILE_PARAMETER_NAME:
        findAnyMatchingChildById(declaredConfigs, POOLING_PROFILE_ELEMENT_IDENTIFIER)
            .ifPresent(config -> {
              ParameterObjectValue.Builder poolingProfile = newObjectValue();
              cloneAsDeclaration(config, poolingProfile);
              declarer.withParameter(POOLING_PROFILE_PARAMETER_NAME, poolingProfile.build());
            });
        return;


      case STREAMING_STRATEGY_PARAMETER_NAME:
        // TODO MULE-12001: switch to EE namespace
        findAnyMatchingChildById(declaredConfigs,
                                 REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS, REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS,
                                 NON_REPEATABLE_BYTE_STREAM_ALIAS)
                                     .ifPresent(config -> {
                                       ParameterObjectValue.Builder streaming = newObjectValue()
                                           .ofType(config.getIdentifier());
                                       cloneAsDeclaration(config, streaming);
                                       declarer.withParameter(STREAMING_STRATEGY_PARAMETER_NAME, streaming.build());
                                     });
        return;

      case TLS_PARAMETER_NAME:
        findAnyMatchingChildById(declaredConfigs, TLS_CONTEXT_ELEMENT_IDENTIFIER)
            .ifPresent(config -> {
              ParameterObjectValue.Builder tls = newObjectValue();
              cloneAsDeclaration(config, tls);
              declarer.withParameter(TLS_PARAMETER_NAME, tls.build());
            });
        return;

      case DISABLE_CONNECTION_VALIDATION_PARAMETER_NAME:
        // Nothing to do here, this has to be propagated as an attribute
        return;
    }
  }

  private Optional<ConfigLine> findAnyMatchingChildById(List<ConfigLine> configs, String... validIds) {
    List<String> ids = Arrays.asList(validIds);
    return configs.stream().filter(c -> ids.contains(c.getIdentifier())).findFirst();
  }

  private void cloneAsDeclaration(ConfigLine config, ParameterObjectValue.Builder poolingProfile) {
    copyExplicitAttributes(config.getConfigAttributes(), poolingProfile);
    copyChildren(config, poolingProfile);
  }

  private String getNamespace(ConfigLine configLine) {
    return configLine.getNamespace() == null ? CORE_PREFIX : configLine.getNamespace();
  }

  private void copyExplicitAttributes(Map<String, SimpleConfigAttribute> attributes,
                                      ParameterizedBuilder<String, ParameterValue, ?> builder) {
    attributes.values().stream()
        .filter(a -> !a.getName().equals(NAME_ATTRIBUTE_NAME) && !a.getName().equals(CONFIG_ATTRIBUTE_NAME))
        .filter(a -> !a.isValueFromSchema())
        .forEach(a -> builder.withParameter(a.getName(), ParameterSimpleValue.of(a.getValue())));
  }

  private void copyChildren(ConfigLine config, ParameterizedBuilder<String, ParameterValue, ?> builder) {
    config.getChildren().forEach(child -> {
      ParameterObjectValue.Builder childBuilder = newObjectValue();
      cloneAsDeclaration(child, childBuilder);
      builder.withParameter(child.getIdentifier(), childBuilder.build());
    });
  }

}
