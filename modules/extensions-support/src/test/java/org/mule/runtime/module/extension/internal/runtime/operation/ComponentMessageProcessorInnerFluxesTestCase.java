/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.EXECUTION_ENGINE;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.ExecutionEngineStory.REACTOR;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.TestComponentMessageProcessor;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

/**
 * Specialization of {@link ComponentMessageProcessorTestCase} in which the events go through the inner fluxes.
 * <p>
 * This is the case for operations that may complete in a different thread or operations with policies.
 * <p>
 * We are intentionally inheriting all the tests from {@link ComponentMessageProcessorTestCase}.
 *
 * @see ComponentMessageProcessor#createOuterFlux
 */
@Feature(EXECUTION_ENGINE)
@Story(REACTOR)
@Issue("W-13563214")
public class ComponentMessageProcessorInnerFluxesTestCase extends ComponentMessageProcessorTestCase {

  @Override
  protected ComponentMessageProcessor<ComponentModel> createProcessor() {
    return new TestComponentMessageProcessor(extensionModel,
                                             componentModel, null, null, null,
                                             resolverSet, null, null, null,
                                             null, extensionManager,
                                             mockPolicyManager, null, null,
                                             muleContext.getConfiguration().getShutdownTimeout()) {

      @Override
      protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {}

      @Override
      public ProcessingType getInnerProcessingType() {
        return ProcessingType.CPU_LITE;
      }

      @Override
      protected boolean mayCompleteInDifferentThread() {
        // For this test case we want all events to be processed as if the processor was non-blocking (through the round-robin of
        // inner fluxes)
        return true;
      }
    };
  }
}
