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
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.service.scheduler.internal.config.ThreadPoolsConfig.CPU_INTENSIVE_PREFIX;
import static org.mule.service.scheduler.internal.config.ThreadPoolsConfig.CPU_LIGHT_PREFIX;
import static org.mule.service.scheduler.internal.config.ThreadPoolsConfig.IO_PREFIX;
import static org.mule.service.scheduler.internal.config.ThreadPoolsConfig.PROP_PREFIX;
import static org.mule.service.scheduler.internal.config.ThreadPoolsConfig.THREAD_POOL_KEEP_ALIVE;
import static org.mule.service.scheduler.internal.config.ThreadPoolsConfig.THREAD_POOL_SIZE;
import static org.mule.service.scheduler.internal.config.ThreadPoolsConfig.THREAD_POOL_SIZE_CORE;
import static org.mule.service.scheduler.internal.config.ThreadPoolsConfig.THREAD_POOL_SIZE_MAX;
import static org.mule.service.scheduler.internal.config.ThreadPoolsConfig.WORK_QUEUE_SIZE;
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

  private static int cores = getRuntime().availableProcessors();
  private static long mem = getRuntime().maxMemory() / 1024;

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
    props.setProperty(PROP_PREFIX + "gracefulShutdownTimeout", "15000");
    props.setProperty(CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE, "2*cores");
    props.setProperty(CPU_LIGHT_PREFIX + "." + WORK_QUEUE_SIZE, "mem / (2*3*32)");
    props.setProperty(IO_PREFIX + "." + THREAD_POOL_SIZE_CORE, "cores");
    props.setProperty(IO_PREFIX + "." + THREAD_POOL_SIZE_MAX, "mem / (2*3*64)");
    props.setProperty(IO_PREFIX + "." + WORK_QUEUE_SIZE, "mem / (2*3*32)");
    props.setProperty(IO_PREFIX + "." + THREAD_POOL_KEEP_ALIVE, "30000");
    props.setProperty(CPU_INTENSIVE_PREFIX + "." + THREAD_POOL_SIZE, "2*cores");
    props.setProperty(CPU_INTENSIVE_PREFIX + "." + WORK_QUEUE_SIZE, "mem / (2*3*32)");
    return props;
  }

  @Test
  public void noMuleHome() throws IOException, MuleException {
    clearProperty(MULE_HOME_DIRECTORY_PROPERTY);

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getGracefulShutdownTimeout(), is(15000l));
    assertThat(config.getCpuLightPoolSize(), is(2 * cores));
    assertThat(config.getCpuLightQueueSize(), is((int) (mem / (2 * 3 * 32))));
    assertThat(config.getIoCorePoolSize(), is(cores));
    assertThat(config.getIoMaxPoolSize(), is((int) (mem / (2 * 3 * 64))));
    assertThat(config.getIoQueueSize(), is((int) (mem / (2 * 3 * 32))));
    assertThat(config.getIoKeepAlive(), is(30000l));
    assertThat(config.getCpuIntensivePoolSize(), is(2 * cores));
    assertThat(config.getCpuIntensiveQueueSize(), is((int) (mem / (2 * 3 * 32))));
  }

  @Test
  public void noConfigFile() throws IOException, MuleException {
    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getGracefulShutdownTimeout(), is(15000l));
    assertThat(config.getCpuLightPoolSize(), is(2 * cores));
    assertThat(config.getCpuLightQueueSize(), is((int) (mem / (2 * 3 * 32))));
    assertThat(config.getIoCorePoolSize(), is(cores));
    assertThat(config.getIoMaxPoolSize(), is((int) (mem / (2 * 3 * 64))));
    assertThat(config.getIoQueueSize(), is((int) (mem / (2 * 3 * 32))));
    assertThat(config.getIoKeepAlive(), is(30000l));
    assertThat(config.getCpuIntensivePoolSize(), is(2 * cores));
    assertThat(config.getCpuIntensiveQueueSize(), is((int) (mem / (2 * 3 * 32))));
  }

  @Test
  public void defaultConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.store(new FileOutputStream(schedulerConfigFile), "defaultConfig");

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getGracefulShutdownTimeout(), is(15000l));
    assertThat(config.getCpuLightPoolSize(), is(2 * cores));
    assertThat(config.getCpuLightQueueSize(), is((int) (mem / (2 * 3 * 32))));
    assertThat(config.getIoCorePoolSize(), is(cores));
    assertThat(config.getIoMaxPoolSize(), is((int) (mem / (2 * 3 * 64))));
    assertThat(config.getIoQueueSize(), is((int) (mem / (2 * 3 * 32))));
    assertThat(config.getIoKeepAlive(), is(30000l));
    assertThat(config.getCpuIntensivePoolSize(), is(2 * cores));
    assertThat(config.getCpuIntensiveQueueSize(), is((int) (mem / (2 * 3 * 32))));
  }

  @Test
  public void defaultConfigSpaced() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE_CORE, "2 *cores");
    props.setProperty(CPU_LIGHT_PREFIX + "." + WORK_QUEUE_SIZE, "mem/ (2* 3*32 )");
    props.setProperty(IO_PREFIX + "." + THREAD_POOL_SIZE_MAX, "cores* cores");
    props.setProperty(IO_PREFIX + "." + WORK_QUEUE_SIZE, "mem /( 2*3*32)");
    props.setProperty(CPU_INTENSIVE_PREFIX + "." + THREAD_POOL_SIZE_CORE, "2  *   cores");
    props.setProperty(CPU_INTENSIVE_PREFIX + "." + WORK_QUEUE_SIZE, "mem / ( 2*3*32) ");
    props.store(new FileOutputStream(schedulerConfigFile), "defaultConfigSpaced");

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getGracefulShutdownTimeout(), is(15000l));
    assertThat(config.getCpuLightPoolSize(), is(2 * cores));
    assertThat(config.getCpuLightQueueSize(), is((int) (mem / (2 * 3 * 32))));
    assertThat(config.getIoCorePoolSize(), is(cores));
    assertThat(config.getIoMaxPoolSize(), is(cores * cores));
    assertThat(config.getIoQueueSize(), is((int) (mem / (2 * 3 * 32))));
    assertThat(config.getIoKeepAlive(), is(30000l));
    assertThat(config.getCpuIntensivePoolSize(), is(2 * cores));
    assertThat(config.getCpuIntensiveQueueSize(), is((int) (mem / (2 * 3 * 32))));
  }

  @Test
  public void withDecimalsConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE, "0.5 *cores");
    props.setProperty(IO_PREFIX + "." + THREAD_POOL_SIZE_MAX, "mem / (2* 2.5 *32)");
    props.store(new FileOutputStream(schedulerConfigFile), "withDecimalsConfig");

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getCpuLightPoolSize(), is(cores / 2));
    assertThat(config.getIoMaxPoolSize(), is((int) (mem / (2 * 2.5 * 32))));
  }

  @Test
  public void withPlusAndMinusConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE, "cores + cores");
    props.setProperty(IO_PREFIX + "." + THREAD_POOL_SIZE_MAX, "2 + cores");
    props.setProperty(CPU_INTENSIVE_PREFIX + "." + THREAD_POOL_SIZE, "cores - 1");
    props.store(new FileOutputStream(schedulerConfigFile), "withPlusAndMinusConfig");

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getCpuLightPoolSize(), is(2 * cores));
    assertThat(config.getIoMaxPoolSize(), is(2 + cores));
    assertThat(config.getCpuIntensivePoolSize(), is(cores - 1));
  }

  @Test
  public void withMultiplyAndDivisionConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE, "cores * 2");
    props.setProperty(IO_PREFIX + "." + THREAD_POOL_SIZE_MAX, "cores / 0.5");
    props.store(new FileOutputStream(schedulerConfigFile), "withMultiplyAndDivisionConfig");

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getCpuLightPoolSize(), is(2 * cores));
    assertThat(config.getIoMaxPoolSize(), is(2 * cores));
  }

  @Test
  public void withParenthesisConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(CPU_LIGHT_PREFIX + "." + WORK_QUEUE_SIZE, "cores * (1+1)");
    props.setProperty(IO_PREFIX + "." + WORK_QUEUE_SIZE, "(cores + 1) * 2");
    props.store(new FileOutputStream(schedulerConfigFile), "withParenthesisConfig");

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getCpuLightQueueSize(), is(2 * cores));
    assertThat(config.getIoQueueSize(), is(2 * (1 + cores)));
  }

  @Test
  public void expressionConfigFixed() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE, "2");
    props.setProperty(IO_PREFIX + "." + THREAD_POOL_SIZE_MAX, "8");
    props.setProperty(CPU_INTENSIVE_PREFIX + "." + THREAD_POOL_SIZE, "4");
    props.store(new FileOutputStream(schedulerConfigFile), "expressionConfigFixed");

    final ThreadPoolsConfig config = loadThreadPoolsConfig();

    assertThat(config.getCpuLightPoolSize(), is(2));
    assertThat(config.getIoMaxPoolSize(), is(8));
    assertThat(config.getCpuIntensivePoolSize(), is(4));
  }

  @Test
  public void expressionConfigNegative() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE, "cores - " + (cores + 1));
    props.store(new FileOutputStream(schedulerConfigFile), "expressionConfigNegative");

    expected.expect(DefaultMuleException.class);
    expected.expectMessage(is(CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE + ": Value has to be greater than 0"));
    loadThreadPoolsConfig();
  }

  @Test
  public void invalidExpressionConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE, "invalid");
    props.store(new FileOutputStream(schedulerConfigFile), "invalidExpressionConfig");

    expected.expect(DefaultMuleException.class);
    expected.expectMessage(is(CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE + ": Expression not valid"));
    loadThreadPoolsConfig();
  }

  @Test
  public void nastyExpressionConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE, "; print('aha!')");
    props.store(new FileOutputStream(schedulerConfigFile), "nastyExpressionConfig");

    expected.expect(DefaultMuleException.class);
    expected.expectMessage(is(CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE + ": Expression not valid"));
    loadThreadPoolsConfig();
  }

  @Test
  public void invalidShutdownTimeConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "gracefulShutdownTimeout", "cores");
    props.store(new FileOutputStream(schedulerConfigFile), "invalidShutdownTimeConfig");

    expected.expect(DefaultMuleException.class);
    expected.expectCause(instanceOf(NumberFormatException.class));
    expected.expectMessage(is(PROP_PREFIX + "gracefulShutdownTimeout: For input string: \"cores\""));
    loadThreadPoolsConfig();
  }

  @Test
  public void invalidIoKeepAliveConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(IO_PREFIX + "." + THREAD_POOL_KEEP_ALIVE, "notANumber");
    props.store(new FileOutputStream(schedulerConfigFile), "invalidIoKeepAliveConfig");

    expected.expect(DefaultMuleException.class);
    expected.expectCause(instanceOf(NumberFormatException.class));
    expected.expectMessage(is(IO_PREFIX + "." + THREAD_POOL_KEEP_ALIVE + ": For input string: \"notANumber\""));
    loadThreadPoolsConfig();
  }

  @Test
  public void negativeShutdownTimeConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(PROP_PREFIX + "gracefulShutdownTimeout", "-1");
    props.store(new FileOutputStream(schedulerConfigFile), "negativeShutdownTimeConfig");

    expected.expect(DefaultMuleException.class);
    expected.expectMessage(is(PROP_PREFIX + "gracefulShutdownTimeout: Value has to be greater than 0"));
    loadThreadPoolsConfig();
  }

  @Test
  public void negativeIoKeepAliveConfig() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(IO_PREFIX + "." + THREAD_POOL_KEEP_ALIVE, "-2");
    props.store(new FileOutputStream(schedulerConfigFile), "negativeIoKeepAliveConfig");

    expected.expect(DefaultMuleException.class);
    expected.expectMessage(is(IO_PREFIX + "." + THREAD_POOL_KEEP_ALIVE + ": Value has to be greater than 0"));
    loadThreadPoolsConfig();
  }

  @Test
  public void unevenParenthesis() throws IOException, MuleException {
    final Properties props = buildDefaultConfigProps();
    props.setProperty(IO_PREFIX + "." + WORK_QUEUE_SIZE, "(-2");
    props.store(new FileOutputStream(schedulerConfigFile), "unevenParenthesis");

    expected.expect(DefaultMuleException.class);
    expected.expectMessage(startsWith(IO_PREFIX + "." + WORK_QUEUE_SIZE + ": <eval>:1:3 Expected ) but found eof"));
    loadThreadPoolsConfig();
  }
}
