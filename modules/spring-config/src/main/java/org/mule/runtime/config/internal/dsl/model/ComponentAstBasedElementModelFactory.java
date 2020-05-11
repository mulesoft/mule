/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getLocalPart;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.ast.api.ComponentAst.BODY_RAW_PARAM_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.EXPIRATION_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_CONFIG_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.SCHEDULING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_BYTE_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getDefaultValue;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isInfrastructure;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isRequired;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isText;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.CRON_STRATEGY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.EE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.EXPIRATION_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_FOREVER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ComposableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
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
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.core.api.source.scheduler.CronScheduler;
import org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.FlattenedTypeAnnotation;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Implementation of {@link DslElementModelFactory} that creates a {@link DslElementModel} based on its {@link ComponentAst}
 * representation.
 *
 * @since 4.0
 */
// TODO MULE-11496 Delete this factory once everything has an ExtensionModel and can be represented with an ElementDeclaration
class ComponentAstBasedElementModelFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComponentAstBasedElementModelFactory.class);

  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  private final Map<ExtensionModel, DslSyntaxResolver> resolvers;
  private ExtensionModel currentExtension;
  private DslSyntaxResolver dsl;

  ComponentAstBasedElementModelFactory(Map<ExtensionModel, DslSyntaxResolver> resolvers) {
    this.resolvers = resolvers;
  }

  public <T> Optional<DslElementModel<T>> create(ComponentAst configuration) {
    return ofNullable(createIdentifiedElement(configuration));
  }

  private DslElementModel createIdentifiedElement(ComponentAst configuration) {

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
        getIdentifier(elementDsl)
            .filter(elementId -> elementId.equals(identifier))
            .ifPresent(elementId -> {
              DslElementModel.Builder<ConfigurationModel> builder = DslElementModel.<ConfigurationModel>builder()
                  .withModel(model)
                  .withDsl(elementDsl)
                  .withConfig(configuration);

              addConnectionProvider(model, dsl, builder, configuration);

              enrichElementModel(model, elementDsl, configuration, builder);

              elementModel.set(builder.build());
              stop();
            });
      }

      @Override
      protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
        final DslElementSyntax providerDsl = dsl.resolve(model);
        getIdentifier(providerDsl)
            .filter(elementId -> elementId.equals(identifier))
            .ifPresent(elementId -> {
              elementModel.set(createConnectionProviderModel(model, providerDsl, configuration));
              stop();
            });
      }

      @Override
      protected void onConstruct(HasConstructModels owner, ConstructModel model) {
        onComponentModel(model);
      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        onComponentModel(model);
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        onComponentModel(model);
      }

      private void onComponentModel(final ComponentModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        getIdentifier(elementDsl)
            .filter(elementId -> elementId.equals(identifier))
            .ifPresent(elementId -> {
              elementModel.set(createElementModel(model, elementDsl, configuration).build());
              stop();
            });
      }

    }.walk(currentExtension);

    if (elementModel.get() == null) {
      resolveBasedOnTypes(configuration)
          .ifPresent(elementModel::set);
    }

    return elementModel.get();
  }

  private Optional<DslElementModel<ObjectType>> resolveBasedOnTypes(ComponentAst configuration) {
    return currentExtension.getTypes().stream()
        .map(type -> resolveBasedOnType(type, configuration, new ArrayDeque<>()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  private void withStackControl(Deque<String> typeResolvingStack, String stackId, Runnable action) {
    if (!typeResolvingStack.contains(stackId)) {
      typeResolvingStack.push(stackId);
      action.run();
      typeResolvingStack.pop();
    }
  }

  private Optional<DslElementModel<ObjectType>> resolveBasedOnType(ObjectType type,
                                                                   ComponentAst configuration,
                                                                   Deque<String> typeResolvingStack) {
    Optional<DslElementSyntax> typeDsl = dsl.resolve(type);
    if (typeDsl.isPresent()) {
      Optional<ComponentIdentifier> elementIdentifier = getIdentifier(typeDsl.get());
      if (elementIdentifier.isPresent() && elementIdentifier.get().equals(configuration.getIdentifier())) {
        DslElementModel.Builder<ObjectType> typeBuilder = DslElementModel.<ObjectType>builder()
            .withModel(type)
            .withDsl(typeDsl.get())
            .withConfig(configuration);

        getId(type).ifPresent(id -> withStackControl(typeResolvingStack, id,
                                                     () -> populateObjectFields(type, configuration, typeDsl.get(),
                                                                                typeBuilder, typeResolvingStack)));

        return Optional.of(typeBuilder.build());
      }
    }
    return Optional.empty();
  }

  private void populateObjectFields(ObjectType type, ComponentAst configuration, DslElementSyntax typeDsl,
                                    DslElementModel.Builder typeBuilder, Deque<String> typeResolvingStack) {
    LOGGER.trace("populateObjectFields: type: '{}'", type);

    type.getFields().forEach(field -> {

      if (field.getValue() instanceof ObjectType && field.getAnnotation(FlattenedTypeAnnotation.class).isPresent()) {
        ((ObjectType) field.getValue()).getFields().forEach(nested -> {
          final String name = getLocalPart(nested);
          LOGGER.trace("populateObjectFields: type: '{}', flattened: {}, field: {}", type, field.getValue(), name);
          typeDsl.getContainedElement(name)
              .ifPresent(fieldDsl -> nested.getValue()
                  .accept(getComponentChildVisitor(typeBuilder, configuration, nested, name, fieldDsl,
                                                   getDefaultValue(name, field.getValue()),
                                                   typeResolvingStack)));

        });

      } else {
        final String name = getLocalPart(field);
        LOGGER.trace("populateObjectFields: type: '{}', field: {}", type, name);
        typeDsl.getContainedElement(name)
            .ifPresent(fieldDsl -> field.getValue()
                .accept(getComponentChildVisitor(typeBuilder, configuration, field, name, fieldDsl,
                                                 getDefaultValue(name, type), typeResolvingStack)));
      }
    });
  }

  private Multimap<ComponentIdentifier, ComponentAst> getNestedComponents(ComponentAst configuration) {
    Multimap<ComponentIdentifier, ComponentAst> result = ArrayListMultimap.create();
    configuration.directChildrenStream().forEach(componentConfiguration -> {
      result.put(componentConfiguration.getIdentifier(), componentConfiguration);
    });
    return result;
  }

  private MetadataTypeVisitor getComponentChildVisitor(final DslElementModel.Builder typeBuilder,
                                                       final ComponentAst configuration,
                                                       final MetadataType model, final String name,
                                                       final DslElementSyntax modelDsl, final Optional<String> defaultValue,
                                                       Deque<String> typeResolvingStack) {

    return new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        DslElementModel.Builder<MetadataType> elementBuilder = DslElementModel.<MetadataType>builder()
            .withModel(model)
            .withDsl(modelDsl);

        Optional<ComponentIdentifier> identifier = getIdentifier(modelDsl);
        String value = configuration.getRawParameterValue(name).orElse(null);
        LOGGER.trace("getComponentChildVisitor#defaultVisit: '{}': '{}'", identifier, value);
        if (isBlank(value)) {
          if (identifier.isPresent()) {
            ComponentAst nested = getSingleComponentConfiguration(getNestedComponents(configuration), identifier);
            if (nested != null && nested.getRawParameterValue(BODY_RAW_PARAM_NAME).isPresent()
                && !isBlank(nested.getRawParameterValue(BODY_RAW_PARAM_NAME).get())) {
              value = nested.getRawParameterValue(BODY_RAW_PARAM_NAME).get().trim();
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
        Optional<ComponentIdentifier> identifier = getIdentifier(modelDsl);
        if (identifier.isPresent()) {
          LOGGER.trace("getComponentChildVisitor#visitArrayType: '{}'", identifier.get());
          ComponentAst fieldComponent = getSingleComponentConfiguration(getNestedComponents(configuration), identifier);
          if (fieldComponent != null) {
            DslElementModel.Builder<Object> list = DslElementModel.builder()
                .withModel(model)
                .withDsl(modelDsl)
                .withConfig(fieldComponent);

            modelDsl.getGeneric(arrayType.getType())
                .ifPresent(itemdsl -> {
                  ComponentIdentifier itemIdentifier = getIdentifier(itemdsl).get();

                  fieldComponent.directChildrenStream()
                      .forEach(c -> {
                        if (c.getIdentifier().equals(itemIdentifier)) {
                          getComponentChildVisitor(list, c, arrayType.getType(), VALUE_ATTRIBUTE_NAME, itemdsl, defaultValue,
                                                   typeResolvingStack);
                        }
                      });

                });

            typeBuilder.containing(list.build());
            return;
          }
        }

        LOGGER.trace("getComponentChildVisitor#visitArrayType: noIdentifier", identifier.get());
        defaultValue.ifPresent(s -> typeBuilder.containing(DslElementModel.builder()
            .withModel(model)
            .withDsl(modelDsl)
            .withValue(defaultValue.get())
            .isExplicitInDsl(false)
            .build()));
      }

      @Override
      public void visitObject(ObjectType objectType) {
        Optional<ComponentIdentifier> identifier = getIdentifier(modelDsl);
        if (identifier.isPresent()) {
          LOGGER.trace("getComponentChildVisitor#visitObject: '{}'", identifier.get());

          ComponentAst fieldComponent = getSingleComponentConfiguration(getNestedComponents(configuration), identifier);

          if (isMap(objectType)) {
            LOGGER.trace("getComponentChildVisitor#visitObject: '{}' -> isMap", identifier.get());
            typeBuilder.containing(createMapElement(objectType, modelDsl, fieldComponent));
            return;
          }

          fieldComponent = fieldComponent == null ? configuration : fieldComponent;

          String value = fieldComponent.getRawParameterValue(modelDsl.getAttributeName()).orElse(null);
          if (!isBlank(value)) {
            typeBuilder.containing(DslElementModel.builder()
                .withModel(model)
                .withDsl(modelDsl)
                .withValue(value)
                .build());
          } else {
            resolveBasedOnType(objectType, fieldComponent, typeResolvingStack)
                .ifPresent(typeBuilder::containing);
          }

          return;
        }

        LOGGER.trace("getComponentChildVisitor#visitObject: noIdentifier");
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
                                           ComponentAst configuration) {
    DslElementModel.Builder<Object> mapBuilder = DslElementModel.builder()
        .withModel(objectType)
        .withDsl(modelDsl)
        .withConfig(configuration);

    if (configuration != null && configuration.directChildrenStream().count() > 0) {
      populateMapEntries(objectType, modelDsl, mapBuilder, configuration);
    }

    return mapBuilder.build();
  }

  private void populateMapEntries(ObjectType objectType, DslElementSyntax modelDsl, DslElementModel.Builder mapBuilder,
                                  ComponentAst mapConfig) {
    mapConfig.directChildrenStream().forEach(entryConfig -> {
      MetadataType entryType = objectType.getOpenRestriction().get();
      modelDsl.getGeneric(entryType).ifPresent(entryDsl -> {
        DslElementModel.Builder<Object> entry = DslElementModel.builder()
            .withModel(entryType)
            .withConfig(entryConfig)
            .withDsl(entryDsl);

        entry.containing(DslElementModel.builder()
            .withModel(typeLoader.load(String.class))
            .withValue(entryConfig.getRawParameterValue(KEY_ATTRIBUTE_NAME).orElse(null))
            .withDsl(entryDsl.getAttribute(KEY_ATTRIBUTE_NAME).get())
            .build());

        String value = entryConfig.getRawParameterValue(VALUE_ATTRIBUTE_NAME).orElse(null);
        if (isBlank(value)) {
          getComponentChildVisitor(entry, entryConfig, entryType,
                                   VALUE_ATTRIBUTE_NAME, entryDsl.getAttribute(VALUE_ATTRIBUTE_NAME).get(),
                                   empty(), new ArrayDeque<>());
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

  private DslElementModel<ConnectionProviderModel> createConnectionProviderModel(ConnectionProviderModel providerModel,
                                                                                 DslElementSyntax providerDsl,
                                                                                 ComponentAst providerConfig) {
    return createElementModel(providerModel, providerDsl, providerConfig).build();

  }

  private DslElementModel.Builder<ConfigurationModel> addConnectionProvider(ConfigurationModel model,
                                                                            DslSyntaxResolver dsl,
                                                                            DslElementModel.Builder<ConfigurationModel> element,
                                                                            ComponentAst configuration) {

    concat(model.getConnectionProviders().stream(), currentExtension.getConnectionProviders()
        .stream())
            .map(provider -> {
              DslElementSyntax providerDsl = dsl.resolve(provider);
              ComponentIdentifier identifier = getIdentifier(providerDsl).orElse(null);
              return configuration.directChildrenStream()
                  .filter(c -> c.getIdentifier().equals(identifier))
                  .findFirst()
                  .map(providerConfig -> element.containing(createConnectionProviderModel(provider, providerDsl, providerConfig))
                      .build())
                  .orElse(null);
            })
            .filter(Objects::nonNull)
            .findFirst();

    return element;
  }

  private <T extends ParameterizedModel> DslElementModel.Builder<T> createElementModel(T model, DslElementSyntax elementDsl,
                                                                                       ComponentAst configuration) {
    DslElementModel.Builder<T> builder = DslElementModel.builder();
    builder.withModel(model)
        .withDsl(elementDsl)
        .withConfig(configuration);

    enrichElementModel(model, elementDsl, configuration, builder);

    return builder;
  }

  private <T extends ParameterizedModel> void enrichElementModel(T model, DslElementSyntax elementDsl,
                                                                 ComponentAst configuration,
                                                                 DslElementModel.Builder<T> builder) {
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
  }

  private void populateParameterizedElements(ParameterizedModel model, DslElementSyntax elementDsl,
                                             DslElementModel.Builder builder, ComponentAst configuration) {

    Multimap<ComponentIdentifier, ComponentAst> innerComponents = getNestedComponents(configuration);
    Map<String, String> parameters = ((org.mule.runtime.config.internal.model.ComponentModel) configuration).getRawParameters();

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
                                          DslElementModel.Builder builder, ComponentAst configuration) {

    configuration.directChildrenStream()
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
                  nestedComponentConfig.directChildrenStream()
                      .forEach(routeElement -> {
                        DslElementModel nestableElementModel = createIdentifiedElement(routeElement);
                        if (nestableElementModel != null) {
                          routeBuilder.containing(nestableElementModel);
                        }
                      });

                  builder.containing(routeBuilder.build());
                });
          }
        });
  }

  private void addInlineGroup(DslElementSyntax elementDsl,
                              Multimap<ComponentIdentifier, ComponentAst> innerComponents,
                              Map<String, String> parameters,
                              DslElementModel.Builder parent, ParameterGroupModel group) {
    elementDsl.getChild(group.getName())
        .ifPresent(groupDsl -> {
          Optional<ComponentIdentifier> identifier = getIdentifier(groupDsl);
          if (!identifier.isPresent()) {
            return;
          }

          ComponentAst groupComponent = getSingleComponentConfiguration(innerComponents, identifier);
          if (groupComponent != null) {
            DslElementModel.Builder<ParameterGroupModel> groupElementBuilder = DslElementModel.<ParameterGroupModel>builder()
                .withModel(group)
                .withDsl(groupDsl)
                .withConfig(groupComponent);

            Multimap<ComponentIdentifier, ComponentAst> groupInnerComponents = getNestedComponents(groupComponent);
            group.getParameterModels()
                .forEach(p -> addElementParameter(groupInnerComponents, parameters, groupDsl, groupElementBuilder, p));

            parent.containing(groupElementBuilder.build());

          } else if (shouldBuildDefaultGroup(group)) {
            buildDefaultInlineGroupElement(parent, group, groupDsl, identifier.get());
          }
        });
  }

  private ComponentAst getSingleComponentConfiguration(Multimap<ComponentIdentifier, ComponentAst> innerComponents,
                                                       Optional<ComponentIdentifier> identifier) {
    return identifier.filter(innerComponents::containsKey)
        .map(innerComponents::get)
        .map(collection -> collection.iterator().next())
        .orElse(null);
  }

  private void buildDefaultInlineGroupElement(DslElementModel.Builder parent, ParameterGroupModel group,
                                              DslElementSyntax groupDsl,
                                              ComponentIdentifier identifier) {
    final org.mule.runtime.config.internal.model.ComponentModel.Builder groupCompAstBuilder =
        new org.mule.runtime.config.internal.model.ComponentModel.Builder();
    groupCompAstBuilder.setIdentifier(identifier);

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
                        .ifPresent(tagId -> groupCompAstBuilder
                            .addChildComponentModel(new org.mule.runtime.config.internal.model.ComponentModel.Builder()
                                .setIdentifier(tagId)
                                .setTextContent(defaultValue)
                                .build()));
                  } else {
                    groupCompAstBuilder.addParameter(paramDsl.getAttributeName(), defaultValue, true);
                  }

                  groupElementBuilder.containing(paramElementBuilder.build());
                })));

    groupElementBuilder.withConfig(groupCompAstBuilder.build());
    parent.containing(groupElementBuilder.build());
  }

  private boolean shouldBuildDefaultGroup(ParameterGroupModel group) {
    return !isRequired(group) && group.getParameterModels().stream().anyMatch(p -> getDefaultValue(p).isPresent());
  }

  private void addElementParameter(Multimap<ComponentIdentifier, ComponentAst> innerComponents,
                                   Map<String, String> parameters,
                                   DslElementSyntax groupDsl, DslElementModel.Builder<ParameterGroupModel> groupElementBuilder,
                                   ParameterModel paramModel) {

    groupDsl.getContainedElement(paramModel.getName())
        .ifPresent(paramDsl -> {

          if (isInfrastructure(paramModel)) {
            handleInfrastructure(paramModel, paramDsl, innerComponents, parameters, groupElementBuilder);
            return;
          }

          ComponentAst paramComponent = getSingleComponentConfiguration(innerComponents, getIdentifier(paramDsl));

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

                        paramComponent.directChildrenStream().forEach(c -> {
                          if (c.getIdentifier().equals(itemIdentifier)) {
                            itemType.accept(
                                            getComponentChildVisitor(paramElementBuilder, c, itemType, VALUE_ATTRIBUTE_NAME,
                                                                     itemdsl,
                                                                     defaultValue,
                                                                     new ArrayDeque<>()));
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

                  populateObjectFields(objectType, paramComponent, paramDsl, paramElementBuilder, new ArrayDeque<>());
                }
              });

            } else {
              if (isBlank(value)) {
                if (paramComponent != null && paramComponent.getRawParameterValue(BODY_RAW_PARAM_NAME).isPresent() && !isBlank(
                                                                                                                               paramComponent
                                                                                                                                   .getRawParameterValue(BODY_RAW_PARAM_NAME)
                                                                                                                                   .get())) {
                  value = paramComponent.getRawParameterValue(BODY_RAW_PARAM_NAME).get().trim();
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
                                     DslElementSyntax pDsl, ComponentAst paramComponent) {
    if (paramComponent != null) {
      DslElementModel.Builder<ParameterModel> paramElement = DslElementModel.<ParameterModel>builder()
          .withModel(p)
          .withDsl(pDsl)
          .withConfig(paramComponent);

      if (paramComponent.directChildrenStream().count() > 0) {
        ExtensionModel wrapperExtension = this.currentExtension;
        DslSyntaxResolver wrapperDsl = this.dsl;

        ComponentAst wrappedComponent = paramComponent.directChildrenStream().findFirst().get();
        this.create(wrappedComponent).ifPresent(paramElement::containing);

        this.currentExtension = wrapperExtension;
        this.dsl = wrapperDsl;
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
                                    final Multimap<ComponentIdentifier, ComponentAst> nested,
                                    final Map<String, String> parameters,
                                    final DslElementModel.Builder<ParameterGroupModel> groupElementBuilder) {

    switch (paramModel.getName()) {
      case RECONNECTION_CONFIG_PARAMETER_NAME:
        ComponentAst reconnection =
            getSingleComponentConfiguration(nested, of(newIdentifier(RECONNECTION_CONFIG_PARAMETER_NAME,
                                                                     paramDsl.getPrefix())));

        if (reconnection != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, reconnection));
        }
        return;

      case RECONNECTION_STRATEGY_PARAMETER_NAME:
        ComponentIdentifier reconnectId = newIdentifier(RECONNECT_ELEMENT_IDENTIFIER,
                                                        paramDsl.getPrefix());

        ComponentAst config = nested.containsKey(reconnectId)
            ? getSingleComponentConfiguration(nested, of(reconnectId))
            : getSingleComponentConfiguration(nested,
                                              of(newIdentifier(RECONNECT_FOREVER_ELEMENT_IDENTIFIER, paramDsl.getPrefix())));

        if (config != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, config));
        }
        return;

      case REDELIVERY_POLICY_PARAMETER_NAME:
        ComponentAst redelivery =
            getSingleComponentConfiguration(nested, of(newIdentifier(REDELIVERY_POLICY_ELEMENT_IDENTIFIER,
                                                                     paramDsl.getPrefix())));
        if (redelivery != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, redelivery));
        }
        return;

      case EXPIRATION_POLICY_PARAMETER_NAME:
        ComponentAst expiration =
            getSingleComponentConfiguration(nested, of(newIdentifier(EXPIRATION_POLICY_ELEMENT_IDENTIFIER,
                                                                     paramDsl.getPrefix())));
        if (expiration != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, expiration));
        }
        return;

      case POOLING_PROFILE_PARAMETER_NAME:
        ComponentAst pooling = getSingleComponentConfiguration(nested, getIdentifier(paramDsl));
        if (pooling != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, pooling));
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
            .ifPresent(s -> groupElementBuilder
                .containing(newElementModel(paramModel, paramDsl, getSingleComponentConfiguration(nested, of(s)))));
        return;

      case TLS_PARAMETER_NAME:
        ComponentAst tls = getSingleComponentConfiguration(nested, getIdentifier(paramDsl));
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

      case SCHEDULING_STRATEGY_PARAMETER_NAME:
        ComponentAst schedulingStrategyWrapper =
            getSingleComponentConfiguration(nested, of(ComponentIdentifier.builder()
                .name(SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER)
                .namespace(CORE_PREFIX)
                .build()));
        if (schedulingStrategyWrapper != null) {
          DslElementModel.Builder wrapper = DslElementModel.builder()
              .withModel(paramModel)
              .withDsl(paramDsl)
              .withConfig(schedulingStrategyWrapper);

          Iterator<ComponentAst> nestedIt = schedulingStrategyWrapper.directChildrenStream().iterator();
          if (nestedIt.hasNext()) {
            final ComponentAst strategy = nestedIt.next();
            final MetadataType type = CRON_STRATEGY_ELEMENT_IDENTIFIER.equals(strategy.getIdentifier().getName())
                ? typeLoader.load(CronScheduler.class)
                : typeLoader.load(FixedFrequencyScheduler.class);

            dsl.resolve(type)
                .ifPresent(typeDsl -> wrapper.containing(DslElementModel.builder()
                    .withModel(type)
                    .withDsl(typeDsl)
                    .withConfig(strategy)
                    .build()));
          }

          groupElementBuilder.containing(wrapper.build());
        }

        return;
    }

  }

  private ComponentIdentifier newIdentifier(String name, String ns) {
    return ComponentIdentifier.builder().name(name).namespace(ns).build();
  }

  private DslElementModel newElementModel(ParameterModel paramModel, DslElementSyntax paramDsl,
                                          ComponentAst configuration) {
    return DslElementModel.builder()
        .withModel(paramModel)
        .withDsl(paramDsl)
        .withConfig(configuration)
        .build();
  }

}
