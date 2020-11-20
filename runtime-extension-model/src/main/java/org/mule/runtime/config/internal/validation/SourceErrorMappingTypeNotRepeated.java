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
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.extension.api.error.ErrorMapping;
import org.mule.runtime.extension.internal.property.NoErrorMappingModelProperty;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Error Mappings source types are not repeated.
 */
public class SourceErrorMappingTypeNotRepeated implements Validation {

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
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.getModel(OperationModel.class)
        .map(opModel -> !opModel.getModelProperty(NoErrorMappingModelProperty.class).isPresent()
            && component.getParameter(ERROR_MAPPINGS_PARAMETER_NAME) != null)
        .orElse(false));
  }

  @Override
  public Optional<String> validate(ComponentAst component, ArtifactAst artifact) {
    return component.getParameter(ERROR_MAPPINGS_PARAMETER_NAME).<List<ErrorMapping>>getValue()
        .reduce(l -> empty(),
                mappings -> {
                  final List<String> repeatedSources = mappings.stream()
                      .map(ErrorMapping::getSource)
                      .collect(groupingBy(Function.identity(), counting()))
                      .entrySet()
                      .stream()
                      .filter(element -> element.getValue() > 1)
                      .map(Entry::getKey)
                      .collect(toList());

                  if (!repeatedSources.isEmpty()) {
                    return of(format("Repeated source types are not allowed. Offending types are '%s'.",
                                     on("', '").join(repeatedSources)));
                  } else {
                    return empty();
                  }
                });
  }

}
