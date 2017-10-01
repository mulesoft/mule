/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.Charset;

@SmallTest
public class DefaultMuleEventTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expected = none();

  public static final Charset CUSTOM_ENCODING = UTF_8;
  public static final String PROPERTY_NAME = "test";
  public static final String PROPERTY_VALUE = "foo";

  private Message muleMessage = of("test-data");
  private Flow flow;
  private EventContext messageContext;
  private PrivilegedEvent muleEvent;

  @Before
  public void before() throws Exception {
    flow = getTestFlow(muleContext);
    messageContext = create(flow, TEST_CONNECTOR_LOCATION);
    muleEvent = InternalEvent.builder(messageContext).message(muleMessage).build();
  }

  @Test
  public void setFlowVariableDefaultDataType() throws Exception {
    muleEvent = (PrivilegedEvent) CoreEvent.builder(muleEvent).addVariable(PROPERTY_NAME, PROPERTY_VALUE).build();

    DataType dataType = muleEvent.getVariables().get(PROPERTY_NAME).getDataType();
    assertThat(dataType, like(String.class, MediaType.ANY, null));
  }

  @Test
  public void setFlowVariableCustomDataType() throws Exception {
    DataType dataType = DataType.builder().type(String.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();

    muleEvent = (PrivilegedEvent) CoreEvent.builder(muleEvent).addVariable(PROPERTY_NAME, PROPERTY_VALUE, dataType).build();

    DataType actualDataType = muleEvent.getVariables().get(PROPERTY_NAME).getDataType();
    assertThat(actualDataType, like(String.class, APPLICATION_XML, CUSTOM_ENCODING));
  }

  @Test
  public void setSessionVariableDefaultDataType() throws Exception {
    muleEvent.getSession().setProperty(PROPERTY_NAME, PROPERTY_VALUE);

    DataType dataType = muleEvent.getSession().getPropertyDataType(PROPERTY_NAME);
    assertThat(dataType, like(String.class, MediaType.ANY, null));
  }

  @Test
  public void setSessionVariableCustomDataType() throws Exception {
    DataType dataType = DataType.builder().type(String.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();

    muleEvent.getSession().setProperty(PROPERTY_NAME, PROPERTY_VALUE, dataType);

    DataType actualDataType = muleEvent.getSession().getPropertyDataType(PROPERTY_NAME);
    assertThat(actualDataType, like(String.class, APPLICATION_XML, CUSTOM_ENCODING));
  }

  @Test
  public void setNullMessage() throws Exception {
    expected.expect(NullPointerException.class);
    builder(messageContext).message(null);
  }

  @Test
  public void dontSetMessage() throws Exception {
    expected.expect(NullPointerException.class);
    muleEvent = (PrivilegedEvent) builder(messageContext).build();
  }
}
