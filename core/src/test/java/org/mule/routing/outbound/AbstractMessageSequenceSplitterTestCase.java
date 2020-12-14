/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.routing.MessageSequence;
import org.mule.tck.junit4.rule.SystemProperty;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractMessageSequenceSplitterTestCase {

  @ClassRule
  public static SystemProperty compoundCorrelationIdDisable = new SystemProperty(MuleProperties.MULE_DISABLE_COMPOUND_CORRELATION_ID, "false");


  private AbstractMessageSequenceSplitter abstractMessageSequenceSplitter;
  private MuleMessage message;
  private boolean isSequential = false;

  @Before
  public void setUp(){
    Object someObject = "Hello";

    MuleContext muleContext = mock(MuleContext.class);

    MuleConfiguration configuration = mock(MuleConfiguration.class);
    when(configuration.getDefaultEncoding()).thenReturn("UTF-8");

    when(muleContext.getConfiguration()).thenReturn(configuration);

    message = new DefaultMuleMessage(someObject, muleContext) {};

    abstractMessageSequenceSplitter = new AbstractMessageSequenceSplitter() {

      @Override
      protected MessageSequence<?> splitMessageIntoSequence(MuleEvent event) throws MuleException {
        return null;
      }

      @Override
      protected boolean isSequential() {
        return isSequential;
      }
    };
  }

  @Test
  public void testSetMessageCorrelationIdIsSequentialSetsMessageCorrelationIdAsIs_WhenCompoundCorrelationIdIsNotDisabled(){
    //Given
    isSequential = true;
    String correlationId = "Jack";
    int correlationSequence = 1;

    // When
    abstractMessageSequenceSplitter.setMessageCorrelationId(message, correlationId, correlationSequence);

    //Then
    assertThat("Correlation Id is not as expected", message.getCorrelationId(), is(correlationId));
  }

  @Test
  public void testSetMessageCorrelationIdIsNotSequentialSetsMessageCorrelationIdAsIs_WhenCompoundCorrelationIdIsNotDisabled(){
    //Given
    isSequential = false;
    String correlationId = "Jack";
    int correlationSequence = 1;

    // When
    abstractMessageSequenceSplitter.setMessageCorrelationId(message, correlationId, correlationSequence);

    //Then
    assertThat("Correlation Id is not as expected", message.getCorrelationId(), is(correlationId));
  }

}