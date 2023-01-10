/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client.source;

import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.source.SourceHandler;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import javax.inject.Inject;

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
