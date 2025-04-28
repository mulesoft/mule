/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.Component.Annotations.NAME_ANNOTATION_KEY;
import static org.mule.runtime.api.component.Component.Annotations.REPRESENTATION_ANNOTATION_KEY;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.config.api.properties.PropertiesResolverUtils.loadProviderFactories;
import static org.mule.runtime.config.internal.dsl.XmlConstants.buildRawParamKeyForDocAttribute;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.addAnnotation;
import static org.mule.runtime.config.internal.dsl.spring.EagerObjectCreator.clearInternalCaches;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_PREFIX;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.NAME_ATTRIBUTE_NAME;
import static org.mule.runtime.config.internal.model.ApplicationModel.ANNOTATIONS_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.DESCRIPTION_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.DOC_DESCRIPTION_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.GLOBAL_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTIES_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.OBJECT_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.SECURITY_MANAGER_IDENTIFIER;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_COMPONENT_CONFIG;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_NAME;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_PARAMETERS;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isText;

import static java.lang.Class.forName;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.SimpleType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.MetadataTypeAdapter;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.internal.context.SpringConfigurationComponentLocator;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.extension.api.property.NoWrapperModelProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * The {@code BeanDefinitionFactory} is the one that knows how to convert a {@link ComponentAst} and its parameters to actual
 * {@link org.springframework.beans.factory.config.BeanDefinition}s that can later be converted to runtime objects that will be
 * part of the artifact.
 * <p>
 * It will recursively process a {@link ComponentAst} to create a {@link BeanDefinition}. For the time being it will collaborate
 * with the old bean definitions parsers for configurations that are partially defined in the new parsing method.
 *
 * @since 4.0
 */
public class BeanDefinitionFactory {

  public static final String SPRING_PROTOTYPE_OBJECT = "prototype";
  public static final String SPRING_SINGLETON_OBJECT = "singleton";
  public static final String SOURCE_TYPE = "sourceType";
  public static final String TARGET_TYPE = "targetType";
  public static final String OBJECT_SERIALIZER_REF = "defaultObjectSerializer-ref";
  public static final String CORE_ERROR_NS = CORE_PREFIX.toUpperCase();

  private final ImmutableSet<ComponentIdentifier> ignoredMuleCoreComponentIdentifiers =
      ImmutableSet.<ComponentIdentifier>builder()
          .add(DESCRIPTION_IDENTIFIER)
          .add(ANNOTATIONS_ELEMENT_IDENTIFIER)
          .add(DOC_DESCRIPTION_IDENTIFIER)
          .add(GLOBAL_PROPERTY_IDENTIFIER)
          .build();

  private final Set<ComponentIdentifier> ignoredMuleExtensionComponentIdentifiers;

  /**
   * These are the set of current language construct that have specific bean definitions parsers since we don't want to include
   * them in the parsing API.
   */
  private final ImmutableSet<ComponentIdentifier> customBuildersComponentIdentifiers =
      ImmutableSet.<ComponentIdentifier>builder()
          .add(MULE_PROPERTIES_IDENTIFIER)
          .add(MULE_PROPERTY_IDENTIFIER)
          .add(OBJECT_IDENTIFIER)
          .build();


  private final String artifactId;
  private final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry;
  private final BeanDefinitionCreator<CreateComponentBeanDefinitionRequest> componentProcessor;
  private final BeanDefinitionCreator<CreateDslParamGroupBeanDefinitionRequest> dslParamGroupProcessor;
  private final BeanDefinitionCreator<CreateParamBeanDefinitionRequest> paramProcessor;
  private final ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();

  /**
   * @param componentBuildingDefinitionRegistry a registry with all the known {@code ComponentBuildingDefinition}s by the
   *                                            artifact.
   * @param errorTypeRepository
   */
  public BeanDefinitionFactory(String artifactId, ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry,
                               boolean disableTrimWhitespaces, boolean disablePojoCdataTrimWhitespaces) {
    this.artifactId = artifactId;
    this.componentBuildingDefinitionRegistry = componentBuildingDefinitionRegistry;
    this.componentProcessor = buildComponentProcessorChainOfResponsability(disableTrimWhitespaces);
    this.dslParamGroupProcessor = buildDslParamGroupChainOfResponsability(disableTrimWhitespaces);
    this.paramProcessor = buildParamChainOfResponsability(disableTrimWhitespaces, disablePojoCdataTrimWhitespaces);
    this.ignoredMuleExtensionComponentIdentifiers = new HashSet<>();

    registerConfigurationPropertyProviders();
  }

