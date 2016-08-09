/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.lifecycle.AbstractLifecycleTracker;

import org.junit.Test;

/**
 * @author David Dossot (david@dossot.net) See http://mule.mulesoft.org/jira/browse/MULE-3846
 */
public class LifecycleTrackerComponentFunctionalTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/components/component-lifecycle-config-flow.xml";
  }

  /**
   * ASSERT: - Mule stop/start lifecycle methods invoked - Mule initialize/dipose lifecycle methods NOT invoked - Spring lifecycle
   * methods invoked - Service and muleContext injected (Component implements ServiceAware/MuleContextAware) NOTE: muleContext is
   * injected twice, once by registry and once by lifecycleAdaptor
   *
   * @throws Exception
   */
  @Test
  public void testSpringBeanServiceLifecycle() throws Exception {
    // TODO(pablo.kraan): MERGE - is this correct to have initialise and setFlowConstruct as expected phases?
    String expectedLifeCycle =
        "[setProperty, setMuleContext, springInitialize, initialise, setFlowConstruct, start, stop, dispose, springDestroy]";

    testComponentLifecycle("SpringBeanService", expectedLifeCycle);
  }

  /**
   * ASSERT: - Mule stop/start lifecycle methods invoked - Mule initialize/dipose lifecycle methods NOT invoked - Spring lifecycle
   * methods NOT invoked - Service and muleContext injected (Component implements ServiceAware/MuleContextAware) NOTE: muleContext
   * is injected twice, once by registry and once by lifecycleAdaptor
   *
   * @throws Exception
   */
  @Test
  public void testSpringBeanService2Lifecycle() throws Exception {
    // TODO(pablo.kraan): MERGE - is this correct to have initialise and setFlowConstruct as expected phases?
    String expectedLifeCycle = "[setProperty, setMuleContext, initialise, setFlowConstruct, start, stop, dispose]";

    testComponentLifecycle("SpringBeanService2", expectedLifeCycle);
  }

  /**
   * ASSERT: - Mule lifecycle methods invoked - Service and muleContext injected (Component implements
   * ServiceAware/MuleContextAware)
   *
   * @throws Exception
   */
  @Test
  public void testSingletonServiceLifecycle() throws Exception {
    String expectedLifeCycle = "[setProperty, setFlowConstruct, setMuleContext, initialise, start, stop, dispose]";

    testComponentLifecycle("MuleSingletonService", expectedLifeCycle);
  }

  /**
   * ASSERT: - Mule lifecycle methods invoked - Service and muleContext injected (Component implements
   * ServiceAware/MuleContextAware)
   *
   * @throws Exception
   */
  @Test
  public void testMulePrototypeServiceLifecycle() throws Exception {
    String expectedLifeCycle = "[setProperty, setFlowConstruct, setMuleContext, initialise, start, stop, dispose]";

    testComponentLifecycle("MulePrototypeService", expectedLifeCycle);
  }

  /**
   * ASSERT: - Mule lifecycle methods invoked - Service and muleContext injected (Component implements
   * ServiceAware/MuleContextAware)
   *
   * @throws Exception
   */
  @Test
  public void testMulePooledPrototypeServiceLifecycle() throws Exception {
    String expectedLifeCycle = "[setProperty, setFlowConstruct, setMuleContext, initialise, start, stop, dispose]";

    testComponentLifecycle("MulePooledPrototypeService", expectedLifeCycle);
  }

  /**
   * ASSERT: - Mule lifecycle methods invoked each time singleton is used to create new object in pool - Service and muleContext
   * injected each time singleton is used to create new object in pool (Component implements ServiceAware/MuleContextAware)
   *
   * @throws Exception
   */
  @Test
  public void testMulePooledSingletonServiceLifecycle() throws Exception {
    String expectedLifeCycle =
        "[setProperty, setFlowConstruct, setMuleContext, initialise, initialise, initialise, start, start, start, stop, stop, stop, dispose, dispose, dispose]";

    testComponentLifecycle("MulePooledSingletonService", expectedLifeCycle);
  }

  private void testComponentLifecycle(final String serviceName, final String expectedLifeCycle) throws Exception {

    final AbstractLifecycleTracker tracker = exerciseComponent(serviceName);

    muleContext.dispose();

    assertEquals(serviceName, expectedLifeCycle, tracker.getTracker().toString());
  }

  private AbstractLifecycleTracker exerciseComponent(final String serviceName) throws Exception {
    final MuleMessage message = flowRunner(serviceName).run().getMessage();
    final AbstractLifecycleTracker ltc = (AbstractLifecycleTracker) message.getPayload();

    assertNotNull(ltc);

    return ltc;
  }
}
