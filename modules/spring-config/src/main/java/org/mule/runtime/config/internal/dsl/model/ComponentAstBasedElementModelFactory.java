/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getLocalPart;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.util.NameUtils.hyphenize;
import static org.mule.runtime.ast.api.ComponentAst.BODY_RAW_PARAM_NAME;
import static org.mule.runtime.config.internal.model.ApplicationModel.CRON_STRATEGY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.EXPIRATION_POLICY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.NON_REPEATABLE_ITERABLE_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.NON_REPEATABLE_STREAM_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.RECONNECTION_CONFIG_PARAMETER_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.RECONNECT_FOREVER_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.RECONNECT_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.REDELIVERY_POLICY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.REPEATABLE_FILE_STORE_ITERABLE_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.REPEATABLE_FILE_STORE_STREAM_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.REPEATABLE_IN_MEMORY_ITERABLE_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.REPEATABLE_IN_MEMORY_STREAM_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.SCHEDULING_STRATEGY_IDENTIFIER;
import static org.mule.runtime.extension.api.ExtensionConstants.EXPIRATION_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_CONFIG_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.SCHEDULING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getDefaultValue;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isInfrastructure;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isRequired;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isText;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.builder.ComponentAstBuilder;
import org.mule.runtime.ast.internal.builder.MetadataTypeModelAdapter;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.DslElementModel.Builder;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.core.api.source.scheduler.CronScheduler;
import org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.FlattenedTypeAnnotation;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.property.QNameModelProperty;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Implementation of {@link DslElementModelFactory} that creates a {@link DslElementModel} based on its {@link ComponentAst}
 * representation.
 *
 * @since 4.4
 */
// TODO MULE-11496 Delete this factory once everything has an ExtensionModel and can be represented with an ElementDeclaration
class ComponentAstBasedElementModelFactory {

  private static final Logger LOGGER = getLogger(ComponentAstBasedElementModelFactory.class);

  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  // TODO MULE-18660 remove resolvers and dsl, and obtain the information provided by those from the AST being processed
  private final Map<ExtensionModel, DslSyntaxResolver> resolvers;
  private DslSyntaxResolver dsl;

  ComponentAstBasedElementModelFactory(Map<ExtensionModel, DslSyntaxResolver> resolvers) {
    this.resolvers = resolvers;
  }

  public <T> Optional<DslElementModel<T>> create(ComponentAst configuration) {
    final ComponentIdentifier identifier = configuration.getIdentifier();

    Optional<Map.Entry<ExtensionModel, DslSyntaxResolver>> entry =
        resolvers.entrySet().stream()
            .filter(e -> e.getKey().getXmlDslModel().getPrefix().equals(identifier.getNamespace()))
            .findFirst();

    if (!entry.isPresent()) {
      return empty();
    }

    dsl = entry.get().getValue();

    return configuration.getModel(ParameterizedModel.class)
        .flatMap(model -> {
          if (model instanceof MetadataTypeModelAdapter) {
            return dsl.resolve(((MetadataTypeModelAdapter) model).getType())
                .map(dslSyntax -> (DslElementModel<T>) createElementModel(model, dslSyntax, configuration).build());
          } else {
            return of((DslElementModel<T>) createElementModel(model, dsl.resolve(model), configuration).build());
          }
        });
  }

  private Optional<DslElementModel.Builder<ObjectType>> resolveBasedOnType(ObjectType type,
                                                                           ComponentAst configuration) {
    return dsl.resolve(type)
        .flatMap(typeDsl -> {
          Optional<ComponentIdentifier> elementIdentifier = getIdentifier(typeDsl);

          if (elementIdentifier.isPresent()
              && configuration.getModel(MetadataTypeModelAdapter.class).map(mtma -> mtma.isWrapperFor(type)).orElse(false)) {
            DslElementModel.Builder<ObjectType> typeBuilder = DslElementModel.<ObjectType>builder()
                .withModel(type)
                .withDsl(typeDsl)
                .withConfig(configuration);

            enrichElementModel(configuration.getModel(ParameterizedModel.class).get(), typeDsl, configuration, typeBuilder);

            return of(typeBuilder);
          } else {
            return empty();
          }
        });
  }

