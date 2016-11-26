/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal.config;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.service.scheduler.internal.config.ThreadPoolsConfig.PROP_PREFIX;
import static org.mule.service.scheduler.internal.config.ThreadPoolsConfig.loadThreadPoolsConfig;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class ThreadPoolsConfigTestCase extends AbstractMuleTestCase {

  private int cores = getRuntime().availableProcessors();

  @Rule
  public TemporaryFolder tempMuleHome = new TemporaryFolder();

  @Rule
  public ExpectedException expected = ExpectedException.none();

  private File schedulerConfigFile;

  @Before
  public void before() {
    final File confDir = new File(tempMuleHome.getRoot(), "conf");
    confDir.mkdir();
    schedulerConfigFile = new File(confDir, "scheduler-pools.conf");
    setProperty(MULE_HOME_DIRECTORY_PROPERTY, tempMuleHome.getRoot().getAbsolutePath());
  }

  @After
  public void after() {
    clearProperty(MULE_HOME_DIRECTORY_PROPERTY);
  }

  protected Properties buildDefaultConfigProps() {
    final Properties props = new Properties();
    props.setProperty(PROP_PREFIX + "gracefulShutdownTimeoutSeconds", "15");
    props.setProperty(PROP_PREFIX + "cpuLight.threadPoolSize", "2*C");
    props.setProperty(PROP_PREFIX + "io.coreThreadPoolSize", "C");
    props.setProperty(PROP_PREFIX + "io.maxThreadPoolSize", "C*C");
    props.setProperty(PROP_PREFIX + "io.threadKeepAliveSeconds", "30");
    props.setProperty(PROP_PREFIX + "cpuIntensive.threadPoolSize", "2*C");
    return props;
  }

  @Test
  public void noMuleHome() throws IOException, MuleException {
    clearProperty(MULE_HOME_DIRECTORY_PROPERTY);

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getGracefulShutdownTimeoutSeconds(), is(15));
    assertThat(config.getCpuLightPoolSize(), is(2 * cores));
    assertThat(config.getIoCorePoolSize(), is(cores));
    assertThat(config.getIoMaxPoolSize(), is(cores * cores));
    assertThat(config.getIoKeepAliveSeconds(), is(30));
    assertThat(config.getCpuIntensivePoolSize(), is(2 * cores));
  }

  @Test
  public void noConfigFile() throws IOException, MuleException {
    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getGracefulShutdownTimeoutSeconds(), is(15));
    assertThat(config.getCpuLightPoolSize(), is(2 * cores));
    assertThat(config.getIoCorePoolSize(), is(cores));
    assertThat(config.getIoMaxPoolSize(), is(cores * cores));
    assertThat(config.getIoKeepAliveSeconds(), is(30));
    assertThat(config.getCpuIntensivePoolSize(), is(2 * cores));
  }

  @Test
  public void defaultConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.store(new FileOutputStream(schedulerConfigFile), "defaultConfig");

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getGracefulShutdownTimeoutSeconds(), is(15));
    assertThat(config.getCpuLightPoolSize(), is(2 * cores));
    assertThat(config.getIoCorePoolSize(), is(cores));
    assertThat(config.getIoMaxPoolSize(), is(cores * cores));
    assertThat(config.getIoKeepAliveSeconds(), is(30));
    assertThat(config.getCpuIntensivePoolSize(), is(2 * cores));
  }

  @Test
  public void defaultConfigSpaced() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "cpuLight.threadPoolSize", "2 *C");
    props.setProperty(PROP_PREFIX + "io.maxThreadPoolSize", "C* C");
    props.setProperty(PROP_PREFIX + "cpuIntensive.threadPoolSize", "2  *   C");
    props.store(new FileOutputStream(schedulerConfigFile), "defaultConfigSpaced");

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getGracefulShutdownTimeoutSeconds(), is(15));
    assertThat(config.getCpuLightPoolSize(), is(2 * cores));
    assertThat(config.getIoCorePoolSize(), is(cores));
    assertThat(config.getIoMaxPoolSize(), is(cores * cores));
    assertThat(config.getIoKeepAliveSeconds(), is(30));
    assertThat(config.getCpuIntensivePoolSize(), is(2 * cores));
  }

  @Test
  public void withDecimalsConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "cpuLight.threadPoolSize", "0.5 *C");
    props.setProperty(PROP_PREFIX + "io.maxThreadPoolSize", "C / 0.25");
    props.store(new FileOutputStream(schedulerConfigFile), "withDecimalsConfig");

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getCpuLightPoolSize(), is(cores / 2));
    assertThat(config.getIoMaxPoolSize(), is(4 * cores));
  }

  @Test
  public void withPlusAndMinusConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "cpuLight.threadPoolSize", "C + C");
    props.setProperty(PROP_PREFIX + "io.maxThreadPoolSize", "2 + C");
    props.setProperty(PROP_PREFIX + "cpuIntensive.threadPoolSize", "C - 1");
    props.store(new FileOutputStream(schedulerConfigFile), "withPlusAndMinusConfig");

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getCpuLightPoolSize(), is(2 * cores));
    assertThat(config.getIoMaxPoolSize(), is(2 + cores));
    assertThat(config.getCpuIntensivePoolSize(), is(cores - 1));
  }

  @Test
  public void expressionConfigFixed() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "cpuLight.threadPoolSize", "2");
    props.setProperty(PROP_PREFIX + "io.maxThreadPoolSize", "8");
    props.setProperty(PROP_PREFIX + "cpuIntensive.threadPoolSize", "4");
    props.store(new FileOutputStream(schedulerConfigFile), "expressionConfigFixed");

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getCpuLightPoolSize(), is(2));
    assertThat(config.getIoMaxPoolSize(), is(8));
    assertThat(config.getCpuIntensivePoolSize(), is(4));
  }

  @Test
  public void expressionConfigNegative() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "cpuLight.threadPoolSize", "C - " + (cores + 1));
    props.store(new FileOutputStream(schedulerConfigFile), "expressionConfigNegative");

    expected.expect(DefaultMuleException.class);
    expected.expectMessage(is(PROP_PREFIX + "cpuLight.threadPoolSize: Value has to be greater than 0"));
    loadThreadPoolsConfig();
  }

  @Test
  public void invalidExpressionConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "cpuLight.threadPoolSize", "invalid");
    props.store(new FileOutputStream(schedulerConfigFile), "expressionConfigNegative");

    expected.expect(DefaultMuleException.class);
    expected.expectMessage(is(PROP_PREFIX + "cpuLight.threadPoolSize: Expression not valid"));
    loadThreadPoolsConfig();
  }

  @Test
  public void nastyExpressionConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "cpuLight.threadPoolSize", "; print('aha!')");
    props.store(new FileOutputStream(schedulerConfigFile), "expressionConfigNegative");

    expected.expect(DefaultMuleException.class);
    expected.expectMessage(is(PROP_PREFIX + "cpuLight.threadPoolSize: Expression not valid"));
    loadThreadPoolsConfig();
  }

  @Test
  public void invalidShutdownTimeConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "gracefulShutdownTimeoutSeconds", "C");
    props.store(new FileOutputStream(schedulerConfigFile), "invalidShutdownTimeConfig");

    expected.expect(DefaultMuleException.class);
    expected.expectCause(instanceOf(NumberFormatException.class));
    expected.expectMessage(is(PROP_PREFIX + "gracefulShutdownTimeoutSeconds: For input string: \"C\""));
    loadThreadPoolsConfig();
  }

  @Test
  public void invalidIoKeepAliveConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "io.threadKeepAliveSeconds", "notANumber");
    props.store(new FileOutputStream(schedulerConfigFile), "invalidIoKeepAliveConfig");

    expected.expect(DefaultMuleException.class);
    expected.expectCause(instanceOf(NumberFormatException.class));
    expected.expectMessage(is(PROP_PREFIX + "io.threadKeepAliveSeconds: For input string: \"notANumber\""));
    loadThreadPoolsConfig();
  }

  @Test
  public void negativeShutdownTimeConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "gracefulShutdownTimeoutSeconds", "-1");
    props.store(new FileOutputStream(schedulerConfigFile), "negativeShutdownTimeConfig");

    expected.expect(DefaultMuleException.class);
    expected.expectMessage(is(PROP_PREFIX + "gracefulShutdownTimeoutSeconds: Value has to be greater than 0"));
    loadThreadPoolsConfig();
  }

  @Test
  public void negativeIoKeepAliveConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "io.threadKeepAliveSeconds", "-2");
    props.store(new FileOutputStream(schedulerConfigFile), "negativeIoKeepAliveConfig");

    expected.expect(DefaultMuleException.class);
    expected.expectMessage(is(PROP_PREFIX + "io.threadKeepAliveSeconds: Value has to be greater than 0"));
    loadThreadPoolsConfig();
  }
}
