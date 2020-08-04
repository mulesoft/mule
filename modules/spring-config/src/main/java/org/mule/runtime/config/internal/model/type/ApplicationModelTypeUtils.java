/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.type;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.resolveComponentType;
import static org.mule.runtime.config.internal.model.type.MetadataTypeModelAdapter.createMetadataTypeModelAdapterWithSterotype;
import static org.mule.runtime.config.internal.model.type.MetadataTypeModelAdapter.createParameterizedTypeModelAdapter;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isInfrastructure;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureTypeMapping.getTypeFor;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.SimpleType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.HasStereotypeModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper.ExtensionWalkerModelDelegate;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.model.DefaultComponentParameterAst;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntaxBuilder;
import org.mule.runtime.extension.api.model.parameter.ImmutableParameterModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Provides utilities to obtain the models/types for the elements of a mule config.
 *
 * @since 4.4
 */
public final class ApplicationModelTypeUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationModelTypeUtils.class);

  private static final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  private ApplicationModelTypeUtils() {
    // Nothing to do
  }

  public static void resolveTypedComponentIdentifier(ComponentModel componentModel, ExtensionModelHelper extensionModelHelper) {
    extensionModelHelper.walkToComponent(componentModel.getIdentifier(), new ExtensionWalkerModelDelegate() {

      @Override
      public void onConfiguration(ConfigurationModel model) {
        componentModel.setConfigurationModel(model);
        onParameterizedModel(componentModel, model, extensionModelHelper);
      }

      @Override
      public void onConnectionProvider(ConnectionProviderModel model) {
        componentModel.setConnectionProviderModel(model);
        onParameterizedModel(componentModel, model, extensionModelHelper);
      }

      @Override
      public void onOperation(OperationModel model) {
        componentModel.setComponentModel(model);
        onParameterizedModel(componentModel, model, extensionModelHelper);
      }

      @Override
      public void onSource(SourceModel model) {
        componentModel.setComponentModel(model);
        onParameterizedModel(componentModel, model, extensionModelHelper);
      }

      @Override
      public void onConstruct(ConstructModel model) {
        componentModel.setComponentModel(model);
        onParameterizedModel(componentModel, model, extensionModelHelper);
      }

      @Override
      public void onNestableElement(NestableElementModel model) {
        componentModel.setNestableElementModel(model);
        if (model instanceof ParameterizedModel) {
          onParameterizedModel(componentModel, (ParameterizedModel) model, extensionModelHelper);
        }
      }

      @Override
      public void onType(MetadataType type) {
        type.accept(new MetadataTypeVisitor() {

          @Override
          public void visitObject(ObjectType objectType) {
            final MetadataTypeModelAdapter model = createMetadataTypeModelAdapterWithSterotype(objectType, extensionModelHelper)
                .orElseGet(() -> createParameterizedTypeModelAdapter(objectType, extensionModelHelper));
            componentModel.setMetadataTypeModelAdapter(model);
            onParameterizedModel(componentModel, model, extensionModelHelper);
          }

          @Override
          public void visitArrayType(ArrayType arrayType) {
            arrayType.getType().accept(this);
          }

          @Override
          public void visitUnion(UnionType unionType) {
            unionType.getTypes().forEach(type -> type.accept(this));
          }
        });
      }

    });

    // Check for infrastructure types, that are not present in an extension model
    if (!componentModel.getModel(HasStereotypeModel.class).isPresent()) {
      getTypeFor(componentModel.getIdentifier())
          .flatMap(type -> extensionModelHelper.findMetadataType(type))
          .ifPresent(type -> {
            final MetadataTypeModelAdapter model = createMetadataTypeModelAdapterWithSterotype(type, extensionModelHelper)
                .orElseGet(() -> createParameterizedTypeModelAdapter(type, extensionModelHelper));
            componentModel.setMetadataTypeModelAdapter(model);
            onParameterizedModel(componentModel, model, extensionModelHelper);
          });
    }

    componentModel.setComponentType(resolveComponentType(componentModel, extensionModelHelper));
  }

  private static void onParameterizedModel(ComponentModel componentModel, ParameterizedModel model,
                                           ExtensionModelHelper extensionModelHelper) {
    DslElementSyntax elementDsl = extensionModelHelper.resolveDslElementModel(model, componentModel.getIdentifier());
    Multimap<ComponentIdentifier, ComponentModel> nestedComponents = getNestedComponents(componentModel);

    List<ParameterModel> inlineGroupedParameters = model.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .map(group -> addInlineGroup(componentModel, elementDsl, nestedComponents, group, extensionModelHelper))
        .flatMap(g -> g.getParameterModels().stream())
        .collect(toList());

    model.getAllParameterModels()
        .stream()
        .filter(paramModel -> !inlineGroupedParameters.contains(paramModel))
        .forEach(paramModel -> elementDsl.getAttribute(paramModel.getName())
            .ifPresent(attrDsl -> setSimpleParameterValue(componentModel, paramModel, attrDsl)));

    handleNestedParameters(componentModel, componentModel.directChildrenStream(),
                           nestedComponents, extensionModelHelper,
                           model,
                           parameterModel -> !inlineGroupedParameters.contains(parameterModel));
  }

  private static ParameterGroupModel addInlineGroup(ComponentModel componentModel, DslElementSyntax elementDsl,
                                                    Multimap<ComponentIdentifier, ComponentModel> nestedComponents,
                                                    ParameterGroupModel group, ExtensionModelHelper extensionModelHelper) {
    elementDsl.getChild(group.getName())
        .ifPresent(groupDsl -> {
          Optional<ComponentIdentifier> groupIdentifier = getIdentifier(groupDsl);
          if (!groupIdentifier.isPresent()) {
            return;
          }

          ComponentModel groupComponent = getSingleComponentModel(nestedComponents, groupIdentifier);
          if (groupComponent != null) {
            handleNestedParameters(componentModel,
                                   groupComponent.directChildrenStream().filter(childComp -> childComp != groupComponent),
                                   getNestedComponents(groupComponent),
                                   extensionModelHelper,
                                   new ParameterizedModel() {

                                     @Override
                                     public List<ParameterGroupModel> getParameterGroupModels() {
                                       return Lists.newArrayList(group);
                                     }

                                     @Override
                                     public String getDescription() {
                                       return group.getDescription();
                                     }

                                     @Override
                                     public String getName() {
                                       return group.getName();
                                     }
                                   }, parameterModel -> true);
          }
        });
    return group;
  }

  private static void handleNestedParameters(ComponentModel componentModel, Stream<ComponentAst> childrenComponentModels,
                                             Multimap<ComponentIdentifier, ComponentModel> nestedComponents,
                                             ExtensionModelHelper extensionModelHelper,
                                             ParameterizedModel model, Predicate<ParameterModel> parameterModelFilter) {
    model.getAllParameterModels()
        .stream()
        .filter(parameterModelFilter)
        // do not handle the callback parameters from the sources
        .filter(paramModel -> {
          if (model instanceof SourceModel) {
            return !(((SourceModel) model).getSuccessCallback()
                .map(sc -> sc.getAllParameterModels().contains(paramModel))
                .orElse(false) ||
                ((SourceModel) model).getErrorCallback()
                    .map(ec -> ec.getAllParameterModels().contains(paramModel))
                    .orElse(false));
          } else {
            return true;
          }
        })
        .filter(paramModel -> paramModel.getDslConfiguration() != null
            && paramModel.getDslConfiguration().allowsInlineDefinition())
        .forEach(paramModel -> {
          final DslElementSyntax paramSyntax =
              extensionModelHelper.resolveDslElementModel(paramModel, componentModel.getIdentifier());

          getIdentifier(paramSyntax)
              .ifPresent(id -> {
                final Collection<ComponentModel> nestedForId = nestedComponents.get(id);

                if (isInfrastructure(paramModel.getType())) {
                  paramModel.getType().accept(new MetadataTypeVisitor() {

                    @Override
                    public void visitObject(ObjectType objectType) {
                      enrichComponentModels(componentModel, nestedComponents,
                                            extensionModelHelper.resolveDslElementModel(objectType, CORE_PREFIX),
                                            paramModel, extensionModelHelper);
                    }

                    @Override
                    public void visitUnion(UnionType unionType) {
                      enrichComponentModels(componentModel, nestedComponents,
                                            of(paramSyntax),
                                            paramModel, extensionModelHelper);

                      unionType.getTypes().forEach(type -> type.accept(this));
                    }
                  });
                } else if (isContent(paramModel) || isSimpleMetadataType(paramModel)) {
                  nestedForId
                      .forEach(childComp -> componentModel
                          .setParameter(paramModel,
                                        new DefaultComponentParameterAst(trim(childComp.getTextContent()),
                                                                         () -> paramModel,
                                                                         childComp.getMetadata())));
                } else {
                  enrichComponentModels(componentModel, nestedComponents,
                                        of(paramSyntax),
                                        paramModel, extensionModelHelper);
                }
              });
        });
  }

  private static boolean isSimpleMetadataType(ParameterModel paramModel) {
    return paramModel.getType() instanceof org.mule.metadata.api.model.SimpleType;
  }

  private static Multimap<ComponentIdentifier, ComponentModel> getNestedComponents(ComponentModel componentModel) {
    Multimap<ComponentIdentifier, ComponentModel> result = ArrayListMultimap.create();
    componentModel.getInnerComponents().forEach(nestedComponent -> result.put(nestedComponent.getIdentifier(), nestedComponent));
    return result;
  }

  private static ComponentModel getSingleComponentModel(Multimap<ComponentIdentifier, ComponentModel> innerComponents,
                                                        Optional<ComponentIdentifier> identifier) {
    return identifier.filter(innerComponents::containsKey)
        .map(innerComponents::get)
        .map(collection -> collection.iterator().next())
        .orElse(null);
  }


  private static void enrichComponentModels(ComponentModel componentModel,
                                            Multimap<ComponentIdentifier, ComponentModel> innerComponents,
                                            Optional<DslElementSyntax> optionalParamDsl, ParameterModel paramModel,
                                            ExtensionModelHelper extensionModelHelper) {
    optionalParamDsl.ifPresent(paramDsl -> {
      ComponentModel paramComponent = getSingleComponentModel(innerComponents, getIdentifier(paramDsl));
      if (paramComponent != null) {
        if (paramDsl.isWrapped()) {
          handleWrappedElement(componentModel, paramComponent, paramModel, paramDsl, extensionModelHelper);
        } else {
          paramModel.getType()
              .accept(getComponentChildVisitor(componentModel, paramModel, paramDsl, paramComponent, extensionModelHelper));
        }
      } else {
        setSimpleParameterValue(componentModel, paramModel, paramDsl);
      }
    });
  }

  private static void handleWrappedElement(ComponentModel componentModel, ComponentModel wrappedComponent,
                                           ParameterModel paramModel, DslElementSyntax paramDsl,
                                           ExtensionModelHelper extensionModelHelper) {
    Multimap<ComponentIdentifier, ComponentModel> nestedWrappedComponents = getNestedComponents(wrappedComponent);

    paramModel.getType().accept(new MetadataTypeVisitor() {

      @Override
      public void visitObject(ObjectType objectType) {
        Map<ObjectType, Optional<DslElementSyntax>> objectTypeOptionalMap =
            extensionModelHelper.resolveSubTypes((ObjectType) paramModel.getType());

        objectTypeOptionalMap.entrySet().stream().filter(entry -> {
          if (entry.getValue().isPresent()) {
            return getSingleComponentModel(nestedWrappedComponents, getIdentifier(entry.getValue().get())) != null;
          }
          return false;
        }).findFirst().ifPresent(wrappedEntryType -> {
          DslElementSyntax wrappedDsl = wrappedEntryType.getValue().get();
          wrappedEntryType.getKey()
              .accept(getComponentChildVisitor(componentModel,
                                               paramModel,
                                               wrappedDsl,
                                               getSingleComponentModel(nestedWrappedComponents, getIdentifier(wrappedDsl)),
                                               extensionModelHelper));
        });
      }

      @Override
      public void visitUnion(UnionType unionType) {
        unionType.getTypes()
            .forEach(type -> getTypeId(type)
                .flatMap(paramDsl::getChild)
                .flatMap(childDsl -> getIdentifier(childDsl))
                .flatMap(childComponentId -> ofNullable(nestedWrappedComponents.get(childComponentId)))
                .orElse(emptySet())
                .forEach(nestedChild -> type.accept(getComponentChildVisitor(componentModel, paramModel, paramDsl, nestedChild,
                                                                             extensionModelHelper))));
      }
    });

  }

  private static void setSimpleParameterValue(ComponentModel componentModel, ParameterModel paramModel,
                                              DslElementSyntax paramDsl) {
    String value = paramDsl.supportsAttributeDeclaration()
        ? componentModel.getRawParameters().get(paramModel.getName())
        : null;

    if (isBlank(value) && isContent(paramModel)) {
      value = componentModel.getTextContent();
    }

    if (isNotBlank(value)) {
      componentModel.setParameter(paramModel, new DefaultComponentParameterAst(value.trim(),
                                                                               () -> paramModel,
                                                                               componentModel.getMetadata()));
    }
  }

  private static MetadataTypeVisitor getComponentChildVisitor(ComponentModel componentModel, ParameterModel paramModel,
                                                              DslElementSyntax paramDsl, ComponentModel paramComponent,
                                                              ExtensionModelHelper extensionModelHelper) {
    return new MetadataTypeVisitor() {

      @Override
      public void visitArrayType(ArrayType arrayType) {
        MetadataType itemType = arrayType.getType();
        itemType.accept(getArrayItemTypeVisitor(componentModel, paramModel, paramDsl, paramComponent, extensionModelHelper));
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (isMap(objectType)) {
          List<ComponentModel> componentModels = handleMap(objectType);

          componentModel.setParameter(paramModel, new DefaultComponentParameterAst(componentModels,
                                                                                   () -> paramModel,
                                                                                   paramComponent.getMetadata()));
          return;
        }

        componentModel.setParameter(paramModel, new DefaultComponentParameterAst(paramComponent,
                                                                                 () -> paramModel, paramComponent.getMetadata()));

        MetadataTypeModelAdapter parameterizedModel = createParameterizedTypeModelAdapter(objectType, extensionModelHelper);
        paramComponent.setMetadataTypeModelAdapter(parameterizedModel);

        final Multimap<ComponentIdentifier, ComponentModel> nestedComponents = getNestedComponents(paramComponent);
        parameterizedModel.getAllParameterModels().stream()
            .forEach(nestedParameter -> {
              final Optional<DslElementSyntax> containedElement = paramDsl
                  .getContainedElement(nestedParameter.getName());
              enrichComponentModels(paramComponent, nestedComponents, containedElement,
                                    nestedParameter, extensionModelHelper);
            });
      }

      @Override
      public void visitUnion(UnionType unionType) {
        componentModel.setParameter(paramModel, new DefaultComponentParameterAst(paramComponent,
                                                                                 () -> paramModel, paramComponent.getMetadata()));

        MetadataTypeModelAdapter parameterizedModel = createParameterizedTypeModelAdapter(unionType, extensionModelHelper);
        paramComponent.setMetadataTypeModelAdapter(parameterizedModel);

        final Multimap<ComponentIdentifier, ComponentModel> nestedComponents = getNestedComponents(paramComponent);
        unionType.getTypes()
            .forEach(type -> {
              MetadataTypeModelAdapter innerTypeParameterizedModel =
                  createParameterizedTypeModelAdapter(type, extensionModelHelper);
              innerTypeParameterizedModel.getAllParameterModels().stream()
                  .forEach(nestedParameter -> {
                    final Optional<DslElementSyntax> containedElement = paramDsl.getContainedElement(nestedParameter.getName());
                    enrichComponentModels(paramComponent, nestedComponents, containedElement, nestedParameter,
                                          extensionModelHelper);
                  });
            });
      }

      private List<ComponentModel> handleMap(ObjectType objectType) {
        return paramComponent.getInnerComponents().stream().filter(entryComponent -> {
          MetadataType entryType = objectType.getOpenRestriction().get();
          Optional<DslElementSyntax> entryValueDslOptional = paramDsl.getGeneric(entryType);
          if (entryValueDslOptional.isPresent()) {
            DslElementSyntax entryValueDsl = entryValueDslOptional.get();
            ParameterModel keyParamModel =
                new ImmutableParameterModel(KEY_ATTRIBUTE_NAME, "", typeLoader.load(String.class), false, true, false, false,
                                            SUPPORTED, null, BEHAVIOUR, null, null, null, null, emptyList(), emptySet());
            String key = entryComponent.getRawParameters().get(KEY_ATTRIBUTE_NAME);
            entryComponent.setParameter(keyParamModel, new DefaultComponentParameterAst(key,
                                                                                        () -> keyParamModel,
                                                                                        entryComponent.getMetadata()));

            String value = entryComponent.getRawParameters().get(VALUE_ATTRIBUTE_NAME);
            ParameterModel valueParamModel =
                new ImmutableParameterModel(VALUE_ATTRIBUTE_NAME, "", entryType, false, true, false, false, SUPPORTED, null,
                                            BEHAVIOUR, null, null, null, null, emptyList(), emptySet());

            if (isBlank(value)) {
              Optional<DslElementSyntax> genericValueDslOptional = entryValueDsl.getGeneric(keyParamModel.getType());

              Multimap<ComponentIdentifier, ComponentModel> nestedComponents = getNestedComponents(entryComponent);

              if (genericValueDslOptional.isPresent()) {
                DslElementSyntax genericValueDsl = genericValueDslOptional.get();
                List<ComponentModel> itemsComponentModels = entryComponent.getInnerComponents().stream()
                    .filter(valueComponent -> valueComponent.getIdentifier()
                        .equals(getIdentifier(genericValueDsl).orElse(null)))
                    .map(entryValueComponent -> {
                      Multimap<ComponentIdentifier, ComponentModel> nested = ArrayListMultimap.create();
                      nested.put(entryValueComponent.getIdentifier(), entryValueComponent);
                      enrichComponentModels(entryComponent, nested, of(genericValueDsl), valueParamModel,
                                            extensionModelHelper);
                      return entryValueComponent;
                    })
                    .collect(toList());

                entryComponent.setParameter(valueParamModel, new DefaultComponentParameterAst(itemsComponentModels,
                                                                                              () -> valueParamModel,
                                                                                              entryComponent.getMetadata()));
              } else {
                Optional<DslElementSyntax> valueDslElementOptional = entryValueDsl.getContainedElement(VALUE_ATTRIBUTE_NAME);
                if (valueDslElementOptional.isPresent() && !valueDslElementOptional.get().isWrapped()) {
                  // Either a simple value or an objectType
                  enrichComponentModels(entryComponent, nestedComponents, valueDslElementOptional, valueParamModel,
                                        extensionModelHelper);
                } else if (entryType instanceof ObjectType) {
                  // This case the value is a baseType therefore we need to go with subTypes
                  extensionModelHelper.resolveSubTypes((ObjectType) entryType)
                      .entrySet()
                      .stream()
                      .filter(entrySubTypeDslOptional -> entrySubTypeDslOptional.getValue().isPresent())
                      .forEach(entrySubTypeDslOptional -> {
                        DslElementSyntax subTypeDsl = entrySubTypeDslOptional.getValue().get();

                        ParameterModel subTypeValueParamModel =
                            new ImmutableParameterModel(VALUE_ATTRIBUTE_NAME, "", entrySubTypeDslOptional.getKey(), false, true,
                                                        false, false, SUPPORTED, null,
                                                        BEHAVIOUR, null, null, null, null, emptyList(), emptySet());

                        enrichComponentModels(entryComponent, nestedComponents, of(subTypeDsl), subTypeValueParamModel,
                                              extensionModelHelper);
                      });
                }
              }
            } else {
              entryComponent.setParameter(valueParamModel, new DefaultComponentParameterAst(value,
                                                                                            () -> valueParamModel,
                                                                                            entryComponent.getMetadata()));
            }

            ObjectTypeBuilder entryObjectTypeBuilder = new BaseTypeBuilder(MetadataFormat.JAVA).objectType();
            entryObjectTypeBuilder.addField().key(keyParamModel.getName()).value(keyParamModel.getType());
            entryObjectTypeBuilder.addField().key(valueParamModel.getName()).value(valueParamModel.getType());

            entryComponent.setMetadataTypeModelAdapter(createParameterizedTypeModelAdapter(entryObjectTypeBuilder.build(),
                                                                                           extensionModelHelper));

            return true;
          }
          return false;
        }).collect(toList());
      }
    };
  }

  private static MetadataTypeVisitor getArrayItemTypeVisitor(ComponentModel componentModel, ParameterModel paramModel,
                                                             DslElementSyntax paramDsl, ComponentModel paramComponent,
                                                             ExtensionModelHelper extensionModelHelper) {
    return new MetadataTypeVisitor() {

      @Override
      public void visitSimpleType(SimpleType simpleType) {
        if (paramComponent.getRawParameters().containsKey(VALUE_ATTRIBUTE_NAME)) {
          ObjectTypeBuilder entryObjectTypeBuilder = new BaseTypeBuilder(MetadataFormat.JAVA).objectType();
          entryObjectTypeBuilder.addField().key(VALUE_ATTRIBUTE_NAME).value(simpleType);

          paramComponent.setMetadataTypeModelAdapter(createParameterizedTypeModelAdapter(entryObjectTypeBuilder.build(),
                                                                                         extensionModelHelper));
          return;
        }

        paramDsl.getGeneric(simpleType)
            .ifPresent(itemDsl -> {
              ComponentIdentifier itemIdentifier = getIdentifier(itemDsl).get();

              List<ComponentModel> componentModels = paramComponent.getInnerComponents().stream()
                  .filter(c -> c.getIdentifier().equals(itemIdentifier))
                  .filter(valueComponentModel -> valueComponentModel.getRawParameters().containsKey(VALUE_ATTRIBUTE_NAME))
                  .map(valueComponentModel -> {
                    ObjectTypeBuilder entryObjectTypeBuilder = new BaseTypeBuilder(MetadataFormat.JAVA).objectType();
                    entryObjectTypeBuilder.addField().key(VALUE_ATTRIBUTE_NAME).value(simpleType);

                    valueComponentModel
                        .setMetadataTypeModelAdapter(createParameterizedTypeModelAdapter(entryObjectTypeBuilder.build(),
                                                                                         extensionModelHelper));
                    return valueComponentModel;
                  })
                  .collect(toList());

              componentModel.setParameter(paramModel, new DefaultComponentParameterAst(componentModels,
                                                                                       () -> paramModel,
                                                                                       paramComponent.getMetadata()));

            });
      }

      @Override
      public void visitObject(ObjectType itemType) {
        paramDsl.getGeneric(itemType)
            .ifPresent(itemDsl -> {
              ComponentIdentifier itemIdentifier = getIdentifier(itemDsl).get();

              Map<String, ObjectType> objectTypeByTypeId = new HashMap<>();
              Map<String, Optional<DslElementSyntax>> typesDslMap = new HashMap<>();
              Map<ComponentIdentifier, String> itemIdentifiers = new HashMap<>();

              LOGGER.debug("getArrayItemTypeVisitor.visitObject: visiting itemType '{}'.", itemType.toString());

              extensionModelHelper.resolveSubTypes(itemType).entrySet()
                  .stream()
                  .forEach(entry -> getTypeId(entry.getKey())
                      .ifPresent(subTypeTypeId -> {
                        typesDslMap.put(subTypeTypeId, entry.getValue());
                        objectTypeByTypeId.put(subTypeTypeId, entry.getKey());
                        entry.getValue().ifPresent(dslElementSyntax -> getIdentifier(dslElementSyntax)
                            .ifPresent(subTypeIdentifier -> itemIdentifiers.put(subTypeIdentifier, subTypeTypeId)));
                      }));

              getTypeId(itemType).ifPresent(itemTypeId -> {
                typesDslMap.put(itemTypeId, of(itemDsl));
                objectTypeByTypeId.put(itemTypeId, itemType);

                itemIdentifiers.put(itemIdentifier, itemTypeId);
              });

              LOGGER.debug("getArrayItemTypeVisitor.visitObject: itemIdentifiers: '{}'.", itemIdentifiers.toString());

              List<ComponentAst> componentModels = paramComponent.getInnerComponents().stream()
                  .filter(c -> itemIdentifiers.containsKey(c.getIdentifier()))
                  .map(c -> (ComponentAst) c)
                  .collect(toList());

              componentModel.setParameter(paramModel, new DefaultComponentParameterAst(componentModels,
                                                                                       () -> paramModel,
                                                                                       paramComponent.getMetadata()));
              componentModels.stream().forEach(itemComponent -> {
                String typeId = itemIdentifiers.get(itemComponent.getIdentifier());
                typesDslMap.get(typeId).ifPresent(subTypeDsl -> {
                  MetadataTypeModelAdapter parameterizedModel =
                      createParameterizedTypeModelAdapter(objectTypeByTypeId.get(typeId), extensionModelHelper);
                  ((ComponentModel) itemComponent).setMetadataTypeModelAdapter(parameterizedModel);

                  final Multimap<ComponentIdentifier, ComponentModel> nestedComponents =
                      getNestedComponents((ComponentModel) itemComponent);
                  parameterizedModel.getAllParameterModels().stream()
                      .forEach(nestedParameter -> enrichComponentModels((ComponentModel) itemComponent, nestedComponents,
                                                                        recursiveAwareContainedElement(typesDslMap, subTypeDsl,
                                                                                                       nestedParameter),
                                                                        nestedParameter, extensionModelHelper));

                });
              });
            });
      }

      private Optional<DslElementSyntax> recursiveAwareContainedElement(Map<String, Optional<DslElementSyntax>> typesDslMap,
                                                                        DslElementSyntax subTypeDsl,
                                                                        ParameterModel nestedParameter) {
        return subTypeDsl.getContainedElement(nestedParameter.getName())
            .map(innerElement -> getTypeId(nestedParameter.getType())
                .flatMap(typeId -> {
                  if (typesDslMap.containsKey(typeId)) {
                    LOGGER
                        .debug("getArrayItemTypeVisitor.recursiveAwareContainedElement: No entry for '{}' in typesDslMap, ignoring.",
                               typeId);
                    return typesDslMap.get(typeId);
                  } else {
                    return empty();
                  }
                })
                .map(referencedDslElement -> {
                  LOGGER.debug("getArrayItemTypeVisitor.recursiveAwareContainedElement: processing typeId {}",
                               referencedDslElement.toString());
                  final DslElementSyntaxBuilder baseReferenced = DslElementSyntaxBuilder.create()
                      .withAttributeName(innerElement.getAttributeName())
                      .withElementName(innerElement.getElementName())
                      .withNamespace(innerElement.getPrefix(), innerElement.getNamespace())
                      .requiresConfig(innerElement.requiresConfig())
                      .supportsAttributeDeclaration(innerElement.supportsAttributeDeclaration())
                      .supportsChildDeclaration(innerElement.supportsChildDeclaration())
                      .supportsTopLevelDeclaration(innerElement.supportsTopLevelDeclaration())
                      .asWrappedElement(innerElement.isWrapped());

                  // Handle recursive types by getting the element structure from the referenced type
                  referencedDslElement.getContainedElementsByName().entrySet()
                      .forEach(containedEntry -> baseReferenced.containing(containedEntry.getKey(), containedEntry.getValue()));
                  referencedDslElement.getGenerics().entrySet()
                      .forEach(genericEntry -> baseReferenced.withGeneric(genericEntry.getKey(), genericEntry.getValue()));

                  return baseReferenced.build();
                })
                .orElse(innerElement));
      }
    };
  }

  private static Optional<ComponentIdentifier> getIdentifier(DslElementSyntax dsl) {
    if (isNotBlank(dsl.getElementName()) && isNotBlank(dsl.getPrefix())) {
      return Optional.of(builder()
          .name(dsl.getElementName())
          .namespace(dsl.getPrefix())
          .build());
    }

    return empty();
  }

}