  private void populateObjectFields(ObjectType type, ComponentAst configuration, DslElementSyntax typeDsl,
                                    DslElementModel.Builder typeBuilder) {
    LOGGER.trace("populateObjectFields: type: '{}'", type);

    type.getFields().forEach(field -> {

      if (field.getValue() instanceof ObjectType && field.getAnnotation(FlattenedTypeAnnotation.class).isPresent()) {
        ((ObjectType) field.getValue()).getFields().forEach(nested -> {
          final String name = getLocalPart(nested);
          LOGGER.trace("populateObjectFields: type: '{}', flattened: {}, field: {}", type, field.getValue(), name);
          typeDsl.getContainedElement(name)
              .ifPresent(fieldDsl -> nested.getValue()
                  .accept(getComponentChildVisitor(typeBuilder, configuration, nested, name, fieldDsl,
                                                   getDefaultValue(name, field.getValue()))));

        });

      } else {
        final String name = getLocalPart(field);
        LOGGER.trace("populateObjectFields: type: '{}', field: {}", type, name);
        typeDsl.getContainedElement(name)
            .ifPresent(fieldDsl -> field.getValue()
                .accept(getComponentChildVisitor(typeBuilder, configuration, field, name, fieldDsl,
                                                 getDefaultValue(name, type))));
      }
    });
  }

  private Multimap<ComponentIdentifier, ComponentAst> getNestedComponents(ComponentAst configuration) {
    Multimap<ComponentIdentifier, ComponentAst> result = ArrayListMultimap.create();
    configuration.directChildrenStream()
        .forEach(componentConfiguration -> result.put(componentConfiguration.getIdentifier(), componentConfiguration));
    return result;
  }

  private MetadataTypeVisitor getComponentChildVisitor(final DslElementModel.Builder typeBuilder,
                                                       final ComponentAst configuration,
                                                       final MetadataType model, final String name,
                                                       final DslElementSyntax modelDsl, final Optional<String> defaultValue) {

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
            if (nested != null) {
              value = nested.getRawParameterValue(BODY_RAW_PARAM_NAME)
                  .flatMap(body -> !isBlank(body) ? of(body.trim()) : empty())
                  .orElse(null);
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
        if (!identifier.isPresent()) {
          LOGGER.trace("getComponentChildVisitor#visitArrayType: noIdentifier");
          visitNoIdentifier(typeBuilder, model, modelDsl, defaultValue);
          return;
        }

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
                    .filter(c -> c.getIdentifier().equals(itemIdentifier))
                    .forEach(c -> getComponentChildVisitor(list, c, arrayType.getType(), VALUE_ATTRIBUTE_NAME, itemdsl,
                                                           defaultValue));

              });

