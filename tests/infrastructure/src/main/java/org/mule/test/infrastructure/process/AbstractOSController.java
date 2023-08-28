/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import static org.mule.test.infrastructure.process.AbstractOSController.MuleProcessStatus.STARTED_STARTED;

import static java.lang.String.format;
import static java.nio.file.FileSystems.getDefault;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.regex.Pattern.compile;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;

public abstract class AbstractOSController {

  private static final long TIMEOUT_MILLIS = SECONDS.toMillis(60);
  private static final long POLL_DELAY_MILLIS = 500;
  protected static final String START_CMD = "start";
  protected static final String STOP_CMD = "stop";
  protected static final String DUMP_CMD = "dump";
  protected static final String RESTART_CMD = "restart";
  protected static final String STATUS_CMD = "status";

  /**
   * These values represent the status returned by running `mule status` for the wrapper and Java processes respectively.
   */
  public enum MuleProcessStatus {
    STARTING_STARTING, STARTED_STARTING, STARTED_STARTED, STARTING_LAUNCH, STARTING_LAUNCHING
  }

  private static final Logger logger = getLogger(AbstractOSController.class);

  protected static final String STATUS_PID_GROUP_NAME = "pid";
  protected static final String STATUS_WRAPPER_GROUP_NAME = "wrapper";
  protected static final String STATUS_JAVA_GROUP_NAME = "java";
  protected static final String STATUS_LABELS =
      format("Mule(?:(\\sEnterprise Edition)? \\(standalone\\))? is running:\\sPID:(?<%s>[0-9]+), Wrapper:(?<%s>\\w+), Java:(?<%s>\\w+)",
             STATUS_PID_GROUP_NAME, STATUS_WRAPPER_GROUP_NAME, STATUS_JAVA_GROUP_NAME);
  protected static final Pattern STATUS_LABELS_PATTERN = compile(STATUS_LABELS);
  private static final int DEFAULT_TIMEOUT = 30000;
  private static final String JAVA_HOME_VARIABLE = "JAVA_HOME";
  private static final String SEPARATOR = getDefault().getSeparator();
  private static final Pattern JAVA_HOME_PATTERN = compile("(.*?)\\" + SEPARATOR + "bin\\" + SEPARATOR + ".*");

  private static final String MULE_HOME_VARIABLE = "MULE_HOME";
  private static final String MULE_APP_VARIABLE = "MULE_APP";
  private static final String MULE_APP_LONG_VARIABLE = "MULE_APP_LONG";
  protected static final String MULE_SERVICE_NAME = "mule";
  protected static final String MULE_EE_SERVICE_NAME = "mule_ee";

  protected final String muleHome;
  protected final String muleAppName;
  protected final String muleAppLongName;
  protected final String muleBin;
  protected final int timeout;
  protected Map<String, String> testEnvVars;

  public AbstractOSController(String muleHome, int timeout) {
    this.muleHome = muleHome;
    this.muleBin = getMuleBin();
    this.timeout = timeout != 0 ? timeout : DEFAULT_TIMEOUT;
    this.muleAppName = null;
    this.muleAppLongName = null;
  }

  public AbstractOSController(String muleHome, int timeout, String locationSuffix) {
    this.muleHome = muleHome;
    this.muleBin = getMuleBin();
    this.timeout = timeout != 0 ? timeout : DEFAULT_TIMEOUT;
    this.muleAppName = "mule_ee_node_" + locationSuffix;
    this.muleAppLongName = "MuleEnterpriseEditionNode" + locationSuffix;
  }

  public String getMuleHome() {
    return muleHome;
  }

  public abstract String getMuleBin();

  public void setTestEnvVars(Map<String, String> testEnvVars) {
    this.testEnvVars = testEnvVars;
  }

