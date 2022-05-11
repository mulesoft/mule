/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.api;

import org.mule.api.annotation.Experimental;

/**
 * Interface that has the {@link TroubleshootingOperationDefinition} and the {@link TroubleshootingOperationCallback} for a given
 * operation.
 *
 * @since 4.5
 */
@Experimental
public interface TroubleshootingOperation {

  /**
   * Returns the {@link TroubleshootingOperationDefinition} for this operation.
   *
   * @return the {@link TroubleshootingOperationDefinition} for this operation.
   */
  TroubleshootingOperationDefinition getDefinition();

  /**
   * Returns the {@link TroubleshootingOperationCallback} for this operation.
   *
   * @return the {@link TroubleshootingOperationCallback} for this operation.
   */

  TroubleshootingOperationCallback getCallback();
}
