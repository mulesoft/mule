/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.transport.jms.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.object.IsCompatibleType.typeCompatibleWith;
import static org.mule.functional.functional.FlowAssert.verify;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Test;

public class JmsObjectToMessageDataTypeTransformerTestCase extends AbstractJmsFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "integration/jms-object-to-message-transformer-data-type-test-case.xml";
  }

  @Test
  public void testDataTypeTransformation() throws Exception {
    Scenario sendTextMessageScenario = new NonTransactedScenario() {

      @Override
      public void send(Session session, MessageProducer producer) throws JMSException {
        producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
      }
    };
    send(sendTextMessageScenario);
    verify("message-to-string-flow");
  }

  public static class CheckTextMessageType implements MessageProcessor {

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      assertThat(event.getMessage().getDataType().getType(), typeCompatibleWith(TextMessage.class));
      return event;
    }
  }

}
