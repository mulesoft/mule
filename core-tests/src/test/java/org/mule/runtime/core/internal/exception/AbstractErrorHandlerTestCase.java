/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.exception.MuleException.MULE_VERBOSE_EXCEPTIONS;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public abstract class AbstractErrorHandlerTestCase extends AbstractMuleContextTestCase {

  @Rule
  public SystemProperty verbose;

  public AbstractErrorHandlerTestCase(SystemProperty verbose) {
    this.verbose = verbose;
  }

  @Parameters
  public static Collection<Object> data() {
    return asList(new SystemProperty(MULE_VERBOSE_EXCEPTIONS, TRUE.toString()),
                  new SystemProperty(MULE_VERBOSE_EXCEPTIONS, FALSE.toString()));
  }

  protected MessagingException mockException = mock(MessagingException.class);

  protected Flow flow;

  protected EventContext context;

  protected CoreEvent muleEvent;

  @Before
  public void before() throws Exception {
    flow = getTestFlow(muleContext);
    flow.initialise();

    context = create(flow, TEST_CONNECTOR_LOCATION);
    muleEvent = InternalEvent.builder(context).message(of("")).build();
  }
}
