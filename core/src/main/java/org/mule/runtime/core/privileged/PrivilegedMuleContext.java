/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

/**
 * Provides access to objects of the {@link MuleContext} for privileged-api only.
 *
 * @since 4.0
 */
public interface PrivilegedMuleContext extends MuleContext {

  /**
   * @return a locator for discovering {@link org.mule.runtime.api.message.ErrorType}s related to exceptions and components.
   */
  ErrorTypeLocator getErrorTypeLocator();

}
