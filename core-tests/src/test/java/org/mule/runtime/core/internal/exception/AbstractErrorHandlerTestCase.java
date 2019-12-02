/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.tck.MuleTestUtils.OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.VerboseExceptions;

import java.util.Collection;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public abstract class AbstractErrorHandlerTestCase extends AbstractMuleContextTestCase {

  @Rule
  public VerboseExceptions verbose;

  public AbstractErrorHandlerTestCase(VerboseExceptions verbose) {
    this.verbose = verbose;
  }

  @Parameters
  public static Collection<Object> data() {
    return asList(new VerboseExceptions(true),
                  new VerboseExceptions(false));
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

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY, createDefaultErrorTypeRepository());
  }

  @After
  public void after() {
    flow.dispose();
  }
}
