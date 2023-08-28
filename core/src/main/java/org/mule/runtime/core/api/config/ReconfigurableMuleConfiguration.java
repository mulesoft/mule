/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
