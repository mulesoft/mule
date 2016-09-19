/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.filters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class SchemaValidationTestCase extends AbstractMuleTestCase {

  private static final String SIMPLE_SCHEMA = "schema1.xsd";

  private static final String INCLUDE_SCHEMA = "schema-with-include.xsd";

  private static final String VALID_XML_FILE = "/validation1.xml";

  private static final String INVALID_XML_FILE = "/validation2.xml";

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContext muleContext;

  @Test
  public void testValidate() throws Exception {
    SchemaValidationFilter filter = new SchemaValidationFilter();
    filter.setSchemaLocations(SIMPLE_SCHEMA);
    filter.initialise();
    FlowConstruct flowConstruct = getTestFlow(muleContext);

    assertThat(filter.accept(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(getClass().getResourceAsStream(VALID_XML_FILE)))
        .build(),
                             mock(Event.Builder.class)),
               is(true));
    assertThat(filter.accept(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(getClass().getResourceAsStream(INVALID_XML_FILE)))
        .build(),
                             mock(Event.Builder.class)),
               is(false));
  }

  @Test
  public void testDefaultResourceResolverIsPresent() throws Exception {
    SchemaValidationFilter filter = new SchemaValidationFilter();
    filter.setSchemaLocations(SIMPLE_SCHEMA);
    filter.initialise();

    assertThat(filter.getResourceResolver(), is(not(nullValue())));
  }

  @Test
  public void testValidateWithIncludes() throws Exception {
    SchemaValidationFilter filter = new SchemaValidationFilter();
    filter.setSchemaLocations(INCLUDE_SCHEMA);
    filter.initialise();
    FlowConstruct flowConstruct = getTestFlow(muleContext);

    assertThat(filter.accept(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(getClass().getResourceAsStream(VALID_XML_FILE)))
        .build(),
                             mock(Event.Builder.class)),
               is(true));
    assertThat(filter.accept(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(getClass().getResourceAsStream(INVALID_XML_FILE)))
        .build(),
                             mock(Event.Builder.class)),
               is(false));
  }
}
