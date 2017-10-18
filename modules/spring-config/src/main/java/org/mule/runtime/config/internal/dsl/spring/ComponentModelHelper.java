/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.component.Component.ANNOTATIONS_PROPERTY_NAME;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ON_ERROR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.config.api.dsl.model.ApplicationModel.FLOW_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.model.ApplicationModel.MODULE_OPERATION_CHAIN;
import static org.mule.runtime.config.api.dsl.model.ApplicationModel.ON_ERROR_CONTINE_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.model.ApplicationModel.ON_ERROR_PROPAGATE_IDENTIFIER;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.meta.model.ComponentModelVisitor;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModelVisitor;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.config.api.dsl.model.ComponentModel;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.internal.dsl.model.ComponentLocationVisitor;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.exception.TemplateOnErrorHandler;
import org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder;
import org.mule.runtime.core.internal.routing.AbstractSelectiveRouter;
import org.mule.runtime.core.privileged.processor.Router;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;

public class ComponentModelHelper {

  /**
   * Resolves the {@link org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType} from a {@link ComponentModel}.
   *
   * @param componentModel a {@link ComponentModel} that represents a component in the configuration.
   * @param extensionModelHelper helper to access components in extension model
   * @return the componentModel type.
   */
  public static TypedComponentIdentifier.ComponentType resolveComponentType(ComponentModel componentModel,
                                                                            ExtensionModelHelper extensionModelHelper) {
    if (componentModel.getIdentifier().equals(MODULE_OPERATION_CHAIN)) {
      return OPERATION;
    }
    Optional<DslElementModel<Object>> elementModelOptional = extensionModelHelper.findDslElementModel(componentModel);
    if (componentModel.getIdentifier().equals(ON_ERROR_CONTINE_IDENTIFIER)
        || componentModel.getIdentifier().equals(ON_ERROR_PROPAGATE_IDENTIFIER)) {
      return ON_ERROR;
    }
    return elementModelOptional.map(elementModel -> {
      Object model = elementModel.getModel();
      Reference<TypedComponentIdentifier.ComponentType> typeReference = new Reference<>(UNKNOWN);
      if (model instanceof org.mule.runtime.api.meta.model.ComponentModel) {
        ((org.mule.runtime.api.meta.model.ComponentModel) model).accept(new ComponentModelVisitor() {

          @Override
          public void visit(OperationModel model) {
            typeReference.set(OPERATION);
          }

          @Override
          public void visit(SourceModel model) {
            typeReference.set(SOURCE);
          }

          @Override
          public void visit(ConstructModel model) {
            StereotypeModel stereotype = model.getStereotype();
            if (stereotype.equals(MuleStereotypes.ERROR_HANDLER)) {
              typeReference.set(TypedComponentIdentifier.ComponentType.ERROR_HANDLER);
              return;
            } else if (stereotype.equals(MuleStereotypes.FLOW)) {
              typeReference.set(TypedComponentIdentifier.ComponentType.FLOW);
              return;
            } else if (stereotype.equals(SOURCE)) {
              typeReference.set(TypedComponentIdentifier.ComponentType.SOURCE);
              return;
            }
            model.getNestedComponents()
                .forEach(nestedElementModel -> nestedElementModel.accept(new NestedComponentVisitor(typeReference)));

            if (typeReference.get() == null && stereotype.equals(MuleStereotypes.PROCESSOR)) {
              typeReference.set(OPERATION);
            }
          }
        });
      }
      return typeReference.get();
    }).orElse(UNKNOWN);
  }

  static class NestedComponentVisitor implements NestableElementModelVisitor {

    private Reference<TypedComponentIdentifier.ComponentType> reference;

    public NestedComponentVisitor(Reference<TypedComponentIdentifier.ComponentType> reference) {
      this.reference = reference;
    }

    @Override
    public void visit(NestedComponentModel component) {

    }

    @Override
    public void visit(NestedChainModel component) {
      reference.set(SCOPE);
    }

    @Override
    public void visit(NestedRouteModel component) {
      reference.set(ROUTER);
    }
  }


  public static boolean isAnnotatedObject(ComponentModel componentModel) {
    return isOfType(componentModel, Component.class);
  }

  public static boolean isProcessor(ComponentModel componentModel) {
    return isOfType(componentModel, Processor.class) || isOfType(componentModel, InterceptingMessageProcessor.class);
  }

  public static boolean isMessageSource(ComponentModel componentModel) {
    return isOfType(componentModel, MessageSource.class);
  }

  public static boolean isErrorHandler(ComponentModel componentModel) {
    return isOfType(componentModel, ErrorHandler.class);
  }

  public static boolean isTemplateOnErrorHandler(ComponentModel componentModel) {
    return isOfType(componentModel, TemplateOnErrorHandler.class);
  }

  public static boolean isFlow(ComponentModel componentModel) {
    return componentModel.getIdentifier().equals(FLOW_IDENTIFIER);
  }

  private static boolean isOfType(ComponentModel componentModel, Class type) {
    Class<?> componentModelType = componentModel.getType();
    if (componentModelType == null) {
      return false;
    }
    return CommonBeanDefinitionCreator.areMatchingTypes(type, componentModelType);
  }

  public static void addAnnotation(QName annotationKey, Object annotationValue, SpringComponentModel componentModel) {
    // TODO MULE-10970 - remove condition once everything is AnnotatedObject.
    if (!ComponentModelHelper.isAnnotatedObject(componentModel) && !componentModel.getIdentifier().getName().equals("flow-ref")) {
      return;
    }
    BeanDefinition beanDefinition = componentModel.getBeanDefinition();
    if (beanDefinition == null) {
      // This is the case of components that are references
      return;
    }
    updateAnnotationValue(annotationKey, annotationValue, beanDefinition);
  }

  public static void updateAnnotationValue(QName annotationKey, Object annotationValue, BeanDefinition beanDefinition) {
    PropertyValue propertyValue =
        beanDefinition.getPropertyValues().getPropertyValue(ANNOTATIONS_PROPERTY_NAME);
    Map<QName, Object> annotations;
    if (propertyValue == null) {
      annotations = new HashMap<>();
      propertyValue = new PropertyValue(ANNOTATIONS_PROPERTY_NAME, annotations);
      beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
    } else {
      annotations = (Map<QName, Object>) propertyValue.getValue();
    }
    annotations.put(annotationKey, annotationValue);
  }

  public static <T> Optional<T> getAnnotation(QName annotationKey, SpringComponentModel componentModel) {
    if (componentModel.getBeanDefinition() == null) {
      return empty();
    }
    PropertyValue propertyValue =
        componentModel.getBeanDefinition().getPropertyValues().getPropertyValue(ANNOTATIONS_PROPERTY_NAME);
    Map<QName, Object> annotations;
    if (propertyValue == null) {
      return empty();
    } else {
      annotations = (Map<QName, Object>) propertyValue.getValue();
      return ofNullable((T) annotations.get(annotationKey));
    }
  }

  public static boolean isRouter(ComponentModel componentModel) {
    return isOfType(componentModel, Router.class) || isOfType(componentModel, AbstractSelectiveRouter.class)
        || ComponentLocationVisitor.BATCH_JOB_COMPONENT_IDENTIFIER.equals(componentModel.getIdentifier())
        || ComponentLocationVisitor.BATCH_PROCESSS_RECORDS_COMPONENT_IDENTIFIER.equals(componentModel.getIdentifier());
  }
}
