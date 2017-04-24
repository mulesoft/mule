/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.AbstractAnnotatedObject.LOCATION_KEY;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.ANNOTATIONS_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.DESCRIPTION_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.DOC_DESCRIPTION_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.ERROR_MAPPING_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.INTERCEPTOR_STACK_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_DOMAIN_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_EE_DOMAIN_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_PROPERTIES_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.NAME_ATTRIBUTE;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.QUEUE_STORE;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_ENTRY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_LIST_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_MAP_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_VALUE_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.from;
import static org.mule.runtime.config.spring.dsl.spring.CommonBeanDefinitionCreator.adaptFilterBeanDefinitions;
import static org.mule.runtime.config.spring.dsl.spring.CommonBeanDefinitionCreator.areMatchingTypes;
import static org.mule.runtime.config.spring.dsl.spring.ComponentModelHelper.addAnnotation;
import static org.mule.runtime.config.spring.dsl.spring.WrapperElementType.COLLECTION;
import static org.mule.runtime.config.spring.dsl.spring.WrapperElementType.MAP;
import static org.mule.runtime.config.spring.dsl.spring.WrapperElementType.SINGLE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import static org.mule.runtime.core.component.ComponentAnnotations.ANNOTATION_NAME;
import static org.mule.runtime.core.component.ComponentAnnotations.ANNOTATION_PARAMETERS;
import static org.mule.runtime.core.exception.ErrorMapping.ANNOTATION_ERROR_MAPPINGS;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.config.spring.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.processor.AbstractAttributeDefinitionVisitor;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.exception.ErrorMapping;
import org.mule.runtime.core.exception.ErrorTypeMatcher;
import org.mule.runtime.core.exception.ErrorTypeRepository;
import org.mule.runtime.core.exception.SingleErrorTypeMatcher;
import org.mule.runtime.dsl.api.component.AttributeDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;

import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.w3c.dom.Element;

/**
 * The {@code BeanDefinitionFactory} is the one that knows how to convert a {@code ComponentModel} to an actual
 * {@link org.springframework.beans.factory.config.BeanDefinition} that can later be converted to a runtime object that will be
 * part of the artifact.
 * <p>
 * It will recursively process a {@code ComponentModel} to create a {@code BeanDefinition}. For the time being it will collaborate
 * with the old bean definitions parsers for configurations that are partially defined in the new parsing method.
 *
 * @since 4.0
 */
public class BeanDefinitionFactory {

  public static final String SPRING_PROTOTYPE_OBJECT = "prototype";
  public static final String SPRING_SINGLETON_OBJECT = "singleton";
  public static final String SOURCE_TYPE = "sourceType";
  public static final String TARGET_TYPE = "targetType";

  private final ImmutableSet<ComponentIdentifier> ignoredMuleCoreComponentIdentifiers =
      ImmutableSet.<ComponentIdentifier>builder()
          .add(MULE_IDENTIFIER)
          .add(MULE_DOMAIN_IDENTIFIER)
          .add(MULE_EE_DOMAIN_IDENTIFIER)
          .add(ERROR_MAPPING_IDENTIFIER)
          .add(DESCRIPTION_IDENTIFIER)
          .add(ANNOTATIONS_ELEMENT_IDENTIFIER)
          .add(DOC_DESCRIPTION_IDENTIFIER)
          // TODO MULE-9638: Uncomment this code when task is finished
          // .add(GLOBAL_PROPERTY_IDENTIFIER)
          .build();

  /**
   * These are the set of current language construct that have specific bean definitions parsers since we don't want to include
   * them in the parsing API.
   */
  private final ImmutableSet<ComponentIdentifier> customBuildersComponentIdentifiers =
      ImmutableSet.<ComponentIdentifier>builder()
          .add(builder().withNamespace(CORE_PREFIX).withName(QUEUE_STORE).build())
          .add(MULE_PROPERTIES_IDENTIFIER)
          .add(MULE_PROPERTY_IDENTIFIER)
          .add(SPRING_ENTRY_IDENTIFIER)
          .add(SPRING_LIST_IDENTIFIER)
          .add(SPRING_MAP_IDENTIFIER)
          .add(SPRING_VALUE_IDENTIFIER)
          .add(INTERCEPTOR_STACK_IDENTIFIER)
          .build();


