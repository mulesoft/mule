/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.lang.Math.max;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ERROR_HANDLER_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.SUBFLOW_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.ORIGINAL_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isErrorHandler;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isMessageSource;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isProcessor;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isRouter;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isTemplateOnErrorHandler;
import static org.mule.runtime.config.internal.model.ApplicationModel.HTTP_PROXY_OPERATION_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.HTTP_PROXY_POLICY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.HTTP_PROXY_SOURCE_POLICY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MODULE_OPERATION_CHAIN;
import static org.mule.runtime.config.internal.model.ApplicationModel.MUNIT_AFTER_SUITE_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MUNIT_AFTER_TEST_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MUNIT_BEFORE_SUITE_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MUNIT_BEFORE_TEST_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MUNIT_TEST_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.REDELIVERY_POLICY_IDENTIFIER;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

/**
 * Visitor that setups the {@link DefaultComponentLocation} for all mule components in the artifact configuration.
 *
 * @since 4.0
 */
// TODO MULE-13618 - Migrate ComponentLocationVisitor to use ExtensionModels
public class ComponentLocationVisitor implements Consumer<Pair<ComponentAst, List<ComponentAst>>> {

  public static final ComponentIdentifier BATCH_JOB_COMPONENT_IDENTIFIER = buildFromStringRepresentation("batch:job");
  public static final ComponentIdentifier BATCH_PROCESSS_RECORDS_COMPONENT_IDENTIFIER =
      buildFromStringRepresentation("batch:process-records");
  private static final ComponentIdentifier BATCH_ON_COMPLETE_IDENTIFIER =
      buildFromStringRepresentation("batch:on-complete");
  private static final ComponentIdentifier BATCH_STEP_COMPONENT_IDENTIFIER = buildFromStringRepresentation("batch:step");
  private static final ComponentIdentifier BATCH_AGGREGATOR_COMPONENT_IDENTIFIER =
      buildFromStringRepresentation("batch:aggregator");
  private static final String PROCESSORS_PART_NAME = "processors";
  private static final String SOURCE_PART_NAME = "source";
  private static final ComponentIdentifier ROUTE_COMPONENT_IDENTIFIER = buildFromStringRepresentation("mule:route");
  private static final ComponentIdentifier CHOICE_WHEN_COMPONENT_IDENTIFIER = buildFromStringRepresentation("mule:when");
  private static final ComponentIdentifier CHOICE_OTHERWISE_COMPONENT_IDENTIFIER =
      buildFromStringRepresentation("mule:otherwise");

  /**
   * For every {@link ComponentModel} in the configuration, sets the {@link DefaultComponentLocation} associated within an
   * annotation under the key {@link AbstractComponent#LOCATION_KEY}.
   *
   * @param componentModel the component model that will be assign it's {@link DefaultComponentLocation}.
   */
  @Override
  public void accept(Pair<ComponentAst, List<ComponentAst>> item) {
    accept(item.getFirst(), item.getSecond());
  }