  private void registerConfigurationPropertyProviders() {
    ignoredMuleExtensionComponentIdentifiers.addAll(loadProviderFactories().keySet());
  }

  public boolean isComponentIgnored(ComponentIdentifier identifier) {
    return ignoredMuleCoreComponentIdentifiers.contains(identifier) ||
        ignoredMuleExtensionComponentIdentifiers.contains(identifier);
  }

  /**
   * Creates a {@code BeanDefinition} for the {@code ComponentModel}.
   *
   * @param springComponentModels a {@link Map} created {@link ComponentAst} and {@link SpringComponentModel}
   * @param parentComponentModel  the container of the component model from which we want to create the bean definition.
   * @param component             the component model from which we want to create the bean definition.
   * @param registry              the bean registry since it may be required to get other bean definitions to create this one or
   *                              to register the bean definition.
   * @param componentLocator      where the locations of any {@link Component}'s locations must be registered
   */
  public void resolveComponent(Map<ComponentAst, SpringComponentModel> springComponentModels,
                               List<ComponentAst> componentHierarchy,
                               ComponentAst component,
                               BeanDefinitionRegistry registry,
                               SpringConfigurationComponentLocator componentLocator) {
    if (isComponentIgnored(component.getIdentifier())) {
      return;
    }

    List<SpringComponentModel> paramsModels = new ArrayList<>();

    component.getModel(ParameterizedModel.class)
        .ifPresent(pmzd -> {
          if (pmzd instanceof SourceModel) {
            ((SourceModel) pmzd).getSuccessCallback()
                .ifPresent(cbk -> cbk.getParameterGroupModels().stream()
                    .map(pmg -> resolveParameterGroup(springComponentModels, componentHierarchy, component,
                                                      pmg, registry, componentLocator))
                    .forEach(paramsModels::addAll));
            ((SourceModel) pmzd).getErrorCallback()
                .ifPresent(cbk -> cbk.getParameterGroupModels().stream()
                    .map(pmg -> resolveParameterGroup(springComponentModels, componentHierarchy, component,
                                                      pmg, registry, componentLocator))
                    .forEach(paramsModels::addAll));
          }

          pmzd.getParameterGroupModels().stream()
              .map(pmg -> resolveParameterGroup(springComponentModels, componentHierarchy, component,
                                                pmg, registry, componentLocator))
              .forEach(paramsModels::addAll);
        });

    final List<ComponentAst> nestedHierarchy = new ArrayList<>(componentHierarchy);
    nestedHierarchy.add(component);

    resolveComponentBeanDefinition(springComponentModels, componentHierarchy, component,
                                   paramsModels, registry, componentLocator,
                                   nestedComp -> resolveComponent(springComponentModels, nestedHierarchy, nestedComp,
                                                                  registry, componentLocator));
  }

  /**
   * Disposal method to be called once it is no longer used to avoid leaks.
   */
  public void close() {
    clearInternalCaches();
  }

  private List<SpringComponentModel> resolveParameterGroup(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                           List<ComponentAst> componentHierarchy,
                                                           ComponentAst component,
                                                           ParameterGroupModel groupModel,
                                                           BeanDefinitionRegistry registry,
                                                           SpringConfigurationComponentLocator componentLocator) {
    List<SpringComponentModel> paramsModels = new ArrayList<>();
    List<SpringComponentModel> groupParamsModels = new ArrayList<>();
    AtomicBoolean anyParamPresent = new AtomicBoolean();

    groupModel.getParameterModels()
        .forEach(pm -> {
          final ComponentParameterAst param = component.getParameter(groupModel.getName(), pm.getName());

          if (param != null && param.getValue() != null && param.getValue().getValue().isPresent()) {
            groupParamsModels
                .addAll(resolveParamBeanDefinition(springComponentModels, componentHierarchy, component, param, registry,
                                                   componentLocator));
            anyParamPresent.set(true);
          }
        });

    if (anyParamPresent.get()) {
      if (groupModel.isShowInDsl()) {
        final List<ComponentAst> nestedHierarchy = new ArrayList<>(componentHierarchy);
        nestedHierarchy.add(component);

        resolveComponentBeanDefinitionDslParamGroup(springComponentModels, nestedHierarchy, groupModel, groupParamsModels)
            .ifPresent(springComponentModel -> {
              paramsModels.add(springComponentModel);
              handleSpringComponentModel(springComponentModel, springComponentModels, registry, componentLocator);
            });
      } else {
        paramsModels.addAll(groupParamsModels);
      }
    }

    return paramsModels;
  }