          typeBuilder.containing(list.build());
        }
      }

      @Override
      public void visitObject(ObjectType objectType) {
        Optional<ComponentIdentifier> identifier = getIdentifier(modelDsl);
        if (!identifier.isPresent()) {
          LOGGER.trace("getComponentChildVisitor#visitObject: noIdentifier");
          visitNoIdentifier(typeBuilder, model, modelDsl, defaultValue);
        } else {
          LOGGER.trace("getComponentChildVisitor#visitObject: '{}'", identifier.get());
        }

        ComponentAst fieldComponent = getSingleComponentConfiguration(getNestedComponents(configuration), identifier);

        if (isMap(objectType)) {
          LOGGER.trace("getComponentChildVisitor#visitObject: '{}' -> isMap", identifier.orElse(null));
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
          resolveBasedOnType(objectType, fieldComponent)
              .ifPresent(elementBuilder -> typeBuilder.containing(elementBuilder
                  .withDsl(modelDsl)
                  .build()));
        }
      }

      private void visitNoIdentifier(final DslElementModel.Builder typeBuilder, final MetadataType model,
                                     final DslElementSyntax modelDsl, final Optional<String> defaultValue) {
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
          entryType.accept(getComponentChildVisitor(entry, entryConfig, entryType,
                                                    VALUE_ATTRIBUTE_NAME, entryDsl.getAttribute(VALUE_ATTRIBUTE_NAME).get(),
                                                    empty()));
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
                                                                 DslElementModel.Builder builder) {
    // Have to keep the order of the elements consistent so the generated metadata keys are backwards compatible
    populateConnectionProviderElements(builder, configuration);
    populateParameterizedElements(model, elementDsl, builder, configuration);
    populateComposableElements(builder, configuration);

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
    Map<String, String> parameters = configuration.getParameters()
        .stream()
        .filter(p -> p.getResolvedRawValue() != null)
        .collect(toMap(p -> p.getModel().getName(), p -> p.getResolvedRawValue(), (u, v) -> u));

    model.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .forEach(group -> addInlineGroup(innerComponents, parameters, builder, group));

    List<ParameterModel> inlineGroupedParameters = model.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .flatMap(g -> g.getParameterModels().stream())
        .collect(toList());

    model.getAllParameterModels().stream()
        .filter(p -> !inlineGroupedParameters.contains(p))
        .forEach(p -> addElementParameter(innerComponents, parameters, elementDsl, builder, p));
  }

  private void populateConnectionProviderElements(DslElementModel.Builder builder, ComponentAst configuration) {
    configuration.directChildrenStream()
        .filter(c -> c.getModel(ConnectionProviderModel.class).isPresent())
        .forEach(nestedComponentConfig -> create(nestedComponentConfig).ifPresent(builder::containing));
  }

  private void populateComposableElements(DslElementModel.Builder builder, ComponentAst configuration) {
    final List<String> paramsAsChildrenNames = configuration.getModel(ParameterizedModel.class)
        .map(pmz -> pmz.getAllParameterModels()
            .stream()
            .flatMap(pm -> pm.getModelProperty(QNameModelProperty.class)
                .map(qnmp -> Stream.of(hyphenize(pm.getName()), qnmp.getValue().getLocalPart()))
                .orElseGet(() -> Stream.of(hyphenize(pm.getName()))))
            .collect(toList()))
        .orElse(emptyList());

    configuration.directChildrenStream()
        // TODO MULE-17711 Remove this filter
        .filter(c -> !paramsAsChildrenNames.contains(c.getIdentifier().getName()))
        .filter(c -> !c.getModel(ConnectionProviderModel.class).isPresent())
        .forEach(nestedComponentConfig -> create(nestedComponentConfig).ifPresent(builder::containing));
  }

  private void addInlineGroup(Multimap<ComponentIdentifier, ComponentAst> innerComponents,
                              Map<String, String> parameters,
                              DslElementModel.Builder parent, ParameterGroupModel group) {
    final DslElementSyntax groupSyntax = dsl.resolveInline(group);
    Optional<ComponentIdentifier> identifier = getIdentifier(groupSyntax);
    if (!identifier.isPresent()) {
      return;
    }
    DslElementModel.Builder<ParameterGroupModel> groupElementBuilder = DslElementModel.<ParameterGroupModel>builder()
        .withModel(group)
        .withDsl(groupSyntax);

    ComponentAst groupComponent = getSingleComponentConfiguration(innerComponents, identifier);
    if (groupComponent != null) {
      groupElementBuilder.withConfig(groupComponent);

      Multimap<ComponentIdentifier, ComponentAst> groupInnerComponents = getNestedComponents(groupComponent);
      group.getParameterModels()
          .forEach(p -> addElementParameter(groupInnerComponents, parameters, groupSyntax, groupElementBuilder, p));

      parent.containing(groupElementBuilder.build());

    } else if (shouldBuildDefaultGroup(group)) {
      buildDefaultInlineGroupElement(parent, groupElementBuilder.isExplicitInDsl(false), group, groupSyntax,
                                     identifier.get());
    }
  }

  private ComponentAst getSingleComponentConfiguration(Multimap<ComponentIdentifier, ComponentAst> innerComponents,
                                                       Optional<ComponentIdentifier> identifier) {
    return identifier.filter(innerComponents::containsKey)
        .map(innerComponents::get)
        .map(collection -> collection.iterator().next())
        .orElse(null);
  }

  private void buildDefaultInlineGroupElement(DslElementModel.Builder parent,
                                              DslElementModel.Builder<ParameterGroupModel> groupElementBuilder,
                                              ParameterGroupModel group,
                                              DslElementSyntax groupDsl,
                                              ComponentIdentifier identifier) {
    final ComponentAstBuilder groupCompAstBuilder = ComponentAstBuilder.builder();

    groupCompAstBuilder.withIdentifier(identifier);

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
                        .ifPresent(tagId -> groupCompAstBuilder.addChildComponent()
                            .withIdentifier(tagId)
                            .withRawParameter(BODY_RAW_PARAM_NAME, defaultValue)
                            .build());
                  } else {
                    groupCompAstBuilder.withRawParameter(defaultValue, paramDsl.getAttributeName());
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
    final DslElementSyntax paramSyntax = dsl.resolve(paramModel);
    ComponentAst paramComponent = getSingleComponentConfiguration(innerComponents, getIdentifier(paramSyntax));

    if (isInfrastructure(paramModel)) {
      handleInfrastructure(paramModel, paramSyntax, innerComponents, parameters, groupElementBuilder);
      return;
    }

    if (paramSyntax.isWrapped()) {
      resolveWrappedElement(groupElementBuilder, paramModel, paramSyntax, paramComponent);
      return;
    }

    String value = paramSyntax.supportsAttributeDeclaration()
        ? parameters.get(paramSyntax.getAttributeName())
        : null;

    Optional<String> defaultValue = getDefaultValue(paramModel);
    if (paramComponent != null || !isBlank(value) || defaultValue.isPresent()) {
      if (paramComponent != null && !isContent(paramModel)) {

        DslElementModel.Builder<ParameterModel> paramElementBuilder = DslElementModel.<ParameterModel>builder()
            .withModel(paramModel)
            .withDsl(paramSyntax);
        paramElementBuilder.withConfig(paramComponent);

        final Optional<ParameterizedModel> parameterized = paramComponent.getModel(ParameterizedModel.class);
        if (parameterized.isPresent()) {
          enrichElementModel(parameterized.get(), groupDsl, paramComponent, paramElementBuilder);
        } else {
          paramModel.getType().accept(new MetadataTypeVisitor() {

            @Override
            public void visitArrayType(ArrayType arrayType) {
              MetadataType itemType = arrayType.getType();
              paramSyntax.getGeneric(itemType)
                  .ifPresent(itemdsl -> paramComponent.directChildrenStream()
                      .filter(c -> c.getModel(MetadataTypeModelAdapter.class).map(mtma -> mtma.isWrapperFor(itemType))
                          .orElse(false))
                      .forEach(c -> {
                        final Builder<Object> arrayModelBuilder = DslElementModel.builder()
                            .withModel(itemType)
                            .withDsl(itemdsl)
                            .withConfig(c);
                        enrichElementModel(c.getModel(MetadataTypeModelAdapter.class).get(), itemdsl, c,
                                           arrayModelBuilder);
                        paramElementBuilder.containing(arrayModelBuilder.build());
                      }));
            }

            @Override
            public void visitObject(ObjectType objectType) {
              if (isMap(objectType)) {
                populateMapEntries(objectType, paramSyntax, paramElementBuilder, paramComponent);
                return;
              }

              populateObjectFields(objectType, paramComponent, paramSyntax, paramElementBuilder);
            }
          });
        }

        groupElementBuilder.containing(paramElementBuilder.build());
      } else if (isBlank(value)) {
        DslElementModel.Builder<ParameterModel> paramElementBuilder = DslElementModel.<ParameterModel>builder()
            .withModel(paramModel)
            .withDsl(paramSyntax);

        final Optional<String> body = paramComponent != null
            ? paramComponent.getRawParameterValue(BODY_RAW_PARAM_NAME)
            : empty();

        paramElementBuilder.withValue(body
            .map(String::trim)
            .orElseGet(() -> {
              paramElementBuilder.isExplicitInDsl(false);
              return defaultValue.get();
            }));
        groupElementBuilder.containing(paramElementBuilder.build());
      } else {
        DslElementModel.Builder<ParameterModel> paramElementBuilder = DslElementModel.<ParameterModel>builder()
            .withModel(paramModel)
            .withDsl(paramSyntax);

        paramElementBuilder.withValue(value);
        groupElementBuilder.containing(paramElementBuilder.build());
      }
    }
  }

  private void resolveWrappedElement(DslElementModel.Builder<ParameterGroupModel> groupElementBuilder, ParameterModel p,
                                     DslElementSyntax pDsl, ComponentAst paramComponent) {
    if (paramComponent != null) {
      DslElementModel.Builder<ParameterModel> paramElement = DslElementModel.<ParameterModel>builder()
          .withModel(p)
          .withDsl(pDsl)
          .withConfig(paramComponent);

      paramComponent.directChildrenStream()
          .findFirst()
          .ifPresent(wrapper -> {
            DslSyntaxResolver wrapperDsl = this.dsl;

            this.create(wrapper)
                .ifPresent(paramElement::containing);

            this.dsl = wrapperDsl;
          });

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
            getSingleComponentConfiguration(nested, of(RECONNECTION_CONFIG_PARAMETER_IDENTIFIER));

        if (reconnection != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, reconnection));
        }
        break;

      case RECONNECTION_STRATEGY_PARAMETER_NAME:
        handleReconnectionStrategy(paramModel, paramDsl, nested, groupElementBuilder);
        break;

      case REDELIVERY_POLICY_PARAMETER_NAME:
        ComponentAst redelivery =
            getSingleComponentConfiguration(nested, of(REDELIVERY_POLICY_IDENTIFIER));
        if (redelivery != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, redelivery));
        }
        break;

      case EXPIRATION_POLICY_PARAMETER_NAME:
        ComponentAst expiration =
            getSingleComponentConfiguration(nested, of(EXPIRATION_POLICY_IDENTIFIER));
        if (expiration != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, expiration));
        }
        break;

      case POOLING_PROFILE_PARAMETER_NAME:
        ComponentAst pooling = getSingleComponentConfiguration(nested, getIdentifier(paramDsl));
        if (pooling != null) {
          groupElementBuilder.containing(newElementModel(paramModel, paramDsl, pooling));
        }
        break;

      case STREAMING_STRATEGY_PARAMETER_NAME:
        Set<ComponentIdentifier> streaming =
            newHashSet(NON_REPEATABLE_STREAM_IDENTIFIER,
                       REPEATABLE_IN_MEMORY_STREAM_IDENTIFIER,
                       REPEATABLE_FILE_STORE_STREAM_IDENTIFIER,
                       REPEATABLE_IN_MEMORY_ITERABLE_IDENTIFIER,
                       REPEATABLE_FILE_STORE_ITERABLE_IDENTIFIER,
                       NON_REPEATABLE_ITERABLE_IDENTIFIER);

        streaming.stream().filter(nested::containsKey).findFirst()
            .ifPresent(s -> groupElementBuilder
                .containing(newElementModel(paramModel, paramDsl, getSingleComponentConfiguration(nested, of(s)))));
        break;

      case TLS_PARAMETER_NAME:
        handleTlsParameter(paramModel, paramDsl, nested, parameters, groupElementBuilder);
        break;

      case SCHEDULING_STRATEGY_PARAMETER_NAME:
        handleSchedulingStrategy(paramModel, paramDsl, nested, groupElementBuilder);
        break;

      default:
        return;
    }
  }

  private void handleReconnectionStrategy(final ParameterModel paramModel, final DslElementSyntax paramDsl,
                                          final Multimap<ComponentIdentifier, ComponentAst> nested,
                                          final DslElementModel.Builder<ParameterGroupModel> groupElementBuilder) {
    ComponentAst config = nested.containsKey(RECONNECT_IDENTIFIER)
        ? getSingleComponentConfiguration(nested, of(RECONNECT_IDENTIFIER))
        : getSingleComponentConfiguration(nested,
                                          of(RECONNECT_FOREVER_IDENTIFIER));

    if (config != null) {
      groupElementBuilder.containing(newElementModel(paramModel, paramDsl, config));
    }
  }

  private void handleTlsParameter(final ParameterModel paramModel, final DslElementSyntax paramDsl,
                                  final Multimap<ComponentIdentifier, ComponentAst> nested, final Map<String, String> parameters,
                                  final DslElementModel.Builder<ParameterGroupModel> groupElementBuilder) {
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
  }

  private void handleSchedulingStrategy(final ParameterModel paramModel, final DslElementSyntax paramDsl,
                                        final Multimap<ComponentIdentifier, ComponentAst> nested,
                                        final DslElementModel.Builder<ParameterGroupModel> groupElementBuilder) {
    ComponentAst schedulingStrategyWrapper =
        getSingleComponentConfiguration(nested, of(SCHEDULING_STRATEGY_IDENTIFIER));
    if (schedulingStrategyWrapper != null) {
      DslElementModel.Builder wrapper = DslElementModel.builder()
          .withModel(paramModel)
          .withDsl(paramDsl)
          .withConfig(schedulingStrategyWrapper);

      Iterator<ComponentAst> nestedIt = schedulingStrategyWrapper.directChildrenStream().iterator();
      if (nestedIt.hasNext()) {
        final ComponentAst strategy = nestedIt.next();
        final MetadataType type = CRON_STRATEGY_IDENTIFIER.equals(strategy.getIdentifier())
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