  public void accept(ComponentAst componentModel, List<ComponentAst> hierarchy) {
    if (((ComponentModel) componentModel).getParent() == null) {
      // do not process root element
      return;
    }

    DefaultComponentLocation componentLocation;
    Optional<TypedComponentIdentifier> typedComponentIdentifier =
        of(builder().identifier(componentModel.getIdentifier()).type(componentModel.getComponentType())
            .build());

    if (((ComponentModel) componentModel).isRoot()) {
      String componentModelNameAttribute = componentModel.getName().orElse(null);
      ImmutableList<DefaultLocationPart> parts =
          ImmutableList.<DefaultLocationPart>builder()
              .add(new DefaultLocationPart(componentModelNameAttribute,
                                           typedComponentIdentifier,
                                           componentModel.getMetadata().getFileName(),
                                           componentModel.getMetadata().getStartLine(),
                                           componentModel.getMetadata().getStartColumn()))
              .build();
      componentLocation = new DefaultComponentLocation(ofNullable(componentModelNameAttribute), parts);
    } else if (existsWithinRootContainer(componentModel, hierarchy)) {
      ComponentAst parentComponentModel;
      if (componentModel.getIdentifier().equals(REDELIVERY_POLICY_IDENTIFIER)) {
        parentComponentModel = hierarchy.get(hierarchy.size() - 1)
            .directChildrenStream()
            .findFirst()
            .orElse(hierarchy.get(hierarchy.size() - 1));
      } else {
        parentComponentModel = hierarchy.get(hierarchy.size() - 1);
      }

      DefaultComponentLocation parentComponentLocation = (DefaultComponentLocation) parentComponentModel.getLocation();
      if (isModuleOperation(componentModel)) {
        // just point to the correct typed component operation identifier
        typedComponentIdentifier = getModuleOperationTypeComponentIdentifier(componentModel);
      }
      if (isHttpProxyPart(componentModel)) {
        componentLocation =
            parentComponentLocation.appendLocationPart(componentModel.getIdentifier().getName(), typedComponentIdentifier,
                                                       componentModel.getMetadata().getFileName(),
                                                       componentModel.getMetadata().getStartLine(),
                                                       componentModel.getMetadata().getStartColumn());
      } else if (isRootProcessorScope(parentComponentModel)) {
        componentLocation = processFlowDirectChild(componentModel, hierarchy, parentComponentLocation, typedComponentIdentifier);
      } else if (isMunitFlowIdentifier(parentComponentModel)) {
        componentLocation = parentComponentLocation.appendRoutePart()
            .appendLocationPart(findNonProcessorPath(componentModel, hierarchy), typedComponentIdentifier,
                                componentModel.getMetadata().getFileName(),
                                componentModel.getMetadata().getStartLine(),
                                componentModel.getMetadata().getStartColumn());
      } else if (isErrorHandler(componentModel)) {
        componentLocation = processErrorHandlerComponent(componentModel, parentComponentLocation, typedComponentIdentifier);
      } else if (isTemplateOnErrorHandler(componentModel)) {
        componentLocation = processOnErrorModel(componentModel, hierarchy, parentComponentLocation, typedComponentIdentifier);
      } else if (parentComponentIsRouter(componentModel, hierarchy)) {
        if (isRoute(componentModel)) {
          componentLocation = parentComponentLocation.appendRoutePart()
              .appendLocationPart(findRoutePath(componentModel, hierarchy), of(TypedComponentIdentifier.builder().type(SCOPE)
                  .identifier(ROUTE_COMPONENT_IDENTIFIER).build()),
                                  componentModel.getMetadata().getFileName(),
                                  componentModel.getMetadata().getStartLine(),
                                  componentModel.getMetadata().getStartColumn());
        } else if (isProcessor(componentModel)) {
          // this is the case of the routes directly inside the router as with scatter-gather
          componentLocation = parentComponentLocation
              .appendRoutePart()
              .appendLocationPart(findProcessorPath(componentModel, hierarchy), empty(), empty(), empty(), empty())
              .appendProcessorsPart()
              .appendLocationPart("0", typedComponentIdentifier,
                                  componentModel.getMetadata().getFileName(),
                                  componentModel.getMetadata().getStartLine(),
                                  componentModel.getMetadata().getStartColumn());
        } else {
          componentLocation =
              parentComponentLocation.appendLocationPart(findNonProcessorPath(componentModel, hierarchy),
                                                         typedComponentIdentifier,
                                                         componentModel.getMetadata().getFileName(),
                                                         componentModel.getMetadata().getStartLine(),
                                                         componentModel.getMetadata().getStartColumn());
        }
      } else if (isProcessor(componentModel)) {
        if (isModuleOperation(parentComponentModel)) {
          final Optional<TypedComponentIdentifier> operationTypedIdentifier =
              MODULE_OPERATION_CHAIN.equals(typedComponentIdentifier.get().getIdentifier())
                  ? getModuleOperationTypeComponentIdentifier(componentModel)
                  : typedComponentIdentifier;
          componentLocation = processModuleOperationChildren(componentModel, hierarchy, operationTypedIdentifier);
        } else {
          componentLocation = parentComponentLocation
              .appendProcessorsPart()
              .appendLocationPart(findProcessorPath(componentModel, hierarchy),
                                  typedComponentIdentifier,
                                  componentModel.getMetadata().getFileName(),
                                  componentModel.getMetadata().getStartLine(),
                                  componentModel.getMetadata().getStartColumn());
        }
      } else {
        if (isBatchAggregator(componentModel)) {
          componentLocation = parentComponentLocation
              .appendLocationPart(BATCH_AGGREGATOR_COMPONENT_IDENTIFIER.getName(),
                                  of(TypedComponentIdentifier.builder().type(UNKNOWN)
                                      .identifier(BATCH_AGGREGATOR_COMPONENT_IDENTIFIER).build()),
                                  componentModel.getMetadata().getFileName(),
                                  componentModel.getMetadata().getStartLine(),
                                  componentModel.getMetadata().getStartColumn());
        } else {
          componentLocation =
              parentComponentLocation.appendLocationPart(findNonProcessorPath(componentModel, hierarchy),
                                                         typedComponentIdentifier,
                                                         componentModel.getMetadata().getFileName(),
                                                         componentModel.getMetadata().getStartLine(),
                                                         componentModel.getMetadata().getStartColumn());
        }
      }
    } else {
      ComponentAst parentComponentModel = hierarchy.get(hierarchy.size() - 1);
      DefaultComponentLocation parentComponentLocation = (DefaultComponentLocation) parentComponentModel.getLocation();
      componentLocation =
          parentComponentLocation.appendLocationPart(findNonProcessorPath(componentModel, hierarchy), typedComponentIdentifier,
                                                     componentModel.getMetadata().getFileName(),
                                                     componentModel.getMetadata().getStartLine(),
                                                     componentModel.getMetadata().getStartColumn());
    }
    ((ComponentModel) componentModel).setComponentLocation(componentLocation);
  }

