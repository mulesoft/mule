/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.lang.Math.max;
import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.CHAIN;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTE;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isErrorHandler;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isMessageSource;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isProcessor;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isRouter;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.isTemplateOnErrorHandler;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;
import org.mule.runtime.module.extension.privileged.loader.java.property.CustomLocationPartModelProperty;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

/**
 * Visitor that setups the {@link DefaultComponentLocation} for all mule components in the artifact configuration.
 *
 * @since 4.0
 */
public class ComponentLocationVisitor implements Consumer<Pair<ComponentAst, List<ComponentAst>>> {

  private static final String PROCESSORS_PART_NAME = "processors";
  private static final String SOURCE_PART_NAME = "source";

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
    if (componentModel.getLocation() != null) {
      return;
    }

    DefaultComponentLocation componentLocation;
    Optional<TypedComponentIdentifier> typedComponentIdentifier =
        of(builder().identifier(componentModel.getIdentifier()).type(componentModel.getComponentType())
            .build());

    if (hierarchy.isEmpty()) {
      String componentModelNameAttribute = componentModel.getComponentId().orElse(null);
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
      ComponentAst parentComponentModel = hierarchy.get(hierarchy.size() - 1);

      DefaultComponentLocation parentComponentLocation = (DefaultComponentLocation) parentComponentModel.getLocation();

      final Optional<DefaultComponentLocation> locationWithCustomPart = componentModel.getModel(EnrichableModel.class)
          .flatMap(em -> em.getModelProperty(CustomLocationPartModelProperty.class))
          .map(customLocationPart -> {
            if (customLocationPart.isIndexed()) {
              return parentComponentLocation
                  .appendLocationPart(customLocationPart.getLocationPart(),
                                      empty(), empty(), OptionalInt.empty(), OptionalInt.empty())
                  .appendLocationPart(findNonProcessorPath(componentModel, hierarchy),
                                      typedComponentIdentifier,
                                      componentModel.getMetadata().getFileName(),
                                      componentModel.getMetadata().getStartLine(),
                                      componentModel.getMetadata().getStartColumn());
            } else {
              return parentComponentLocation
                  .appendLocationPart(customLocationPart.getLocationPart(),
                                      typedComponentIdentifier,
                                      componentModel.getMetadata().getFileName(),
                                      componentModel.getMetadata().getStartLine(),
                                      componentModel.getMetadata().getStartColumn());
            }
          });

      if (locationWithCustomPart.isPresent()) {
        componentLocation = locationWithCustomPart.get();
      } else if (componentModel.getComponentType().equals(CHAIN)) {
        DefaultLocationPart newLastPart = new DefaultLocationPart(parentComponentModel.getComponentId().orElse(null),
                                                                  typedComponentIdentifier,
                                                                  componentModel.getMetadata().getFileName(),
                                                                  componentModel.getMetadata().getStartLine(),
                                                                  componentModel.getMetadata().getStartColumn());


        componentLocation = new DefaultComponentLocation(parentComponentModel.getComponentId(),
                                                         singletonList(newLastPart));
      } else if (isRootProcessorScope(parentComponentModel)) {
        componentLocation = processFlowDirectChild(componentModel, hierarchy, parentComponentLocation, typedComponentIdentifier);
      } else if (isErrorHandler(componentModel)) {
        componentLocation = processErrorHandlerComponent(componentModel, parentComponentLocation, typedComponentIdentifier);
      } else if (isTemplateOnErrorHandler(componentModel)) {
        componentLocation = processOnErrorModel(componentModel, hierarchy, parentComponentLocation, typedComponentIdentifier);
      } else if (parentComponentIsRouter(componentModel, hierarchy)) {
        if (isRoute(componentModel)) {
          componentLocation = parentComponentLocation.appendRoutePart()
              .appendLocationPart(findRoutePath(componentModel, hierarchy), typedComponentIdentifier,
                                  componentModel.getMetadata().getFileName(),
                                  componentModel.getMetadata().getStartLine(),
                                  componentModel.getMetadata().getStartColumn());
        } else if (isProcessor(componentModel)) {
          // this is the case of the routes directly inside the router as with scatter-gather
          componentLocation = parentComponentLocation
              .appendRoutePart()
              .appendLocationPart(findProcessorPath(componentModel, hierarchy), empty(), empty(), OptionalInt.empty(),
                                  OptionalInt.empty())
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
      } else if (isProcessor(componentModel) && !existsWithinSource(componentModel, hierarchy)) {
        componentLocation = parentComponentLocation
            .appendProcessorsPart()
            .appendLocationPart(findProcessorPath(componentModel, hierarchy),
                                typedComponentIdentifier,
                                componentModel.getMetadata().getFileName(),
                                componentModel.getMetadata().getStartLine(),
                                componentModel.getMetadata().getStartColumn());
      } else if (isConnection(componentModel)) {
        componentLocation = parentComponentLocation.appendConnectionPart(typedComponentIdentifier,
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

  private boolean isConnection(ComponentAst componentModel) {
    return componentModel.getModel(ConnectionProviderModel.class).isPresent();
  }

  private boolean isRoute(ComponentAst componentModel) {
    return componentModel.getComponentType().equals(ROUTE);
  }

  private boolean isRootProcessorScope(ComponentAst componentModel) {
    return componentModel.getModel(ConstructModel.class)
        .map(ConstructModel::allowsTopLevelDeclaration)
        .orElse(false);
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
          .appendLocationPart(PROCESSORS_PART_NAME, empty(), empty(), OptionalInt.empty(), OptionalInt.empty())
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
    return hierarchy.stream()
        .anyMatch(p -> p.getModel(ConfigurationModel.class).isPresent()
            || isRootProcessorScope(p));
  }

  private boolean existsWithinSource(ComponentAst componentModel, List<ComponentAst> hierarchy) {
    return hierarchy.stream()
        .anyMatch(p -> p.getModel(SourceModel.class).isPresent());
  }

}
