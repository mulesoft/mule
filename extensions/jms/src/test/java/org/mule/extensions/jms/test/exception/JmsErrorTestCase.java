/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.exception;

import static java.util.Collections.emptyMap;
import static org.mule.extensions.jms.api.exception.JmsErrors.CONSUMING;
import static org.mule.extensions.jms.api.exception.JmsErrors.ILLEGAL_BODY;
import static org.mule.extensions.jms.api.exception.JmsErrors.PUBLISHING;
import static org.mule.extensions.jms.api.exception.JmsErrors.TIMEOUT;
import org.mule.extensions.jms.api.exception.JmsConsumeException;
import org.mule.extensions.jms.api.exception.JmsPublishException;
import org.mule.extensions.jms.test.JmsAbstractTestCase;
import org.mule.functional.junit4.rules.ExpectedError;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class JmsErrorTestCase extends JmsAbstractTestCase {

  public static final String TEST_DESTINATION = "test";
  public static final String MESSAGE = "Body";
  public static final String AN_ERROR_OCCURRED_WHILE_SENDING_A_MESSAGE = "An error occurred while sending a message";
  public static final String AN_ERROR_OCCURRED_WHILE_CONSUMING_A_MESSAGE = "An error occurred while consuming a message";
  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"config/activemq/activemq-default.xml", "exception/jms-error-flow.xml"};
  }

  @Test
  public void nullMessageBody() throws Exception {
    expectedError.expectError(NAMESPACE, ILLEGAL_BODY.getType(), JmsPublishException.class,
                              AN_ERROR_OCCURRED_WHILE_SENDING_A_MESSAGE);
    destination = newDestination(TEST_DESTINATION);
    publish(null);
  }

  @Test
  public void nullDestinationPublishing() throws Exception {
    expectedError.expectError(NAMESPACE, PUBLISHING.getType(), JmsPublishException.class,
                              AN_ERROR_OCCURRED_WHILE_SENDING_A_MESSAGE);
    destination = null;
    publish(MESSAGE);
  }

  @Test
  public void nullDestinationConsuming() throws Exception {
    expectedError.expectError(NAMESPACE, CONSUMING.getType(), JmsConsumeException.class,
                              AN_ERROR_OCCURRED_WHILE_CONSUMING_A_MESSAGE);
    destination = null;
    consume();
  }

  @Test
  @Ignore
  public void timeout() throws Exception {
    expectedError.expectError(NAMESPACE, TIMEOUT.getType(), JmsConsumeException.class,
                              AN_ERROR_OCCURRED_WHILE_CONSUMING_A_MESSAGE);
    destination = newDestination(TEST_DESTINATION);
    consume(destination, emptyMap(), 5);
  }
}