  private List<SpringComponentModel> resolveParamBeanDefinition(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                                List<ComponentAst> componentHierarchy,
                                                                ComponentAst paramOwnerComponent,
                                                                ComponentParameterAst param,
                                                                BeanDefinitionRegistry registry,
                                                                SpringConfigurationComponentLocator componentLocator) {
    return param.getValue()
        .reduce(expr -> resolveParamBeanDefinitionSimpleType(springComponentModels, componentHierarchy, paramOwnerComponent,
                                                             param, registry, componentLocator)
                                                                 .map(Collections::singletonList)
                                                                 .orElse(emptyList()),
                v -> resolveParamBeanDefinitionFixedValue(springComponentModels, componentHierarchy, paramOwnerComponent,
                                                          param, registry, componentLocator));
  }

  private List<SpringComponentModel> resolveParamBeanDefinitionFixedValue(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                                          List<ComponentAst> componentHierarchy,
                                                                          ComponentAst paramOwnerComponent,
                                                                          ComponentParameterAst param,
                                                                          BeanDefinitionRegistry registry,
                                                                          SpringConfigurationComponentLocator componentLocator) {
    AtomicReference<SpringComponentModel> model = new AtomicReference<>();
    param.getModel().getType().accept(new MetadataTypeVisitor() {

      protected void visitMultipleChildren(List<Object> values) {
        final List<ComponentAst> updatedHierarchy = new ArrayList<>(componentHierarchy);
        updatedHierarchy.add(paramOwnerComponent);

        if (values != null) {
          values.stream()
              .filter(ComponentAst.class::isInstance)
              .forEach(child -> resolveComponentBeanDefinition(springComponentModels, updatedHierarchy, (ComponentAst) child,
                                                               emptyList(),
                                                               registry, componentLocator,
                                                               nestedComp -> resolveComponent(springComponentModels,
                                                                                              updatedHierarchy,
                                                                                              nestedComp, registry,
                                                                                              componentLocator)));
        }

        resolveComponentBeanDefinitionComplexParam(springComponentModels, updatedHierarchy, paramOwnerComponent, param,
                                                   emptySet(), registry, componentLocator,
                                                   nestedComp -> resolveComponent(springComponentModels, componentHierarchy,
                                                                                  nestedComp, registry, componentLocator))
                                                                                      .ifPresent(model::set);
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        final Object complexValue = param.getValue().getRight();
        if (complexValue instanceof List) {
          visitMultipleChildren((List) complexValue);
        } else {
          // references to a list defined elsewhere
          resolveParamBeanDefinitionSimpleType(springComponentModels, componentHierarchy, paramOwnerComponent, param, registry,
                                               componentLocator)
                                                   .ifPresent(model::set);
        }
      }

      @Override
      public void visitObject(ObjectType objectType) {
        final Object complexValue = param.getValue().getRight();

        if (isMap(objectType)) {
          if (complexValue instanceof List) {
            visitMultipleChildren((List) complexValue);
          } else {
            // references to a map defined elsewhere
            resolveParamBeanDefinitionSimpleType(springComponentModels, componentHierarchy, paramOwnerComponent, param, registry,
                                                 componentLocator)
                                                     .ifPresent(model::set);
          }
          return;
        }

        final List<ComponentAst> updatedHierarchy = new ArrayList<>(componentHierarchy);
        updatedHierarchy.add(paramOwnerComponent);

        if (complexValue instanceof final ComponentAst child) {
          List<SpringComponentModel> childParamsModels = new ArrayList<>();

          child.getModel(ParameterizedModel.class)
              .ifPresent(pmzd -> pmzd.getParameterGroupModels().stream()
                  .map(pmg -> resolveParameterGroup(springComponentModels, componentHierarchy, child, pmg, registry,
                                                    componentLocator))
                  .forEach(childParamsModels::addAll));

          updatedHierarchy.add(child);
          resolveComponentBeanDefinitionComplexParam(springComponentModels, updatedHierarchy, paramOwnerComponent, param,
                                                     childParamsModels, registry, componentLocator,
                                                     nestedComp -> resolveComponent(springComponentModels, componentHierarchy,
                                                                                    nestedComp, registry, componentLocator))
                                                                                        .ifPresent(model::set);
        }
      }

      @Override
      public void visitUnion(UnionType unionType) {
        final Object complexValue = param.getValue().getRight();
        if (complexValue instanceof ComponentAst) {
          unionType.getTypes()
              .stream()
              .filter(t -> ((ComponentAst) complexValue).getModel(MetadataTypeAdapter.class)
                  .map(a -> a.getType().equals(t))
                  .orElse(false))
              .forEach(t -> visitObject((ObjectType) t));
        }

      }

      @Override
      public void visitSimpleType(SimpleType simpleType) {
        resolveParamBeanDefinitionSimpleType(springComponentModels, componentHierarchy, paramOwnerComponent, param, registry,
                                             componentLocator)
                                                 .ifPresent(model::set);
      }
    });

    if (model.get() != null) {
      return singletonList(model.get());
    } else {
      return emptyList();
    }
  }

