/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.component.Component.ANNOTATIONS_PROPERTY_NAME;
import static org.mule.runtime.api.component.Component.Annotations.SOURCE_ELEMENT_ANNOTATION_KEY;
import static org.mule.runtime.ast.api.ComponentAst.BODY_RAW_PARAM_NAME;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.SPRING_PROTOTYPE_OBJECT;
import static org.mule.runtime.config.internal.dsl.spring.PropertyComponentUtils.getPropertyValueFromPropertyComponent;
import static org.mule.runtime.config.internal.model.ApplicationModel.ANNOTATIONS_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTIES_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider.maskPasswords;
import static org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoader.resolveContextArtifactPluginClassLoaders;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ComponentAst;
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
import java.util.Map.Entry;
import java.util.ServiceConfigurationError;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Processor in the chain of responsibility that knows how to handle a generic {@code ComponentBuildingDefinition}.
 *
 * @since 4.0
 *        <p/>
 *        TODO MULE-9638 set visibility to package
 */
public class CommonBeanDefinitionCreator extends BeanDefinitionCreator {

  private final ObjectFactoryClassRepository objectFactoryClassRepository;
  private final BeanDefinitionPostProcessor beanDefinitionPostProcessor;

  public CommonBeanDefinitionCreator(ObjectFactoryClassRepository objectFactoryClassRepository) {
    this.objectFactoryClassRepository = objectFactoryClassRepository;

    this.beanDefinitionPostProcessor = resolvePostProcessor();
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
  public boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                               CreateBeanDefinitionRequest request) {
    ObjectTypeVisitor objectTypeVisitor = request.retrieveTypeVisitor();
    ComponentBuildingDefinition buildingDefinition = request.getComponentBuildingDefinition();
    request.getSpringComponentModel().setType(objectTypeVisitor.getType());
    BeanDefinitionBuilder beanDefinitionBuilder =
        createBeanDefinitionBuilder(objectTypeVisitor, request.getSpringComponentModel(), buildingDefinition);
    processAnnotations(request.getSpringComponentModel(), beanDefinitionBuilder);
    processComponentDefinitionModel(springComponentModels, request, buildingDefinition, beanDefinitionBuilder);
    return true;
  }

  private BeanDefinitionBuilder createBeanDefinitionBuilder(final ObjectTypeVisitor objectTypeVisitor,
                                                            SpringComponentModel componentModel,
                                                            ComponentBuildingDefinition buildingDefinition) {
    BeanDefinitionBuilder beanDefinitionBuilder;
    if (buildingDefinition.getObjectFactoryType() != null) {
      beanDefinitionBuilder = createBeanDefinitionBuilderFromObjectFactory(objectTypeVisitor, componentModel, buildingDefinition);
    } else {
      beanDefinitionBuilder = genericBeanDefinition(componentModel.getType());
    }
    return beanDefinitionBuilder;
  }

  private void processNestedAnnotations(ComponentAst componentModel, Map<QName, Object> previousAnnotations) {
    componentModel.directChildrenStream()
        .filter(cdm -> cdm.getIdentifier().equals(ANNOTATIONS_ELEMENT_IDENTIFIER))
        .findFirst()
        .ifPresent(annotationsCdm -> annotationsCdm.directChildrenStream()
            .forEach(annotationCdm -> previousAnnotations
                .put(new QName(annotationCdm.getIdentifier().getNamespaceUri(),
                               annotationCdm.getIdentifier().getName()),
                     annotationCdm.getRawParameterValue(BODY_RAW_PARAM_NAME).orElse(null))));
  }

  private void processAnnotations(SpringComponentModel componentModel, BeanDefinitionBuilder beanDefinitionBuilder) {
    if (Component.class.isAssignableFrom(componentModel.getType())
        // ValueResolver end up generating pojos from the extension whose class is enhanced to have annotations
        || ValueResolver.class.isAssignableFrom(componentModel.getType())) {
      Map<QName, Object> annotations =
          processMetadataAnnotationsHelper(beanDefinitionBuilder, componentModel.getComponent());
      processNestedAnnotations(componentModel.getComponent(), annotations);
      if (!annotations.isEmpty()) {
        beanDefinitionBuilder.addPropertyValue(ANNOTATIONS_PROPERTY_NAME, annotations);
      }
    }
  }

  private Map<QName, Object> processMetadataAnnotationsHelper(BeanDefinitionBuilder builder, ComponentAst componentModel) {
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
        .collect(toMap(e -> QName.valueOf(e.getKey()), Entry::getValue)));
  }

  private BeanDefinitionBuilder createBeanDefinitionBuilderFromObjectFactory(final ObjectTypeVisitor objectTypeVisitor,
                                                                             final SpringComponentModel componentModel,
                                                                             final ComponentBuildingDefinition componentBuildingDefinition) {
    componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
    Class<?> objectFactoryType = componentBuildingDefinition.getObjectFactoryType();

    return rootBeanDefinition(objectFactoryClassRepository
        .getObjectFactoryClass(componentBuildingDefinition, objectFactoryType, objectTypeVisitor.getType(),
                               new LazyValue<>(() -> componentModel.getBeanDefinition().isLazyInit())));
  }

  private void processComponentDefinitionModel(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                               final CreateBeanDefinitionRequest request,
                                               ComponentBuildingDefinition componentBuildingDefinition,
                                               final BeanDefinitionBuilder beanDefinitionBuilder) {
    final ComponentAst componentModel = request.getComponentModel();

    processObjectConstructionParameters(springComponentModels, componentModel, componentBuildingDefinition,
                                        new BeanDefinitionBuilderHelper(beanDefinitionBuilder));
    processMuleProperties(componentModel, beanDefinitionBuilder, beanDefinitionPostProcessor);
    if (componentBuildingDefinition.isPrototype()) {
      beanDefinitionBuilder.setScope(SPRING_PROTOTYPE_OBJECT);
    }
    AbstractBeanDefinition originalBeanDefinition = beanDefinitionBuilder.getBeanDefinition();
    AbstractBeanDefinition wrappedBeanDefinition = adaptBeanDefinition(originalBeanDefinition);
    if (originalBeanDefinition != wrappedBeanDefinition) {
      request.getSpringComponentModel().setType(wrappedBeanDefinition.getBeanClass());
    }
    request.getSpringComponentModel().setBeanDefinition(wrappedBeanDefinition);
  }

  static void processMuleProperties(ComponentAst componentModel, BeanDefinitionBuilder beanDefinitionBuilder,
                                    BeanDefinitionPostProcessor beanDefinitionPostProcessor) {
    // for now we skip custom-transformer since requires injection by the object factory.
    if (beanDefinitionPostProcessor != null && beanDefinitionPostProcessor.getGenericPropertiesCustomProcessingIdentifiers()
        .contains(componentModel.getIdentifier())) {
      return;
    }
    componentModel.directChildrenStream()
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

  private void processObjectConstructionParameters(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                   final ComponentAst componentModel,
                                                   final ComponentBuildingDefinition componentBuildingDefinition,
                                                   final BeanDefinitionBuilderHelper beanDefinitionBuilderHelper) {
    new ComponentConfigurationBuilder(springComponentModels, componentModel, componentBuildingDefinition,
                                      beanDefinitionBuilderHelper)
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
      return originalBeanDefinition;
    }
  }

  public static boolean areMatchingTypes(Class<?> superType, Class<?> childType) {
    return superType.isAssignableFrom(childType);
  }

}
