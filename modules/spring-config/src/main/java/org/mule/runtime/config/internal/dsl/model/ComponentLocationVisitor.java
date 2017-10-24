/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ERROR_HANDLER_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.SUBFLOW_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.ORIGINAL_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isErrorHandler;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isMessageSource;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isProcessor;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isRouter;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isTemplateOnErrorHandler;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.resolveComponentType;
import static org.mule.runtime.config.internal.model.ApplicationModel.HTTP_PROXY_OPERATION_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.HTTP_PROXY_POLICY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.HTTP_PROXY_SOURCE_POLICY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MODULE_OPERATION_CHAIN;
import static org.mule.runtime.config.internal.model.ApplicationModel.MUNIT_AFTER_SUITE_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MUNIT_AFTER_TEST_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MUNIT_BEFORE_SUITE_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MUNIT_BEFORE_TEST_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MUNIT_TEST_IDENTIFIER;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;

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
// TODO MULE-13618 - Migrate ComponentLocationVisitor to use ExtensionModels
public class ComponentLocationVisitor implements Consumer<ComponentModel> {

  public static final ComponentIdentifier BATCH_JOB_COMPONENT_IDENTIFIER = buildFromStringRepresentation("batch:job");
  public static final ComponentIdentifier BATCH_PROCESSS_RECORDS_COMPONENT_IDENTIFIER =
      buildFromStringRepresentation("batch:process-records");
  private static final ComponentIdentifier BATCH_ON_COMPLETE_IDENTIFIER =
      buildFromStringRepresentation("batch:on-complete");
  private static final ComponentIdentifier BATCH_STEP_COMPONENT_IDENTIFIER = buildFromStringRepresentation("batch:step");
  private static final ComponentIdentifier BATCH_AGGREGATOR_COMPONENT_IDENTIFIER =
      buildFromStringRepresentation("batch:aggregator");
  private static final String PROCESSORS_PART_NAME = "processors";
  private static final ComponentIdentifier ROUTE_COMPONENT_IDENTIFIER = buildFromStringRepresentation("mule:route");
  private static final ComponentIdentifier CHOICE_WHEN_COMPONENT_IDENTIFIER = buildFromStringRepresentation("mule:when");
  private static final ComponentIdentifier CHOICE_OTHERWISE_COMPONENT_IDENTIFIER =
      buildFromStringRepresentation("mule:otherwise");
  private final ExtensionModelHelper extensionModelHelper;

  public ComponentLocationVisitor(ExtensionModelHelper extensionModelHelper) {
    this.extensionModelHelper = extensionModelHelper;
  }

