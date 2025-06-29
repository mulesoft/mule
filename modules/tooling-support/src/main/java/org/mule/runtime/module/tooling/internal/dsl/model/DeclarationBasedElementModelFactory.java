/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.dsl.model;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.NAME_ATTRIBUTE_NAME;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.VALUE_ATTRIBUTE_NAME;
import static org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser.IS_CDATA;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getAlias;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isFlattenedParameterGroup;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getDefaultValue;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isInfrastructure;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isRequired;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ComposableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ConnectionElementDeclaration;
import org.mule.runtime.app.declaration.api.ConstructElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterGroupElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.ParameterValueVisitor;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.app.declaration.api.ReferableElementDeclaration;
import org.mule.runtime.app.declaration.api.RouteElementDeclaration;
import org.mule.runtime.app.declaration.api.SourceElementDeclaration;
import org.mule.runtime.app.declaration.api.TopLevelParameterDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntaxBuilder;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.property.NoWrapperModelProperty;
import org.mule.runtime.metadata.api.dsl.DslElementModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

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
  private final Map<ExtensionModel, DslSyntaxResolver> resolvers;
  private ExtensionModel currentExtension;
  private DslSyntaxResolver dsl;

  DeclarationBasedElementModelFactory(DslResolvingContext context, Map<ExtensionModel, DslSyntaxResolver> resolvers) {
    this.context = context;
    this.resolvers = resolvers;
    this.infrastructureDelegate = new InfrastructureElementModelDelegate();
  }

  public <T> Optional<DslElementModel<T>> create(ElementDeclaration declaration) {

    setupCurrentExtensionContext(declaration.getDeclaringExtension());
    final Predicate<NamedObject> equalsName = named -> named.getName().equals(declaration.getName());

    if (declaration instanceof TopLevelParameterDeclaration) {
      return createFromType((TopLevelParameterDeclaration) declaration);
    }

    Reference<DslElementModel> elementModel = new Reference<>();
    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        if (equalsName.test(model) && declaration instanceof ConfigurationElementDeclaration) {
          elementModel.set(createConfigurationElement(model, (ConfigurationElementDeclaration) declaration));
          stop();
        }
      }

      @Override
      protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
        if (equalsName.test(model) && declaration instanceof ConnectionElementDeclaration) {
          elementModel.set(createConnectionProviderModel(model, (ConnectionElementDeclaration) declaration));
          stop();
        }
      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        if (equalsName.test(model) && declaration instanceof OperationElementDeclaration) {
          elementModel.set(createComponentElement(model, (OperationElementDeclaration) declaration));
          stop();
        }
      }

      @Override
      protected void onConstruct(HasConstructModels owner, ConstructModel model) {
        if (equalsName.test(model) && declaration instanceof ConstructElementDeclaration) {
          elementModel.set(createComponentElement(model, (ConstructElementDeclaration) declaration));
          stop();
        }
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        if (equalsName.test(model) && declaration instanceof SourceElementDeclaration) {
          elementModel.set(createComponentElement(model, (SourceElementDeclaration) declaration));
          stop();
        }
      }

    }.walk(currentExtension);

    if (LOGGER.isDebugEnabled() && elementModel.get() == null) {
      LOGGER.debug(format("No model found with name [%s] of type [%s] for extension [%s]",
                          declaration.getName(), declaration.getClass().getName(), declaration.getDeclaringExtension()));
    }

    return ofNullable(elementModel.get());
  }

  public <T> Optional<DslElementModel<T>> create(ComponentModel parentModel, ComponentElementDeclaration childDeclaration) {
    return parentModel.getNestedComponents().stream()
        .filter(nestedComponent -> nestedComponent.getName().equals(childDeclaration.getName())).findFirst()
        .map(nestedComponentModel -> (DslElementModel<T>) createComponentElement(nestedComponentModel, childDeclaration));
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

    DslElementModel.Builder<ConfigurationModel> element = DslElementModel.<ConfigurationModel>builder()
        .withModel(model)
        .withDsl(configDsl);

    configDeclaration.getConnection()
        .ifPresent(connection -> addConnectionProvider(connection, model, configuration, element));

    enrichParameterizedElementModel(model, configDsl, configDeclaration, configuration, element);

    return element.withConfig(configuration.build()).build();
  }

  private DslElementModel<? extends ComponentModel> createComponentElement(ComponentModel model,
                                                                           ComponentElementDeclaration<?> componentDeclaration) {
    DslElementSyntax configDsl = dsl.resolve(model);
    ComponentConfiguration.Builder configuration = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(configDsl));

    if (componentDeclaration instanceof ReferableElementDeclaration) {
      model.getAllParameterModels().stream().filter(ParameterModel::isComponentId).findAny()
          .ifPresent(pm -> configuration.withParameter(pm.getName(),
                                                       ((ReferableElementDeclaration) componentDeclaration).getRefName()));
    }

    if (componentDeclaration.getConfigRef() != null) {
      configuration.withParameter(CONFIG_ATTRIBUTE_NAME, componentDeclaration.getConfigRef());
    }

    DslElementModel.Builder<? extends ComponentModel> componentElement =
        createParameterizedElementModel(model, configDsl, componentDeclaration, configuration);

    ExtensionModel componentsOwner = currentExtension;
    DslSyntaxResolver componentsDslResolver = dsl;

    componentDeclaration.getComponents().forEach(nestedComponentDeclaration -> {

      if (nestedComponentDeclaration instanceof RouteElementDeclaration) {
        if (model instanceof ComposableModel) {
          ((ComposableModel) model).getNestedComponents().stream()
              .filter(nestedModel -> nestedModel instanceof NestedRouteModel
                  && nestedModel.getName().equals(nestedComponentDeclaration.getName()))
              .findFirst()
              .ifPresent(nestedRouteModel -> componentElement.containing(
                                                                         crateRouteElement((NestedRouteModel) nestedRouteModel,
                                                                                           (RouteElementDeclaration) nestedComponentDeclaration)));
        }

      } else {
        DslElementModel<Object> nestedComponentElement =
            create(nestedComponentDeclaration).orElseGet(() -> create(model, nestedComponentDeclaration).orElse(null));
        if (nestedComponentElement != null) {
          nestedComponentElement.getConfiguration().ifPresent(configuration::withNestedComponent);
          componentElement.containing(nestedComponentElement);
        }
      }
      currentExtension = componentsOwner;
      dsl = componentsDslResolver;
    });

    return componentElement.withConfig(configuration.build()).build();
  }

  private DslElementModel<? extends NestedRouteModel> crateRouteElement(NestedRouteModel model,
                                                                        RouteElementDeclaration routeDeclaration) {
    DslElementSyntax routeDsl = dsl.resolve(model);
    ComponentConfiguration.Builder routeConfiguration = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(routeDsl));

    DslElementModel.Builder<? extends NestedRouteModel> routeElement =
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
    this.currentExtension = context.getExtension(extension)
        .orElseThrow(() -> new IllegalArgumentException(format("Extension [%s] is not present in the current context. Available extensions are: %s",
                                                               extension,
                                                               context.getExtensions().stream().map(NamedObject::getName)
                                                                   .collect(toList()))));
    this.dsl = resolvers.get(currentExtension);
  }

  private DslElementModel<ConnectionProviderModel> createConnectionProviderModel(ConnectionProviderModel providerModel,
                                                                                 ConnectionElementDeclaration connection) {
    DslElementSyntax providerDsl = dsl.resolve(providerModel);

    ComponentConfiguration.Builder builder = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(providerDsl));

    DslElementModel.Builder<ConnectionProviderModel> element =
        createParameterizedElementModel(providerModel, providerDsl, connection, builder);

    ComponentConfiguration providerConfig = builder.build();
    return element.withConfig(providerConfig).build();
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
              DslElementModel<ConnectionProviderModel> connectionProviderModel =
                  createConnectionProviderModel(provider, connection);
              connectionProviderModel.getConfiguration().ifPresent(configuration::withNestedComponent);
              configElement.containing(connectionProviderModel);
            });
  }

  private <T extends ParameterizedModel> DslElementModel.Builder<T> createParameterizedElementModel(T model,
                                                                                                    DslElementSyntax elementDsl,
                                                                                                    ParameterizedElementDeclaration declaration,
                                                                                                    ComponentConfiguration.Builder parentConfig) {
    DslElementModel.Builder<T> parentElement = DslElementModel.<T>builder()
        .withModel(model)
        .withDsl(elementDsl);

    enrichParameterizedElementModel(model, elementDsl, declaration, parentConfig, parentElement);

    return parentElement;
  }

  private <T extends ParameterizedModel> void enrichParameterizedElementModel(T model, DslElementSyntax elementDsl,
                                                                              ParameterizedElementDeclaration declaration,
                                                                              ComponentConfiguration.Builder parentConfig,
                                                                              DslElementModel.Builder<T> parentElement) {
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

    groups.forEach(group -> {
      Optional<ParameterGroupElementDeclaration> groupDeclaration = parameterizedDeclaration
          .getParameterGroup(group.getName());
      if (groupDeclaration.isPresent()) {
        if (group.isShowInDsl()) {
          addInlineGroupElement(group, parentDsl, parentConfig, parentElement, groupDeclaration);
        } else {
          addGroupParameterElements(group, parentDsl, parentConfig, parentElement, groupDeclaration);
        }
      } else {
        if (!isRequired(group) && group.getParameterModels().stream().anyMatch(p -> getDefaultValue(p).isPresent())) {
          if (group.isShowInDsl()) {
            addInlineGroupElement(group, parentDsl, parentConfig, parentElement, empty());
          } else {
            addGroupParameterElements(group, parentDsl, parentConfig, parentElement, empty());
          }
        }
      }
    });
  }


  private <T> void addGroupParameterElements(ParameterGroupModel group,
                                             DslElementSyntax elementDsl,
                                             ComponentConfiguration.Builder parentConfig,
                                             DslElementModel.Builder<T> parentElement,
                                             Optional<ParameterGroupElementDeclaration> declaration) {
    group.getParameterModels()
        .forEach(paramModel -> elementDsl.getContainedElement(paramModel.getName())
            .ifPresent(paramDsl -> {

              boolean declared = false;
              if (declaration.isPresent()) {
                Optional<ParameterElementDeclaration> parameterDeclaration = declaration.get().getParameter(paramModel.getName());

                if (parameterDeclaration.isPresent()) {
                  addParameter(parameterDeclaration.get().getName(), parameterDeclaration.get().getValue(),
                               paramModel, paramDsl, parentConfig, parentElement);
                  declared = true;
                }
              }

              if (!declared) {
                getDefaultValue(paramModel)
                    .ifPresent(value -> createSimpleParameter((ParameterSimpleValue) ParameterSimpleValue.of(value),
                                                              paramDsl, parentConfig, parentElement, paramModel, false));
              }
            }));
  }

  private <T> void addInlineGroupElement(ParameterGroupModel group,
                                         DslElementSyntax elementDsl,
                                         ComponentConfiguration.Builder parentConfig,
                                         DslElementModel.Builder<T> parentElement,
                                         Optional<ParameterGroupElementDeclaration> declaration) {
    elementDsl.getChild(group.getName())
        .ifPresent(groupDsl -> {
          DslElementModel.Builder<ParameterGroupModel> groupElementBuilder = DslElementModel.<ParameterGroupModel>builder()
              .withModel(group)
              .withDsl(groupDsl)
              .isExplicitInDsl(declaration.isPresent());

          ComponentConfiguration.Builder groupConfigBuilder =
              ComponentConfiguration.builder().withIdentifier(asIdentifier(groupDsl));

          addGroupParameterElements(group, groupDsl, groupConfigBuilder, groupElementBuilder, declaration);

          ComponentConfiguration groupConfig = groupConfigBuilder.build();
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
      infrastructureDelegate.addParameter(parameterName, value, parameterModel, paramDsl, parentConfig,
                                          parentElement, context, dsl);
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
    createMapParameter(objectValue, paramDsl, model, mapType, parentConfig, parentElement,
                       isParameterWrappedByContainer(model));
  }

  private boolean isParameterWrappedByContainer(Object model) {
    return !((model instanceof ParameterModel)
        && ((ParameterModel) model).getModelProperty(NoWrapperModelProperty.class).isPresent());
  }


  private void createMapParameter(ParameterObjectValue objectValue, DslElementSyntax paramDsl, Object model, ObjectType mapType,
                                  ComponentConfiguration.Builder parentConfig, DslElementModel.Builder parentElement,
                                  boolean isParameterWrappedByContainer) {
    ComponentConfiguration.Builder mapConfig = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(paramDsl));

    DslElementModel.Builder<Object> mapElement = DslElementModel.builder()
        .withModel(model)
        .withDsl(paramDsl);

    if (!mapType.getOpenRestriction().isPresent()) {
      throw new MuleRuntimeException(new IllegalArgumentException("Map Type without value type defined"));
    }
    MetadataType valueType = mapType.getOpenRestriction().get();
    Optional<DslElementSyntax> generic = paramDsl.getGeneric(valueType);

    generic.ifPresent(entryDsl -> objectValue.getParameters()
        .forEach((key, value) -> addKeyValueEntry(entryDsl, valueType, key, value, isParameterWrappedByContainer, mapConfig,
                                                  mapElement, parentConfig, parentElement)));

    if (isParameterWrappedByContainer) {
      ComponentConfiguration result = mapConfig.build();
      parentConfig.withNestedComponent(result);
      parentElement.containing(mapElement.withConfig(result).build());
    } else if (generic.isEmpty()) {
      objectValue.getParameters().forEach((key, value) -> addKeyValueEntry(paramDsl, valueType, key, value, false, null, null,
                                                                           parentConfig, parentElement));
    }
  }

  private void addKeyValueEntry(DslElementSyntax entryDsl, MetadataType valueType, String key, ParameterValue value,
                                boolean isParameterWrappedByContainer, ComponentConfiguration.Builder mapConfig,
                                DslElementModel.Builder<Object> mapElement, ComponentConfiguration.Builder parentConfig,
                                DslElementModel.Builder<Object> parentElement) {
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
    if (isParameterWrappedByContainer) {
      mapConfig.withNestedComponent(entryConfig);
      mapElement.containing(entryElement.withConfig(entryConfig).build());
    } else {
      parentConfig.withNestedComponent(entryConfig);
      parentElement.containing(entryElement.withConfig(entryConfig).build());
    }
  }

  private void createComplexParameter(ParameterObjectValue objectValue, DslElementSyntax paramDsl, ParameterModel parameterModel,
                                      ComponentConfiguration.Builder parentConfig,
                                      DslElementModel.Builder parentElement) {
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

    ObjectType nestedElementType;
    if (objectValue.getTypeId() == null || objectValue.getTypeId().trim().isEmpty() ||
        getId(parameterModel.getType()).map(id -> id.equals(objectValue.getTypeId())).orElse(false)) {

      nestedElementType = (ObjectType) parameterModel.getType();
    } else {
      nestedElementType = lookupType(objectValue);
      context.getTypeCatalog()
          .getDeclaringExtension(objectValue.getTypeId())
          .ifPresent(owner -> context.getExtension(owner)
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
      // skip the compId since it was already added in createComponentElement
      if (parameterModel.isComponentId()) {
        return;
      }

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
    createList(list, listDsl, model, listType, parentConfig, parentElement, isParameterWrappedByContainer(model));
  }

  private void createList(ParameterListValue list,
                          DslElementSyntax listDsl,
                          Object model,
                          ArrayType listType,
                          ComponentConfiguration.Builder parentConfig,
                          DslElementModel.Builder parentElement, boolean isParameterWrappedByContainer) {

    final DslElementModel.Builder listElement = DslElementModel.builder()
        .withModel(model)
        .withDsl(listDsl);

    final ComponentConfiguration.Builder listConfig = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(listDsl));

    final MetadataType itemType = listType.getType();
    listDsl.getGeneric(itemType)
        .ifPresent(itemDsl -> list.getValues()
            .forEach(value -> {
              if (isParameterWrappedByContainer) {
                createListItemConfig(itemType, value, itemDsl, listConfig, listElement);
              } else {
                createListItemConfig(itemType, value, itemDsl, parentConfig, parentElement);
              }
            }));

    if (isParameterWrappedByContainer) {
      ComponentConfiguration result = listConfig.build();
      parentConfig.withNestedComponent(result);
      parentElement.containing(listElement.withConfig(result).build());
    }
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
    checkArgument(isNotBlank(fieldDsl.getElementName()),
                  format("The given component '%s' does not support element-like declaration", fieldDsl.getAttributeName()));
    checkArgument(isNotBlank(fieldDsl.getPrefix()),
                  format("The given component '%s' does not support element-like declaration", fieldDsl.getElementName()));

    return builder()
        .name(fieldDsl.getElementName())
        .namespace(fieldDsl.getPrefix())
        .build();
  }

  private boolean isText(ParameterModel parameter) {
    return parameter.getLayoutModel().map(LayoutModel::isText).orElse(false);
  }

}