  private Optional<SpringComponentModel> resolveParamBeanDefinitionSimpleType(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                                              List<ComponentAst> componentHierarchy,
                                                                              ComponentAst paramOwnerComponent,
                                                                              ComponentParameterAst param,
                                                                              BeanDefinitionRegistry registry,
                                                                              SpringConfigurationComponentLocator componentLocator) {
    if (isContent(param.getModel()) || isText(param.getModel())) {
      final List<ComponentAst> updatedHierarchy = new ArrayList<>(componentHierarchy);
      updatedHierarchy.add(paramOwnerComponent);
      return resolveComponentBeanDefinitionComplexParam(springComponentModels, updatedHierarchy, paramOwnerComponent, param,
                                                        emptySet(), registry, componentLocator,
                                                        nestedComp -> resolveComponent(springComponentModels, componentHierarchy,
                                                                                       nestedComp, registry, componentLocator));
    } else {
      return empty();
    }
  }

  private Optional<SpringComponentModel> resolveComponentBeanDefinition(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                                        List<ComponentAst> componentHierarchy,
                                                                        ComponentAst component,
                                                                        List<SpringComponentModel> paramsModels,
                                                                        BeanDefinitionRegistry registry,
                                                                        SpringConfigurationComponentLocator componentLocator,
                                                                        Consumer<ComponentAst> nestedComponentParamProcessor) {
    Optional<ComponentBuildingDefinition<?>> buildingDefinitionOptional =
        componentBuildingDefinitionRegistry.getBuildingDefinition(component.getIdentifier());
    if (buildingDefinitionOptional.isPresent() || customBuildersComponentIdentifiers.contains(component.getIdentifier())) {
      final CreateComponentBeanDefinitionRequest request =
          new CreateComponentBeanDefinitionRequest(componentHierarchy, component, paramsModels,
                                                   buildingDefinitionOptional.orElse(null), nestedComponentParamProcessor);

      this.componentProcessor.processRequest(springComponentModels, request);
      handleSpringComponentModel(request.getSpringComponentModel(), springComponentModels, registry, componentLocator);
      return of(request.getSpringComponentModel());
    } else {
      return empty();
    }
  }

