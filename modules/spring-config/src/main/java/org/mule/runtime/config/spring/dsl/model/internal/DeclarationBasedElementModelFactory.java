/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.internal;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.IS_CDATA;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getAlias;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isFlattenedParameterGroup;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getDefaultValue;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isInfrastructure;
import static org.mule.runtime.extension.internal.dsl.syntax.DslSyntaxUtils.getId;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.NAME_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.app.declaration.ComponentElementDeclaration;
import org.mule.runtime.api.app.declaration.ConfigurationElementDeclaration;
import org.mule.runtime.api.app.declaration.ConnectionElementDeclaration;
import org.mule.runtime.api.app.declaration.ElementDeclaration;
import org.mule.runtime.api.app.declaration.OperationElementDeclaration;
import org.mule.runtime.api.app.declaration.ParameterElementDeclaration;
import org.mule.runtime.api.app.declaration.ParameterGroupElementDeclaration;
import org.mule.runtime.api.app.declaration.ParameterValue;
import org.mule.runtime.api.app.declaration.ParameterValueVisitor;
import org.mule.runtime.api.app.declaration.ParameterizedElementDeclaration;
import org.mule.runtime.api.app.declaration.RouteElementDeclaration;
import org.mule.runtime.api.app.declaration.RouterElementDeclaration;
import org.mule.runtime.api.app.declaration.ScopeElementDeclaration;
import org.mule.runtime.api.app.declaration.SourceElementDeclaration;
import org.mule.runtime.api.app.declaration.TopLevelParameterDeclaration;
import org.mule.runtime.api.app.declaration.fluent.ParameterListValue;
import org.mule.runtime.api.app.declaration.fluent.ParameterObjectValue;
import org.mule.runtime.api.app.declaration.fluent.ParameterSimpleValue;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
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
import org.mule.runtime.config.spring.dsl.model.DslElementModel;
import org.mule.runtime.config.spring.dsl.model.DslElementModelFactory;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.internal.dsl.syntax.DslElementSyntaxBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link DslElementModelFactory} that creates a {@link DslElementModel} based on its {@link ElementDeclaration}
 * representation.
 *
 * @since 4.0
 */
class DeclarationBasedElementModelFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeclarationBasedElementModelFactory.class);

  private final DslResolvingContext context;
  private final InfrastructureElementModelDelegate infrastructureDelegate;
  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  private Map<ExtensionModel, DslSyntaxResolver> resolvers;
  private ExtensionModel currentExtension;
  private DslSyntaxResolver dsl;

  DeclarationBasedElementModelFactory(DslResolvingContext context, Map<ExtensionModel, DslSyntaxResolver> resolvers) {
    this.context = context;
    this.resolvers = resolvers;
    this.infrastructureDelegate = new InfrastructureElementModelDelegate();
  }

  public <T> Optional<DslElementModel<T>> create(ElementDeclaration declaration) {

    setupCurrentExtensionContext(declaration.getDeclaringExtension());
    final Function<NamedObject, Boolean> equalsName = (named) -> named.getName().equals(declaration.getName());

    if (declaration instanceof TopLevelParameterDeclaration) {
      return createFromType((TopLevelParameterDeclaration) declaration);
    }

    Reference<DslElementModel> elementModel = new Reference<>();
    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        if (equalsName.apply(model) && declaration instanceof ConfigurationElementDeclaration) {
          elementModel.set(createConfigurationElement(model, (ConfigurationElementDeclaration) declaration));
          stop();
        }
      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        if (equalsName.apply(model) && declaration instanceof OperationElementDeclaration) {
          elementModel.set(createComponentElement(model, (OperationElementDeclaration) declaration));
          stop();
        }
      }

      @Override
      protected void onScope(HasOperationModels owner, ScopeModel model) {
        if (equalsName.apply(model) && declaration instanceof ScopeElementDeclaration) {
          elementModel.set(createScopeElement(model, (ScopeElementDeclaration) declaration));
          stop();
        }
      }

      @Override
      protected void onRouter(HasOperationModels owner, RouterModel model) {
        if (equalsName.apply(model) && declaration instanceof RouterElementDeclaration) {
          elementModel.set(createRouterElement(model, (RouterElementDeclaration) declaration));
          stop();
        }
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        if (equalsName.apply(model) && declaration instanceof SourceElementDeclaration) {
          elementModel.set(createComponentElement(model, (SourceElementDeclaration) declaration));
          stop();
        }
      }

    }.walk(currentExtension);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format("No model found with name [%s] of type [%s] for extension [%s]",
                                 declaration.getName(), declaration.getClass().getName(), declaration.getDeclaringExtension()));
    }

    return Optional.ofNullable(elementModel.get());
  }

  private <T> Optional<DslElementModel<T>> createFromType(TopLevelParameterDeclaration declaration) {
    return context.getTypeCatalog()
        .getType(declaration.getValue().getTypeId())
        .map(objectType -> createTopLevelElement(objectType, declaration));
  }

  private DslElementModel<ConfigurationModel> createConfigurationElement(ConfigurationModel model,
                                                                         ConfigurationElementDeclaration configDeclaration) {
    DslElementSyntax configDsl = dsl.resolve(model);

    ComponentConfiguration.Builder configuration = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(configDsl))
        .withParameter(NAME_ATTRIBUTE_NAME, configDeclaration.getRefName());

    DslElementModel.Builder<ConfigurationModel> element =
        createParameterizedElementModel(model, configDsl, configDeclaration, configuration);

    configDeclaration.getConnection()
        .ifPresent(connection -> addConnectionProvider(connection, model, configuration, element));

    return element.withConfig(configuration.build()).build();
  }

  private DslElementModel<? extends ComponentModel> createComponentElement(ComponentModel model,
                                                                           ComponentElementDeclaration componentDeclaration) {
    DslElementSyntax configDsl = dsl.resolve(model);
    ComponentConfiguration.Builder configuration = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(configDsl));

    if (componentDeclaration.getConfigRef() != null) {
      configuration.withParameter(CONFIG_ATTRIBUTE_NAME, componentDeclaration.getConfigRef());
    }

    DslElementModel.Builder<? extends ComponentModel> element =
        createParameterizedElementModel(model, configDsl, componentDeclaration, configuration);
    return element.withConfig(configuration.build()).build();
  }

  private DslElementModel<? extends ComponentModel> createRouterElement(RouterModel model,
                                                                        RouterElementDeclaration routerDeclaration) {
    DslElementSyntax configDsl = dsl.resolve(model);
    ComponentConfiguration.Builder configuration = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(configDsl));

    if (routerDeclaration.getConfigRef() != null) {
      configuration.withParameter(CONFIG_ATTRIBUTE_NAME, routerDeclaration.getConfigRef());
    }

    DslElementModel.Builder<? extends ComponentModel> element =
        createParameterizedElementModel(model, configDsl, routerDeclaration, configuration);

    routerDeclaration.getRoutes().forEach(routeDeclaration -> model.getRouteModels().stream()
        .filter(routeModel -> routeDeclaration.getName().equals(routeModel.getName()))
        .findFirst()
        .ifPresent(routeModel -> element.containing(crateRouteElement(routeModel, routeDeclaration))));

    return element.withConfig(configuration.build()).build();
  }

  private DslElementModel<? extends ComponentModel> createScopeElement(ScopeModel model,
                                                                       ScopeElementDeclaration scopeDeclaration) {
    DslElementSyntax configDsl = dsl.resolve(model);
    ComponentConfiguration.Builder configuration = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(configDsl));

    if (scopeDeclaration.getConfigRef() != null) {
      configuration.withParameter(CONFIG_ATTRIBUTE_NAME, scopeDeclaration.getConfigRef());
    }

    DslElementModel.Builder<? extends ComponentModel> element =
        createParameterizedElementModel(model, configDsl, scopeDeclaration, configuration);

    scopeDeclaration.getComponents()
        .forEach(componentDeclaration -> create(componentDeclaration)
            .ifPresent(componentElement -> {
              componentElement.getConfiguration().ifPresent(configuration::withNestedComponent);
              element.containing(componentElement);
            }));

    return element.withConfig(configuration.build()).build();
  }

  private DslElementModel<? extends RouteModel> crateRouteElement(RouteModel model, RouteElementDeclaration routeDeclaration) {
    DslElementSyntax routeDsl = dsl.resolve(model);
    ComponentConfiguration.Builder routeConfiguration = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(routeDsl));

    DslElementModel.Builder<? extends RouteModel> routeElement =
        createParameterizedElementModel(model, routeDsl, routeDeclaration, routeConfiguration);

    ExtensionModel routerOwner = currentExtension;
    DslSyntaxResolver routerDslResolver = dsl;
    routeDeclaration.getComponents().forEach(componentDeclaration -> {
      create(componentDeclaration)
          .ifPresent(componentElement -> {
            componentElement.getConfiguration().ifPresent(routeConfiguration::withNestedComponent);
            routeElement.containing(componentElement);
          });
      currentExtension = routerOwner;
      dsl = routerDslResolver;
    });

    return routeElement.withConfig(routeConfiguration.build()).build();
  }

  private DslElementModel createTopLevelElement(ObjectType model, TopLevelParameterDeclaration declaration) {

    DslElementSyntax objectDsl = dsl.resolve(model)
        .orElseThrow(() -> new IllegalArgumentException("Failed to resolve the DSL syntax for type '" + getId(model) + "'"));
    DslElementModel.Builder<MetadataType> parentElement = DslElementModel.<MetadataType>builder()
        .withModel(model)
        .withDsl(objectDsl);

    ComponentConfiguration.Builder configuration = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(objectDsl))
        .withParameter(NAME_ATTRIBUTE_NAME, declaration.getRefName());

    populateObjectElementFields(model, declaration.getValue(), objectDsl, configuration, parentElement);

    return parentElement.withConfig(configuration.build()).build();
  }

  private void setupCurrentExtensionContext(String extension) {
    this.currentExtension = context.getExtension(extension).orElseThrow(() -> new IllegalArgumentException());
    this.dsl = resolvers.get(currentExtension);
  }

  private void addConnectionProvider(ConnectionElementDeclaration connection,
                                     ConfigurationModel model,
                                     ComponentConfiguration.Builder configuration,
                                     DslElementModel.Builder<ConfigurationModel> configElement) {

    concat(model.getConnectionProviders().stream(), currentExtension.getConnectionProviders()
        .stream())
            .filter(c -> c.getName().equals(connection.getName()))
            .findFirst()
            .ifPresent(provider -> {
              DslElementSyntax providerDsl = dsl.resolve(provider);

              ComponentConfiguration.Builder builder = ComponentConfiguration.builder()
                  .withIdentifier(asIdentifier(providerDsl));

              DslElementModel.Builder<ConnectionProviderModel> element =
                  createParameterizedElementModel(provider, providerDsl, connection, builder);

              ComponentConfiguration providerConfig = builder.build();

              configuration.withNestedComponent(providerConfig);
              configElement.containing(element.withConfig(providerConfig).build());
            });
  }

  private <T extends ParameterizedModel> DslElementModel.Builder<T> createParameterizedElementModel(T model,
                                                                                                    DslElementSyntax elementDsl,
                                                                                                    ParameterizedElementDeclaration declaration,
                                                                                                    ComponentConfiguration.Builder parentConfig) {
    DslElementModel.Builder<T> parentElement = DslElementModel.<T>builder()
        .withModel(model)
        .withDsl(elementDsl);

    addAllDeclaredParameters(model.getParameterGroupModels(), declaration, elementDsl, parentConfig, parentElement);

    if (model instanceof SourceModel) {
      ((SourceModel) model).getSuccessCallback()
          .ifPresent(
                     cb -> addAllDeclaredParameters(cb.getParameterGroupModels(), declaration, elementDsl, parentConfig,
                                                    parentElement));
      ((SourceModel) model).getErrorCallback()
          .ifPresent(
                     cb -> addAllDeclaredParameters(cb.getParameterGroupModels(), declaration, elementDsl, parentConfig,
                                                    parentElement));
    }

    addCustomParameters(declaration, parentConfig, parentElement);

    declaration.getMetadataProperties().forEach(parentConfig::withProperty);

    return parentElement;
  }

  private <T extends ParameterizedModel> void addCustomParameters(ParameterizedElementDeclaration declaration,
                                                                  ComponentConfiguration.Builder parentConfig,
                                                                  DslElementModel.Builder<T> parentElement) {
    declaration.getCustomConfigurationParameters()
        .forEach(p -> {
          parentConfig.withParameter(p.getName(), p.getValue().toString());
          parentElement.containing(DslElementModel.builder()
              .withModel(typeLoader.load(String.class))
              .withDsl(DslElementSyntaxBuilder.create().withAttributeName(p.getName()).build())
              .withValue(p.getValue().toString())
              .isExplicitInDsl(true)
              .build());
        });
  }

  private void addAllDeclaredParameters(List<ParameterGroupModel> groups,
                                        ParameterizedElementDeclaration parameterizedDeclaration,
                                        DslElementSyntax parentDsl,
                                        ComponentConfiguration.Builder parentConfig,
                                        DslElementModel.Builder parentElement) {

    groups.forEach(group -> parameterizedDeclaration
        .getParameterGroup(group.getName())
        .ifPresent(groupDeclaration -> {
          if (group.isShowInDsl()) {
            addInlineGroupElement(group, parentDsl, parentConfig, parentElement, groupDeclaration);
          } else {
            group.getParameterModels()
                .forEach(paramModel -> parentDsl.getContainedElement(paramModel.getName())
                    .ifPresent(paramDsl -> {
                      Optional<ParameterElementDeclaration> declared = groupDeclaration.getParameters().stream()
                          .filter(d -> d.getName().equals(paramModel.getName()))
                          .findFirst();

                      if (declared.isPresent()) {
                        addParameter(declared.get().getName(), declared.get().getValue(), paramModel, paramDsl, parentConfig,
                                     parentElement);
                      } else {
                        getDefaultValue(paramModel)
                            .ifPresent(value -> createSimpleParameter((ParameterSimpleValue) ParameterSimpleValue.of(value),
                                                                      paramDsl, parentConfig, parentElement, paramModel, false));
                      }
                    }));
          }
        }));
  }

  private <T extends ParameterizedModel> void addInlineGroupElement(ParameterGroupModel group,
                                                                    DslElementSyntax elementDsl,
                                                                    ComponentConfiguration.Builder parentConfig,
                                                                    DslElementModel.Builder<T> parentElement,
                                                                    ParameterGroupElementDeclaration declaration) {
    elementDsl.getChild(group.getName())
        .ifPresent(groupDsl -> {
          DslElementModel.Builder<ParameterGroupModel> groupElementBuilder = DslElementModel.<ParameterGroupModel>builder()
              .withModel(group)
              .withDsl(groupDsl);

          ComponentConfiguration.Builder groupBuilder = ComponentConfiguration.builder().withIdentifier(asIdentifier(groupDsl));

          group.getParameterModels()
              .forEach(paramModel -> declaration.getParameter(paramModel.getName())
                  .ifPresent(paramDeclaration -> groupDsl.getContainedElement(paramModel.getName())
                      .ifPresent(paramDsl -> addParameter(paramModel.getName(), paramDeclaration.getValue(),
                                                          paramModel, paramDsl, groupBuilder, groupElementBuilder))));

          ComponentConfiguration groupConfig = groupBuilder.build();
          groupElementBuilder.withConfig(groupConfig);

          parentConfig.withNestedComponent(groupConfig);
          parentElement.containing(groupElementBuilder.build());
        });
  }

  private void addParameter(String parameterName, ParameterValue value,
                            ParameterModel parameterModel,
                            DslElementSyntax paramDsl,
                            final ComponentConfiguration.Builder parentConfig,
                            final DslElementModel.Builder parentElement) {

    if (isInfrastructure(parameterModel)) {
      infrastructureDelegate.addParameter(parameterName, value, parameterModel, paramDsl, parentConfig, parentElement);
      return;
    }

    value.accept(new ParameterValueVisitor() {

      @Override
      public void visitSimpleValue(ParameterSimpleValue text) {
        checkArgument(paramDsl.supportsAttributeDeclaration() || isContent(parameterModel) || isText(parameterModel),
                      "Simple values can only be declared for parameters of simple type, or those with Content role."
                          + " Invalid declaration for parameter: " + parameterName);
        createSimpleParameter(text, paramDsl, parentConfig, parentElement, parameterModel, true);
      }

      @Override
      public void visitListValue(ParameterListValue list) {
        checkArgument(paramDsl.supportsChildDeclaration(),
                      format("Inline List values are not allowed for this parameter [%s]", parameterModel.getName()));
        checkArgument(parameterModel.getType() instanceof ArrayType,
                      format("List values can only be associated to ArrayType parameters. Parameter [%s] is of type [%s]",
                             parameterModel.getName(), getId(parameterModel.getType())));
        // the parameter is of list type, so we have nested elements
        // we'll resolve this based on the type of the parameter, since no
        // further model information is available
        createList(list, paramDsl, parameterModel, (ArrayType) parameterModel.getType(), parentConfig, parentElement);
      }

      @Override
      public void visitObjectValue(ParameterObjectValue objectValue) {
        checkArgument(parameterModel.getType() instanceof ObjectType,
                      format("Complex values can only be associated to ObjectType parameters. Parameter [%s] is of type [%s]",
                             parameterModel.getName(), getId(parameterModel.getType())));
        checkArgument(paramDsl.supportsChildDeclaration(),
                      format("Complex values are not allowed for this parameter [%s]", parameterModel.getName()));

        if (isMap(parameterModel.getType())) {
          createMapParameter(objectValue, paramDsl, parameterModel, (ObjectType) parameterModel.getType(),
                             parentConfig, parentElement);
        } else {
          createComplexParameter(objectValue, paramDsl, parameterModel, parentConfig, parentElement);
        }
      }
    });
  }

  private void createMapParameter(ParameterObjectValue objectValue, DslElementSyntax paramDsl, Object model, ObjectType mapType,
                                  ComponentConfiguration.Builder parentConfig, DslElementModel.Builder parentElement) {

    ComponentConfiguration.Builder mapConfig = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(paramDsl));

    DslElementModel.Builder mapElement = DslElementModel.builder()
        .withModel(model)
        .withDsl(paramDsl);

    MetadataType valueType = mapType.getOpenRestriction().get();

    paramDsl.getGeneric(valueType)
        .ifPresent(entryDsl -> objectValue.getParameters().forEach((key, value) -> {
          ComponentConfiguration.Builder entryConfigBuilder = ComponentConfiguration.builder()
              .withIdentifier(asIdentifier(entryDsl));

          DslElementModel.Builder<MetadataType> entryElement = DslElementModel.<MetadataType>builder()
              .withModel(valueType)
              .withDsl(entryDsl);


          entryDsl.getAttribute(KEY_ATTRIBUTE_NAME)
              .ifPresent(keyDsl -> {
                entryConfigBuilder.withParameter(KEY_ATTRIBUTE_NAME, key);
                entryElement.containing(DslElementModel.builder()
                    .withModel(typeLoader.load(String.class))
                    .withDsl(keyDsl)
                    .withValue(key)
                    .build());
              });

          entryDsl.getAttribute(VALUE_ATTRIBUTE_NAME)
              .ifPresent(valueDsl -> value.accept(new ParameterValueVisitor() {

                @Override
                public void visitSimpleValue(ParameterSimpleValue text) {
                  entryConfigBuilder.withParameter(VALUE_ATTRIBUTE_NAME, text.getValue());
                  entryElement.containing(DslElementModel.builder()
                      .withModel(valueType)
                      .withDsl(valueDsl)
                      .withValue(text.getValue())
                      .build());
                }

                @Override
                public void visitListValue(ParameterListValue list) {
                  createList(list, valueDsl, valueType, (ArrayType) valueType, entryConfigBuilder, entryElement);
                }

                @Override
                public void visitObjectValue(ParameterObjectValue objectValue) {
                  if (isMap(valueType)) {
                    createMapParameter(objectValue, valueDsl, valueType, (ObjectType) valueType, entryConfigBuilder,
                                       entryElement);
                  } else {
                    createObject(objectValue, valueDsl, valueType, (ObjectType) valueType, entryConfigBuilder, entryElement);
                  }
                }
              }));

          ComponentConfiguration entryConfig = entryConfigBuilder.build();
          mapConfig.withNestedComponent(entryConfig);
          mapElement.containing(entryElement.withConfig(entryConfig).build());
        }));

    ComponentConfiguration result = mapConfig.build();
    parentConfig.withNestedComponent(result);

    parentElement.containing(mapElement.withConfig(result).build());
  }

  private void createComplexParameter(ParameterObjectValue objectValue, DslElementSyntax paramDsl, ParameterModel parameterModel,
                                      ComponentConfiguration.Builder parentConfig, DslElementModel.Builder parentElement) {
    if (!paramDsl.isWrapped()) {
      // the parameter is of a complex object type, so we have both nested elements
      // and attributes as values of this element.
      // we'll resolve this based on the type of the parameter, since no
      // further model information is available
      createObject(objectValue, paramDsl, parameterModel, (ObjectType) parameterModel.getType(), parentConfig, parentElement);

    } else {
      // the parameter is of an extensible object type, so we need a wrapper element
      // before defining the actual object structure
      // we'll resolve this structure based on the configured type, since no
      // further model information is available
      createWrappedObject(objectValue, parameterModel, paramDsl, parentConfig, parentElement);
    }
  }

  private void createWrappedObject(ParameterObjectValue objectValue, ParameterModel parameterModel, DslElementSyntax paramDsl,
                                   ComponentConfiguration.Builder parentConfig, DslElementModel.Builder parentElement) {
    DslElementModel.Builder<ParameterModel> wrapperElement = DslElementModel.<ParameterModel>builder()
        .withModel(parameterModel)
        .withDsl(paramDsl);

    ComponentConfiguration.Builder wrapperConfig = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(paramDsl));

    Reference<DslSyntaxResolver> customDsl = new Reference<>(dsl);

    // TODO MULE-12002: Revisit DslSyntaxUtils as part of the API
    ObjectType nestedElementType;
    if (objectValue.getTypeId() == null || objectValue.getTypeId().trim().isEmpty() ||
        objectValue.getTypeId().equals(getId(parameterModel.getType()))) {

      nestedElementType = (ObjectType) parameterModel.getType();
    } else {
      nestedElementType = lookupType(objectValue);
      context.getTypeCatalog().getDeclaringExtension(objectValue.getTypeId()).ifPresent(owner -> context.getExtension(owner)
          .ifPresent(extensionModel -> customDsl.set(resolvers.get(extensionModel))));
    }

    customDsl.get().resolve(nestedElementType)
        .ifPresent(typeDsl -> createObject(objectValue, typeDsl, nestedElementType, nestedElementType, wrapperConfig,
                                           wrapperElement));

    ComponentConfiguration result = wrapperConfig.build();

    parentConfig.withNestedComponent(result);
    parentElement.containing(wrapperElement.withConfig(result).build());
  }

  private void createSimpleParameter(ParameterSimpleValue value, DslElementSyntax paramDsl,
                                     ComponentConfiguration.Builder parentConfig,
                                     DslElementModel.Builder parentElement, ParameterModel parameterModel, boolean explicit) {
    if (paramDsl.supportsAttributeDeclaration()) {
      // attribute parameters imply no further nesting in the configs
      parentConfig.withParameter(paramDsl.getAttributeName(), value.getValue());
      parentElement.containing(DslElementModel.<ParameterModel>builder()
          .withModel(parameterModel)
          .withDsl(paramDsl)
          .withValue(value.getValue())
          .isExplicitInDsl(explicit)
          .build());
    } else {
      // we are in the text or content case, so we have one more nesting level
      ComponentConfiguration parameterConfig = ComponentConfiguration.builder()
          .withIdentifier(asIdentifier(paramDsl))
          .withValue(value.getValue())
          .withProperty(IS_CDATA, value.isCData() ? true : null)
          .build();

      parentConfig.withNestedComponent(parameterConfig);
      parentElement.containing(DslElementModel.<ParameterModel>builder()
          .withModel(parameterModel)
          .withDsl(paramDsl)
          .withConfig(parameterConfig)
          .isExplicitInDsl(explicit)
          .withValue(value.getValue())
          .build());
    }
  }

  private ObjectType lookupType(ParameterObjectValue objectValue) {
    ObjectType nestedElementType;
    nestedElementType = context.getTypeCatalog().getType(objectValue.getTypeId())
        .orElseThrow(() -> new IllegalArgumentException(format("Could not find Type with ID '%s' in the current context",
                                                               objectValue.getTypeId())));
    return nestedElementType;
  }

  private void createListItemConfig(MetadataType itemValueType,
                                    ParameterValue itemValue,
                                    DslElementSyntax itemDsl,
                                    ComponentConfiguration.Builder parentConfig,
                                    DslElementModel.Builder parentElement) {

    itemValue.accept(new ParameterValueVisitor() {

      @Override
      public void visitSimpleValue(ParameterSimpleValue text) {
        itemDsl.getContainedElement(VALUE_ATTRIBUTE_NAME)
            .ifPresent(valueDsl -> {
              ComponentConfiguration item = ComponentConfiguration.builder()
                  .withIdentifier(asIdentifier(itemDsl))
                  .withParameter(VALUE_ATTRIBUTE_NAME, text.getValue())
                  .build();

              parentConfig.withNestedComponent(item);
              parentElement.containing(DslElementModel.builder()
                  .withModel(itemValueType)
                  .withDsl(itemDsl)
                  .withConfig(item)
                  .containing(DslElementModel.builder()
                      .withModel(itemValueType)
                      .withDsl(valueDsl)
                      .withValue(text.getValue())
                      .build())
                  .build());
            });
      }

      @Override
      public void visitListValue(ParameterListValue list) {
        createList(list, itemDsl, itemValueType, (ArrayType) itemValueType, parentConfig, parentElement);
      }

      @Override
      public void visitObjectValue(ParameterObjectValue objectValue) {
        itemValueType.accept(new MetadataTypeVisitor() {

          @Override
          public void visitObject(ObjectType objectType) {
            if (isMap(objectType)) {
              createMapParameter(objectValue, itemDsl, itemValueType, objectType, parentConfig, parentElement);
            } else {
              createObject(objectValue, itemDsl, objectType, objectType, parentConfig, parentElement);
            }
          }
        });
      }
    });
  }

  private void addObjectField(MetadataType fieldType, ParameterValue fieldValue,
                              DslElementSyntax fieldDsl,
                              ComponentConfiguration.Builder objectConfig,
                              DslElementModel.Builder objectElement) {

    fieldValue.accept(new ParameterValueVisitor() {

      @Override
      public void visitSimpleValue(ParameterSimpleValue text) {
        if (fieldDsl.supportsAttributeDeclaration()) {
          objectConfig.withParameter(fieldDsl.getAttributeName(), text.getValue());
          objectElement.containing(DslElementModel.builder()
              .withModel(fieldType)
              .withDsl(fieldDsl)
              .withValue(text.getValue())
              .build());
        } else {
          ComponentConfiguration contentConfiguration = ComponentConfiguration.builder()
              .withIdentifier(asIdentifier(fieldDsl))
              .withValue(text.getValue())
              .withProperty(IS_CDATA, text.isCData() ? true : null)
              .build();

          objectConfig.withNestedComponent(contentConfiguration);
          objectElement.containing(DslElementModel.builder()
              .withModel(fieldType)
              .withDsl(fieldDsl)
              .withConfig(contentConfiguration)
              .withValue(text.getValue())
              .build());
        }
      }

      @Override
      public void visitListValue(ParameterListValue list) {
        createList(list, fieldDsl, fieldType, (ArrayType) fieldType, objectConfig, objectElement);
      }

      @Override
      public void visitObjectValue(ParameterObjectValue objectValue) {

        fieldType.accept(new MetadataTypeVisitor() {

          @Override
          public void visitObject(ObjectType objectType) {
            if (isMap(objectType)) {
              createMapParameter(objectValue, fieldDsl, fieldType, objectType, objectConfig, objectElement);
            } else {
              createObject(objectValue, fieldDsl, objectType, objectType, objectConfig, objectElement);
            }
          }
        });
      }
    });
  }

  private void createList(ParameterListValue list,
                          DslElementSyntax listDsl,
                          Object model,
                          ArrayType listType,
                          ComponentConfiguration.Builder parentConfig,
                          DslElementModel.Builder parentElement) {

    final DslElementModel.Builder listElement = DslElementModel.builder()
        .withModel(model)
        .withDsl(listDsl);

    final ComponentConfiguration.Builder listConfig = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(listDsl));

    final MetadataType itemType = listType.getType();
    listDsl.getGeneric(itemType)
        .ifPresent(itemDsl -> list.getValues()
            .forEach(value -> createListItemConfig(itemType, value, itemDsl, listConfig, listElement)));

    ComponentConfiguration result = listConfig.build();
    parentConfig.withNestedComponent(result);
    parentElement.containing(listElement.withConfig(result).build());
  }

  private void createObject(ParameterObjectValue objectValue, DslElementSyntax objectDsl, Object model, ObjectType objectType,
                            ComponentConfiguration.Builder parentConfig,
                            DslElementModel.Builder parentElement) {

    ComponentConfiguration.Builder objectConfig = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(objectDsl));

    DslElementModel.Builder objectElement = DslElementModel.builder()
        .withModel(model)
        .withDsl(objectDsl);

    populateObjectElementFields(objectType, objectValue, objectDsl, objectConfig, objectElement);

    ComponentConfiguration result = objectConfig.build();
    parentConfig.withNestedComponent(result);

    parentElement.containing(objectElement.withConfig(result).build());
  }

  private void populateObjectElementFields(ObjectType objectType, ParameterObjectValue objectValue, DslElementSyntax objectDsl,
                                           ComponentConfiguration.Builder objectConfig,
                                           DslElementModel.Builder objectElement) {
    List<ObjectFieldType> fields = objectType.getFields()
        .stream()
        .flatMap(f -> isFlattenedParameterGroup(f) ? ((ObjectType) f.getValue()).getFields().stream() : of(f))
        .collect(toList());

    fields.forEach(field -> objectValue.getParameters().entrySet().stream()
        .filter(e -> getAlias(field).equals(e.getKey()))
        .findFirst()
        .ifPresent(e -> objectDsl.getContainedElement(e.getKey())
            .ifPresent(nestedDsl -> addObjectField(field.getValue(), e.getValue(), nestedDsl, objectConfig, objectElement))));
  }

  private ComponentIdentifier asIdentifier(DslElementSyntax fieldDsl) {
    checkArgument(fieldDsl.supportsTopLevelDeclaration() || fieldDsl.supportsChildDeclaration(),
                  format("The given component '%s' does not support element-like declaration", fieldDsl.getAttributeName()));

    return builder()
        .withName(fieldDsl.getElementName())
        .withNamespace(fieldDsl.getPrefix())
        .build();
  }

  private boolean isText(ParameterModel parameter) {
    return parameter.getLayoutModel().map(LayoutModel::isText).orElse(false);
  }

}
