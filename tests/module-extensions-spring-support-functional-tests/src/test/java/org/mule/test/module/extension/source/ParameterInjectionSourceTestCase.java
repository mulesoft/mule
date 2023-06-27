/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.module.extension.source.HeisenbergMessageSourceTestCase.POLL_DELAY_MILLIS;
import static org.mule.test.module.extension.source.HeisenbergMessageSourceTestCase.TIMEOUT_MILLIS;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.heisenberg.extension.HeisenbergSource;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(SOURCES)
public class ParameterInjectionSourceTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "source/map-parameter-injection-config.xml";
  }

  @Override
  public Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> customProperties = new HashMap<>();
    Map<String, Object> debtConfiguration = new HashMap<>();

    debtConfiguration.put("maxDebt", "Jesse's");
    debtConfiguration.put("minDebt", "Krazy-8");

    customProperties.put("referableDebtProperties", debtConfiguration);

    return customProperties;
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    HeisenbergSource.receivedDebtProperties = null;
    HeisenbergSource.receivedUsableWeapons = null;
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    HeisenbergSource.receivedDebtProperties = null;
    HeisenbergSource.receivedUsableWeapons = null;
  }

  @Test
  public void injectMapParameters() throws Exception {
    Flow flow = (Flow) getFlowConstruct("source");
    flow.start();

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS,
          () -> HeisenbergSource.receivedDebtProperties != null && HeisenbergSource.receivedDebtProperties.containsKey("maxDebt")
              && HeisenbergSource.receivedUsableWeapons != null && HeisenbergSource.receivedUsableWeapons.containsKey("Ricin"));

  }
}
