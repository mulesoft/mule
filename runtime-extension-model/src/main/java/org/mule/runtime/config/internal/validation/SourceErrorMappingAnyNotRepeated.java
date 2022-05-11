/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.List;
import java.util.Optional;

/**
 * Mapping for 'ANY' or empty is not repeated.
 */
public class SourceErrorMappingAnyNotRepeated extends AbstractErrorMappingValidation {

  @Override
  public String getName() {
    return "Source error-mapping ANY not repeated";
  }

  @Override
  public String getDescription() {
    return "Mapping for 'ANY' or empty is not repeated.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  protected Optional<ValidationResultItem> validateErrorMapping(ComponentAst component,
                                                                final ComponentParameterAst errorMappingsParam,
                                                                List<ErrorMapping> mappings) {
    if (mappings.stream()
        .filter(this::isErrorMappingWithSourceAny)
        .count() > 1) {
      return of(create(component, errorMappingsParam, this,
                       "Only one mapping for 'ANY' or an empty source type is allowed."));
    } else {
      return empty();
    }
  }

}
