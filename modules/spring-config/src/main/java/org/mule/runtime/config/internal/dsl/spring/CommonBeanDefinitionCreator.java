/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.beanutils.BeanUtils.copyProperty;
import static org.mule.runtime.api.component.Component.ANNOTATIONS_PROPERTY_NAME;
import static org.mule.runtime.api.component.Component.Annotations.SOURCE_ELEMENT_ANNOTATION_KEY;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.SPRING_PROTOTYPE_OBJECT;
import static org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository.INSTANCE_CUSTOMIZATION_FUNCTION_OPTIONAL;
import static org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository.IS_EAGER_INIT;
import static org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository.IS_PROTOTYPE;
import static org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository.IS_SINGLETON;
import static org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository.OBJECT_TYPE_CLASS;
import static org.mule.runtime.config.internal.dsl.spring.PropertyComponentUtils.getPropertyValueFromPropertyComponent;
import static org.mule.runtime.config.internal.model.ApplicationModel.ANNOTATIONS_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.CUSTOM_TRANSFORMER_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTIES_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider.maskPasswords;
import static org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoader.resolveContextArtifactPluginClassLoaders;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.privileged.dsl.BeanDefinitionPostProcessor;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.privileged.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;

import com.google.common.collect.ImmutableSet;

/**
 * Processor in the chain of responsibility that knows how to handle a generic {@code ComponentBuildingDefinition}.
 *
 * @since 4.0
 *        <p/>
 *        TODO MULE-9638 set visibility to package
 */
public class CommonBeanDefinitionCreator extends BeanDefinitionCreator {

  private static Set<ComponentIdentifier> genericPropertiesCustomProcessingIdentifiers =
      ImmutableSet.<ComponentIdentifier>builder()
          .add(CUSTOM_TRANSFORMER_IDENTIFIER)
          .build();

  private final ObjectFactoryClassRepository objectFactoryClassRepository;
  private final BeanDefinitionPostProcessor beanDefinitionPostProcessor;
  private final boolean enableByteBuddy;

  public CommonBeanDefinitionCreator(ObjectFactoryClassRepository objectFactoryClassRepository, boolean enableByteBuddy) {
    this.objectFactoryClassRepository = objectFactoryClassRepository;

    this.beanDefinitionPostProcessor = resolvePostProcessor();
    this.enableByteBuddy = enableByteBuddy;
  }

  private BeanDefinitionPostProcessor resolvePostProcessor() {
    for (ClassLoader classLoader : resolveContextArtifactPluginClassLoaders()) {
      try {
        final BeanDefinitionPostProcessor foundProvider =
            new SpiServiceRegistry().lookupProvider(BeanDefinitionPostProcessor.class, classLoader);
        if (foundProvider != null) {
          return foundProvider;
        }
      } catch (Exception | ServiceConfigurationError e) {
        // Nothing to do, we just don't have compatibility plugin in the app
      }
    }
    return (componentModel, helper) -> {
    };
  }

  @Override
  public boolean handleRequest(CreateBeanDefinitionRequest request) {
    SpringComponentModel componentModel = request.getComponentModel();
    ComponentBuildingDefinition buildingDefinition = request.getComponentBuildingDefinition();
    componentModel.setType(retrieveComponentType(componentModel, buildingDefinition));
    BeanDefinitionBuilder beanDefinitionBuilder = createBeanDefinitionBuilder(componentModel, buildingDefinition);
    processAnnotations(componentModel, beanDefinitionBuilder);
    processComponentDefinitionModel(componentModel, buildingDefinition, beanDefinitionBuilder);
    return true;
  }

  private BeanDefinitionBuilder createBeanDefinitionBuilder(SpringComponentModel componentModel,
                                                            ComponentBuildingDefinition buildingDefinition) {
    BeanDefinitionBuilder beanDefinitionBuilder;
    if (buildingDefinition.getObjectFactoryType() != null) {
      beanDefinitionBuilder = createBeanDefinitionBuilderFromObjectFactory(componentModel, buildingDefinition);
    } else {
      beanDefinitionBuilder = genericBeanDefinition(componentModel.getType());
    }
    return beanDefinitionBuilder;
  }

  private void processNestedAnnotations(ComponentModel componentModel, Map<QName, Object> previousAnnotations) {
    componentModel.getInnerComponents().stream()
        .filter(cdm -> cdm.getIdentifier().equals(ANNOTATIONS_ELEMENT_IDENTIFIER))
        .findFirst()
        .ifPresent(annotationsCdm -> annotationsCdm.getInnerComponents().forEach(
                                                                                 annotationCdm -> previousAnnotations
                                                                                     .put(new QName(annotationCdm.getIdentifier()
                                                                                         .getNamespaceUri(),
                                                                                                    annotationCdm
                                                                                                        .getIdentifier()
                                                                                                        .getName()),
                                                                                          annotationCdm.getTextContent())));
  }

