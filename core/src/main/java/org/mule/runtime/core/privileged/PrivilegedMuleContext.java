/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

/**
 * Provides access to objects of the {@link MuleContext} for privileged-api only.
 *
 * @since 4.0
 */
@NoImplement
public interface PrivilegedMuleContext extends MuleContext {

  /**
   * @return a locator for discovering {@link org.mule.runtime.api.message.ErrorType}s related to exceptions and components.
   */
  ErrorTypeLocator getErrorTypeLocator();

}