  private boolean isBatchAggregator(ComponentAst componentModel) {
    return BATCH_AGGREGATOR_COMPONENT_IDENTIFIER.equals(componentModel.getIdentifier());
  }

  private boolean isRoute(ComponentAst componentModel) {
    return componentModel.getIdentifier().equals(ROUTE_COMPONENT_IDENTIFIER)
        || componentModel.getIdentifier().equals(CHOICE_WHEN_COMPONENT_IDENTIFIER)
        || componentModel.getIdentifier().equals(CHOICE_OTHERWISE_COMPONENT_IDENTIFIER)
        || componentModel.getIdentifier().equals(BATCH_PROCESSS_RECORDS_COMPONENT_IDENTIFIER)
        || componentModel.getIdentifier().equals(BATCH_ON_COMPLETE_IDENTIFIER)
        || componentModel.getIdentifier().equals(BATCH_STEP_COMPONENT_IDENTIFIER)
        || componentModel.getComponentType().equals(ROUTE);
  }

  private boolean isHttpProxyPart(ComponentAst componentModel) {
    return componentModel.getIdentifier().equals(HTTP_PROXY_SOURCE_POLICY_IDENTIFIER)
        || componentModel.getIdentifier().equals(HTTP_PROXY_OPERATION_IDENTIFIER);
  }

  private boolean isMunitFlowIdentifier(ComponentAst componentModel) {
    return componentModel.getIdentifier().equals(MUNIT_TEST_IDENTIFIER);
  }

  private boolean isRootProcessorScope(ComponentAst componentModel) {
    ComponentIdentifier identifier = componentModel.getIdentifier();
    return identifier.equals(FLOW_IDENTIFIER)
        || identifier.equals(MUNIT_BEFORE_SUITE_IDENTIFIER)
        || identifier.equals(SUBFLOW_IDENTIFIER)
        || identifier.equals(MUNIT_BEFORE_TEST_IDENTIFIER)
        || identifier.equals(MUNIT_AFTER_SUITE_IDENTIFIER)
        || identifier.equals(MUNIT_AFTER_TEST_IDENTIFIER);
  }

