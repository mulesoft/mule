/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal.dsl.model;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getLocalPart;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.extension.api.ExtensionConstants.DISABLE_CONNECTION_VALIDATION_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_BYTE_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.TypeUtils.isContent;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getDefaultValue;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isInfrastructure;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isRequired;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isText;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.EE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_FOREVER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ComposableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
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
import org.mule.runtime.config.spring.api.dsl.model.DslElementModel;
import org.mule.runtime.config.spring.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.FlattenedTypeAnnotation;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of {@link DslElementModelFactory} that creates a {@link DslElementModel} based on its
 * {@link ComponentConfiguration} representation.
 *
 * @since 4.0
 */
// TODO MULE-11496 Delete this factory once everything has an ExtensionModel and can be represented with an ElementDeclaration
class ConfigurationBasedElementModelFactory {

  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  private Map<ExtensionModel, DslSyntaxResolver> resolvers;
  private ExtensionModel currentExtension;
  private DslSyntaxResolver dsl;

  ConfigurationBasedElementModelFactory(Map<ExtensionModel, DslSyntaxResolver> resolvers) {
    this.resolvers = resolvers;
  }

  public <T> Optional<DslElementModel<T>> create(ComponentConfiguration configuration) {
    return Optional.ofNullable(createIdentifiedElement(configuration));
  }

  private DslElementModel createIdentifiedElement(ComponentConfiguration configuration) {

    final ComponentIdentifier identifier = configuration.getIdentifier();

    Optional<Map.Entry<ExtensionModel, DslSyntaxResolver>> entry =
        resolvers.entrySet().stream()
            .filter(e -> e.getKey().getXmlDslModel().getPrefix().equals(identifier.getNamespace()))
            .findFirst();

    if (!entry.isPresent()) {
      return null;
    }

    currentExtension = entry.get().getKey();
    dsl = entry.get().getValue();


    Reference<DslElementModel> elementModel = new Reference<>();
    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        getIdentifier(elementDsl).ifPresent(elementId -> {
          if (elementId.equals(identifier)) {
            DslElementModel.Builder<ConfigurationModel> element = createElementModel(model, elementDsl, configuration);
            addConnectionProvider(model, dsl, element, configuration);
            elementModel.set(element.build());
            stop();
          }
        });
      }

