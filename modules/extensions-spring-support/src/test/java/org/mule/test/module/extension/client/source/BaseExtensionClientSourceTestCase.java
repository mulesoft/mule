/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.client.source;

import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.source.SourceHandler;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import javax.inject.Inject;

/**
 * Base class for tests about message source support in {@link ExtensionsClient}
 *
 * @since 4.5.0
 */
abstract class BaseExtensionClientSourceTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  protected ExtensionsClient extensionsClient;

  protected SourceHandler handler;

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    stopAndDispose(handler);
  }

  private void stopAndDispose(SourceHandler handler) throws Exception {
    if (handler != null) {
      try {
        handler.stop();
      } finally {
        handler.dispose();
      }
    }
  }
}
