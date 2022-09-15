/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
