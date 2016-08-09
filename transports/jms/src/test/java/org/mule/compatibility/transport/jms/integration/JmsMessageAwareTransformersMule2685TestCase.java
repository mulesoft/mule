/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import org.mule.compatibility.core.routing.outbound.ExpressionRecipientList;
import org.mule.compatibility.transport.jms.transformers.AbstractJmsTransformer;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;

import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <code>JmsTransformersTestCase</code> Tests the JMS transformer implementations. */
public class JmsMessageAwareTransformersMule2685TestCase extends AbstractJmsFunctionalTestCase {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private Session session = null;

  @Override
  protected String getConfigFile() {
    return "integration/jms-transformers.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    session = getConnection(false, false).createSession(false, Session.AUTO_ACKNOWLEDGE);
  }

  @Override
  protected void doTearDown() throws Exception {
    setCurrentEvent(null);
    if (session != null) {
      session.close();
      session = null;
    }
  }

  @Test
  public void testMessageAwareTransformerChainedWithObjectToJMSMessage() throws Exception {
    setCurrentEvent(getTestEvent("test"));

    MuleMessage message = getTestMuleMessage("This is a test TextMessage");

    SetTestRecipientsTransformer trans = new SetTestRecipientsTransformer();
    trans.setMuleContext(muleContext);
    MuleMessage result1 = (MuleMessage) trans.transform(message);

    // Check that transformer 1 set message property ok.
    assertEquals("vm://recipient1, vm://recipient1, vm://recipient3",
                 result1.getOutboundProperty(ExpressionRecipientList.DEFAULT_SELECTOR_PROPERTY));

    AbstractJmsTransformer trans2 = new SessionEnabledObjectToJMSMessage(session);
    trans2.setMuleContext(muleContext);
    Message result2 = (Message) trans2.transform(result1);

    // Test to see that ObjectToJMSMessage transformer transformed to JMS message
    // correctly
    assertThat(result2, instanceOf(TextMessage.class));
    assertEquals("This is a test TextMessage", ((TextMessage) result2).getText());

    // Check to see if after the ObjectToJMSMessage transformer these properties
    // are on JMS message
    assertEquals("vm://recipient1, vm://recipient1, vm://recipient3",
                 result2.getStringProperty("recipients"));

  }

  /** Test <i>AbstractMessageTransformer</i> which sets Message properties */
  private class SetTestRecipientsTransformer extends AbstractMessageTransformer {

    public SetTestRecipientsTransformer() {
      registerSourceType(DataType.MULE_MESSAGE);
    }

    @Override
    public Object transformMessage(MuleEvent event, Charset outputEncoding) {
      String recipients = "vm://recipient1, vm://recipient1, vm://recipient3";
      logger.debug("Setting recipients to '" + recipients + "'");

      event.setMessage(MuleMessage.builder(event.getMessage())
          .addOutboundProperty(ExpressionRecipientList.DEFAULT_SELECTOR_PROPERTY, recipients)
          .build());
      return event.getMessage();
    }

  }

}
