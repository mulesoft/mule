/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.meta.model.display.PathModel.Location;
import org.mule.runtime.api.meta.model.display.PathModel.Type;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ResourceExistsAndAccessible implements Validation {

  private final ClassLoader artifactRegionClassLoader;

  public ResourceExistsAndAccessible(ClassLoader artifactRegionClassLoader) {
    this.artifactRegionClassLoader = artifactRegionClassLoader;
  }

  public final ClassLoader getArtifactRegionClassLoader() {
    return artifactRegionClassLoader;
  }

  @Override
  public String getName() {
    return "Resource exist and accessible";
  }

  @Override
  public String getDescription() {
    return "Artifact resources/files referenced in component parameters exist and are accessible.";
  }

  @Override
  public Level getLevel() {
    return WARN;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(comp -> comp.getModel(ParameterizedModel.class).isPresent()
        && comp.getParameters()
            .stream()
            .anyMatch(p -> fixedValuePresent(p) && resourceParameter(p)));
  }

  protected final boolean fixedValuePresent(ComponentParameterAst p) {
    return p.getValue() != null
        && p.getValue().getRight() != null;
  }

  protected final boolean resourceParameter(ComponentParameterAst paramAst) {
    return paramAst.getModel().getDisplayModel()
        .map(dm -> dm.getPathModel()
            .map(pm -> (pm.getType().equals(Type.FILE)
                || pm.getType().equals(Type.ANY))
                && (pm.getLocation().equals(Location.EMBEDDED)
                    || pm.getLocation().equals(Location.ANY)))
            .orElse(false))
        .orElse(false);
  }

  @Override
  public List<ValidationResultItem> validateMany(ComponentAst component, ArtifactAst artifact) {
    return component.getParameters()
        .stream()
        .filter(p -> fixedValuePresent(p) && resourceParameter(p))
        .map(p -> {
          final ComponentParameterAst parameter = component.getParameter(p.getGroupModel().getName(), p.getModel().getName());
          final String resourceLocationValue = parameter.getValue().getRight().toString();

          return validateResourceExists(component, p, resourceLocationValue);
        })
        .filter(Optional::isPresent)
        .map(Optional::orElseThrow)
        .collect(toList());
  }

  protected final Optional<ValidationResultItem> validateResourceExists(ComponentAst component,
                                                                        ComponentParameterAst resourceParam,
                                                                        String resource) {
    if (getArtifactRegionClassLoader().getResource(resource) == null && !new File(resource).exists()) {
      return of(create(component, resourceParam, this,
                       "Invalid configuration found for parameter '" + resourceParam.getModel().getName() + "': " + resource));
    }
    return empty();
  }
}
