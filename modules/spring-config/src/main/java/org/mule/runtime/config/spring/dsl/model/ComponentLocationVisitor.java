/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.FLOW_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SUBFLOW_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.spring.ComponentModelHelper.isErrorHandler;
import static org.mule.runtime.config.spring.dsl.spring.ComponentModelHelper.isMessageSource;
import static org.mule.runtime.config.spring.dsl.spring.ComponentModelHelper.isProcessor;
import static org.mule.runtime.config.spring.dsl.spring.ComponentModelHelper.isTemplateOnErrorHandler;
import static org.mule.runtime.config.spring.dsl.spring.ComponentModelHelper.resolveComponentType;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.config.spring.dsl.spring.ComponentModelHelper;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Visitor that setups the {@link DefaultComponentLocation} for all mule components in the artifact configuration.
 *
 * @since 4.0
 */
public class ComponentLocationVisitor implements Consumer<ComponentModel> {

  /**
   * For every {@link ComponentModel} in the configuration, sets the {@link DefaultComponentLocation} associated within an
   * annotation under the key {@link org.mule.runtime.api.meta.AbstractAnnotatedObject#LOCATION_KEY}.
   *
   * @param componentModel the component model that will be assign it's {@link DefaultComponentLocation}.
   */
  @Override
  public void accept(ComponentModel componentModel) {
    if (componentModel.getParent() == null) {
      // do not process root element
      return;
    }
    DefaultComponentLocation componentLocation;
    Optional<TypedComponentIdentifier> typedComponentIdentifier =
        of(builder().withIdentifier(componentModel.getIdentifier()).withType(resolveComponentType(componentModel)).build());
    if (componentModel.isRoot()) {
      String componentModelNameAttribute = componentModel.getNameAttribute();
      ImmutableList<DefaultComponentLocation.DefaultLocationPart> parts =
          ImmutableList.<DefaultComponentLocation.DefaultLocationPart>builder()
              .add(new DefaultComponentLocation.DefaultLocationPart(componentModelNameAttribute,
                                                                    typedComponentIdentifier,
                                                                    componentModel.getConfigFileName(),
                                                                    componentModel.getLineNumber()))
              .build();
      componentLocation = new DefaultComponentLocation(ofNullable(componentModelNameAttribute), parts);
    } else if (existsWithinFlow(componentModel)) {
      ComponentModel parentComponentModel = componentModel.getParent();
      DefaultComponentLocation parentComponentLocation = parentComponentModel.getComponentLocation();
      if (parentComponentModel.getIdentifier().equals(FLOW_IDENTIFIER)) {
        componentLocation = processFlowDirectChild(componentModel, parentComponentLocation, typedComponentIdentifier);
      } else if (isErrorHandler(componentModel)) {
        componentLocation = processErrorHandlerComponent(componentModel, parentComponentLocation, typedComponentIdentifier);
      } else if (isTemplateOnErrorHandler(componentModel)) {
        componentLocation = processOnErrorModel(componentModel, parentComponentLocation, typedComponentIdentifier);
      } else if (isProcessor(componentModel)) {
        componentLocation =
            parentComponentLocation.appendLocationPart(findProcessorPath(componentModel), typedComponentIdentifier,
                                                       componentModel.getConfigFileName(), componentModel.getLineNumber());
      } else {
        componentLocation =
            parentComponentLocation.appendLocationPart(findNonProcessorPath(componentModel), typedComponentIdentifier,
                                                       componentModel.getConfigFileName(), componentModel.getLineNumber());
      }
    } else if (existsWithinSubflow(componentModel)) {
      DefaultComponentLocation parentComponentLocation = componentModel.getParent().getComponentLocation();
      componentLocation =
          parentComponentLocation.appendLocationPart(findProcessorPath(componentModel), typedComponentIdentifier,
                                                     componentModel.getConfigFileName(), componentModel.getLineNumber());
    } else {
      DefaultComponentLocation parentComponentLocation = componentModel.getParent().getComponentLocation();
      componentLocation =
          parentComponentLocation.appendLocationPart(findNonProcessorPath(componentModel), typedComponentIdentifier,
                                                     componentModel.getConfigFileName(), componentModel.getLineNumber());
    }
    componentModel.setComponentLocation(componentLocation);
  }