  private void processAnnotations(ComponentModel componentModel, BeanDefinitionBuilder beanDefinitionBuilder) {
    if (Component.class.isAssignableFrom(componentModel.getType())
        // ValueResolver end up generating pojos from the extension whose class is enhanced to have annotations
        || ValueResolver.class.isAssignableFrom(componentModel.getType())) {
      Map<QName, Object> annotations =
          processMetadataAnnotationsHelper(beanDefinitionBuilder, componentModel);
      processNestedAnnotations(componentModel, annotations);
      if (!annotations.isEmpty()) {
        beanDefinitionBuilder.addPropertyValue(ANNOTATIONS_PROPERTY_NAME, annotations);
      }
    }
  }

  private Map<QName, Object> processMetadataAnnotationsHelper(BeanDefinitionBuilder builder, ComponentModel componentModel) {
    Map<QName, Object> annotations = new HashMap<>();
    if (componentModel == null) {
      return annotations;
    } else {
      if (Component.class.isAssignableFrom(builder.getBeanDefinition().getBeanClass())) {
        addMetadataAnnotationsFromDocAttributes(annotations, componentModel.getMetadata());
        builder.getBeanDefinition().getPropertyValues().addPropertyValue(ANNOTATIONS_PROPERTY_NAME, annotations);
      }

      return annotations;
    }
  }

  /**
   * Populates the passed beanAnnotations with the other passed parameters.
   *
   * @param beanAnnotations the map with annotations to populate
   * @param metadata the parser metadata for the object being created
   */
  public static void addMetadataAnnotationsFromDocAttributes(Map<QName, Object> beanAnnotations,
                                                             ComponentMetadataAst metadata) {
    String sourceCode = metadata.getSourceCode().orElse(null);

    if (sourceCode != null) {
      beanAnnotations.put(SOURCE_ELEMENT_ANNOTATION_KEY, maskPasswords(sourceCode));
    }

    beanAnnotations.putAll(metadata.getDocAttributes().entrySet().stream()
        .collect(toMap(e -> QName.valueOf(e.getKey()), e -> e.getValue())));
  }

  private Class<?> retrieveComponentType(final ComponentModel componentModel,
                                         ComponentBuildingDefinition componentBuildingDefinition) {
    ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(componentModel);
    componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
    return objectTypeVisitor.getType();
  }

  private BeanDefinitionBuilder createBeanDefinitionBuilderFromObjectFactory(final SpringComponentModel componentModel,
                                                                             final ComponentBuildingDefinition componentBuildingDefinition) {
    ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(componentModel);
    componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
    Class<?> objectFactoryType = componentBuildingDefinition.getObjectFactoryType();

    Optional<Consumer<Object>> instanceCustomizationFunctionOptional;

    Map<String, Object> customProperties = getTransformerCustomProperties(componentModel);
    if (customProperties.isEmpty()) {
      instanceCustomizationFunctionOptional = empty();
    } else {
      instanceCustomizationFunctionOptional = of(object -> injectSpringProperties(customProperties, object));
    }

    if (!enableByteBuddy) {
      return rootBeanDefinition(objectFactoryClassRepository
          .getObjectFactoryDynamicClass(componentBuildingDefinition,
                                        objectFactoryType, componentModel.getType(),
                                        new LazyValue<>(() -> componentModel.getBeanDefinition().isLazyInit()),
                                        instanceCustomizationFunctionOptional));
    }

    return rootBeanDefinition(objectFactoryClassRepository
        .getObjectFactoryClass(objectFactoryType, instanceCustomizationFunctionOptional.isPresent(), componentModel.getType()))
            .addPropertyValue(IS_SINGLETON, !componentBuildingDefinition.isPrototype())
            .addPropertyValue(IS_PROTOTYPE, componentBuildingDefinition.isPrototype())
            .addPropertyValue(IS_EAGER_INIT, new LazyValue<>(() -> !componentModel.getBeanDefinition().isLazyInit()))
            .addPropertyValue(INSTANCE_CUSTOMIZATION_FUNCTION_OPTIONAL, instanceCustomizationFunctionOptional);
  }

