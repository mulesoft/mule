/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.runtime.core.routing.ScatterGatherRouter;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.yourkit.util.Asserts;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ScatterGatherOneRouteTestCase extends AbstractMuleTestCase {

  private MuleContext context;

  @Before
  public void before() throws InitialisationException, ConfigurationException {
    context = mockContextWithServices();
  }

  @After
  public void after() {
    if (context != null) {
      context.dispose();
    }
  }

  // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
  @Test(expected = ConfigurationException.class)
  public void oneRouteOnXml() throws Exception {
    new DefaultMuleContextFactory().createMuleContext(new SpringXmlConfigurationBuilder("scatter-gather-one-route-test.xml"));
  }

  @Test(expected = InitialisationException.class)
  public void oneRouteProgramatically() throws Exception {
    ScatterGatherRouter sc = new ScatterGatherRouter();
    sc.setRoutes(Collections.<Processor>emptyList());

    try {
      sc.initialise();
    } catch (InitialisationException e) {
      Asserts.assertTrue(e.getCause() instanceof IllegalStateException);
      throw e;
    }
  }
}
