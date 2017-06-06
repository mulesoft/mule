/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.util.UUID;
import org.mule.tck.junit4.rule.FreePortFinder;
import org.mule.test.infrastructure.deployment.FakeMuleServer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleContextProcessBuilder implements Cloneable, ConfigurableProcessBuilder {

  protected transient final static Logger logger = LoggerFactory.getLogger(MuleContextProcessBuilder.class);

  public static final String MULE_CONTEXT_CONFIGURATION_ID_KEY = MuleProperties.SYSTEM_PROPERTY_PREFIX + "serverId";
  public static final String CONFIG_FILE_KEY = "configFile";
  public static final String TIMEOUT_IN_SECONDS = "processTimeout";
  public static final String LOG_PORT_PROPERTY = "test.log.port";
  public static final String COMMAND_PORT_PROPERTY = "test.command.port";
  public static final String MULE_CORE_EXTENSIONS_PROPERTY = "test.mule.coreextension";
  public static final int DEFAULT_DEBUG_PORT = 5005;

  private final File testDirectory;
  private String muleAppClass;
  private String instanceId = "unknown";
  private Map<String, String> systemProperties = new HashMap<String, String>();
  private FreePortFinder freePortFinder = new FreePortFinder(8000, 60000);
  private List<ProcessBuilderConfigurer> processBuilderConfigurers = new ArrayList<ProcessBuilderConfigurer>();

  public MuleContextProcessBuilder(File testDirectory) {
    this.testDirectory = testDirectory;
    systemProperties.put(MULE_CONTEXT_CONFIGURATION_ID_KEY, UUID.getUUID());
    systemProperties.put(TIMEOUT_IN_SECONDS, "120");
    systemProperties.put(FakeMuleServer.FAKE_SERVER_DISABLE_LOG_REPOSITORY_SELECTOR, "true");
  }

  public MuleContextProcessBuilder(MuleContextProcessBuilder clusteredMuleContextProcessBuilder) {
    this.testDirectory = clusteredMuleContextProcessBuilder.testDirectory;
    this.muleAppClass = clusteredMuleContextProcessBuilder.muleAppClass;
    this.systemProperties = new HashMap<String, String>(clusteredMuleContextProcessBuilder.systemProperties);
  }

  public MuleContextProcessBuilder setApplicationConfigFile(String appConfigFile) {
    this.systemProperties.put("configFile", appConfigFile);
    return this;
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return new MuleContextProcessBuilder(this);
  }

  public TestProcess buildInstance(int instanceId) {
    MuleContextProcessBuilder clusteredMuleContextProcessBuilder = new MuleContextProcessBuilder(this);
    clusteredMuleContextProcessBuilder.instanceId = String.valueOf(instanceId);
    clusteredMuleContextProcessBuilder.addConfigurationAttribute(MULE_CONTEXT_CONFIGURATION_ID_KEY,
                                                                 getMuleContextConfigurationId());
    clusteredMuleContextProcessBuilder.addConfigurationAttribute(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY,
                                                                 getMuleHomeDirectoryFor(instanceId));
    clusteredMuleContextProcessBuilder.addConfigurationAttribute(LOG_PORT_PROPERTY, freePortFinder.find().toString());
    clusteredMuleContextProcessBuilder.addConfigurationAttribute(COMMAND_PORT_PROPERTY, freePortFinder.find().toString());
    for (ProcessBuilderConfigurer processBuilderConfigurer : processBuilderConfigurers) {
      processBuilderConfigurer.configure(clusteredMuleContextProcessBuilder.instanceId, clusteredMuleContextProcessBuilder);
    }
    return clusteredMuleContextProcessBuilder.build();
  }

  public String getMuleHomeDirectoryFor(int instanceId) {
    return testDirectory.getAbsolutePath() + File.separator + "mule-home-" + instanceId;
  }

  private TestProcess build() {
    List<String> command = new ArrayList<String>();
    command.add("java");
    command.add("-cp");
    command.add(System.getProperty("java.class.path"));
    if (Boolean.getBoolean("debug")) {
      command.add("-Xdebug");
      int debugPort = getDebugPort();
      command.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + debugPort);
      logger.info(String.format("To connect to process %s for debugging use port %d", instanceId, debugPort));
    }
    for (String systemPropertyKey : systemProperties.keySet()) {
      command.add(String.format("-D%s=%s", systemPropertyKey, systemProperties.get(systemPropertyKey)));
    }
    if (muleAppClass == null) {
      command.add("org.mule.test.infrastructure.process.MuleContextProcessApplication");
    } else {
      command.add(muleAppClass);
    }
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    try {
      TestProcess testProcess = new TestProcess(instanceId, Integer.valueOf(systemProperties.get(LOG_PORT_PROPERTY)),
                                                Integer.valueOf(systemProperties.get(COMMAND_PORT_PROPERTY)));
      Process process = processBuilder.start();
      testProcess.setProcess(process);
      return testProcess;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private int getDebugPort() {
    return (DEFAULT_DEBUG_PORT + Integer.valueOf(instanceId));
  }

  public String getMuleContextConfigurationId() {
    return systemProperties.get(MULE_CONTEXT_CONFIGURATION_ID_KEY);
  }

  @Override
  public ConfigurableProcessBuilder addConfigurationAttribute(String propertyName, String propertyValue) {
    this.systemProperties.put(propertyName, propertyValue);
    return this;
  }

  public MuleContextProcessBuilder addProcessBuilderConfigurer(ProcessBuilderConfigurer processBuilderConfigurer) {
    processBuilderConfigurers.add(processBuilderConfigurer);
    return this;
  }

}
