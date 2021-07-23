/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static com.google.common.base.Joiner.on;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

/**
 * Error Mappings source types are not repeated.
 */
public class SourceErrorMappingTypeNotRepeated extends AbstractErrorMappingValidation {

  @Override
  public String getName() {
    return "Source error-mapping type not repeated";
  }

  @Override
  public String getDescription() {
    return "Error Mappings source types are not repeated.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  protected Optional<ValidationResultItem> validateErrorMapping(ComponentAst component,
                                                                final ComponentParameterAst errorMappingsParam,
                                                                List<ErrorMapping> mappings) {
    final List<String> repeatedSources = mappings.stream()
        .map(ErrorMapping::getSource)
        .collect(groupingBy(Function.identity(), counting()))
        .entrySet()
        .stream()
        .filter(element -> element.getValue() > 1)
        .map(Entry::getKey)
        .collect(toList());

    if (!repeatedSources.isEmpty()) {
      return of(create(component, errorMappingsParam, this,
                       format("Repeated source types are not allowed. Offending types are '%s'.",
                              on("', '").join(repeatedSources))));
    } else {
      return empty();
    }
  }

}