  /**
   * For every {@link ComponentModel} in the configuration, sets the {@link DefaultComponentLocation} associated within an
   * annotation under the key {@link AbstractComponent#LOCATION_KEY}.
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
        of(builder().identifier(componentModel.getIdentifier()).type(resolveComponentType(componentModel, extensionModelHelper))
            .build());
    if (componentModel.isRoot()) {
      String componentModelNameAttribute = componentModel.getNameAttribute();
      ImmutableList<DefaultLocationPart> parts =
          ImmutableList.<DefaultLocationPart>builder()
              .add(new DefaultLocationPart(componentModelNameAttribute,
                                           typedComponentIdentifier,
                                           componentModel.getConfigFileName(),
                                           componentModel.getLineNumber()))
              .build();
      componentLocation = new DefaultComponentLocation(ofNullable(componentModelNameAttribute), parts);
    } else if (existsWithinRootContainer(componentModel)) {
      ComponentModel parentComponentModel = componentModel.getParent();
      DefaultComponentLocation parentComponentLocation = parentComponentModel.getComponentLocation();
      if (isHttpProxyPart(componentModel)) {
        componentLocation =
            parentComponentLocation.appendLocationPart(componentModel.getIdentifier().getName(), typedComponentIdentifier,
                                                       componentModel.getConfigFileName(),
                                                       componentModel.getLineNumber());
      } else if (isRootProcessorScope(parentComponentModel)) {
        componentLocation = processFlowDirectChild(componentModel, parentComponentLocation, typedComponentIdentifier);
      } else if (isMunitFlowIdentifier(parentComponentModel)) {
        componentLocation = parentComponentLocation.appendRoutePart()
            .appendLocationPart(findNonProcessorPath(componentModel), typedComponentIdentifier,
                                componentModel.getConfigFileName(),
                                componentModel.getLineNumber());
      } else if (isErrorHandler(componentModel)) {
        componentLocation = processErrorHandlerComponent(componentModel, parentComponentLocation, typedComponentIdentifier);
      } else if (isTemplateOnErrorHandler(componentModel)) {
        componentLocation = processOnErrorModel(componentModel, parentComponentLocation, typedComponentIdentifier);
      } else if (parentComponentIsRouter(componentModel)) {
        if (isRoute(componentModel)) {
          componentLocation = parentComponentLocation.appendRoutePart()
              .appendLocationPart(findNonProcessorPath(componentModel), of(TypedComponentIdentifier.builder().type(SCOPE)
                  .identifier(ROUTE_COMPONENT_IDENTIFIER).build()), componentModel.getConfigFileName(),
                                  componentModel.getLineNumber());
        } else if (isProcessor(componentModel)) {
          // this is the case of the routes directly inside the router as with scatter-gather
          componentLocation = parentComponentLocation
              .appendRoutePart()
              .appendLocationPart(findProcessorPath(componentModel), empty(), empty(), empty())
              .appendProcessorsPart()
              .appendLocationPart("0", typedComponentIdentifier, componentModel.getConfigFileName(),
                                  componentModel.getLineNumber());
        } else {
          // this is the case of the when element inside the choice
          componentLocation = parentComponentLocation.appendRoutePart()
              .appendLocationPart(findNonProcessorPath(componentModel), of(TypedComponentIdentifier.builder().type(UNKNOWN)
                  .identifier(ROUTE_COMPONENT_IDENTIFIER).build()), empty(), empty());
        }
      } else if (isProcessor(componentModel)) {
        if (isModuleOperation(componentModel.getParent())) {
          final Optional<TypedComponentIdentifier> operationTypedIdentifier =
              ApplicationModel.MODULE_OPERATION_CHAIN.equals(typedComponentIdentifier.get().getIdentifier())
                  ? getModuleOperationTypeComponentIdentifier(componentModel) : typedComponentIdentifier;
          componentLocation = processModuleOperationChildren(componentModel, operationTypedIdentifier);
        } else {
          componentLocation = parentComponentLocation.appendProcessorsPart().appendLocationPart(findProcessorPath(componentModel),
                                                                                                typedComponentIdentifier,
                                                                                                componentModel
                                                                                                    .getConfigFileName(),
                                                                                                componentModel.getLineNumber());
        }
      } else {
        if (isBatchAggregator(componentModel)) {
          componentLocation = parentComponentLocation
              .appendLocationPart(BATCH_AGGREGATOR_COMPONENT_IDENTIFIER.getName(),
                                  of(TypedComponentIdentifier.builder().type(UNKNOWN)
                                      .identifier(BATCH_AGGREGATOR_COMPONENT_IDENTIFIER).build()),
                                  componentModel.getConfigFileName(),
                                  componentModel.getLineNumber());
        } else {
          componentLocation =
              parentComponentLocation.appendLocationPart(findNonProcessorPath(componentModel), typedComponentIdentifier,
                                                         componentModel.getConfigFileName(), componentModel.getLineNumber());
        }
      }
    } else {
      DefaultComponentLocation parentComponentLocation = componentModel.getParent().getComponentLocation();
      componentLocation =
          parentComponentLocation.appendLocationPart(findNonProcessorPath(componentModel), typedComponentIdentifier,
                                                     componentModel.getConfigFileName(), componentModel.getLineNumber());
    }
    componentModel.setComponentLocation(componentLocation);
  }

  private boolean isBatchAggregator(ComponentModel componentModel) {
    return BATCH_AGGREGATOR_COMPONENT_IDENTIFIER.equals(componentModel.getIdentifier());
  }

  private boolean isRoute(ComponentModel componentModel) {
    return componentModel.getIdentifier().equals(ROUTE_COMPONENT_IDENTIFIER)
        || componentModel.getIdentifier().equals(CHOICE_WHEN_COMPONENT_IDENTIFIER)
        || componentModel.getIdentifier().equals(CHOICE_OTHERWISE_COMPONENT_IDENTIFIER)
        || componentModel.getIdentifier().equals(BATCH_PROCESSS_RECORDS_COMPONENT_IDENTIFIER)
        || componentModel.getIdentifier().equals(BATCH_ON_COMPLETE_IDENTIFIER)
        || componentModel.getIdentifier().equals(BATCH_STEP_COMPONENT_IDENTIFIER);
  }

  private boolean isHttpProxyPart(ComponentModel componentModel) {
    return componentModel.getIdentifier().equals(HTTP_PROXY_SOURCE_POLICY_IDENTIFIER)
        || componentModel.getIdentifier().equals(HTTP_PROXY_OPERATION_IDENTIFIER);
  }

  private boolean isMunitFlowIdentifier(ComponentModel componentModel) {
    return componentModel.getIdentifier().equals(MUNIT_TEST_IDENTIFIER);
  }

  private boolean isRootProcessorScope(ComponentModel componentModel) {
    ComponentIdentifier identifier = componentModel.getIdentifier();
    return identifier.equals(FLOW_IDENTIFIER) || identifier.equals(MUNIT_BEFORE_SUITE_IDENTIFIER) ||
        identifier.equals(MUNIT_BEFORE_TEST_IDENTIFIER) || identifier.equals(MUNIT_AFTER_SUITE_IDENTIFIER) ||
        identifier.equals(MUNIT_AFTER_TEST_IDENTIFIER);
  }

  private boolean isModuleOperation(ComponentModel componentModel) {
    return componentModel.getIdentifier().equals(MODULE_OPERATION_CHAIN);
  }

  private boolean parentComponentIsRouter(ComponentModel componentModel) {
    return existsWithinRouter(componentModel) && isRouter(componentModel.getParent());
  }

  private boolean existsWithinRouter(ComponentModel componentModel) {
    while (componentModel.getParent() != null) {
      if (isRouter(componentModel)) {
        return true;
      }
      componentModel = componentModel.getParent();
    }
    return false;
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
      if (isModuleOperation(componentModel)) {
        // just point to the correct typed component operation identifier
        typedComponentIdentifier = getModuleOperationTypeComponentIdentifier(componentModel);
      }
      componentLocation = parentComponentLocation
          .appendLocationPart(PROCESSORS_PART_NAME, empty(), empty(), empty())
          .appendLocationPart(findProcessorPath(componentModel), typedComponentIdentifier, componentModel.getConfigFileName(),
                              componentModel.getLineNumber());
    } else if (isErrorHandler(componentModel)) {
      componentLocation = processErrorHandlerComponent(componentModel, parentComponentLocation, typedComponentIdentifier);
    } else {
      componentLocation =
          parentComponentLocation.appendLocationPart(findNonProcessorPath(componentModel), typedComponentIdentifier,
                                                     componentModel.getConfigFileName(), componentModel.getLineNumber());
    }
    return componentLocation;
  }

  private Optional<TypedComponentIdentifier> getModuleOperationTypeComponentIdentifier(ComponentModel componentModel) {
    final ComponentIdentifier originalIdentifier =
        (ComponentIdentifier) componentModel.getCustomAttributes().get(ORIGINAL_IDENTIFIER);

    final String namespace = originalIdentifier.getNamespace();
    final String operationName = originalIdentifier.getName();

    final ComponentIdentifier operationIdentifier =
        ComponentIdentifier.builder().namespace(namespace).name(operationName).build();
    return of(builder().identifier(operationIdentifier).type(OPERATION).build());
  }

  /**
   * It rewrites the history for those macro expanded operations that are not direct children from a flow, which means the
   * returned {@link ComponentLocation} are mapped to the new operation rather the original flow.
   *
   * @param componentModel source to generate the new {@link ComponentLocation}, it also relies in its parent
   *        {@link ComponentModel#getParent()}
   * @param operationTypedIdentifier identifier of the current operation
   * @return a fictitious {@link ComponentLocation}
   */
  private DefaultComponentLocation processModuleOperationChildren(ComponentModel componentModel,
                                                                  Optional<TypedComponentIdentifier> operationTypedIdentifier) {
    final Optional<TypedComponentIdentifier> parentOperationTypedIdentifier =
        getModuleOperationTypeComponentIdentifier(componentModel.getParent());
    final String operationName = parentOperationTypedIdentifier.get().getIdentifier().getName();
    return new DefaultComponentLocation(of(operationName), emptyList())
        .appendLocationPart(operationName, parentOperationTypedIdentifier, componentModel.getConfigFileName(),
                            componentModel.getLineNumber())
        .appendLocationPart(PROCESSORS_PART_NAME, empty(), empty(), empty())
        .appendLocationPart(findProcessorPath(componentModel), operationTypedIdentifier, componentModel.getConfigFileName(),
                            componentModel.getLineNumber());
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

  private boolean existsWithinRootContainer(ComponentModel componentModel) {
    return existsWithin(componentModel, FLOW_IDENTIFIER)
        || existsWithin(componentModel, MUNIT_TEST_IDENTIFIER)
        || existsWithin(componentModel, MUNIT_BEFORE_SUITE_IDENTIFIER)
        || existsWithin(componentModel, MUNIT_BEFORE_TEST_IDENTIFIER)
        || existsWithin(componentModel, MUNIT_AFTER_SUITE_IDENTIFIER)
        || existsWithin(componentModel, MUNIT_AFTER_TEST_IDENTIFIER)
        || existsWithin(componentModel, HTTP_PROXY_POLICY_IDENTIFIER)
        || existsWithinRootErrorHandler(componentModel)
        || existsWithinSubflow(componentModel);
  }

  private boolean existsWithinRootErrorHandler(ComponentModel componentModel) {
    while (componentModel.getParent() != null) {
      if (componentModel.getParent().getIdentifier().equals(ERROR_HANDLER_IDENTIFIER) && componentModel.getParent().isRoot()) {
        return true;
      }
      componentModel = componentModel.getParent();
    }
    return false;
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
