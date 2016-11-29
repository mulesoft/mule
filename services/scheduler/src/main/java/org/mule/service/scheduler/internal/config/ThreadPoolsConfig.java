/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal.config;

import static java.io.File.separator;
import static java.lang.Long.parseLong;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.util.regex.Pattern.compile;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.scheduler.ThreadType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.scheduler.ThreadType.CPU_LIGHT;
import static org.mule.runtime.core.api.scheduler.ThreadType.IO;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.DefaultMuleException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;

/**
 * Bean that contains the thread pools configuration for the runtime.
 *
 * @since 4.0
 */
public class ThreadPoolsConfig {


  private static final Logger logger = getLogger(ThreadPoolsConfig.class);

  public static final String PROP_PREFIX = "org.mule.runtime.scheduler.";
  public static final String THREAD_POOL_SIZE = "threadPoolSize";
  public static final String THREAD_POOL_SIZE_MAX = THREAD_POOL_SIZE + ".max";
  public static final String THREAD_POOL_SIZE_CORE = THREAD_POOL_SIZE + ".core";

  private static final String NUMBER_OR_VAR_REGEXP = "([0-9]+(\\.[0-9]+)?)|cores";
  private static final Pattern POOLSIZE_PATTERN =
      compile("(" + NUMBER_OR_VAR_REGEXP + ")?(\\s*[-+\\/*]\\s*(" + NUMBER_OR_VAR_REGEXP + ")?)*");

  /**
   * Loads the configuration from the {@code &#123;mule.home&#125;/conf/scheduler-pools.conf} file.
   * 
   * @return The loaded configuration, or the default if the file is unavailable.
   * @throws MuleException for any trouble that happens while parsing the file.
   */
  public static ThreadPoolsConfig loadThreadPoolsConfig() throws MuleException {
    File muleHome =
        getProperty(MULE_HOME_DIRECTORY_PROPERTY) != null ? new File(getProperty(MULE_HOME_DIRECTORY_PROPERTY)) : null;

    final ThreadPoolsConfig config = new ThreadPoolsConfig();

    if (muleHome == null) {
      logger.info("No " + MULE_HOME_DIRECTORY_PROPERTY + " defined. Using default values for thread pools.");
      return config;
    }

    File defaultConfigFile = new File(muleHome, "conf" + separator + "scheduler-pools.conf");
    if (!defaultConfigFile.exists()) {
      logger.info("No thread pools config file found. Using default values.");
      return config;
    }

    logger.info("Loading thread pools configuration from " + defaultConfigFile.getPath());

    final Properties properties = new Properties();
    try (final FileInputStream configIs = new FileInputStream(defaultConfigFile)) {
      properties.load(configIs);
    } catch (IOException e) {
      throw new DefaultMuleException(e);
    }

    ScriptEngineManager manager = new ScriptEngineManager();
    ScriptEngine engine = manager.getEngineByName("js");
    engine.put("cores", cores);

    config.setGracefulShutdownTimeout(resolveNumber(properties, PROP_PREFIX + "gracefulShutdownTimeout"));
    config.setCpuLightPoolSize(resolveExpression(properties, PROP_PREFIX + CPU_LIGHT.getName() + "." + THREAD_POOL_SIZE_CORE,
                                                 config, engine));
    config.setIoCorePoolSize(resolveExpression(properties, PROP_PREFIX + IO.getName() + "." + THREAD_POOL_SIZE_CORE, config,
                                               engine));
    config
        .setIoMaxPoolSize(resolveExpression(properties, PROP_PREFIX + IO.getName() + "." + THREAD_POOL_SIZE_MAX, config, engine));
    config.setIoKeepAlive(resolveNumber(properties, PROP_PREFIX + IO.getName() + ".threadKeepAlive"));
    config.setCpuIntensivePoolSize(resolveExpression(properties,
                                                     PROP_PREFIX + CPU_INTENSIVE.getName() + "." + THREAD_POOL_SIZE_CORE, config,
                                                     engine));

    return config;
  }