  private boolean isModuleOperation(ComponentAst componentModel) {
    return componentModel.getIdentifier().equals(MODULE_OPERATION_CHAIN);
  }

  private boolean parentComponentIsRouter(ComponentAst componentModel, List<ComponentAst> hierarchy) {
    return existsWithinRouter(componentModel, hierarchy)
        && isRouter(hierarchy.get(hierarchy.size() - 1));
  }

  private boolean existsWithinRouter(ComponentAst componentModel, List<ComponentAst> hierarchy) {
    return hierarchy.stream().anyMatch(c -> isRouter(c));
  }

  private String findNonProcessorPath(ComponentAst componentModel, List<ComponentAst> hierarchy) {
    // we just lookup the position of the component model within the children
    return valueOf(max(0, hierarchy.get(hierarchy.size() - 1)
        .directChildrenStream()
        .filter(c -> !(c.getIdentifier().equals(REDELIVERY_POLICY_IDENTIFIER)))
        .collect(toList()).indexOf(componentModel)));
  }

  private String findRoutePath(ComponentAst componentModel, List<ComponentAst> hierarchy) {
    return String.valueOf(hierarchy.get(hierarchy.size() - 1)
        .directChildrenStream()
        .filter(this::isRoute)
        .collect(toList()).indexOf(componentModel));
  }

  private DefaultComponentLocation processOnErrorModel(ComponentAst componentModel, List<ComponentAst> hierarchy,
                                                       DefaultComponentLocation parentComponentLocation,
                                                       Optional<TypedComponentIdentifier> typedComponentIdentifier) {
    final int position = hierarchy.get(hierarchy.size() - 1)
        .directChildrenStream()
        .collect(toList()).indexOf(componentModel);

    return parentComponentLocation.appendLocationPart(String.valueOf(position), typedComponentIdentifier,
                                                      componentModel.getMetadata().getFileName(),
                                                      componentModel.getMetadata().getStartLine(),
                                                      componentModel.getMetadata().getStartColumn());
  }

  private DefaultComponentLocation processFlowDirectChild(ComponentAst componentModel, List<ComponentAst> hierarchy,
                                                          DefaultComponentLocation parentComponentLocation,
                                                          Optional<TypedComponentIdentifier> typedComponentIdentifier) {
    DefaultComponentLocation componentLocation;
    if (isMessageSource(componentModel)) {
      componentLocation =
          parentComponentLocation.appendLocationPart(SOURCE_PART_NAME, typedComponentIdentifier,
                                                     componentModel.getMetadata().getFileName(),
                                                     componentModel.getMetadata().getStartLine(),
                                                     componentModel.getMetadata().getStartColumn());
    } else if (isProcessor(componentModel)) {
      componentLocation = parentComponentLocation
          .appendLocationPart(PROCESSORS_PART_NAME, empty(), empty(), empty(), empty())
          .appendLocationPart(findProcessorPath(componentModel, hierarchy), typedComponentIdentifier,
                              componentModel.getMetadata().getFileName(),
                              componentModel.getMetadata().getStartLine(),
                              componentModel.getMetadata().getStartColumn());
    } else if (isErrorHandler(componentModel)) {
      componentLocation =
          processErrorHandlerComponent(componentModel, parentComponentLocation, typedComponentIdentifier);
    } else {
      componentLocation =
          parentComponentLocation.appendLocationPart(findNonProcessorPath(componentModel, hierarchy),
                                                     typedComponentIdentifier,
                                                     componentModel.getMetadata().getFileName(),
                                                     componentModel.getMetadata().getStartLine(),
                                                     componentModel.getMetadata().getStartColumn());
    }
    return componentLocation;
  }