  private Optional<SpringComponentModel> resolveComponentBeanDefinitionComplexParam(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                                                    List<ComponentAst> componentHierarchy,
                                                                                    ComponentAst paramOwnerComponent,
                                                                                    ComponentParameterAst param,
                                                                                    Collection<SpringComponentModel> paramsModels,
                                                                                    BeanDefinitionRegistry registry,
                                                                                    SpringConfigurationComponentLocator componentLocator,
                                                                                    Consumer<ComponentAst> nestedComponentParamProcessor) {
    return paramOwnerComponent.getGenerationInformation().getSyntax()
        .flatMap(ownerSyntax -> {
          if (param.getGroupModel().isShowInDsl()) {
            return ownerSyntax.getChild(param.getGroupModel().getName())
                .flatMap(dslGroupSyntax -> dslGroupSyntax.getChild(param.getModel().getName()));
          } else {
            if (ownerSyntax.getContainedElement(param.getModel().getName()).isPresent()
                && ownerSyntax.getContainedElement(param.getModel().getName()).get().isWrapped()) {
              return ownerSyntax.getContainedElement(param.getModel().getName());
            } else {
              return param.getGenerationInformation().getSyntax();
            }
          }
        })
        .filter(paramSyntax -> !isEmpty(paramSyntax.getElementName()))
        .flatMap(paramSyntax -> {
          ComponentIdentifier paramComponentIdentifier = ComponentIdentifier.builder()
              .namespaceUri(paramSyntax.getNamespace())
              .namespace(paramSyntax.getPrefix())
              .name(paramSyntax.getElementName())
              .build();

          final ComponentIdentifier paramValueComponentIdentifier =
              getParamValueComponentIdentifier(param, paramComponentIdentifier);

          return resolveComplexParamBuildingDefinition(param, paramValueComponentIdentifier)
              .map(buildingDefinition -> {
                CreateParamBeanDefinitionRequest request =
                    new CreateParamBeanDefinitionRequest(componentHierarchy, paramsModels, paramOwnerComponent, param,
                                                         buildingDefinition, paramComponentIdentifier,
                                                         nestedComponentParamProcessor);
                this.paramProcessor.processRequest(springComponentModels, request);

                param.getValue().applyRight(v -> {
                  if (v instanceof ComponentAst) {
                    request.getSpringComponentModel().setComponent((ComponentAst) v);
                  }
                });

                handleSpringComponentModel(request.getSpringComponentModel(), springComponentModels, registry, componentLocator);
                return request.getSpringComponentModel();
              });
        });
  }

  private ComponentIdentifier getParamValueComponentIdentifier(final ComponentParameterAst param,
                                                               final ComponentIdentifier paramComponentIdentifier) {
    if (param.getValue().getValue().isPresent()) {
      Object valueObject = param.getValue().getValue().get();
      if (valueObject instanceof ComponentAst valueAst) {
        return valueAst.getIdentifier();
      }
    }

    return param.getGenerationInformation().getSyntax()
        .filter(paramValueSyntax -> !isEmpty(paramValueSyntax.getElementName()))
        .map(paramValueSyntax -> ComponentIdentifier.builder()
            .namespaceUri(paramValueSyntax.getNamespace())
            .namespace(paramValueSyntax.getPrefix())
            .name(paramValueSyntax.getElementName())
            .build())
        .orElse(paramComponentIdentifier);
  }

  private Optional<ComponentBuildingDefinition<?>> resolveComplexParamBuildingDefinition(ComponentParameterAst param,
                                                                                         final ComponentIdentifier paramValueComponentIdentifier) {
    if (param.getModel().getModelProperty(NoWrapperModelProperty.class).isPresent()) {
      return param.getModel().getType()
          .getAnnotation(ClassInformationAnnotation.class)
          .map(cia -> {
            try {
              return new ComponentBuildingDefinition.Builder()
                  .withNamespace(paramValueComponentIdentifier.getNamespace())
                  .withIdentifier(paramValueComponentIdentifier.getName())
                  .withTypeDefinition(fromType(forName(cia.getClassname())))
                  .build();
            } catch (ClassNotFoundException e) {
              throw new MuleRuntimeException(e);
            }
          });
    } else {
      return componentBuildingDefinitionRegistry.getBuildingDefinition(paramValueComponentIdentifier);
    }
  }

  private ComponentBuildingDefinition noWrapperBeanDefinition(ComponentParameterAst param,
                                                              final ComponentIdentifier paramValueComponentIdentifier) {
    return param.getModel().getType()
        .getAnnotation(ClassInformationAnnotation.class)
        .map(cia -> {
          try {
            return new ComponentBuildingDefinition.Builder()
                .withNamespace(paramValueComponentIdentifier.getNamespace())
                .withIdentifier(paramValueComponentIdentifier.getName())
                .withTypeDefinition(fromType(forName(cia.getClassname())))
                .build();
          } catch (ClassNotFoundException e) {
            throw new MuleRuntimeException(e);
          }
        })
        .orElse(null);
  }

