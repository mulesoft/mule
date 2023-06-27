/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mule.test.module.extension.connector.PetStoreSourceRetryPolicyProviderTestCase.POLL_DELAY_MILLIS;
import static org.mule.test.module.extension.connector.PetStoreSourceRetryPolicyProviderTestCase.TIMEOUT_MILLIS;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreConnector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PetStoreDefaultEncodingTestCase extends AbstractExtensionFunctionalTestCase {

  private String defaultEncoding;
  private static Reference<Message> messageHolder = new Reference<>();

  @Override
  protected String getConfigFile() {
    return "petstore-default-mule-encoding.xml";
  }

  @Before
  public void setUp() {
    messageHolder.set(null);
    defaultEncoding = muleContext.getConfiguration().getDefaultEncoding();
    assertThat(defaultEncoding, is(notNullValue()));
  }

  @After
  public void tearDown() {
    messageHolder.set(null);
  }

  @Test
  public void configEncoding() throws Exception {
    PetStoreConnector config = (PetStoreConnector) flowRunner("configEncoding").run().getMessage().getPayload().getValue();
    assertDefaultEncoding(config.getEncoding());
  }

  @Test
  public void topLevelEncoding() throws Exception {
    assertDefaultEncoding((String) flowRunner("topLevelEncoding").run().getMessage().getPayload().getValue());
  }

  @Test
  public void inlinePojoEncoding() throws Exception {
    assertDefaultEncoding((String) flowRunner("inlinePojoEncoding").run().getMessage().getPayload().getValue());
  }

  @Test
  public void argumentEncoding() throws Exception {
    assertDefaultEncoding((String) flowRunner("argumentEncoding").run().getMessage().getPayload().getValue());
  }

  @Test
  public void sourceEncoding() throws Exception {
    startFlow("sourceEncoding");
    assertDefaultEncoding((String) listen().getPayload().getValue());
  }

  private void assertDefaultEncoding(String encoding) {
    assertThat(encoding, notNullValue());
    assertThat(encoding, is(defaultEncoding));
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

  private void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }
}
