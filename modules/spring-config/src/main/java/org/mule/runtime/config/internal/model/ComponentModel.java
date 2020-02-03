/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.runtime.api.component.Component.NS_MULE_DOCUMENTATION;
import static org.mule.runtime.api.component.Component.NS_MULE_PARSER_METADATA;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.resolveComponentType;
import static org.mule.runtime.config.internal.model.MetadataTypeModelAdapter.createMetadataTypeModelAdapterWithSterotype;
import static org.mule.runtime.config.internal.model.MetadataTypeModelAdapter.createParameterizedTypeModelAdapter;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
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
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.HasStereotypeModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper.ExtensionWalkerModelDelegate;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.core.privileged.processor.Router;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.internal.component.config.InternalComponentConfiguration;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.model.parameter.ImmutableParameterModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * An {@code ComponentModel} represents the user configuration of a component (flow, config, message processor, etc) defined in an
 * artifact configuration file.
 * <p/>
 * Every {@code ComponentModel} represents the configuration of a core configuration or an extension configuration. Which
 * configuration element this object represents is identified by a {@link ComponentIdentifier} that can be retrieved using
 * {@code #getName}.
 * <p/>
 * It may have simple configuration parameters which are retrieve by using {@code #getParameterGroups} or complex parameters which
 * are retrieved using {@code #getInnerComponents}.
 * <p/>
 * There's a set of configuration attributes or custom attributes that may not be mapped directly to the object that runs on
 * runtime but may be hold by a {@code ComponentModel}. Those attributes are retrieved by using {@code #getCustomAttributes}.
 *
 * @since 4.0
 */
public abstract class ComponentModel {

  public static String COMPONENT_MODEL_KEY = "ComponentModel";

  private boolean root = false;
  private ComponentIdentifier identifier;
  private final Map<String, String> parameters = new HashMap<>();
  private final Set<String> schemaValueParameter = new HashSet<>();
  // TODO MULE-9638 This must go away from component model once it's immutable.
  private ComponentModel parent;
  private final List<ComponentModel> innerComponents = new ArrayList<>();
  private String textContent;
  private DefaultComponentLocation componentLocation;
  private TypedComponentIdentifier.ComponentType componentType;
  private org.mule.runtime.api.meta.model.ComponentModel componentModel;
  private NestableElementModel nestableElementModel;
  private ConfigurationModel configurationModel;
  private ConnectionProviderModel connectionProviderModel;
  private MetadataTypeModelAdapter metadataTypeModelAdapter;

  private ComponentMetadataAst componentMetadata;

  private Object objectInstance;
  private Class<?> type;

  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();


  /**
   * @return the line number in which the component was defined in the configuration file. It may be empty if the component was
   * created pragmatically.
   */
  @Deprecated
  public Optional<Integer> getLineNumber() {
    return componentMetadata.getStartLine().isPresent() ? of(componentMetadata.getStartLine().getAsInt()) : empty();
  }

  /**
   * @return the start column in which the component was defined in the configuration file. It may be empty if the component was
   * created pragmatically.
   */
  @Deprecated
  public Optional<Integer> getStartColumn() {
    return componentMetadata.getStartColumn().isPresent() ? of(componentMetadata.getStartColumn().getAsInt()) : empty();
  }

  /**
   * @return the config file name in which the component was defined. It may be empty if the component was created pragmatically.
   */
  @Deprecated
  public Optional<String> getConfigFileName() {
    return componentMetadata.getFileName();
  }

  /**
   * @return the configuration identifier.
   */
  public ComponentIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * @return a {@code java.util.Map} with the simple parameters of the configuration.
   */
  public Map<String, String> getRawParameters() {
    return unmodifiableMap(parameters);
  }

  /**
   * @return a {@code java.util.List} of all the child {@code ComponentModel}s
   */
  public List<ComponentModel> getInnerComponents() {
    return innerComponents;
  }

  /**
   * @return a {@code java.util.Map} with all the custom attributes.
   */
  @Deprecated
  public Map<String, Object> getCustomAttributes() {
    Map<String, Object> attrs = new HashMap<>();

    attrs.putAll(componentMetadata.getParserAttributes());
    componentMetadata.getDocAttributes()
        .forEach((k, v) -> attrs.put("{" + NS_MULE_DOCUMENTATION + "}" + k, v));

    return attrs;
  }

  /**
   * @return true if the {@code ComponentModel} is a top level configuration element, false otherwise.
   */
  public boolean isRoot() {
    return root;
  }

  /**
   * Marked as true if it's a top level configuration.
   */
  public void setRoot(boolean root) {
    this.root = root;
  }

  /**
   * @param parameterName name of the configuration parameter.
   * @param value         value contained by the configuration parameter.
   */
  public void setParameter(ParameterModel parameterModel, ComponentParameterAst value) {
    this.parameters.put(parameterModel.getName(), value.getRawValue());
  }

  /**
   * @return the type of the object to be created when processing this {@code ComponentModel}.
   */
  public Class<?> getType() {
    return type;
  }

  /**
   * @param type the type of the object to be created when processing this {@code ComponentModel}.
   */
  public void setType(Class<?> type) {
    this.type = type;
  }

  /**
   * @param componentType the {@link org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType} of the object to be
   *                      created when processing this {@code ComponentModel}.
   */
  public void setComponentType(TypedComponentIdentifier.ComponentType componentType) {
    this.componentType = componentType;
  }

  public TypedComponentIdentifier.ComponentType getComponentType() {
    return componentType != null ? componentType : TypedComponentIdentifier.ComponentType.UNKNOWN;
  }

  public <M> Optional<M> getModel(Class<M> modelClass) {
    if (componentModel != null) {
      if (modelClass.isAssignableFrom(componentModel.getClass())) {
        return Optional.of((M) componentModel);
      }
    }

    if (configurationModel != null) {
      if (modelClass.isAssignableFrom(configurationModel.getClass())) {
        return Optional.of((M) configurationModel);
      }
    }

    if (connectionProviderModel != null) {
      if (modelClass.isAssignableFrom(connectionProviderModel.getClass())) {
        return Optional.of((M) connectionProviderModel);
      }
    }

    if (nestableElementModel != null) {
      if (modelClass.isAssignableFrom(nestableElementModel.getClass())) {
        return Optional.of((M) nestableElementModel);
      }
    }

    if (metadataTypeModelAdapter != null) {
      if (modelClass.isAssignableFrom(metadataTypeModelAdapter.getClass())) {
        return Optional.of((M) metadataTypeModelAdapter);
      }
    }

    return empty();
  }

  public void resolveTypedComponentIdentifier(ExtensionModelHelper extensionModelHelper) {
    executeOnComponentTree(this, componentModel -> {
      componentModel.doResolveTypedComponentIdentifier(extensionModelHelper);
    });
  }

  private void doResolveTypedComponentIdentifier(ExtensionModelHelper extensionModelHelper) {
    extensionModelHelper.walkToComponent(getIdentifier(), new ExtensionWalkerModelDelegate() {

      @Override
      public void onConfiguration(ConfigurationModel model) {
        setConfigurationModel(model);
        onParameterizedModel(model);
      }

      @Override
      public void onConnectionProvider(ConnectionProviderModel model) {
        setConnectionProviderModel(model);
        onParameterizedModel(model);
      }

      @Override
      public void onOperation(OperationModel model) {
        setComponentModel(model);
        onParameterizedModel(model);
      }

      @Override
      public void onSource(SourceModel model) {
        setComponentModel(model);
        onParameterizedModel(model);
      }

      @Override
      public void onConstruct(ConstructModel model) {
        setComponentModel(model);
        onParameterizedModel(model);
      }

      @Override
      public void onNestableElement(NestableElementModel model) {
        setNestableElementModel(model);
        if (model instanceof ParameterizedModel) {
          onParameterizedModel((ParameterizedModel) model);
        }
      }

      private void onParameterizedModel(ParameterizedModel model) {
        handleNestedParameters(extensionModelHelper, model);
      }

    });

    // TODO this is expecting to have a getType so it is coupled to parsers code
    // Last resort to try to find a matching metadata type for this component
    if (!getModel(HasStereotypeModel.class).isPresent()) {
      extensionModelHelper.findMetadataType(getType())
          .flatMap(type -> createMetadataTypeModelAdapterWithSterotype(type, extensionModelHelper))
          .ifPresent(this::setMetadataTypeModelAdapter);
    }

    setComponentType(resolveComponentType((ComponentAst) this, extensionModelHelper));
  }

  private void handleNestedParameters(ExtensionModelHelper extensionModelHelper, ParameterizedModel model) {
    Set<ComponentAst> paramChildren = new HashSet<>();

    ((ComponentAst) ComponentModel.this).directChildrenStream()
        .filter(childComp -> childComp != ComponentModel.this)
        .forEach(childComp -> {
          extensionModelHelper.findParameterModel(childComp.getIdentifier(), model)
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
              .filter(paramModel -> paramModel.getDslConfiguration().allowsInlineDefinition())
              .ifPresent(paramModel -> {
                paramChildren.add(childComp);

                if (paramModel.getExpressionSupport() == NOT_SUPPORTED
                    || childComp.directChildrenStream().findFirst().isPresent()) {
                  enrichComponentModels(this, getNestedComponents(this),
                                        of(extensionModelHelper.resolveDslElementModel(paramModel, this.getIdentifier())),
                                        paramModel, extensionModelHelper);
                } else {
                  setParameter(paramModel, new DefaultComponentParameterAst(((ComponentModel) childComp).getTextContent(),
                                                                            () -> paramModel, childComp.getMetadata()));
                }
              });
        });

    // TODO MULE-17711 When these are removed, the ast parameters may need to be traversed with recursive/direct spliterators
    // ComponentModel.this.innerComponents.removeAll(paramChildren);
  }

  private Multimap<ComponentIdentifier, ComponentModel> getNestedComponents(ComponentModel componentModel) {
    Multimap<ComponentIdentifier, ComponentModel> result = ArrayListMultimap.create();
    componentModel.getInnerComponents().forEach(nestedComponent -> {
      result.put(nestedComponent.getIdentifier(), nestedComponent);
    });
    return result;
  }

  private ComponentModel getSingleComponentModel(Multimap<ComponentIdentifier, ComponentModel> innerComponents,
                                                 Optional<ComponentIdentifier> identifier) {
    return identifier.filter(innerComponents::containsKey)
        .map(innerComponents::get)
        .map(collection -> collection.iterator().next())
        .orElse(null);
  }


  private void enrichComponentModels(ComponentModel componentModel, Multimap<ComponentIdentifier, ComponentModel> innerComponents,
                                     Optional<DslElementSyntax> dslContainedElement, ParameterModel paramModel,
                                     ExtensionModelHelper extensionModelHelper) {
    dslContainedElement.ifPresent(paramDsl -> {
      if (paramDsl.isWrapped()) {
        if (!(paramModel.getType() instanceof ObjectType)) {
          return;
        }
        ComponentModel wrappedComponent = getSingleComponentModel(innerComponents, getIdentifier(dslContainedElement.get()));
        if (wrappedComponent != null) {
          Multimap<ComponentIdentifier, ComponentModel> nestedWrappedComponents = getNestedComponents(wrappedComponent);

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
      }

      ComponentModel paramComponent = getSingleComponentModel(innerComponents, getIdentifier(paramDsl));

      if (paramComponent != null) {
        paramModel.getType()
            .accept(getComponentChildVisitor(componentModel, paramModel, paramDsl, paramComponent, extensionModelHelper));
      }
    });
  }

  private MetadataTypeVisitor getComponentChildVisitor(ComponentModel componentModel, ParameterModel paramModel,
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
          List<ComponentModel> componentModels = paramComponent.getInnerComponents().stream().filter(entryComponent -> {
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
                  enrichComponentModels(entryComponent, nestedComponents, of(genericValueDsl), valueParamModel,
                                        extensionModelHelper);

                  List<ComponentModel> itemsComponentModels = entryComponent.getInnerComponents().stream()
                      .filter(valueComponent -> valueComponent.getIdentifier()
                          .equals(getIdentifier(genericValueDsl).orElse(null)))
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
                                                                                              () -> keyParamModel,
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

          componentModel.setParameter(paramModel, new DefaultComponentParameterAst(componentModels,
                                                                                   () -> paramModel,
                                                                                   paramComponent.getMetadata()));
          return;
        }

        componentModel.setParameter(paramModel, new DefaultComponentParameterAst(paramComponent,
                                                                                 () -> paramModel, paramComponent.getMetadata()));

        MetadataTypeModelAdapter parameterizedModel = createParameterizedTypeModelAdapter(objectType, extensionModelHelper);
        paramComponent.setMetadataTypeModelAdapter(parameterizedModel);

        parameterizedModel.getAllParameterModels().stream().forEach(nestedParameter -> enrichComponentModels(paramComponent,
                                                                                                             getNestedComponents(paramComponent),
                                                                                                             paramDsl
                                                                                                                 .getContainedElement(nestedParameter
                                                                                                                     .getName()),
                                                                                                             nestedParameter,
                                                                                                             extensionModelHelper));
      }
    };
  }

  private MetadataTypeVisitor getArrayItemTypeVisitor(ComponentModel componentModel, ParameterModel paramModel,
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
        }
      }

      @Override
      public void visitObject(ObjectType itemType) {
        paramDsl.getGeneric(itemType)
            .ifPresent(itemDsl -> {
              ComponentIdentifier itemIdentifier = getIdentifier(itemDsl).get();
              Map<MetadataType, Optional<DslElementSyntax>> typesDslMap = new HashMap<>();
              typesDslMap.putAll(extensionModelHelper.resolveSubTypes(itemType));

              if (!itemDsl.isWrapped()) {
                typesDslMap.put(itemType, of(itemDsl));
              }

              Map<ComponentIdentifier, MetadataType> itemIdentifiers = new HashMap<>();
              itemIdentifiers.put(itemIdentifier, itemType);
              typesDslMap.entrySet().stream()
                  .filter(entry -> entry.getValue().isPresent())
                  .forEach(entry -> {
                    getIdentifier(entry.getValue().get()).ifPresent(subTypeIdentifier -> {
                      itemIdentifiers.put(subTypeIdentifier, entry.getKey());
                    });
                  });

              List<ComponentAst> componentModels = paramComponent.getInnerComponents().stream()
                  .filter(c -> itemIdentifiers.keySet().contains(c.getIdentifier()))
                  .map(componentModel -> (ComponentAst) componentModel)
                  .collect(toList());

              componentModel.setParameter(paramModel, new DefaultComponentParameterAst(componentModels,
                                                                                       () -> paramModel,
                                                                                       paramComponent.getMetadata()));
              paramComponent.getInnerComponents().stream().forEach(itemComponent -> {
                MetadataType type = itemIdentifiers.get(itemComponent.getIdentifier());
                typesDslMap.get(type).ifPresent(subTypeDsl -> {
                  MetadataTypeModelAdapter parameterizedModel = createParameterizedTypeModelAdapter(type, extensionModelHelper);
                  itemComponent.setMetadataTypeModelAdapter(parameterizedModel);

                  parameterizedModel.getAllParameterModels().stream().forEach(nestedParameter -> {
                    enrichComponentModels(itemComponent, getNestedComponents(itemComponent),
                                          subTypeDsl.getContainedElement(nestedParameter.getName()),
                                          nestedParameter, extensionModelHelper);
                  });

                });
              });
            });
      }
    };
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


  private void executeOnComponentTree(final ComponentModel component, final Consumer<ComponentModel> task)
      throws MuleRuntimeException {
    task.accept(component);
    component.getInnerComponents().forEach((innerComponent) -> {
      executeOnComponentTree(innerComponent, task);
    });
  }

  public void setComponentModel(org.mule.runtime.api.meta.model.ComponentModel model) {
    this.componentModel = model;
  }

  public void setNestableElementModel(NestableElementModel nestableElementModel) {
    this.nestableElementModel = nestableElementModel;
  }

  public void setConfigurationModel(ConfigurationModel model) {
    this.configurationModel = model;
  }

  public void setConnectionProviderModel(ConnectionProviderModel connectionProviderModel) {
    this.connectionProviderModel = connectionProviderModel;
  }

  public void setMetadataTypeModelAdapter(MetadataTypeModelAdapter metadataTypeModelAdapter) {
    this.metadataTypeModelAdapter = metadataTypeModelAdapter;
  }

  /**
   * @return the value of the name attribute.
   * @deprecated Use {@link ComponentAst#getComponentId()} instead.
   */
  @Deprecated
  public String getNameAttribute() {
    if (this instanceof ComponentAst) {
      return ((ComponentAst) this).getComponentId()
          .orElseGet(() -> parameters.get(ApplicationModel.NAME_ATTRIBUTE));
    } else {
      return parameters.get(ApplicationModel.NAME_ATTRIBUTE);
    }
  }

  /**
   * @return true if this {@code ComponentModel} represents a {@code org.mule.runtime.core.api.processor.MessageProcessor} scope.
   */
  public boolean isScope() {
    return Router.class.isAssignableFrom(type);
  }

  public void setParent(ComponentModel parent) {
    this.parent = parent;
  }

  /**
   * @return the parent component model in the configuration.
   */
  public ComponentModel getParent() {
    return parent;
  }

  /**
   * @return content of the configuration element.
   */
  public String getTextContent() {
    return textContent;
  }

  /**
   * @param componentLocation the location of the component in the configuration.
   */
  public void setComponentLocation(DefaultComponentLocation componentLocation) {
    this.componentLocation = componentLocation;
  }

  /**
   * @return the location of the component in the configuration.
   */
  public DefaultComponentLocation getComponentLocation() {
    return componentLocation;
  }

  /**
   * @return the object instance already created for this model
   */
  public Object getObjectInstance() {
    return objectInstance;
  }

  /**
   * Setter used for components that should be created eagerly without going through spring. This is the case of models
   * contributing to IoC {@link org.mule.runtime.api.ioc.ObjectProvider} interface that require to be created before the
   * application components so they can be referenced.
   *
   * @param objectInstance the object instance created from this model.
   */
  public void setObjectInstance(Object objectInstance) {
    this.objectInstance = objectInstance;
  }

  /**
   * @param parameterName configuration parameter name
   * @return true if the value provided for the configuration parameter was get from the DSL schema, false if it was explicitly
   * defined in the config
   */
  public boolean isParameterValueProvidedBySchema(String parameterName) {
    return this.schemaValueParameter.contains(parameterName);
  }

  // TODO MULE-11355: Make the ComponentModel haven an ComponentConfiguration internally
  @Deprecated
  public ComponentConfiguration getConfiguration() {
    InternalComponentConfiguration.Builder builder = InternalComponentConfiguration.builder()
        .withIdentifier(this.getIdentifier())
        .withValue(textContent);

    parameters.entrySet().forEach(e -> builder.withParameter(e.getKey(), e.getValue()));
    innerComponents.forEach(i -> builder.withNestedComponent(i.getConfiguration()));
    getMetadata().getParserAttributes().forEach(builder::withProperty);
    builder.withComponentLocation(this.componentLocation);
    builder.withProperty(COMPONENT_MODEL_KEY, this);

    return builder.build();
  }

  /**
   * Executes the task on every inner component associated to this componentModel.
   *
   * @param task to be executed on inner components.
   */
  public void executedOnEveryInnerComponent(final Consumer<ComponentModel> task) {
    for (ComponentModel componentModel : innerComponents) {
      task.accept(componentModel);
      componentModel.executedOnEveryInnerComponent(task);
    }
  }

  /**
   * @return the source code associated with this component.
   */
  @Deprecated
  public String getSourceCode() {
    return componentMetadata.getSourceCode().orElse(null);
  }

  public ComponentMetadataAst getMetadata() {
    return componentMetadata;
  }

  /**
   * Builder to create instances of {@code ComponentModel}.
   */
  public static class Builder {

    private final ComponentModel model = new SpringComponentModel();
    private ComponentModel root;

    private final org.mule.runtime.ast.api.ComponentMetadataAst.Builder metadataBuilder = ComponentMetadataAst.builder();

    /**
     * Default constructor for this builder.
     */
    public Builder() {}

    /**
     * Creates an instance of the Builder which will allow to merge other root component models to the given one. The root
     * component model provided here will be modified instead of cloned.
     *
     * @param root {@link ComponentModel} to be used as root. It will be modified.
     */
    public Builder(ComponentModel root) {
      this.root = root;
    }

    /**
     * @param identifier identifier for the configuration element this object represents.
     * @return the builder.
     */
    public Builder setIdentifier(ComponentIdentifier identifier) {
      checkIsNotBuildingFromRootComponentModel("identifier");
      this.model.identifier = identifier;
      return this;
    }

    /**
     * @param parameterName   name of the configuration parameter.
     * @param value           value contained by the configuration parameter.
     * @param valueFromSchema
     * @return the builder.
     */
    public Builder addParameter(String parameterName, String value, boolean valueFromSchema) {
      checkIsNotBuildingFromRootComponentModel("parameters");
      this.model.parameters.put(parameterName, value);
      if (valueFromSchema) {
        this.model.schemaValueParameter.add(parameterName);
      }
      return this;
    }

    /**
     * Adds a new complex child object to this {@code ComponentModel}.
     *
     * @param componentModel child {@code ComponentModel} declared in the configuration.
     * @return the builder.
     */
    public Builder addChildComponentModel(ComponentModel componentModel) {
      checkIsNotBuildingFromRootComponentModel("innerComponents");
      this.model.innerComponents.add(componentModel);
      componentModel.setParent(model);
      return this;
    }

    /**
     * Sets the inner content of the configuration element.
     *
     * @param textContent inner text content from the configuration.
     * @return the builder.
     */
    public Builder setTextContent(String textContent) {
      checkIsNotBuildingFromRootComponentModel("textComponent");
      this.model.textContent = textContent;
      return this;
    }

    /**
     * When invoked the created {@code ComponentModel} will be marked us a top level configuration.
     *
     * @return the builder.
     */
    public Builder markAsRootComponent() {
      checkIsNotBuildingFromRootComponentModel("root");
      this.model.root = true;
      return this;
    }

    /**
     * Adds a custom attribute to the {@code ComponentModel}. This custom attribute is meant to hold metadata of the configuration
     * and not to be used to instantiate the runtime object that corresponds to this configuration.
     *
     * @param name  custom attribute name.
     * @param value custom attribute value.
     * @return the builder.
     */
    public Builder addCustomAttribute(String name, Object value) {
      checkIsNotBuildingFromRootComponentModel("customAttributes");
      return addCustomAttribute(QName.valueOf(name), value);
    }

    /**
     * Adds a custom attribute to the {@code ComponentModel}. This custom attribute is meant to hold metadata of the configuration
     * and not to be used to instantiate the runtime object that corresponds to this configuration.
     *
     * @param name  custom attribute name.
     * @param value custom attribute value.
     * @return the builder.
     */
    public Builder addCustomAttribute(final QName qname, Object value) {
      checkIsNotBuildingFromRootComponentModel("customAttributes");
      if (isEmpty(qname.getNamespaceURI()) || NS_MULE_PARSER_METADATA.equals(qname.getNamespaceURI())) {
        this.metadataBuilder.putParserAttribute(qname.getLocalPart(), value);
      } else {
        this.metadataBuilder.putDocAttribute(qname.toString(), value.toString());
        if (NS_MULE_DOCUMENTATION.equals(qname.getNamespaceURI())) {
          // This is added for compatibility, since in previous versions the doc attributes were looked up without the namespace.
          this.metadataBuilder.putDocAttribute(qname.getLocalPart(), value.toString());
        }
      }
      return this;
    }

    /**
     * @param configFileName the config file name in which this component was defined.
     * @return the builder.
     */
    public Builder setConfigFileName(String configFileName) {
      checkIsNotBuildingFromRootComponentModel("configFileName");
      this.metadataBuilder.setFileName(configFileName);
      return this;
    }

    /**
     * @param lineNumber the line number within the config file in which this component was defined.
     * @return the builder.
     */
    public Builder setLineNumber(int lineNumber) {
      checkIsNotBuildingFromRootComponentModel("lineNumber");
      this.metadataBuilder.setStartLine(lineNumber);
      this.metadataBuilder.setEndLine(lineNumber);
      return this;
    }

    /**
     * @param startColumn the start column within the config file in which this component was defined.
     * @return the builder.
     */
    public Builder setStartColumn(int startColumn) {
      checkIsNotBuildingFromRootComponentModel("startColumn");
      this.metadataBuilder.setStartColumn(startColumn);
      this.metadataBuilder.setEndColumn(startColumn);
      return this;
    }

    /**
     * @param sourceCode the source code associated with this component.
     * @return the builder.
     */
    public Builder setSourceCode(String sourceCode) {
      checkIsNotBuildingFromRootComponentModel("sourceCode");
      this.metadataBuilder.setSourceCode(sourceCode);
      return this;
    }

    /**
     * Given the following root component it will merge its customAttributes, parameters and schemaValueParameters to the root
     * component model.
     *
     * @param otherRootComponentModel another component model created as root to be merged.
     * @return the builder.
     */
    public Builder merge(ComponentModel otherRootComponentModel) {
      ((ComponentAst) otherRootComponentModel).getMetadata().getParserAttributes()
          .forEach((k, v) -> this.metadataBuilder.putParserAttribute(k, v));
      ((ComponentAst) otherRootComponentModel).getMetadata().getDocAttributes()
          .forEach((k, v) -> this.metadataBuilder.putDocAttribute(k, v));
      this.root.parameters.putAll(otherRootComponentModel.parameters);
      this.root.schemaValueParameter.addAll(otherRootComponentModel.schemaValueParameter);

      this.root.innerComponents.addAll(otherRootComponentModel.innerComponents);
      return this;
    }

    /**
     * @return a {@code ComponentModel} created based on the supplied parameters.
     */
    public ComponentModel build() {
      if (root != null) {
        return root;
      }
      checkState(model.identifier != null, "An identifier must be provided");
      model.componentMetadata = metadataBuilder.build();
      return model;
    }

    private void checkIsNotBuildingFromRootComponentModel(String parameter) {
      checkState(root == null,
                 format("%s cannot be modified when builder has been constructed from a root component", parameter));
    }

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ComponentModel that = (ComponentModel) o;

    if (root != that.root) {
      return false;
    }
    if (!Objects.equals(componentLocation, that.componentLocation)) {
      return false;
    }
    if (!identifier.equals(that.identifier)) {
      return false;
    }
    return parameters.equals(that.parameters);
  }

  @Override
  public int hashCode() {
    int result = (root ? 1 : 0);
    result = 31 * result + Objects.hashCode(componentLocation);
    result = 31 * result + identifier.hashCode();
    result = 31 * result + parameters.hashCode();
    return result;
  }

}
