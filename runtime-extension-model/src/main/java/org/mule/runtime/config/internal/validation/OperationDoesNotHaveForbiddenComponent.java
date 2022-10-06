/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.Optional;

public abstract class OperationDoesNotHaveForbiddenComponent extends OperationValidation {

  protected abstract ComponentIdentifier forbiddenComponentIdentifier();

  @Override
  public String getName() {
    return format("Operation doesn't have '%s'", forbiddenComponentIdentifier());
  }

  @Override
  public String getDescription() {
    return format("Operations cannot have a '%s' component within", forbiddenComponentIdentifier());
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    if (component.recursiveStream().anyMatch(c -> c.getIdentifier().equals(forbiddenComponentIdentifier()))) {
      return of(create(component, this,
                       format("Usages of the component '%s' are not allowed inside a Mule Operation Definition (%s)",
                              forbiddenComponentIdentifier(), OPERATION_IDENTIFIER)));
    } else {
      return empty();
    }
  }
}
