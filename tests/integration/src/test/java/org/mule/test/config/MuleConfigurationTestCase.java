/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class MuleConfigurationTestCase extends AbstractMuleTestCase {

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  private boolean failOnMessageScribbling;
  protected String workingDirectory = "target";
  private MuleContext muleContext;

  @After
  public void tearDown() throws Exception {
    muleContext.dispose();
    muleContext = null;
  }

  /** Test for MULE-3092 */
  @Test
  public void testConfigureProgramatically() throws Exception {
    DefaultMuleConfiguration config = new DefaultMuleConfiguration();
    config.setDefaultEncoding("UTF-16");
    config.setDefaultSynchronousEndpoints(true);
    config.setSystemModelType("direct");
    config.setDefaultResponseTimeout(30000);
    config.setDefaultTransactionTimeout(60000);
    config.setWorkingDirectory(workingDirectory);
    config.setClientMode(true);
    config.setId("MY_SERVER");
    config.setDomainId("MY_DOMAIN");
    config.setCacheMessageAsBytes(false);
    config.setEnableStreaming(false);
    config.setMaxQueueTransactionFilesSize(500);
    config.setAutoWrapMessageAwareTransform(false);

    MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
    contextBuilder.setMuleConfiguration(config);
    muleContext = new DefaultMuleContextFactory()
        .createMuleContext(asList(testServicesConfigurationBuilder, new DefaultsConfigurationBuilder()), contextBuilder);
    muleContext.start();

    verifyConfiguration();
  }

  /** Test for MULE-3092 */
  @Test
  public void testConfigureWithSystemProperties() throws Exception {
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "encoding", "UTF-16");
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "endpoints.synchronous", "true");
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "systemModelType", "direct");
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "timeout.synchronous", "30000");
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "timeout.transaction", "60000");
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "remoteSync", "true");
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "workingDirectory", workingDirectory);
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "clientMode", "true");

    System.setProperty(SYSTEM_PROPERTY_PREFIX + "serverId", "MY_SERVER");
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "domainId", "MY_DOMAIN");
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheBytes", "false");
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheOriginal", "false");
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "streaming.enable", "false");
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "message.assertAccess", "false");
    System.setProperty(SYSTEM_PROPERTY_PREFIX + "transform.autoWrap", "false");

    muleContext = new DefaultMuleContextFactory()
        .createMuleContext(testServicesConfigurationBuilder, new DefaultsConfigurationBuilder());
    muleContext.start();

    verifyConfiguration();

    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "encoding");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "endpoints.synchronous");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "systemModelType");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "timeout.synchronous");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "timeout.transaction");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "remoteSync");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "workingDirectory");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "clientMode");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "disable.threadsafemessages");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "serverId");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "domainId");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheBytes");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheOriginal");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "streaming.enable");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "message.assertAccess");
    System.clearProperty(SYSTEM_PROPERTY_PREFIX + "transform.autoWrap");
  }

  /** Test for MULE-3110 */
  @Test
  public void testConfigureAfterInitFails() throws Exception {
    muleContext = new DefaultMuleContextFactory()
        .createMuleContext(testServicesConfigurationBuilder, new DefaultsConfigurationBuilder());

    DefaultMuleConfiguration mutableConfig = ((DefaultMuleConfiguration) muleContext.getConfiguration());

    // These are OK to change after init but before start
    mutableConfig.setDefaultSynchronousEndpoints(true);
    mutableConfig.setSystemModelType("direct");
    mutableConfig.setClientMode(true);

    // These are not OK to change after init
    mutableConfig.setDefaultEncoding("UTF-16");
    mutableConfig.setWorkingDirectory(workingDirectory);
    mutableConfig.setId("MY_SERVER");
    mutableConfig.setDomainId("MY_DOMAIN");

    MuleConfiguration config = muleContext.getConfiguration();

    // These are OK to change after init but before start
    assertEquals("direct", config.getSystemModelType());
    assertTrue(config.isClientMode());

    // These are not OK to change after init
    assertFalse("UTF-16".equals(config.getDefaultEncoding()));
    assertFalse(workingDirectory.equals(config.getWorkingDirectory()));
    assertFalse("MY_SERVER".equals(config.getId()));
    assertFalse("MY_DOMAIN".equals(config.getDomainId()));
  }

  /** Test for MULE-3110 */
  @Test
  public void testConfigureAfterStartFails() throws Exception {
    muleContext = new DefaultMuleContextFactory().createMuleContext(testServicesConfigurationBuilder,
                                                                    new DefaultsConfigurationBuilder());
    muleContext.start();

    DefaultMuleConfiguration mutableConfig = ((DefaultMuleConfiguration) muleContext.getConfiguration());
    mutableConfig.setDefaultSynchronousEndpoints(true);
    mutableConfig.setSystemModelType("direct");
    mutableConfig.setClientMode(true);

    MuleConfiguration config = muleContext.getConfiguration();
    assertFalse("direct".equals(config.getSystemModelType()));
    assertFalse(config.isClientMode());
  }

  protected void verifyConfiguration() {
    MuleConfiguration config = muleContext.getConfiguration();
    assertEquals("UTF-16", config.getDefaultEncoding());
    assertEquals("direct", config.getSystemModelType());
    // on windows this ends up with a c:/ in it
    assertTrue(config.getWorkingDirectory().indexOf(workingDirectory) != -1);
    assertTrue(config.isClientMode());
    assertEquals("MY_SERVER", config.getId());
    assertEquals("MY_DOMAIN", config.getDomainId());
    assertFalse(config.isCacheMessageAsBytes());
    assertFalse(config.isEnableStreaming());
    assertFalse(config.isAutoWrapMessageAwareTransform());
    assertThat(config.getMaxQueueTransactionFilesSizeInMegabytes(), is(500));
  }
}


