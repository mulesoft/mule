/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.util.Optional.empty;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.ERROR_MAPPINGS;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.core.api.error.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.extension.api.error.ErrorMapping;
import org.mule.runtime.extension.internal.property.NoErrorMappingModelProperty;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

abstract class AbstractErrorMappingValidation implements Validation {

  @Override
  public final Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.getModel(OperationModel.class)
        .map(opModel -> !opModel.getModelProperty(NoErrorMappingModelProperty.class).isPresent()
            && component.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME) != null
            && component.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME).getValue().getValue().isPresent())
        .orElse(false));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final ComponentParameterAst errorMappingsParam = component.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME);
    return errorMappingsParam.<List<ErrorMapping>>getValue()
        .reduce(l -> empty(),
                mappings -> validateErrorMapping(component, errorMappingsParam, mappings));
  }

  protected abstract Optional<ValidationResultItem> validateErrorMapping(ComponentAst component,
                                                                         final ComponentParameterAst errorMappingsParam,
                                                                         List<ErrorMapping> mappings);

  protected final boolean isErrorMappingWithSourceAny(ErrorMapping model) {
    return ANY_IDENTIFIER.equals(model.getSource());
  }

}
