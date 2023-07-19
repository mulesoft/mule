/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.policy;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategySupplier;

import java.util.Optional;

/**
 * Instance of a policy which has {@link org.mule.runtime.core.api.processor.Processor}s to be applied to a source and another
 * chain to be applied to an operation.
 *
 * Both of them are optionals since a policy may apply only to a source or only to an operation.
 *
 * @since 4.0
 */
@NoImplement
public interface PolicyInstance extends Initialisable, Startable, ProcessingStrategySupplier {

  /**
   * @return chain of processors to intercept the source execution
   */
  Optional<PolicyChain> getSourcePolicyChain();

  /**
   * @return chain of processors to intercept an operation execution.
   */
  Optional<PolicyChain> getOperationPolicyChain();

}
