/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.ast.api.validation.ArtifactValidation;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationsProvider;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

public class CoreValidationsProvider implements ValidationsProvider {

  private ClassLoader artifactRegionClassLoader;

  @Inject
  private final Optional<FeatureFlaggingService> featureFlaggingService = empty();

  @Override
  public List<Validation> get() {
    return asList(new SingletonsAreNotRepeated(),
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
                  new ExpressionsInRequiredExpressionsParams(),
                  new NoExpressionsInNoExpressionsSupportedParams(),
                  new DynamicConfigWithStatefulOperationConfigurationOverride(),
                  new PollingSourceHasSchedulingStrategy(),
                  new RoundRobinRoutes(),
                  new FirstSuccessfulRoutes(),
                  new ScatterGatherRoutes(),
                  new ParseTemplateResourceExist(artifactRegionClassLoader),
                  new SourcePositiveMaxItemsPerPoll());
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
