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

import org.junit.Test;

public class ExtensionClientPollingSourceTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  private ExtensionsClient extensionsClient;

  @Override
  protected String getConfigFile() {
    return "petstore.xml";
  }

  @Test
  public void initPollingSource() throws Exception {
    SourceHandler handler = extensionsClient.createSource("petstore",
                                                          "connected-pet-adoption-source",
                                                          callback -> {
                                                          },
                                                          parameters -> parameters
                                                            .withConfigRef("config")
                                                            .withParameter("watermark", true)
                                                            .withParameter("idempotent", true));
  }
}