  private Optional<SpringComponentModel> resolveComponentBeanDefinitionDslParamGroup(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                                                     List<ComponentAst> componentHierarchy,
                                                                                     ParameterGroupModel paramGroupModel,
                                                                                     Collection<SpringComponentModel> paramsModels) {
    final ComponentAst paramOwnerComponentModel = componentHierarchy.get(componentHierarchy.size() - 1);

    return paramOwnerComponentModel.getGenerationInformation().getSyntax()
        .flatMap(ownerSyntax -> ownerSyntax.getChild(paramGroupModel.getName()))
        .flatMap(groupSyntax -> {

          ComponentIdentifier paramGroupComponentIdentifier = ComponentIdentifier.builder()
              .namespaceUri(groupSyntax.getNamespace())
              .namespace(groupSyntax.getPrefix())
              .name(groupSyntax.getElementName())
              .build();

          return componentBuildingDefinitionRegistry.getBuildingDefinition(paramGroupComponentIdentifier)
              .map(buildingDefinition -> {
                final CreateDslParamGroupBeanDefinitionRequest request =
                    new CreateDslParamGroupBeanDefinitionRequest(paramGroupModel, componentHierarchy, paramsModels,
                                                                 paramOwnerComponentModel, buildingDefinition,
                                                                 paramGroupComponentIdentifier);

                this.dslParamGroupProcessor.processRequest(springComponentModels, request);
                return request.getSpringComponentModel();
              });
        });
  }

  protected void handleSpringComponentModel(SpringComponentModel springComponentModel,
                                            Map<ComponentAst, SpringComponentModel> springComponentModels,
                                            BeanDefinitionRegistry registry,
                                            SpringConfigurationComponentLocator componentLocator) {
    ComponentAst component = springComponentModel.getComponent();
    springComponentModels.put(component, springComponentModel);

    if (component == null) {
      return;
    }

    // TODO MULE-9638: Once we migrate all core definitions we need to define a mechanism for customizing
    // how core constructs are processed.
    processMuleSecurityManager(springComponentModels, component, registry);

    componentBuildingDefinitionRegistry.getBuildingDefinition(component.getIdentifier())
        .ifPresent(componentBuildingDefinition -> {
          if ((springComponentModel.getType() != null)
              && Component.class.isAssignableFrom(springComponentModel.getType())) {
            addAnnotation(ANNOTATION_NAME, component.getIdentifier(), springComponentModel);
            // We need to use a mutable map since spring will resolve the properties placeholder present in the value if
            // needed and it will be done by mutating the same map.

            final Map<String, String> rawParams = new HashMap<>();
            component.getMetadata().getDocAttributes().entrySet().stream()
                .forEach(docAttr -> buildRawParamKeyForDocAttribute(docAttr)
                    .ifPresent(key -> rawParams.put(key, docAttr.getValue())));

            addAnnotation(ANNOTATION_PARAMETERS,
                          component.getModel(ParameterizedModel.class)
                              .map(pm -> {
                                component.getParameters().stream()
                                    .filter(param -> param.getValue().getValue().isPresent())
                                    .forEach(param -> rawParams.put(param.getModel().getName(),
                                                                    param.getValue()
                                                                        .mapLeft(expr -> "#[" + expr + "]")
                                                                        .getValue().get().toString()));

                                return rawParams;
                              })
                              .orElse(rawParams),
                          springComponentModel);

            componentLocator.addComponentLocation(component.getLocation());
            addAnnotation(ANNOTATION_COMPONENT_CONFIG, component, springComponentModel);
          }
        });

    addAnnotation(LOCATION_KEY, component.getLocation(), springComponentModel);
    addAnnotation(REPRESENTATION_ANNOTATION_KEY, resolveProcessorRepresentation(artifactId,
                                                                                component.getLocation(),
                                                                                component.getMetadata()),
                  springComponentModel);
  }

  /**
   * Generates a representation of a flow element to be logged in a standard way.
   *
   * @param appId
   * @param processorPath
   * @param element
   * @return
   */
  public static String resolveProcessorRepresentation(String appId, ComponentLocation processorPath,
                                                      ComponentMetadataAst metadata) {
    StringBuilder stringBuilder = new StringBuilder();

    stringBuilder.append(processorPath.getLocation())
        .append(" @ ")
        .append(appId);

    String sourceFile = metadata.getFileName().orElse(null);
    if (sourceFile != null) {
      stringBuilder.append(":")
          .append(sourceFile)
          .append(":")
          .append(metadata.getStartLine().orElse(-1));
    }

    Object docName = metadata.getDocAttributes().get(NAME_ANNOTATION_KEY.getLocalPart());
    if (docName != null) {
      stringBuilder.append(" (")
          .append(docName)
          .append(")");
    }

    return stringBuilder.toString();
  }