  private String findNonProcessorPath(ComponentModel componentModel) {
    // we just lookup the position of the component model within the children
    int i = 0;
    for (ComponentModel child : componentModel.getParent().getInnerComponents()) {
      if (child == componentModel) {
        break;
      }
      i++;
    }
    return String.valueOf(i);
  }


  private DefaultComponentLocation processOnErrorModel(ComponentModel componentModel,
                                                       DefaultComponentLocation parentComponentLocation,
                                                       Optional<TypedComponentIdentifier> typedComponentIdentifier) {
    ComponentModel parentComponentModel = componentModel.getParent();
    int i = 0;
    for (ComponentModel childComponent : parentComponentModel.getInnerComponents()) {
      if (childComponent == componentModel) {
        break;
      }
      i++;
    }
    return parentComponentLocation.appendLocationPart(String.valueOf(i), typedComponentIdentifier,
                                                      componentModel.getConfigFileName(), componentModel.getLineNumber());
  }

  private DefaultComponentLocation processFlowDirectChild(ComponentModel componentModel,
                                                          DefaultComponentLocation parentComponentLocation,
                                                          Optional<TypedComponentIdentifier> typedComponentIdentifier) {
    DefaultComponentLocation componentLocation;
    if (isMessageSource(componentModel)) {
      componentLocation =
          parentComponentLocation.appendLocationPart("source", typedComponentIdentifier, componentModel.getConfigFileName(),
                                                     componentModel.getLineNumber());
    } else if (isProcessor(componentModel)) {
      componentLocation =
          parentComponentLocation.appendLocationPart("processors", empty(), empty(), empty())
              .appendLocationPart(findProcessorPath(componentModel),
                                  typedComponentIdentifier,
                                  componentModel.getConfigFileName(), componentModel.getLineNumber());
    } else if (isErrorHandler(componentModel)) {
      componentLocation = processErrorHandlerComponent(componentModel, parentComponentLocation, typedComponentIdentifier);
    } else {
      componentLocation =
          parentComponentLocation.appendLocationPart(findNonProcessorPath(componentModel), typedComponentIdentifier,
                                                     componentModel.getConfigFileName(), componentModel.getLineNumber());
    }
    return componentLocation;
  }

  private DefaultComponentLocation processErrorHandlerComponent(ComponentModel componentModel,
                                                                DefaultComponentLocation parentComponentLocation,
                                                                Optional<TypedComponentIdentifier> typedComponentIdentifier) {
    DefaultComponentLocation componentLocation;
    componentLocation =
        parentComponentLocation.appendLocationPart("errorHandler", typedComponentIdentifier,
                                                   componentModel.getConfigFileName(), componentModel.getLineNumber());
    return componentLocation;
  }

  private String findProcessorPath(ComponentModel componentModel) {
    ComponentModel parentComponentModel = componentModel.getParent();
    List<ComponentModel> processorModels =
        parentComponentModel.getInnerComponents().stream().filter(ComponentModelHelper::isProcessor).collect(Collectors.toList());
    int i = 0;
    for (ComponentModel processorModel : processorModels) {
      if (processorModel == componentModel) {
        break;
      }
      i++;
    }
    return String.valueOf(i);
  }

  private boolean existsWithinFlow(ComponentModel componentModel) {
    return existsWithin(componentModel, FLOW_IDENTIFIER);
  }

  private boolean existsWithinSubflow(ComponentModel componentModel) {
    return existsWithin(componentModel, SUBFLOW_IDENTIFIER);
  }

  private boolean existsWithin(ComponentModel componentModel, ComponentIdentifier componentIdentifier) {
    while (componentModel.getParent() != null) {
      if (componentModel.getParent().getIdentifier().equals(componentIdentifier)) {
        return true;
      }
      componentModel = componentModel.getParent();
    }
    return false;
  }
}
