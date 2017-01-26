/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.config.builders;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.builders.AutoConfigurationBuilder;
import org.mule.runtime.core.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class AutoConfigurationBuilderTestCase extends AbstractMuleTestCase {

  @Test
  public void testConfigureSpring() throws ConfigurationException, InitialisationException {
    MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
    MuleContext muleContext = muleContextFactory.createMuleContext(asList(new TestServicesConfigurationBuilder(),
                                                                          new SimpleConfigurationBuilder(null),
                                                                          new AutoConfigurationBuilder("org/mule/test/spring/config1/test-xml-mule2-config.xml",
                                                                                                       emptyMap(), APP)),
                                                                   new DefaultMuleContextBuilder());

    // Just a few of the asserts from AbstractConfigBuilderTestCase
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("appleComponent");
    assertNotNull(flow.getExceptionListener());
    assertTrue(flow.getExceptionListener() instanceof MessagingExceptionHandler);
  }

}
