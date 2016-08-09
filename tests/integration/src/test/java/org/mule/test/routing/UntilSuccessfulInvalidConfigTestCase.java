/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.After;
import org.junit.Test;

public class UntilSuccessfulInvalidConfigTestCase extends AbstractMuleTestCase {

  private MuleContext context;

  @After
  public void after() {
    if (context != null) {
      context.dispose();
    }
  }

  // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
  @Test(expected = InitialisationException.class)
  public void exclusiveWaitConfig() throws Exception {
    context = new DefaultMuleContextFactory()
        .createMuleContext(new SpringXmlConfigurationBuilder("until-successful-invalid-wait-test.xml"));
  }
}
