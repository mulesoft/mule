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
import static org.mule.service.scheduler.ThreadType.CPU_INTENSIVE;
import static org.mule.service.scheduler.ThreadType.CPU_LIGHT;
import static org.mule.service.scheduler.ThreadType.IO;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.scheduler.SchedulerPoolsConfig;

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
public class ContainerThreadPoolsConfig implements SchedulerPoolsConfig {


  private static final Logger logger = getLogger(ContainerThreadPoolsConfig.class);

  public static final String PROP_PREFIX = "org.mule.runtime.scheduler.";
  public static final String CPU_LIGHT_PREFIX = PROP_PREFIX + CPU_LIGHT.getName();
  public static final String IO_PREFIX = PROP_PREFIX + IO.getName();
  public static final String CPU_INTENSIVE_PREFIX = PROP_PREFIX + CPU_INTENSIVE.getName();
  public static final String THREAD_POOL = "threadPool";
  public static final String THREAD_POOL_SIZE = THREAD_POOL + ".size";
  public static final String THREAD_POOL_SIZE_MAX = THREAD_POOL + ".maxSize";
  public static final String THREAD_POOL_SIZE_CORE = THREAD_POOL + ".coreSize";
  public static final String THREAD_POOL_KEEP_ALIVE = THREAD_POOL + ".threadKeepAlive";
  public static final String WORK_QUEUE = "workQueue";
  public static final String WORK_QUEUE_SIZE = WORK_QUEUE + ".size";

  private static final String NUMBER_OR_VAR_REGEXP = "([0-9]+(\\.[0-9]+)?)|cores|mem";
  private static final Pattern POOLSIZE_PATTERN =
      compile("(" + NUMBER_OR_VAR_REGEXP + ")?(\\s*[-+\\/*\\(\\)]\\s*(" + NUMBER_OR_VAR_REGEXP + ")?)*");

  /**
   * Loads the configuration from the {@code &#123;mule.home&#125;/conf/scheduler-pools.conf} file.
   * 
   * @return The loaded configuration, or the default if the file is unavailable.
   * @throws MuleException for any trouble that happens while parsing the file.
   */
  public static ContainerThreadPoolsConfig loadThreadPoolsConfig() throws MuleException {
    File muleHome =
        getProperty(MULE_HOME_DIRECTORY_PROPERTY) != null ? new File(getProperty(MULE_HOME_DIRECTORY_PROPERTY)) : null;

    final ContainerThreadPoolsConfig config = new ContainerThreadPoolsConfig();

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
    engine.put("mem", mem);

    config.setGracefulShutdownTimeout(resolveNumber(properties, PROP_PREFIX + "gracefulShutdownTimeout"));

    config.setCpuLightPoolSize(resolveExpression(properties, CPU_LIGHT_PREFIX + "." + THREAD_POOL_SIZE, config, engine));
    config.setCpuLightQueueSize(resolveExpression(properties, CPU_LIGHT_PREFIX + "." + WORK_QUEUE_SIZE, config, engine));

    config.setIoCorePoolSize(resolveExpression(properties, IO_PREFIX + "." + THREAD_POOL_SIZE_CORE, config, engine));
    config.setIoMaxPoolSize(resolveExpression(properties, IO_PREFIX + "." + THREAD_POOL_SIZE_MAX, config, engine));
    config.setIoQueueSize(resolveExpression(properties, IO_PREFIX + "." + WORK_QUEUE_SIZE, config, engine));
    config.setIoKeepAlive(resolveNumber(properties, IO_PREFIX + "." + THREAD_POOL_KEEP_ALIVE));

    config.setCpuIntensivePoolSize(resolveExpression(properties, CPU_INTENSIVE_PREFIX + "." + THREAD_POOL_SIZE, config, engine));
    config.setCpuIntensiveQueueSize(resolveExpression(properties, CPU_INTENSIVE_PREFIX + "." + WORK_QUEUE_SIZE, config, engine));

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

  private static int resolveExpression(Properties properties, String propName, ContainerThreadPoolsConfig threadPoolsConfig,
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
  private static long mem = getRuntime().maxMemory() / 1024;

  private long gracefulShutdownTimeout = 15000;
  private int cpuLightQueueSize = 1024;
  private int cpuLightPoolSize = 2 * cores;
  private int ioQueueSize = 1024;
  private int ioCorePoolSize = cores;
  private int ioMaxPoolSize = 256;
  private long ioKeepAlive = 30000;
  private int cpuIntensiveQueueSize = 1024;
  private int cpuIntensivePoolSize = 2 * cores;

  private ContainerThreadPoolsConfig() {

  }

  @Override
  public long getGracefulShutdownTimeout() {
    return gracefulShutdownTimeout;
  }

  private void setGracefulShutdownTimeout(long gracefulShutdownTimeout) {
    this.gracefulShutdownTimeout = gracefulShutdownTimeout;
  }

  @Override
  public int getCpuLightPoolSize() {
    return cpuLightPoolSize;
  }

  private void setCpuLightPoolSize(int cpuLightPoolSize) {
    this.cpuLightPoolSize = cpuLightPoolSize;
  }

  @Override
  public int getCpuLightQueueSize() {
    return cpuLightQueueSize;
  }

  private void setCpuLightQueueSize(int cpuLightQueueSize) {
    this.cpuLightQueueSize = cpuLightQueueSize;
  }

  @Override
  public int getIoCorePoolSize() {
    return ioCorePoolSize;
  }

  private void setIoCorePoolSize(int ioCorePoolSize) {
    this.ioCorePoolSize = ioCorePoolSize;
  }

  @Override
  public int getIoMaxPoolSize() {
    return ioMaxPoolSize;
  }

  private void setIoMaxPoolSize(int ioMaxPoolSize) {
    this.ioMaxPoolSize = ioMaxPoolSize;
  }

  @Override
  public int getIoQueueSize() {
    return ioQueueSize;
  }

  private void setIoQueueSize(int ioQueueSize) {
    this.ioQueueSize = ioQueueSize;
  }

  @Override
  public long getIoKeepAlive() {
    return ioKeepAlive;
  }

  private void setIoKeepAlive(long ioKeepAlive) {
    this.ioKeepAlive = ioKeepAlive;
  }

  @Override
  public int getCpuIntensivePoolSize() {
    return cpuIntensivePoolSize;
  }

  private void setCpuIntensivePoolSize(int cpuIntensivePoolSize) {
    this.cpuIntensivePoolSize = cpuIntensivePoolSize;
  }

  @Override
  public int getCpuIntensiveQueueSize() {
    return cpuIntensiveQueueSize;
  }

  private void setCpuIntensiveQueueSize(int cpuIntensiveQueueSize) {
    this.cpuIntensiveQueueSize = cpuIntensiveQueueSize;
  }

  @Override
  public String getThreadNamePrefix() {
    return "[MuleRuntime].";
  }
}