  private void processMuleSecurityManager(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                          ComponentAst component, BeanDefinitionRegistry registry) {
    if (component.getIdentifier().equals(SECURITY_MANAGER_IDENTIFIER)) {
      component.directChildrenStream().forEach(childComponentModel -> {
        String identifier = childComponentModel.getIdentifier().getName();
        if (identifier.equals("password-encryption-strategy")
            || identifier.equals("secret-key-encryption-strategy")) {
          registry.registerBeanDefinition(childComponentModel.getParameter(DEFAULT_GROUP_NAME, NAME_ATTRIBUTE_NAME)
              .getResolvedRawValue(),
                                          springComponentModels.get(childComponentModel).getBeanDefinition());
        }
      });
    }
  }

  private BeanDefinitionCreator<CreateComponentBeanDefinitionRequest> buildComponentProcessorChainOfResponsability(boolean disableTrimWhitespaces) {
    EagerObjectCreator eagerObjectCreator = new EagerObjectCreator();
    ObjectBeanDefinitionCreator objectBeanDefinitionCreator = new ObjectBeanDefinitionCreator();
    SimpleTypeBeanComponentDefinitionCreator simpleTypeBeanDefinitionCreator = new SimpleTypeBeanComponentDefinitionCreator();
    MapEntryBeanDefinitionCreator mapEntryBeanDefinitionCreator = new MapEntryBeanDefinitionCreator();
    CommonComponentBeanDefinitionCreator commonComponentModelProcessor =
        new CommonComponentBeanDefinitionCreator(objectFactoryClassRepository, disableTrimWhitespaces);

    eagerObjectCreator.setNext(objectBeanDefinitionCreator);
    objectBeanDefinitionCreator.setNext(simpleTypeBeanDefinitionCreator);
    simpleTypeBeanDefinitionCreator.setNext(mapEntryBeanDefinitionCreator);
    mapEntryBeanDefinitionCreator.setNext(commonComponentModelProcessor);

    return eagerObjectCreator;
  }

  private BeanDefinitionCreator<CreateDslParamGroupBeanDefinitionRequest> buildDslParamGroupChainOfResponsability(boolean disableTrimWhitespaces) {
    return new CommonDslParamGroupBeanDefinitionCreator(objectFactoryClassRepository, disableTrimWhitespaces);
  }

  private BeanDefinitionCreator<CreateParamBeanDefinitionRequest> buildParamChainOfResponsability(boolean disableTrimWhitespaces,
                                                                                                  boolean disablePojoCdataTrimWhitespaces) {
    SimpleTypeBeanParamDefinitionCreator simpleTypeBeanDefinitionCreator =
        new SimpleTypeBeanParamDefinitionCreator(disableTrimWhitespaces, disablePojoCdataTrimWhitespaces);
    CollectionBeanDefinitionCreator collectionBeanDefinitionCreator = new CollectionBeanDefinitionCreator();
    MapBeanDefinitionCreator mapBeanDefinitionCreator = new MapBeanDefinitionCreator();
    CommonParamBeanDefinitionCreator commonComponentModelProcessor =
        new CommonParamBeanDefinitionCreator(objectFactoryClassRepository, disableTrimWhitespaces);

    simpleTypeBeanDefinitionCreator.setNext(collectionBeanDefinitionCreator);
    collectionBeanDefinitionCreator.setNext(mapBeanDefinitionCreator);
    mapBeanDefinitionCreator.setNext(commonComponentModelProcessor);

    return simpleTypeBeanDefinitionCreator;
  }

  /**
   * Used to collaborate with the bean definition parsers mechanism.
   *
   * @param componentIdentifier a {@code ComponentModel} identifier.
   * @return true if there's a {@code ComponentBuildingDefinition} for the specified configuration identifier, false if there's
   *         not.
   */
  public boolean hasDefinition(ComponentIdentifier componentIdentifier) {
    return isComponentIgnored(componentIdentifier)
        || customBuildersComponentIdentifiers.contains(componentIdentifier)
        || componentBuildingDefinitionRegistry.getBuildingDefinition(componentIdentifier).isPresent()
        || componentBuildingDefinitionRegistry.getWrappedComponent(componentIdentifier).isPresent();
  }

  /**
   * @param componentIdentifier the component identifier to check
   * @return {@code true} if the component identifier is one of the current language construct that have specific bean definitions
   *         parsers since we don't want to include them in the parsing API.
   */
  public boolean isLanguageConstructComponent(ComponentIdentifier componentIdentifier) {
    return customBuildersComponentIdentifiers.contains(componentIdentifier);
  }

}