      @Override
      protected void onConstruct(HasConstructModels owner, ConstructModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        getIdentifier(elementDsl).ifPresent(elementId -> {
          if (elementId.equals(identifier)) {
            elementModel.set(createElementModel(model, elementDsl, configuration).build());
            stop();
          }
        });
      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        getIdentifier(elementDsl).ifPresent(elementId -> {
          if (elementId.equals(identifier)) {
            elementModel.set(createElementModel(model, elementDsl, configuration).build());
            stop();
          }
        });
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        getIdentifier(elementDsl).ifPresent(elementId -> {
          if (elementId.equals(identifier)) {
            elementModel.set(createElementModel(model, elementDsl, configuration).build());
            stop();
          }
        });
      }

    }.walk(currentExtension);

    if (elementModel.get() == null) {
      resolveBasedOnTypes(configuration)
          .ifPresent(elementModel::set);
    }

    return elementModel.get();
  }

  private Optional<DslElementModel<ObjectType>> resolveBasedOnTypes(ComponentConfiguration configuration) {
    return currentExtension.getTypes().stream()
        .map(type -> resolveBasedOnType(type, configuration))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  private Optional<DslElementModel<ObjectType>> resolveBasedOnType(ObjectType type, ComponentConfiguration configuration) {
    Optional<DslElementSyntax> typeDsl = dsl.resolve(type);
    if (typeDsl.isPresent()) {
      Optional<ComponentIdentifier> elementIdentifier = getIdentifier(typeDsl.get());
      if (elementIdentifier.isPresent() && elementIdentifier.get().equals(configuration.getIdentifier())) {
        DslElementModel.Builder<ObjectType> typeBuilder = DslElementModel.<ObjectType>builder()
            .withModel(type)
            .withDsl(typeDsl.get())
            .withConfig(configuration);

        populateObjectFields(type, configuration, typeDsl.get(), typeBuilder);

        return Optional.of(typeBuilder.build());
      }
    }
    return Optional.empty();
  }

  private void populateObjectFields(ObjectType type, ComponentConfiguration configuration, DslElementSyntax typeDsl,
                                    DslElementModel.Builder typeBuilder) {
    type.getFields().forEach(field -> {

      if (field.getValue() instanceof ObjectType && field.getAnnotation(FlattenedTypeAnnotation.class).isPresent()) {
        ((ObjectType) field.getValue()).getFields().forEach(nested -> {
          final String name = getLocalPart(nested);
          final DslElementSyntax fieldDsl = typeDsl.getContainedElement(name).get();
          final Optional<String> defaultValue = getDefaultValue(name, type);

          nested.getValue()
              .accept(getComponentChildVisitor(typeBuilder, configuration, nested, name, fieldDsl, defaultValue));
        });

      } else {
        final String name = getLocalPart(field);
        final DslElementSyntax fieldDsl = typeDsl.getContainedElement(name).get();
        final Optional<String> defaultValue = getDefaultValue(name, type);

        field.getValue().accept(getComponentChildVisitor(typeBuilder, configuration, field, name, fieldDsl, defaultValue));
      }
    });
  }

  private Map<ComponentIdentifier, ComponentConfiguration> geteNestedComponents(ComponentConfiguration configuration) {
    return configuration.getNestedComponents().stream()
        .collect(toMap(ComponentConfiguration::getIdentifier, e -> e));
  }

  private MetadataTypeVisitor getComponentChildVisitor(final DslElementModel.Builder typeBuilder,
                                                       final ComponentConfiguration configuration,
                                                       final MetadataType model, final String name,
                                                       final DslElementSyntax modelDsl, final Optional<String> defaultValue) {

    final Map<String, String> parameters = configuration.getParameters();

    return new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        DslElementModel.Builder<MetadataType> elementBuilder = DslElementModel.<MetadataType>builder()
            .withModel(model)
            .withDsl(modelDsl);

        Optional<ComponentIdentifier> identifier = getIdentifier(modelDsl);
        String value = parameters.get(name);
        if (isBlank(value)) {
          if (identifier.isPresent()) {
            ComponentConfiguration nested = geteNestedComponents(configuration).get(identifier.get());
            if (nested != null && nested.getValue().isPresent() && !isBlank(nested.getValue().get())) {
              value = nested.getValue().get().trim();
            }
          } else if (defaultValue.isPresent()) {
            value = defaultValue.get();
            elementBuilder.isExplicitInDsl(false);
          }
        }

        if (!isBlank(value)) {
          typeBuilder.containing(elementBuilder.withValue(value).build());
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        if (isContent(model)) {
          defaultVisit(arrayType);
          return;
        }

        Optional<ComponentIdentifier> identifier = getIdentifier(modelDsl);
        if (identifier.isPresent()) {
          ComponentConfiguration fieldComponent = geteNestedComponents(configuration).get(identifier.get());
          if (fieldComponent != null) {
            DslElementModel.Builder<Object> list = DslElementModel.builder()
                .withModel(model)
                .withDsl(modelDsl)
                .withConfig(fieldComponent);

            modelDsl.getGeneric(arrayType.getType())
                .ifPresent(itemdsl -> {
                  ComponentIdentifier itemIdentifier = getIdentifier(itemdsl).get();

                  fieldComponent.getNestedComponents()
                      .forEach(c -> {
                        if (c.getIdentifier().equals(itemIdentifier)) {
                          getComponentChildVisitor(list, c, arrayType.getType(), VALUE_ATTRIBUTE_NAME, itemdsl, defaultValue);
                        }
                      });

                });

            typeBuilder.containing(list.build());
            return;
          }
        }

        defaultValue.ifPresent(s -> typeBuilder.containing(DslElementModel.builder()
            .withModel(model)
            .withDsl(modelDsl)
            .withValue(defaultValue.get())
            .isExplicitInDsl(false)
            .build()));
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (isContent(model)) {
          defaultVisit(objectType);
          return;
        }

        Optional<ComponentIdentifier> identifier = getIdentifier(modelDsl);
        if (identifier.isPresent()) {
          if (isMap(objectType)) {
            typeBuilder.containing(createMapElement(objectType, modelDsl, configuration));
            return;
          }

          ComponentConfiguration fieldComponent = geteNestedComponents(configuration).get(identifier.get());
          fieldComponent = fieldComponent == null ? configuration : fieldComponent;

          String value = fieldComponent.getParameters().get(modelDsl.getAttributeName());
          if (!isBlank(value)) {
            typeBuilder.containing(DslElementModel.builder()
                .withModel(model)
                .withDsl(modelDsl)
                .withValue(value)
                .build());
          } else {
            resolveBasedOnType(objectType, fieldComponent).ifPresent(typeBuilder::containing);
          }

          return;
        }

        defaultValue.ifPresent(s -> typeBuilder.containing(DslElementModel.builder()
            .withModel(model)
            .withDsl(modelDsl)
            .withValue(defaultValue.get())
            .isExplicitInDsl(false)
            .build()));
      }
    };
  }

  private DslElementModel createMapElement(ObjectType objectType, DslElementSyntax modelDsl,
                                           ComponentConfiguration configuration) {
    DslElementModel.Builder<Object> mapBuilder = DslElementModel.builder()
        .withModel(objectType)
        .withDsl(modelDsl)
        .withConfig(configuration);

    if (!configuration.getNestedComponents().isEmpty()) {
      populateMapEntries(objectType, modelDsl, mapBuilder, configuration);
    }

    return mapBuilder.build();
  }

  private void populateMapEntries(ObjectType objectType, DslElementSyntax modelDsl, DslElementModel.Builder mapBuilder,
                                  ComponentConfiguration mapConfig) {
    mapConfig.getNestedComponents().forEach(entryConfig -> {
      MetadataType entryType = objectType.getOpenRestriction().get();
      modelDsl.getGeneric(entryType).ifPresent(entryDsl -> {
        DslElementModel.Builder<Object> entry = DslElementModel.builder()
            .withModel(entryType)
            .withConfig(entryConfig)
            .withDsl(entryDsl);

        entry.containing(DslElementModel.builder()
            .withModel(typeLoader.load(String.class))
            .withValue(entryConfig.getParameters().get(KEY_ATTRIBUTE_NAME))
            .withDsl(entryDsl.getAttribute(KEY_ATTRIBUTE_NAME).get())
            .build());

        String value = entryConfig.getParameters().get(VALUE_ATTRIBUTE_NAME);
        if (isBlank(value)) {
          getComponentChildVisitor(entry, entryConfig, entryType,
                                   VALUE_ATTRIBUTE_NAME, entryDsl.getAttribute(VALUE_ATTRIBUTE_NAME).get(),
                                   empty());
        } else {
          entry.containing(DslElementModel.builder()
              .withModel(typeLoader.load(String.class))
              .withValue(value)
              .withDsl(entryDsl.getAttribute(VALUE_ATTRIBUTE_NAME).get())
              .build());
        }

        mapBuilder.containing(entry.build());
      });
    });
  }

  private DslElementModel.Builder<ConfigurationModel> addConnectionProvider(ConfigurationModel model,
                                                                            DslSyntaxResolver dsl,
                                                                            DslElementModel.Builder<ConfigurationModel> element,
                                                                            ComponentConfiguration configuration) {

    concat(model.getConnectionProviders().stream(), currentExtension.getConnectionProviders()
        .stream())
            .map(provider -> {
              DslElementSyntax providerDsl = dsl.resolve(provider);
              ComponentIdentifier identifier = getIdentifier(providerDsl).orElse(null);
              return configuration.getNestedComponents().stream()
                  .filter(c -> c.getIdentifier().equals(identifier))
                  .findFirst()
                  .map(providerConfig -> element.containing(createElementModel(provider, providerDsl, providerConfig).build()))
                  .orElse(null);
            })
            .filter(Objects::nonNull)
            .findFirst();

    return element;
  }

  private <T extends ParameterizedModel> DslElementModel.Builder<T> createElementModel(T model, DslElementSyntax elementDsl,
                                                                                       ComponentConfiguration configuration) {
    DslElementModel.Builder<T> builder = DslElementModel.builder();
    builder.withModel(model)
        .withDsl(elementDsl)
        .withConfig(configuration);

    populateParameterizedElements(model, elementDsl, builder, configuration);
    if (model instanceof ComposableModel) {
      populateComposableElements((ComposableModel) model, elementDsl, builder, configuration);
    }

    if (model instanceof SourceModel) {
      ((SourceModel) model).getSuccessCallback()
          .ifPresent(cb -> populateParameterizedElements(cb, elementDsl, builder, configuration));

      ((SourceModel) model).getErrorCallback()
          .ifPresent(cb -> populateParameterizedElements(cb, elementDsl, builder, configuration));
    }

    return builder;
  }

  private void populateParameterizedElements(ParameterizedModel model, DslElementSyntax elementDsl,
                                             DslElementModel.Builder builder, ComponentConfiguration configuration) {

    Map<ComponentIdentifier, ComponentConfiguration> innerComponents = geteNestedComponents(configuration);
    Map<String, String> parameters = configuration.getParameters();

    List<ParameterModel> inlineGroupedParameters = model.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .peek(group -> addInlineGroup(elementDsl, innerComponents, parameters, builder, group))
        .flatMap(g -> g.getParameterModels().stream())
        .collect(toList());

    model.getAllParameterModels().stream()
        .filter(p -> !inlineGroupedParameters.contains(p))
        .forEach(p -> addElementParameter(innerComponents, parameters, elementDsl, builder, p));
  }

  private void populateComposableElements(ComposableModel model, DslElementSyntax elementDsl,
                                          DslElementModel.Builder builder, ComponentConfiguration configuration) {

    configuration.getNestedComponents()
        .forEach(nestedComponentConfig -> {
          DslElementModel nestedElement = createIdentifiedElement(nestedComponentConfig);
          if (nestedElement != null) {
            builder.containing(nestedElement);
          } else {
            model.getNestedComponents()
                .stream()
                .filter(nestedModel -> nestedModel instanceof NestedRouteModel)
                .filter(nestedModel -> elementDsl.getContainedElement(nestedModel.getName())
                    .map(nestedDsl -> getIdentifier(nestedDsl).map(id -> nestedComponentConfig.getIdentifier().equals(id))
                        .orElse(false))
                    .orElse(false))
                .findFirst()
                .ifPresent(nestedModel -> {
                  DslElementSyntax routeDsl = elementDsl.getContainedElement(nestedModel.getName()).get();
                  DslElementModel.Builder<? extends NestableElementModel> routeBuilder =
                      DslElementModel.<NestableElementModel>builder()
                          .withModel(nestedModel)
                          .withDsl(routeDsl)
                          .withConfig(nestedComponentConfig)
                          .isExplicitInDsl(true);

                  populateParameterizedElements((ParameterizedModel) nestedModel, routeDsl, routeBuilder, nestedComponentConfig);
                  nestedComponentConfig.getNestedComponents()
                      .forEach(routeElement -> routeBuilder.containing(createIdentifiedElement(routeElement)));

                  builder.containing(routeBuilder.build());
                });
          }
        });
  }

  private void addInlineGroup(DslElementSyntax elementDsl,
                              Map<ComponentIdentifier, ComponentConfiguration> innerComponents,
                              Map<String, String> parameters,
                              DslElementModel.Builder parent, ParameterGroupModel group) {
    elementDsl.getChild(group.getName())
        .ifPresent(groupDsl -> {
          Optional<ComponentIdentifier> identifier = getIdentifier(groupDsl);
          if (!identifier.isPresent()) {
            return;
          }

          ComponentConfiguration groupComponent = identifier.map(innerComponents::get).orElse(null);
          if (groupComponent != null) {
            DslElementModel.Builder<ParameterGroupModel> groupElementBuilder = DslElementModel.<ParameterGroupModel>builder()
                .withModel(group)
                .withDsl(groupDsl)
                .withConfig(groupComponent);

            Map<ComponentIdentifier, ComponentConfiguration> groupInnerComponents = geteNestedComponents(groupComponent);
            group.getParameterModels()
                .forEach(p -> addElementParameter(groupInnerComponents, parameters, groupDsl, groupElementBuilder, p));

            parent.containing(groupElementBuilder.build());

          } else if (shoulBuildDefaultGroup(group)) {
            builDefaultInlineGroupElement(parent, group, groupDsl, identifier.get());
          }
        });
  }

  private void builDefaultInlineGroupElement(DslElementModel.Builder parent, ParameterGroupModel group, DslElementSyntax groupDsl,
                                             ComponentIdentifier identifier) {
    ComponentConfiguration.Builder groupConfigBuilder = ComponentConfiguration.builder().withIdentifier(identifier);
    DslElementModel.Builder<ParameterGroupModel> groupElementBuilder = DslElementModel.<ParameterGroupModel>builder()
        .withModel(group)
        .isExplicitInDsl(false)
        .withDsl(groupDsl);

    group.getParameterModels()
        .forEach(paramModel -> groupDsl.getContainedElement(paramModel.getName())
            .ifPresent(paramDsl -> getDefaultValue(paramModel)
                .ifPresent(defaultValue -> {
                  DslElementModel.Builder<ParameterModel> paramElementBuilder = DslElementModel.<ParameterModel>builder()
                      .withModel(paramModel)
                      .withDsl(paramDsl)
                      .isExplicitInDsl(false)
                      .withValue(defaultValue);

                  if (isContent(paramModel) || isText(paramModel)) {
                    getIdentifier(paramDsl)
                        .ifPresent(tagId -> groupConfigBuilder.withNestedComponent(ComponentConfiguration.builder()
                            .withIdentifier(tagId)
                            .withValue(defaultValue)
                            .build()));
                  } else {
                    groupConfigBuilder.withParameter(paramDsl.getAttributeName(), defaultValue);
                  }

                  groupElementBuilder.containing(paramElementBuilder.build());
                })));

    groupElementBuilder.withConfig(groupConfigBuilder.build());
    parent.containing(groupElementBuilder.build());
  }

  private boolean shoulBuildDefaultGroup(ParameterGroupModel group) {
    return !isRequired(group) && group.getParameterModels().stream().anyMatch(p -> getDefaultValue(p).isPresent());
  }

  private void addElementParameter(Map<ComponentIdentifier, ComponentConfiguration> innerComponents,
                                   Map<String, String> parameters,
                                   DslElementSyntax groupDsl, DslElementModel.Builder<ParameterGroupModel> groupElementBuilder,
                                   ParameterModel paramModel) {

    groupDsl.getContainedElement(paramModel.getName())
        .ifPresent(paramDsl -> {

          if (isInfrastructure(paramModel)) {
            handleInfrastructure(paramModel, paramDsl, innerComponents, parameters, groupElementBuilder);
            return;
          }

          ComponentConfiguration paramComponent = getIdentifier(paramDsl).map(innerComponents::get).orElse(null);

          if (paramDsl.isWrapped()) {
            resolveWrappedElement(groupElementBuilder, paramModel, paramDsl, paramComponent);
            return;
          }

          String value = paramDsl.supportsAttributeDeclaration() ? parameters.get(paramDsl.getAttributeName()) : null;
          Optional<String> defaultValue = getDefaultValue(paramModel);
          if (paramComponent != null || !isBlank(value) || defaultValue.isPresent()) {

            DslElementModel.Builder<ParameterModel> paramElementBuilder = DslElementModel.<ParameterModel>builder()
                .withModel(paramModel).withDsl(paramDsl);

            if (paramComponent != null && !isContent(paramModel)) {

              paramElementBuilder.withConfig(paramComponent);
              paramModel.getType().accept(new MetadataTypeVisitor() {

                @Override
                public void visitArrayType(ArrayType arrayType) {
                  MetadataType itemType = arrayType.getType();
                  paramDsl.getGeneric(itemType)
                      .ifPresent(itemdsl -> {
                        ComponentIdentifier itemIdentifier = getIdentifier(itemdsl).get();

                        paramComponent.getNestedComponents().forEach(c -> {
                          if (c.getIdentifier().equals(itemIdentifier)) {
                            itemType.accept(
                                            getComponentChildVisitor(paramElementBuilder, c, itemType, VALUE_ATTRIBUTE_NAME,
                                                                     itemdsl,
                                                                     defaultValue));
                          }
                        });

                      });
                }

                @Override
                public void visitObject(ObjectType objectType) {
                  if (isMap(objectType)) {
                    populateMapEntries(objectType, paramDsl, paramElementBuilder, paramComponent);
                    return;
                  }

                  populateObjectFields(objectType, paramComponent, paramDsl, paramElementBuilder);
                }
              });

            } else {
              if (isBlank(value)) {
                if (paramComponent != null && paramComponent.getValue().isPresent() && !isBlank(
                                                                                                paramComponent.getValue()
                                                                                                    .get())) {
                  value = paramComponent.getValue().get().trim();
                } else if (defaultValue.isPresent()) {
                  value = defaultValue.get();
                  paramElementBuilder.isExplicitInDsl(false);
                }
              }
              paramElementBuilder.withValue(value);
            }

            groupElementBuilder.containing(paramElementBuilder.build());
          }

        });
  }

  private void resolveWrappedElement(DslElementModel.Builder<ParameterGroupModel> groupElementBuilder, ParameterModel p,
                                     DslElementSyntax pDsl, ComponentConfiguration paramComponent) {
    if (paramComponent != null) {
      DslElementModel.Builder<ParameterModel> paramElement =
          DslElementModel.<ParameterModel>builder().withModel(p).withDsl(pDsl).withConfig(paramComponent);

      if (paramComponent.getNestedComponents().size() > 0) {
        ComponentConfiguration wrappedComponent = paramComponent.getNestedComponents().get(0);
        this.create(wrappedComponent).ifPresent(paramElement::containing);
      }

      groupElementBuilder.containing(paramElement.build());
    }
  }

  private Optional<ComponentIdentifier> getIdentifier(DslElementSyntax dsl) {
    if (isNotBlank(dsl.getElementName()) && isNotBlank(dsl.getPrefix())) {
      return Optional.of(builder()
          .name(dsl.getElementName())
          .namespace(dsl.getPrefix())
          .build());
    }

    return empty();
  }

  private void handleInfrastructure(final ParameterModel paramModel, final DslElementSyntax paramDsl,
                                    final Map<ComponentIdentifier, ComponentConfiguration> nested,
                                    final Map<String, String> parameters,
                                    final DslElementModel.Builder<ParameterGroupModel> groupElementBuilder) {

    switch (paramModel.getName()) {
      case RECONNECTION_STRATEGY_PARAMETER_NAME:
        ComponentIdentifier reconnectId = newIdentifier(RECONNECT_ELEMENT_IDENTIFIER,
                                                        paramDsl.getPrefix());

        ComponentConfiguration config = nested.containsKey(reconnectId)
            ? nested.get(reconnectId)
            : nested.get(newIdentifier(RECONNECT_FOREVER_ELEMENT_IDENTIFIER, paramDsl.getPrefix()));

        if (config != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, config));
        }
        return;

      case REDELIVERY_POLICY_PARAMETER_NAME:
        ComponentConfiguration redelivery = nested.get(newIdentifier(REDELIVERY_POLICY_ELEMENT_IDENTIFIER,
                                                                     paramDsl.getPrefix()));
        if (redelivery != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, redelivery));
        }
        return;

      case POOLING_PROFILE_PARAMETER_NAME:
        ComponentConfiguration pooling = nested.get(getIdentifier(paramDsl).get());
        if (pooling != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, pooling));
        }
        return;

      case DISABLE_CONNECTION_VALIDATION_PARAMETER_NAME:
        if (!isBlank(parameters.get(DISABLE_CONNECTION_VALIDATION_PARAMETER_NAME))) {
          groupElementBuilder.containing(DslElementModel.builder()
              .withModel(paramModel)
              .withDsl(paramDsl)
              .withValue(parameters.get(DISABLE_CONNECTION_VALIDATION_PARAMETER_NAME))
              .build());
        }
        return;

      case STREAMING_STRATEGY_PARAMETER_NAME:
        Set<ComponentIdentifier> streaming =
            newHashSet(newIdentifier(NON_REPEATABLE_BYTE_STREAM_ALIAS, CORE_PREFIX),
                       newIdentifier(REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS, CORE_PREFIX),
                       newIdentifier(REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS, EE_PREFIX),
                       newIdentifier(REPEATABLE_IN_MEMORY_OBJECTS_STREAM_ALIAS, CORE_PREFIX),
                       newIdentifier(REPEATABLE_FILE_STORE_OBJECTS_STREAM_ALIAS, EE_PREFIX),
                       newIdentifier(NON_REPEATABLE_OBJECTS_STREAM_ALIAS, CORE_PREFIX));

        streaming.stream().filter(nested::containsKey).findFirst()
            .ifPresent(s -> groupElementBuilder.containing(newElementModel(paramModel, paramDsl, nested.get(s))));
        return;

      case TLS_PARAMETER_NAME:
        ComponentConfiguration tls = nested.get(getIdentifier(paramDsl).get());
        if (tls != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, tls));
        } else if (!isBlank(parameters.get(TLS_PARAMETER_NAME))) {
          groupElementBuilder.containing(DslElementModel.builder()
              .withModel(paramModel)
              .withDsl(paramDsl)
              .withValue(parameters.get(TLS_PARAMETER_NAME))
              .build());
        }

        return;
    }

  }

  private ComponentIdentifier newIdentifier(String name, String ns) {
    return ComponentIdentifier.builder().name(name).namespace(ns).build();
  }

  private DslElementModel newElementModel(ParameterModel paramModel, DslElementSyntax paramDsl,
                                          ComponentConfiguration configuration) {
    return DslElementModel.builder()
        .withModel(paramModel)
        .withDsl(paramDsl)
        .withConfig(configuration)
        .build();
  }

}
