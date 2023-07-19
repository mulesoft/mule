/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.client.operation;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.client.OperationParameterizer;

import java.util.Optional;

/**
 * Expands the {@link OperationParameterizer} contract with internal behavior
 *
 * @since 4.5.0
 */
public interface InternalOperationParameterizer extends OperationParameterizer {

  /**
   * @return an {@link Optional} contextual {@link CoreEvent}
   */
  Optional<CoreEvent> getContextEvent();

}