  private Optional<TypedComponentIdentifier> getModuleOperationTypeComponentIdentifier(ComponentAst componentModel) {
    final ComponentIdentifier originalIdentifier =
        (ComponentIdentifier) componentModel.getMetadata().getParserAttributes().get(ORIGINAL_IDENTIFIER);

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
   * @param componentModel source to generate the new {@link ComponentLocation}, it also relies on the provided {@code hierarchy}
   * @param hierarchy the ancestors of {@code componentModel}
   * @param operationTypedIdentifier identifier of the current operation
   * @return a fictitious {@link ComponentLocation}
   */
  private DefaultComponentLocation processModuleOperationChildren(ComponentAst componentModel, List<ComponentAst> hierarchy,
                                                                  Optional<TypedComponentIdentifier> operationTypedIdentifier) {
    final Optional<TypedComponentIdentifier> parentOperationTypedIdentifier =
        getModuleOperationTypeComponentIdentifier(hierarchy.get(hierarchy.size() - 1));
    final String operationName = parentOperationTypedIdentifier.get().getIdentifier().getName();
    return new DefaultComponentLocation(of(operationName), emptyList())
        .appendLocationPart(operationName, parentOperationTypedIdentifier,
                            componentModel.getMetadata().getFileName(),
                            componentModel.getMetadata().getStartLine(),
                            componentModel.getMetadata().getStartColumn())
        .appendLocationPart(PROCESSORS_PART_NAME, empty(), empty(), empty(), empty())
        .appendLocationPart(findProcessorPath(componentModel, hierarchy), operationTypedIdentifier,
                            componentModel.getMetadata().getFileName(),
                            componentModel.getMetadata().getStartLine(),
                            componentModel.getMetadata().getStartColumn());
  }

  private DefaultComponentLocation processErrorHandlerComponent(ComponentAst componentModel,
                                                                DefaultComponentLocation parentComponentLocation,
                                                                Optional<TypedComponentIdentifier> typedComponentIdentifier) {
    return parentComponentLocation.appendLocationPart("errorHandler", typedComponentIdentifier,
                                                      componentModel.getMetadata().getFileName(),
                                                      componentModel.getMetadata().getStartLine(),
                                                      componentModel.getMetadata().getStartColumn());
  }

  private String findProcessorPath(ComponentAst componentModel, List<ComponentAst> hierarchy) {
    return String.valueOf(hierarchy.get(hierarchy.size() - 1)
        .directChildrenStream()
        .filter(ComponentModelHelper::isProcessor)
        .collect(toList()).indexOf(componentModel));
  }

  private boolean existsWithinRootContainer(ComponentAst componentModel, List<ComponentAst> hierarchy) {
    return existsWithin(hierarchy, FLOW_IDENTIFIER)
        || existsWithin(hierarchy, MUNIT_TEST_IDENTIFIER)
        || existsWithin(hierarchy, MUNIT_BEFORE_SUITE_IDENTIFIER)
        || existsWithin(hierarchy, MUNIT_BEFORE_TEST_IDENTIFIER)
        || existsWithin(hierarchy, MUNIT_AFTER_SUITE_IDENTIFIER)
        || existsWithin(hierarchy, MUNIT_AFTER_TEST_IDENTIFIER)
        || existsWithin(hierarchy, HTTP_PROXY_POLICY_IDENTIFIER)
        || existsWithinRootErrorHandler(componentModel, hierarchy)
        || existsWithinSubflow(hierarchy);
  }

  private boolean existsWithinRootErrorHandler(ComponentAst componentAst, List<ComponentAst> hierarchy) {
    return !hierarchy.isEmpty()
        && hierarchy.get(0).getIdentifier().equals(ERROR_HANDLER_IDENTIFIER);
  }

  private boolean existsWithinSubflow(List<ComponentAst> hierarchy) {
    return existsWithin(hierarchy, SUBFLOW_IDENTIFIER);
  }

  private boolean existsWithin(List<ComponentAst> hierarchy, ComponentIdentifier componentIdentifier) {
    return hierarchy.stream().anyMatch(componentModel -> componentModel.getIdentifier().equals(componentIdentifier));
  }
}