  private ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry;
  private BeanDefinitionCreator componentModelProcessor;
  private ErrorTypeRepository errorTypeRepository;
  private ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();

  /**
   * @param componentBuildingDefinitionRegistry a registry with all the known {@code ComponentBuildingDefinition}s by the
   *        artifact.
   * @param errorTypeRepository
   */
  public BeanDefinitionFactory(ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry,
                               ErrorTypeRepository errorTypeRepository) {
    this.componentBuildingDefinitionRegistry = componentBuildingDefinitionRegistry;
    this.errorTypeRepository = errorTypeRepository;
    this.componentModelProcessor = buildComponentModelProcessorChainOfResponsability();
  }

  /**
   * Creates a {@code BeanDefinition} by traversing the {@code ComponentModel} and its children.
   *
   * @param parentComponentModel the parent component model since the bean definition to be created may depend on the context.
   * @param componentModel the component model from which we want to create the bean definition.
   * @param registry the bean registry since it may be required to get other bean definitions to create this one or to register
   *        the bean definition.
   * @param componentModelPostProcessor a function to post process the bean definition.
   * @param oldParsingMechanism a function to execute the old parsing mechanism if required by children {@code ComponentModel}s
   * @return the {@code BeanDefinition} of the component model.
   */
  public BeanDefinition resolveComponentRecursively(ComponentModel parentComponentModel, ComponentModel componentModel,
                                                    BeanDefinitionRegistry registry,
                                                    BiConsumer<ComponentModel, BeanDefinitionRegistry> componentModelPostProcessor,
                                                    BiFunction<Element, BeanDefinition, Either<BeanDefinition, BeanReference>> oldParsingMechanism) {
    List<ComponentModel> innerComponents = componentModel.getInnerComponents();
    if (!innerComponents.isEmpty()) {
      for (ComponentModel innerComponent : innerComponents) {
        if (innerComponent.isEnabled()) {
          if (hasDefinition(innerComponent.getIdentifier(), of(innerComponent.getParent().getIdentifier()))) {
            resolveComponentRecursively(componentModel, innerComponent, registry, componentModelPostProcessor,
                                        oldParsingMechanism);
          } else {
            Either<BeanDefinition, BeanReference> oldParseResult =
                oldParsingMechanism.apply((Element) from(innerComponent).getNode(), null);

            if (oldParseResult.isLeft()) {
              AbstractBeanDefinition oldBeanDefinition = (AbstractBeanDefinition) oldParseResult.getLeft();
              oldBeanDefinition = adaptFilterBeanDefinitions(componentModel, oldBeanDefinition);
              innerComponent.setBeanDefinition(oldBeanDefinition);
            } else if (oldParseResult.isRight()) {
              innerComponent.setBeanReference(oldParseResult.getRight());
            }
          }
        }
      }
    }
    return resolveComponent(parentComponentModel, componentModel, registry, componentModelPostProcessor);
  }

