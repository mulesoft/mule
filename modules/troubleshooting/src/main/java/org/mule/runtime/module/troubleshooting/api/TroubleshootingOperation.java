/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.api;

import org.mule.api.annotation.Experimental;

/**
 * Internal interface that has the definition and the callback for a given operation.
 */
@Experimental
public interface TroubleshootingOperation {

  /**
   * @return the operation definition.
   */
  TroubleshootingOperationDefinition getDefinition();

  /**
   * @return the operation callback.
   */
  TroubleshootingOperationCallback getCallback();
}
