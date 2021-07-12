/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.mule.tck.probe.PollingProber.check;
import static org.mule.tck.probe.PollingProber.checkNot;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.POLLING;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PooledPetStoreConnectionProvider;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SOURCES)
@Story(POLLING)
public class PollingSourceReleasesConnectionTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "source/polling-source-releases-connection-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    PooledPetStoreConnectionProvider.TIMES_CONNECTED = 0;
  }

  @Test
  public void watermarkedItemsReleaseConnectionsTestCase() throws Exception {
    startFlow("watermarkWithPooledConnection");
    check(10000, 100, () -> PooledPetStoreConnectionProvider.TIMES_CONNECTED > 0);
    checkNot(10000, 1000, () -> PooledPetStoreConnectionProvider.TIMES_CONNECTED > 10);
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

}
