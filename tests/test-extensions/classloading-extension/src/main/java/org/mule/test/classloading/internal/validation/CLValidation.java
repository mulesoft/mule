/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.classloading.internal.validation;

import static com.google.common.base.Predicates.alwaysFalse;
import static java.util.Optional.empty;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CLValidation implements Validation {

  @Override
  public String getName() {
    return "Contributed CL Validation";
  }

  @Override
  public String getDescription() {
    return "Verify the validations providing mechanism for extensions.";
  }

  @Override
  public Level getLevel() {
    return WARN;
  }

  public String getComponentName() {
    return "config";
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return alwaysFalse();
  }

  @Override
  public Optional<String> validate(ComponentAst component, ArtifactAst artifact) {
    return empty();
  }

}
