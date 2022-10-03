/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_TRUST_STORE_ELEMENT_IDENTIFIER;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class InsecureTLSValidation implements Validation {

  private static final ComponentIdentifier TLS_IDENTIFIER =
      builder().namespace(TLS_PREFIX).name(TLS_TRUST_STORE_ELEMENT_IDENTIFIER).build();

  @Override
  public String getName() {
    return "Insecure TLS validation";
  }

  @Override
  public String getDescription() {
    return "Warning for insecure communication";
  }

  @Override
  public Level getLevel() {
    return WARN;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.getIdentifier().equals(TLS_IDENTIFIER));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final ComponentParameterAst param = component.getParameter("TrustStore", "insecure");
    Object insecure = param.getValue().getRight();
    if (insecure instanceof Boolean && (Boolean) insecure) {
      return of(create(component, param, this,
                       "Setting insecure to true renders connections vulnerable to attack. Use it only for prototyping or testing. Never use it in production environments."));
    } else {
      return empty();
    }
  }

}