  private BeanDefinition resolveComponent(ComponentModel parentComponentModel, ComponentModel componentModel,
                                          BeanDefinitionRegistry registry,
                                          BiConsumer<ComponentModel, BeanDefinitionRegistry> componentDefinitionModelProcessor) {
    if (ignoredMuleCoreComponentIdentifiers.contains(componentModel.getIdentifier()) || !componentModel.isEnabled()) {
      return null;
    }
    resolveComponentBeanDefinition(parentComponentModel, componentModel);
    componentDefinitionModelProcessor.accept(componentModel, registry);
    // TODO MULE-9638: Once we migrate all core definitions we need to define a mechanism for customizing
    // how core constructs are processed.
    processMuleConfiguration(componentModel, registry);
    componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier())
        .ifPresent(componentBuildingDefinition -> {
          if ((componentModel.getType() != null) && AnnotatedObject.class.isAssignableFrom(componentModel.getType())) {
            addAnnotation(ANNOTATION_NAME, componentModel.getIdentifier(), componentModel);
            // We need to use a mutable map since spring will resolve the properties placeholder present in the value if needed
            // and it will be done by mutating the same map.
            addAnnotation(ANNOTATION_PARAMETERS, new HashMap<>(componentModel.getParameters()), componentModel);
            // add any error mappings if present
            List<ComponentModel> errorMappingComponents = componentModel.getInnerComponents().stream()
                .filter(innerComponent -> ERROR_MAPPING_IDENTIFIER.equals(innerComponent.getIdentifier())).collect(toList());
            if (!errorMappingComponents.isEmpty()) {
              addAnnotation(ANNOTATION_ERROR_MAPPINGS, errorMappingComponents.stream().map(innerComponent -> {
                Map<String, String> parameters = innerComponent.getParameters();
                ComponentIdentifier source = buildFromStringRepresentation(parameters.get(SOURCE_TYPE));
                ComponentIdentifier target = buildFromStringRepresentation(parameters.get(TARGET_TYPE));

                ErrorType errorType = errorTypeRepository
                    .lookupErrorType(source)
                    .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Unable to find error '%s' from Error Type Repository",
                                                                                    source)));

                ErrorTypeMatcher errorTypeMatcher = new SingleErrorTypeMatcher(errorType);
                ErrorType targetType = resolveErrorType(target);
                return new ErrorMapping(errorTypeMatcher, targetType);
              }).collect(toList()), componentModel);
            }
          }
        });

    addAnnotation(LOCATION_KEY, componentModel.getComponentLocation(), componentModel);

    BeanDefinition beanDefinition = componentModel.getBeanDefinition();
    return beanDefinition;
  }

  private ErrorType resolveErrorType(ComponentIdentifier errorIdentifier) {
    Optional<ErrorType> optionalErrorType = errorTypeRepository.lookupErrorType(errorIdentifier);
    return optionalErrorType.isPresent()
        ? optionalErrorType.get()
        : errorTypeRepository.addErrorType(errorIdentifier, errorTypeRepository.getAnyErrorType());
  }

  private void processMuleConfiguration(ComponentModel componentModel, BeanDefinitionRegistry registry) {
    if (componentModel.getIdentifier().equals(CONFIGURATION_IDENTIFIER)) {
      AtomicReference<BeanDefinition> defaultRetryPolicyTemplate = new AtomicReference<>();
      componentModel.getInnerComponents().stream().forEach(childComponentModel -> {
        if (areMatchingTypes(RetryPolicyTemplate.class, childComponentModel.getType())) {
          defaultRetryPolicyTemplate.set(childComponentModel.getBeanDefinition());
        }
      });
      if (defaultRetryPolicyTemplate.get() != null) {
        registry.registerBeanDefinition(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE, defaultRetryPolicyTemplate.get());
      }
    }
  }


  private void resolveComponentBeanDefinition(ComponentModel parentComponentModel, ComponentModel componentModel) {
    Optional<ComponentBuildingDefinition> buildingDefinitionOptional =
        componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier());
    if (buildingDefinitionOptional.isPresent() || customBuildersComponentIdentifiers.contains(componentModel.getIdentifier())) {
      this.componentModelProcessor.processRequest(new CreateBeanDefinitionRequest(parentComponentModel, componentModel,
                                                                                  buildingDefinitionOptional.orElse(null)));
    } else {
      boolean isWrapperComponent = isWrapperComponent(componentModel.getIdentifier(), of(parentComponentModel.getIdentifier()));
      if (!isWrapperComponent) {
        throw new MuleRuntimeException(createStaticMessage(format("No component building definition for element %s. It may be that there's a dependency "
            + "missing to the project that handle that extension.", componentModel.getIdentifier())));
      }
      processComponentWrapper(componentModel);
    }
  }

  private void processComponentWrapper(ComponentModel componentModel) {
    ComponentBuildingDefinition parentBuildingDefinition =
        componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getParent().getIdentifier()).get();
    Map<String, WrapperElementType> wrapperIdentifierAndTypeMap = getWrapperIdentifierAndTypeMap(parentBuildingDefinition);
    WrapperElementType wrapperElementType = wrapperIdentifierAndTypeMap.get(componentModel.getIdentifier().getName());
    if (wrapperElementType.equals(SINGLE)) {
      componentModel.setType(componentModel.getInnerComponents().get(0).getType());
      componentModel.setBeanDefinition(componentModel.getInnerComponents().get(0).getBeanDefinition());
      componentModel.setBeanReference(componentModel.getInnerComponents().get(0).getBeanReference());
    } else {
      throw new IllegalStateException(format("Element %s does not have a building definition and it should since it's of type %s",
                                             componentModel.getIdentifier(), wrapperElementType));
    }
  }

  public static void checkElementNameUnique(BeanDefinitionRegistry registry, Element element) {
    if (null != element.getAttributeNode(NAME_ATTRIBUTE)) {
      String name = element.getAttribute(NAME_ATTRIBUTE);
      if (registry.containsBeanDefinition(name)) {
        throw new IllegalArgumentException("A component named " + name + " already exists.");
      }
    }
  }

  private BeanDefinitionCreator buildComponentModelProcessorChainOfResponsability() {
    ExceptionStrategyRefBeanDefinitionCreator exceptionStrategyRefBeanDefinitionCreator =
        new ExceptionStrategyRefBeanDefinitionCreator();
    PropertiesMapBeanDefinitionCreator propertiesMapBeanDefinitionCreator = new PropertiesMapBeanDefinitionCreator();
    InterceptorStackBeanDefinitionCreator interceptorStackBeanDefinitionCreator = new InterceptorStackBeanDefinitionCreator();
    FilterReferenceBeanDefinitionCreator filterReferenceBeanDefinitionCreator = new FilterReferenceBeanDefinitionCreator();
    ReferenceBeanDefinitionCreator referenceBeanDefinitionCreator = new ReferenceBeanDefinitionCreator();
    SimpleTypeBeanDefinitionCreator simpleTypeBeanDefinitionCreator = new SimpleTypeBeanDefinitionCreator();
    CollectionBeanDefinitionCreator collectionBeanDefinitionCreator = new CollectionBeanDefinitionCreator();
    MapEntryBeanDefinitionCreator mapEntryBeanDefinitionCreator = new MapEntryBeanDefinitionCreator();
    MapBeanDefinitionCreator mapBeanDefinitionCreator = new MapBeanDefinitionCreator();
    CommonBeanDefinitionCreator commonComponentModelProcessor = new CommonBeanDefinitionCreator(objectFactoryClassRepository);
    propertiesMapBeanDefinitionCreator.setNext(interceptorStackBeanDefinitionCreator);
    interceptorStackBeanDefinitionCreator.setNext(exceptionStrategyRefBeanDefinitionCreator);
    exceptionStrategyRefBeanDefinitionCreator.setNext(exceptionStrategyRefBeanDefinitionCreator);
    exceptionStrategyRefBeanDefinitionCreator.setNext(filterReferenceBeanDefinitionCreator);
    filterReferenceBeanDefinitionCreator.setNext(referenceBeanDefinitionCreator);
    referenceBeanDefinitionCreator.setNext(simpleTypeBeanDefinitionCreator);
    simpleTypeBeanDefinitionCreator.setNext(collectionBeanDefinitionCreator);
    collectionBeanDefinitionCreator.setNext(mapEntryBeanDefinitionCreator);
    mapEntryBeanDefinitionCreator.setNext(mapBeanDefinitionCreator);
    mapBeanDefinitionCreator.setNext(commonComponentModelProcessor);
    return propertiesMapBeanDefinitionCreator;
  }

  /**
   * Used to collaborate with the bean definition parsers mechanism. If {@code #hasDefinition} returns false, then the old
   * mechanism must be used.
   *
   * @param componentIdentifier a {@code ComponentModel} identifier.
   * @param parentComponentModelOptional the {@code ComponentModel} parent identifier.
   * @return true if there's a {@code ComponentBuildingDefinition} for the specified configuration identifier, false if there's
   *         not.
   */
  public boolean hasDefinition(ComponentIdentifier componentIdentifier,
                               Optional<ComponentIdentifier> parentComponentModelOptional) {
    return ignoredMuleCoreComponentIdentifiers.contains(componentIdentifier)
        || customBuildersComponentIdentifiers.contains(componentIdentifier)
        || componentBuildingDefinitionRegistry.getBuildingDefinition(componentIdentifier).isPresent()
        || isWrapperComponent(componentIdentifier, parentComponentModelOptional);
  }

  // TODO MULE-9638 this code will be removed and a cache will be implemented
  public boolean isWrapperComponent(ComponentIdentifier componentModel,
                                    Optional<ComponentIdentifier> parentComponentModelOptional) {
    if (!parentComponentModelOptional.isPresent()) {
      return false;
    }
    Optional<ComponentBuildingDefinition> buildingDefinitionOptional =
        componentBuildingDefinitionRegistry.getBuildingDefinition(parentComponentModelOptional.get());
    if (!buildingDefinitionOptional.isPresent()) {
      return false;
    }
    final Map<String, WrapperElementType> wrapperIdentifierAndTypeMap =
        getWrapperIdentifierAndTypeMap(buildingDefinitionOptional.get());
    return wrapperIdentifierAndTypeMap.containsKey(componentModel.getName());
  }

  private Map<String, WrapperElementType> getWrapperIdentifierAndTypeMap(ComponentBuildingDefinition buildingDefinition) {
    final Map<String, WrapperElementType> wrapperIdentifierAndTypeMap = new HashMap<>();
    AbstractAttributeDefinitionVisitor wrapperIdentifiersCollector = new AbstractAttributeDefinitionVisitor() {

      @Override
      public void onComplexChildCollection(Class<?> type, Optional<String> wrapperIdentifierOptional) {
        wrapperIdentifierOptional.ifPresent(wrapperIdentifier -> wrapperIdentifierAndTypeMap.put(wrapperIdentifier, COLLECTION));
      }

      @Override
      public void onComplexChild(Class<?> type, Optional<String> wrapperIdentifierOptional, Optional<String> childIdentifier) {
        wrapperIdentifierOptional.ifPresent(wrapperIdentifier -> wrapperIdentifierAndTypeMap.put(wrapperIdentifier, SINGLE));
      }

      @Override
      public void onComplexChildMap(Class<?> keyType, Class<?> valueType, String wrapperIdentifier) {
        wrapperIdentifierAndTypeMap.put(wrapperIdentifier, MAP);
      }

      @Override
      public void onMultipleValues(KeyAttributeDefinitionPair[] definitions) {
        for (KeyAttributeDefinitionPair attributeDefinition : definitions) {
          attributeDefinition.getAttributeDefinition().accept(this);
        }
      }
    };

    Consumer<AttributeDefinition> collectWrappersConsumer =
        attributeDefinition -> attributeDefinition.accept(wrapperIdentifiersCollector);
    buildingDefinition.getSetterParameterDefinitions().stream()
        .map(setterAttributeDefinition -> setterAttributeDefinition.getAttributeDefinition()).forEach(collectWrappersConsumer);
    buildingDefinition.getConstructorAttributeDefinition().stream().forEach(collectWrappersConsumer);
    return wrapperIdentifierAndTypeMap;
  }

  /**
   * Release resources from the bean factory.
   */
  public void destroy() {
    objectFactoryClassRepository.destroy();
  }
}
