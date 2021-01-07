/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mule.test.module.extension.connector.PetStoreSourceRetryPolicyProviderTestCase.POLL_DELAY_MILLIS;
import static org.mule.test.module.extension.connector.PetStoreSourceRetryPolicyProviderTestCase.TIMEOUT_MILLIS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreClient;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class PetStoreRuntimeVersionTestCase extends AbstractExtensionFunctionalTestCase {

  private static final Reference<Message> messageHolder = new Reference<>();

  @Override
  protected String getConfigFile() {
    return "petstore-runtime-version.xml";
  }

  @Before
  public void setUp() {
    messageHolder.set(null);
  }

  @Test
  public void configWithRuntimeVersionField() throws Exception {
    PetStoreConnector config =
        (PetStoreConnector) flowRunner("configWithRuntimeVersion").run().getMessage().getPayload().getValue();

    assertRuntimeVersion(config.getRuntimeVersion());
  }

  @Test
  public void sourceWithRuntimeVersionField() throws Exception {
    startFlow("sourceWithRuntimeVersion");
    MuleVersion muleVersion = (MuleVersion) ((Map) listen().getAttributes().getValue()).get("muleRuntime");

    assertRuntimeVersion(muleVersion);
  }

  @Test
  public void connectionProviderWithRuntimeVersionField() throws Exception {
    PetStoreClient petStoreClient =
        (PetStoreClient) flowRunner("connectionProviderWithRuntimeVersion").run().getMessage().getPayload().getValue();

    assertRuntimeVersion(petStoreClient.getMuleVersion());
  }

  @Test
  public void operationClassWithRuntimeVersionField() throws Exception {
    MuleVersion muleVersion =
        (MuleVersion) flowRunner("operationClassWithRuntimeVersion").run().getMessage().getPayload().getValue();

    assertRuntimeVersion(muleVersion);
  }

  private void assertRuntimeVersion(MuleVersion muleVersion) {
    assertThat(muleVersion, notNullValue());
  }

  private void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }

  public static class TestProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      messageHolder.set(event.getMessage());
      return event;
    }
  }

  private Message listen() {
    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitLambdaProbe(() -> messageHolder.get() != null));

    return messageHolder.get();
  }
}