  private void injectSpringProperties(Map<String, Object> customProperties, Object createdInstance) {
    try {
      for (String propertyName : customProperties.keySet()) {
        copyProperty(createdInstance, propertyName, customProperties.get(propertyName));
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private Map<String, Object> getTransformerCustomProperties(ComponentModel componentModel) {
    ComponentIdentifier identifier = componentModel.getIdentifier();
    if (!identifier.equals(CUSTOM_TRANSFORMER_IDENTIFIER)) {
      return emptyMap();
    }
    return componentModel.getInnerComponents().stream()
        .filter(innerComponent -> innerComponent.getIdentifier().equals(MULE_PROPERTY_IDENTIFIER))
        .map(springComponent -> getPropertyValueFromPropertyComponent(springComponent))
        .collect(toMap(propValue -> propValue.getFirst(), propValue -> propValue.getSecond()));
  }

  private void processComponentDefinitionModel(final SpringComponentModel componentModel,
                                               ComponentBuildingDefinition componentBuildingDefinition,
                                               final BeanDefinitionBuilder beanDefinitionBuilder) {
    processObjectConstructionParameters(componentModel, componentBuildingDefinition,
                                        new BeanDefinitionBuilderHelper(beanDefinitionBuilder));
    processMuleProperties(componentModel, beanDefinitionBuilder, beanDefinitionPostProcessor);
    if (componentBuildingDefinition.isPrototype()) {
      beanDefinitionBuilder.setScope(SPRING_PROTOTYPE_OBJECT);
    }
    AbstractBeanDefinition originalBeanDefinition = beanDefinitionBuilder.getBeanDefinition();
    AbstractBeanDefinition wrappedBeanDefinition = adaptBeanDefinition(originalBeanDefinition);
    if (originalBeanDefinition != wrappedBeanDefinition) {
      componentModel.setType(wrappedBeanDefinition.getBeanClass());
    }
    final SpringPostProcessorIocHelper iocHelper =
        new SpringPostProcessorIocHelper(objectFactoryClassRepository, wrappedBeanDefinition, enableByteBuddy);
    componentModel.setBeanDefinition(iocHelper.getBeanDefinition());
  }

  static void processMuleProperties(ComponentModel componentModel, BeanDefinitionBuilder beanDefinitionBuilder,
                                    BeanDefinitionPostProcessor beanDefinitionPostProcessor) {
    // for now we skip custom-transformer since requires injection by the object factory.
    if (genericPropertiesCustomProcessingIdentifiers.contains(componentModel.getIdentifier())
        || (beanDefinitionPostProcessor != null && beanDefinitionPostProcessor.getGenericPropertiesCustomProcessingIdentifiers()
            .contains(componentModel.getIdentifier()))) {
      return;
    }
    componentModel.getInnerComponents()
        .stream()
        .filter(innerComponent -> {
          ComponentIdentifier identifier = innerComponent.getIdentifier();
          return identifier.equals(MULE_PROPERTY_IDENTIFIER)
              || identifier.equals(MULE_PROPERTIES_IDENTIFIER);
        })
        .forEach(propertyComponentModel -> {
          Pair<String, Object> propertyValue = getPropertyValueFromPropertyComponent(propertyComponentModel);
          beanDefinitionBuilder.addPropertyValue(propertyValue.getFirst(), propertyValue.getSecond());
        });
  }

  public static List<Pair<String, Object>> getPropertyValueFromPropertiesComponent(ComponentModel propertyComponentModel) {
    List<Pair<String, Object>> propertyValues = new ArrayList<>();
    propertyComponentModel.getInnerComponents().stream().forEach(entryComponentModel -> {
      propertyValues.add(new Pair<>(entryComponentModel.getRawParameters().get("key"),
                                    entryComponentModel.getRawParameters().get("value")));
    });
    return propertyValues;
  }

  private void processObjectConstructionParameters(final ComponentModel componentModel,
                                                   final ComponentBuildingDefinition componentBuildingDefinition,
                                                   final BeanDefinitionBuilderHelper beanDefinitionBuilderHelper) {
    new ComponentConfigurationBuilder(componentModel, componentBuildingDefinition, beanDefinitionBuilderHelper)
        .processConfiguration();

  }

  private AbstractBeanDefinition adaptBeanDefinition(AbstractBeanDefinition originalBeanDefinition) {
    Class beanClass;
    if (originalBeanDefinition instanceof RootBeanDefinition) {
      beanClass = ((RootBeanDefinition) originalBeanDefinition).getBeanClass();
    } else {
      try {
        beanClass = originalBeanDefinition.getBeanClass();
      } catch (IllegalStateException e) {
        try {
          beanClass = org.apache.commons.lang3.ClassUtils.getClass(originalBeanDefinition.getBeanClassName());
        } catch (ClassNotFoundException e2) {
          throw new RuntimeException(e2);
        }
      }
    }

    BeanDefinition newBeanDefinition;
    if (areMatchingTypes(SecurityFilter.class, beanClass)) {
      newBeanDefinition = rootBeanDefinition(SecurityFilterMessageProcessor.class)
          .addConstructorArgValue(originalBeanDefinition)
          .getBeanDefinition();
      return (AbstractBeanDefinition) newBeanDefinition;
    } else {
      final SpringPostProcessorIocHelper iocHelper =
          new SpringPostProcessorIocHelper(objectFactoryClassRepository, originalBeanDefinition, enableByteBuddy);
      return iocHelper.getBeanDefinition();
    }
  }

  public static boolean areMatchingTypes(Class<?> superType, Class<?> childType) {
    return superType.isAssignableFrom(childType);
  }

}