  private static long resolveNumber(Properties properties, String propName) throws DefaultMuleException {
    final String property = properties.getProperty(propName);
    try {
      final long result = parseLong(property);
      if (result <= 0) {
        throw new DefaultMuleException(propName + ": Value has to be greater than 0");
      }

      return result;
    } catch (NumberFormatException e) {
      throw new DefaultMuleException(propName + ": " + e.getMessage(), e);
    }
  }

  private static int resolveExpression(Properties properties, String propName, ThreadPoolsConfig threadPoolsConfig,
                                       ScriptEngine engine)
      throws DefaultMuleException {
    final String property = properties.getProperty(propName).trim().toLowerCase();
    if (!POOLSIZE_PATTERN.matcher(property).matches()) {
      throw new DefaultMuleException(propName + ": Expression not valid");
    }
    try {
      final int result = ((Number) engine.eval(property)).intValue();
      if (result <= 0) {
        throw new DefaultMuleException(propName + ": Value has to be greater than 0");
      }

      return result;
    } catch (ScriptException e) {
      throw new DefaultMuleException(propName + ": " + e.getMessage(), e);
    }
  }

  private static int cores = getRuntime().availableProcessors();

  private long gracefulShutdownTimeout = 15000;
  private int cpuLightPoolSize = 2 * cores;
  private int ioCorePoolSize = cores;
  private int ioMaxPoolSize = cores * cores;
  private long ioKeepAlive = 30000;
  private int cpuIntensivePoolSize = 2 * cores;

  private ThreadPoolsConfig() {

  }

  /**
   * @return the maximum time (in milliseconds) to wait until all tasks in all the runtime thread pools have completed execution
   *         when stopping the scheduler service.
   */
  public long getGracefulShutdownTimeout() {
    return gracefulShutdownTimeout;
  }

  private void setGracefulShutdownTimeout(long gracefulShutdownTimeout) {
    this.gracefulShutdownTimeout = gracefulShutdownTimeout;
  }

  /**
   * @return the number of threads to keep in the {@code cpu_lite} pool, even if they are idle.
   */
  public int getCpuLightPoolSize() {
    return cpuLightPoolSize;
  }

  private void setCpuLightPoolSize(int cpuLightPoolSize) {
    this.cpuLightPoolSize = cpuLightPoolSize;
  }

  /**
   * @return the number of threads to keep in the {@code I/O} pool.
   */
  public int getIoCorePoolSize() {
    return ioCorePoolSize;
  }

  private void setIoCorePoolSize(int ioCorePoolSize) {
    this.ioCorePoolSize = ioCorePoolSize;
  }

  /**
   * @return the maximum number of threads to allow in the {@code I/O} pool.
   */
  public int getIoMaxPoolSize() {
    return ioMaxPoolSize;
  }

  private void setIoMaxPoolSize(int ioMaxPoolSize) {
    this.ioMaxPoolSize = ioMaxPoolSize;
  }

  /**
   * @return when the number of threads in the {@code I/O} pool is greater than {@link #getIoCorePoolSize()}, this is the maximum
   *         time (in milliseconds) that excess idle threads will wait for new tasks before terminating.
   */
  public long getIoKeepAlive() {
    return ioKeepAlive;
  }

  private void setIoKeepAlive(long ioKeepAlive) {
    this.ioKeepAlive = ioKeepAlive;
  }

  /**
   * @return the number of threads to keep in the {@code cpu_intensive} pool, even if they are idle.
   */
  public int getCpuIntensivePoolSize() {
    return cpuIntensivePoolSize;
  }

  private void setCpuIntensivePoolSize(int cpuIntensivePoolSize) {
    this.cpuIntensivePoolSize = cpuIntensivePoolSize;
  }

  public Properties defaultQuartzProperties(String name) {
    Properties factoryProperties = new Properties();

    factoryProperties.setProperty("org.quartz.scheduler.instanceName", name);
    factoryProperties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
    factoryProperties.setProperty("org.quartz.threadPool.threadNamePrefix", name + "_qz");
    factoryProperties.setProperty("org.quartz.threadPool.threadCount", "1");
    return factoryProperties;
  }

}
