/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENFORCE_EXPRESSION_VALIDATION;
import static org.mule.runtime.api.el.validation.Severity.WARNING;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;

import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.validation.Severity;
import org.mule.runtime.ast.api.validation.ArtifactValidation;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.Validation.Level;
import org.mule.runtime.ast.api.validation.ValidationsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

public class CoreValidationsProvider implements ValidationsProvider {

  private ClassLoader artifactRegionClassLoader;

  @Inject
  private final Optional<FeatureFlaggingService> featureFlaggingService = empty();

  @Inject
  private ExpressionLanguage expressionLanguage;

  @Override
  public List<Validation> get() {
    List<Validation> validations = new ArrayList<>(asList(new SingletonsAreNotRepeated(),
                                                          new SingletonsPerFileAreNotRepeated(),
                                                          new NamedTopLevelElementsHaveName(),
                                                          new NameHasValidCharacters(),
                                                          new NameIsNotRepeated(),
                                                          // make this general for all references via stereotypes
                                                          new FlowRefPointsToExistingFlow(),
                                                          new SourceErrorMappingAnyNotRepeated(),
                                                          new SourceErrorMappingAnyLast(),
                                                          new SourceErrorMappingTypeNotRepeated(),
                                                          new ErrorHandlerRefOrOnErrorExclusiveness(),
                                                          new ErrorHandlerOnErrorHasTypeOrWhen(),
                                                          new RaiseErrorTypeReferencesPresent(featureFlaggingService),
                                                          new RaiseErrorTypeReferencesExist(featureFlaggingService),
                                                          new ErrorMappingTargetTypeReferencesExist(featureFlaggingService),
                                                          new ErrorMappingSourceTypeReferencesExist(featureFlaggingService),
                                                          new ErrorHandlerOnErrorTypeExists(featureFlaggingService),
                                                          new RequiredParametersPresent(),
                                                          new ParameterGroupExclusiveness(),
                                                          new ExpressionsInRequiredExpressionsParams(featureFlaggingService),
                                                          new NoExpressionsInNoExpressionsSupportedParams(),
                                                          new DynamicConfigWithStatefulOperationConfigurationOverride(),
                                                          new PollingSourceHasSchedulingStrategy(),
                                                          new RoundRobinRoutes(),
                                                          new FirstSuccessfulRoutes(),
                                                          new ScatterGatherRoutes(),
                                                          new ParseTemplateResourceExist(artifactRegionClassLoader),
                                                          new SourcePositiveMaxItemsPerPoll()));

    // Do not fail if the expressionLanguage was not provided, skip these validations.
    if (expressionLanguage != null) {
      validations.add(new ExpressionParametersSyntacticallyValid(expressionLanguage,
                                                                 () -> getExpressionSyntacticValidationErrorLevel(featureFlaggingService),
                                                                 Severity.ERROR));
      validations.add(new ExpressionParametersSyntacticallyValid(expressionLanguage, () -> WARN, WARNING));
    }

    return validations;
  }

  public static Level getExpressionSyntacticValidationErrorLevel(Optional<FeatureFlaggingService> featureFlaggingService) {
    // Honour the system property consistently with MuleConfiguration#isValidateExpressions
    boolean validateExpressions = true;
    String validateExpressionsPropValue = getProperty(SYSTEM_PROPERTY_PREFIX + "validate.expressions");
    if (validateExpressionsPropValue != null) {
      validateExpressions = Boolean.valueOf(validateExpressionsPropValue);
    }

    // TODO W-10883564 Remove this
    return validateExpressions && featureFlaggingService.map(ffs -> ffs.isEnabled(ENFORCE_EXPRESSION_VALIDATION)).orElse(true)
        ? ERROR
        : WARN;
  }

  @Override
  public List<ArtifactValidation> getArtifactValidations() {
    return asList(new ImportValidTarget(),
                  new ConfigReferenceParametersStereotypesValidations(featureFlaggingService),
                  new ReferenceParametersStereotypesValidations());
  }

  @Override
  public void setArtifactRegionClassLoader(ClassLoader artifactRegionClassLoader) {
    this.artifactRegionClassLoader = artifactRegionClassLoader;
  }
}
