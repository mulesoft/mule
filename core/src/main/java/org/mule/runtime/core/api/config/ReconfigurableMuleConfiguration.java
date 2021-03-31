/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import org.mule.api.annotation.NoInstantiate;

/**
 * Specialization of {@link DefaultMuleConfiguration} that allows modifications to take place even after the artifact has been
 * initialized/started.
 * <p>
 * This is in an {@code api} package because this needs to be referenced by the test infrastructure.
 *
 * @since 4.4
 */
@NoInstantiate
public final class ReconfigurableMuleConfiguration extends DefaultMuleConfiguration {

  public ReconfigurableMuleConfiguration() {
    super();
  }

  public ReconfigurableMuleConfiguration(boolean containerMode) {
    super(containerMode);
  }

  @Override
  protected boolean verifyContextNotInitialized() {
    return true;
  }

  @Override
  protected boolean verifyContextNotStarted() {
    return true;
  }

}
