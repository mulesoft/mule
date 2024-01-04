/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import static java.util.Optional.of;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.validation.Severity;
import org.mule.runtime.ast.api.validation.ArtifactValidation;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.Validation.Level;
import org.mule.runtime.ast.api.validation.ValidationsProvider;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphProvider;
import org.mule.runtime.config.internal.validation.ast.ArtifactAstGraphDependencyProviderAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

public class CoreValidationsProvider implements ValidationsProvider, ArtifactAstGraphDependencyProviderAware {

  private ClassLoader artifactRegionClassLoader;

  private boolean ignoreParamsWithProperties;

  @Inject
  private Optional<ArtifactAstDependencyGraphProvider> artifactAstDependencyGraphProvider = empty();

  @Inject
  private final Optional<FeatureFlaggingService> featureFlaggingService = empty();

  @Inject
  private ExpressionLanguage expressionLanguage;

  @Inject
  @Named("_compatibilityPluginInstalled")
  private Optional<Object> compatibilityPluginInstalled;

  @Override
  public List<Validation> get() {
    List<Validation> validations = new ArrayList<>(asList(new AllComponentsBelongToSomeExtensionModel(isCompatibilityInstalled()),
                                                          new SingletonsAreNotRepeated(),
                                                          new SingletonsPerFileAreNotRepeated(),
                                                          new NamedTopLevelElementsHaveName(),
                                                          new NameHasValidCharacters(),
                                                          new NameIsNotRepeated(),
                                                          // make these general for all references via stereotypes
                                                          new FlowRefPointsToNonPropertyValue(ignoreParamsWithProperties),
                                                          new FlowRefPointsToExistingFlow(ignoreParamsWithProperties),
                                                          // --

                                                          // Error types + error handling
                                                          new SourceErrorMappingAnyNotRepeated(),
                                                          new SourceErrorMappingAnyLast(),
                                                          new SourceErrorMappingTypeNotRepeated(),
                                                          new ErrorHandlerRefOrOnErrorExclusiveness(),
                                                          new ErrorHandlerOnErrorHasTypeOrWhen(),
                                                          new RaiseErrorTypeReferencesPresent(featureFlaggingService),
                                                          new RaiseErrorReferenceDoNotUseExtensionNamespaces(featureFlaggingService),
                                                          new RaiseErrorTypeReferencesNonPropertyValue(ignoreParamsWithProperties),
                                                          new RaiseErrorTypeReferencesExist(featureFlaggingService,
                                                                                            ignoreParamsWithProperties),
                                                          new ErrorMappingTargetTypeReferencesNonPropertyValue(ignoreParamsWithProperties),
                                                          new ErrorMappingTargetTypeReferencesExist(featureFlaggingService,
                                                                                                    ignoreParamsWithProperties),
                                                          new ErrorMappingTargetTypeReferencesDoNotUseExtensionNamespace(featureFlaggingService,
                                                                                                                         ignoreParamsWithProperties),
                                                          new ErrorMappingSourceTypeReferencesNonPropertyValue(ignoreParamsWithProperties),
                                                          new ErrorMappingSourceTypeReferencesExist(featureFlaggingService,
                                                                                                    ignoreParamsWithProperties),
                                                          new ErrorHandlerOnErrorTypeNonPropertyValue(ignoreParamsWithProperties),
                                                          new ErrorHandlerOnErrorTypeExists(featureFlaggingService,
                                                                                            ignoreParamsWithProperties),
                                                          // --

                                                          new RequiredParametersPresent(),
                                                          new ParameterGroupExclusiveness(),
                                                          new NumberParameterWithinRange(),
                                                          new OperationErrorHandlersDoNotReferGlobalErrorHandlers(),
                                                          new ExpressionsInRequiredExpressionsParamsNonPropertyValue(ignoreParamsWithProperties),
                                                          new ExpressionsInRequiredExpressionsParams(featureFlaggingService,
                                                                                                     ignoreParamsWithProperties),
                                                          new OperationParameterDefaultValueDoesntSupportExpressions(),
                                                          new NoExpressionsInNoExpressionsSupportedParams(),
                                                          new DynamicConfigWithStatefulOperationConfigurationOverride(),
                                                          new PollingSourceHasSchedulingStrategy(),
                                                          new RoundRobinRoutes(),
                                                          new FirstSuccessfulRoutes(),
                                                          new ScatterGatherRoutes(),
                                                          new ParseTemplateResourceNotPropertyValue(ignoreParamsWithProperties),
                                                          new ParseTemplateResourceExist(artifactRegionClassLoader,
                                                                                         ignoreParamsWithProperties),
                                                          new SourcePositiveMaxItemsPerPoll(),
                                                          new OperationRaiseErrorDoesntSpecifyNamespace(),
                                                          new OperationDoesNotHaveCoreRaiseError(),
                                                          new OperationDoesNotHaveFlowRef(),
                                                          new OperationDoesNotHaveApikitRouter(),
                                                          new OperationDoesNotHaveApikitConsole(),
                                                          new InsecureTLSValidation()));

    validations.add(new ExpressionParametersNotUsingMel());
    // Do not fail if the expressionLanguage was not provided, skip these validations.
    if (expressionLanguage != null) {
      validations.add(new ExpressionParametersSyntacticallyValid(expressionLanguage,
                                                                 () -> getExpressionSyntacticValidationErrorLevel(featureFlaggingService),
                                                                 Severity.ERROR));
      validations.add(new ExpressionParametersSyntacticallyValid(expressionLanguage, () -> WARN, WARNING));
      validations.add(new MuleSdkOperationDoesNotHaveForbiddenFunctionsInExpressions(expressionLanguage));
    }

    return validations;
  }

  private boolean isCompatibilityInstalled() {
    return compatibilityPluginInstalled != null && compatibilityPluginInstalled.isPresent();
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
    // TODO W-13931931: Create a context for dependencies needed to be injected in deployment
    // When this is done the artifactAstDependencyGraphProvider will probably be mandatory.
    ArtifactAstDependencyGraphProvider artifactAstDependencyGraphProviderForValidator =
        artifactAstDependencyGraphProvider.orElse(new DefaultArtifactAstDependencyGraphProvider());

    return asList(new ImportValidTarget(),
                  new ConfigReferenceParametersNonPropertyValueValidations(ignoreParamsWithProperties,
                                                                           artifactAstDependencyGraphProviderForValidator),
                  new ConfigReferenceParametersStereotypesValidations(featureFlaggingService, ignoreParamsWithProperties,
                                                                      artifactAstDependencyGraphProviderForValidator),
                  new ReferenceParametersStereotypesValidations(artifactAstDependencyGraphProviderForValidator),
                  new MelNotEnabled(isCompatibilityInstalled()));
  }

  @Override
  public void setArtifactRegionClassLoader(ClassLoader artifactRegionClassLoader) {
    this.artifactRegionClassLoader = artifactRegionClassLoader;
  }

  @Override
  public void setIgnoreParamsWithProperties(boolean ignoreParamsWithProperties) {
    this.ignoreParamsWithProperties = ignoreParamsWithProperties;
  }

  @Override
  public void setArtifactAstDependencyGraphProvider(ArtifactAstDependencyGraphProvider artifactAstDependencyGraphProvider) {
    // TODO W-13931931: Create a context for dependencies needed to be injected in deployment
    // This setter and the implementation of the interface will not be needed after that.
    // We cannot add an inject here because in the muleContext there is no provider.
    this.artifactAstDependencyGraphProvider = of(artifactAstDependencyGraphProvider);
  }
}