  public void start(String... args) {
    int error = runSync(START_CMD, args);
    if (error != 0) {
      throw new MuleControllerException("The mule instance couldn't be started. Errno: " + error);
    }

    AtomicReference<MuleProcessStatus> status = new AtomicReference<>();
    final Probe probe = new Probe() {

      @Override
      public boolean isSatisfied() {
        final MuleProcessStatus currentStatus = getProcessesStatus();
        status.set(currentStatus);
        return currentStatus == STARTED_STARTED;
      }

      @Override
      public String describeFailure() {
        return format("The mule instance didn't start on time: %s", status.get());
      }
    };
    if (!new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS).poll(probe)) {
      runSync(DUMP_CMD);
      throw new MuleControllerException(probe.describeFailure());
    }
  }

  public int stop(String... args) {
    return runSync(STOP_CMD, args);
  }

  public abstract int status(String... args);

  public abstract int getProcessId();

  public abstract MuleProcessStatus getProcessesStatus();

  public String getMuleAppName() {
    return muleAppName;
  }

  public void restart(String... args) {
    int error = runSync(RESTART_CMD, args);
    if (error != 0) {
      throw new MuleControllerException("The mule instance couldn't be restarted");
    }
  }

  protected int runSync(String command, String... args) {
    Map<String, String> newEnv = copyEnvironmentVariables();
    return executeSyncCommand(command, args, newEnv, timeout);
  }

  private int executeSyncCommand(String command, String[] args, Map<String, String> newEnv, int timeout) {
    CommandLine commandLine = new CommandLine(muleBin);
    commandLine.addArgument(command);
    commandLine.addArguments(args, false);
    Executor executor = new DefaultExecutor();
    ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
    executor.setWatchdog(watchdog);

    final ByteArrayOutputStream outAndErr = new ByteArrayOutputStream();
    executor.setStreamHandler(new PumpStreamHandler(outAndErr));
    return doExecution(executor, commandLine, newEnv);
  }

  protected int doExecution(Executor executor, CommandLine commandLine, Map<String, String> env) {
    final StringJoiner paramsJoiner = new StringJoiner(" ");
    for (String cmdArg : commandLine.toStrings()) {
      paramsJoiner.add(cmdArg.replaceAll("(?<=\\.password=)(.*)", "****"));
    }

    try {
      logger.info("Executing: {}", paramsJoiner);
      return executor.execute(commandLine, env);
    } catch (ExecuteException e) {
      logger.error("Error executing " + paramsJoiner);
      return e.getExitValue();
    } catch (Exception e) {
      throw new MuleControllerException("Error executing [" + commandLine.getExecutable() + " "
          + Arrays.toString(commandLine.getArguments())
          + "]", e);
    }
  }

  protected Map<String, String> copyEnvironmentVariables() {
    Map<String, String> env = System.getenv();
    Map<String, String> newEnv = new HashMap<>();
    for (Map.Entry<String, String> it : env.entrySet()) {
      newEnv.put(it.getKey(), it.getValue());
    }

    if (this.testEnvVars != null) {
      for (Map.Entry<String, String> it : this.testEnvVars.entrySet()) {
        newEnv.put(it.getKey(), it.getValue());
      }
    }

    newEnv.put(MULE_HOME_VARIABLE, muleHome);
    if (isNotEmpty(muleAppName) && isNotEmpty(muleAppLongName)) {
      newEnv.put(MULE_APP_VARIABLE, muleAppName);
      newEnv.put(MULE_APP_LONG_VARIABLE, muleAppLongName);
    }


    // Use the jvm running the tests as configured in surefire to run the Mule Runtime...
    String jvmProperty = System.getProperty("jvm");
    if (jvmProperty != null) {
      final Matcher javaHomeMatcher = JAVA_HOME_PATTERN.matcher(jvmProperty);
      if (javaHomeMatcher.matches()) {
        newEnv.put(JAVA_HOME_VARIABLE, javaHomeMatcher.group(1));
      } else {
        fail("Could not extract `JAVA_HOME` from `jvm` property:" + jvmProperty);
      }
    }

    return newEnv;
  }
}
