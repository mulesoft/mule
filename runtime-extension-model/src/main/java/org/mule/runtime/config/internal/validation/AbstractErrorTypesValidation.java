/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENFORCE_ERROR_TYPES_VALIDATION;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.ERROR_MAPPINGS;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.extension.api.error.ErrorMapping;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Ensures consistent access to the {@link ErrorTypeRepository} from validations.
 */
public abstract class AbstractErrorTypesValidation implements Validation {

  protected static final String RAISE_ERROR = "raise-error";

  protected static final String ON_ERROR = "on-error";
  protected static final String ON_ERROR_PROPAGATE = "on-error-propagate";
  protected static final String ON_ERROR_CONTINUE = "on-error-continue";

  protected static final ComponentIdentifier RAISE_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(RAISE_ERROR).build();

  protected static final ComponentIdentifier ON_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR).build();
  protected static final ComponentIdentifier ON_ERROR_PROPAGATE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR_PROPAGATE).build();
  protected static final ComponentIdentifier ON_ERROR_CONTINUE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR_CONTINUE).build();

  private final Optional<FeatureFlaggingService> featureFlaggingService;

  private static final Collection<String> allowedBorrowedNamespaces = getAllowedBorrowedNamespaces();

  public AbstractErrorTypesValidation(Optional<FeatureFlaggingService> featureFlaggingService) {
    this.featureFlaggingService = featureFlaggingService;
  }

  @Override
  public Level getLevel() {
    return featureFlaggingService.map(ffs -> ffs.isEnabled(ENFORCE_ERROR_TYPES_VALIDATION)).orElse(true)
        ? ERROR
        : WARN;
  }

  protected static boolean errorMappingPresent(ComponentAst operationComponent) {
    if (!operationComponent.getModel(ParameterizedModel.class).isPresent()) {
      return false;
    }
    final ComponentParameterAst errorMappingsAst =
        operationComponent.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME);
    return errorMappingsAst != null && errorMappingsAst.getValue().getValue().isPresent();
  }

  protected static List<ErrorMapping> getErrorMappings(ComponentAst component) {
    return (List<ErrorMapping>) component.getParameter(ERROR_MAPPINGS, ERROR_MAPPINGS_PARAMETER_NAME).getValue().getRight();
  }

  protected static Optional<ValidationResultItem> validateErrorTypeId(ComponentAst component, ComponentParameterAst parameter,
                                                                      ArtifactAst artifact,
                                                                      Validation validation, String errorTypeString,
                                                                      final ComponentIdentifier errorTypeId) {
    final Optional<ErrorType> errorType = artifact.getErrorTypeRepository().lookupErrorType(errorTypeId);

    if (!errorType.isPresent()) {
      if (CORE_PREFIX.toUpperCase().equals(errorTypeId.getNamespace())) {
        return of(create(component, parameter, validation,
                         format("There's no MULE error named '%s'.", errorTypeId.getName())));
      } else {
        return of(create(component, parameter, validation,
                         format("Could not find error '%s'.", errorTypeId.getName())));
      }
    }

    return empty();
  }

  protected static Optional<ErrorType> lookup(ComponentAst component, String errorTypeParamName, ArtifactAst artifact) {
    return artifact.getErrorTypeRepository()
        .lookupErrorType(parserErrorType(component.getParameter(ERROR_MAPPINGS, "type").getResolvedRawValue()));
  }

  protected static ComponentIdentifier parserErrorType(String representation) {
    int separator = representation.indexOf(':');
    String namespace;
    String identifier;
    if (separator > 0) {
      namespace = representation.substring(0, separator).toUpperCase();
      identifier = representation.substring(separator + 1).toUpperCase();
    } else {
      namespace = CORE_PREFIX.toUpperCase();
      identifier = representation.toUpperCase();
    }

    return builder().name(identifier).namespace(namespace).build();
  }

  protected static boolean isAllowedBorrowedNamespace(String namespace) {
    return allowedBorrowedNamespaces.contains(namespace);
  }

  private static Collection<String> getAllowedBorrowedNamespaces() {
    Collection<String> namespaces = new HashSet<>(3);

    // raise-error is allowed to throw errors from the MULE namespace.
    namespaces.add("MULE");

    // TODO W-11464525: We have several tests using an extension with namespace "test" and a raise-error with the same
    // namespace. Refactor all those tests and remove this line.
    namespaces.add("TEST");
    return namespaces;
  }

  protected static Set<String> getAlreadyUsedErrorNamespaces(ArtifactAst artifact) {
    return artifact.dependencies().stream()
        .map(d -> d.getXmlDslModel().getPrefix().toUpperCase())
        .collect(toSet());
  }
}
