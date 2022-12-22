/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client.source;

import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.source.SourceHandler;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;

public class ExtensionClientPollingSourceTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public SystemProperty configProperty = new SystemProperty("configName", "petstore");

  @Inject
  private ExtensionsClient extensionsClient;

  @Override
  protected String getConfigFile() {
    return "petstore.xml";
  }

  @Test
  public void initPollingSource() throws Exception {
    SourceHandler handler = extensionsClient.createSource("petstore",
                                                          "ConnectedPetAdoptionSource",
                                                          callback -> {
                                                          },
                                                          parameters -> parameters
                                                            .withConfigRef(configProperty.getValue())
                                                            .withParameter("watermark", true)
                                                            .withParameter("idempotent", true));
  }
}
