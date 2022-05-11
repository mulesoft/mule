/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@IgnoreOnLazyInit
public class ScatterGatherRoutes implements Validation {

  private static final String SCATTER_GATHER_ELEMENT = "scatter-gather";

  private static final ComponentIdentifier SCATTER_GATHER_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(SCATTER_GATHER_ELEMENT).build();

  @Override
  public String getName() {
    return "'scatter-gather' has at least 2 routes";
  }

  @Override
  public String getDescription() {
    return "'scatter-gather' has at least 2 routes";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.getIdentifier().equals(SCATTER_GATHER_IDENTIFIER));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    if (component.directChildrenStream()
        .filter(c -> c.getIdentifier().getName().equals("route"))
        .count() < 2) {
      return of(create(component, this, "At least 2 routes are required for '" + SCATTER_GATHER_ELEMENT + "'."));
    } else {
      return empty();
    }
  }

}
