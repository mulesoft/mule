/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import static org.mule.test.infrastructure.process.AbstractOSController.MuleProcessStatus.STARTED_STARTED;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.regex.Pattern.compile;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
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
      format("Mule(?:(\\sEnterprise Edition)? \\([^\\)]+\\))? is running:\\sPID:(?<%s>[0-9]+), Wrapper:(?<%s>\\w+), Java:(?<%s>\\w+)",
             STATUS_PID_GROUP_NAME, STATUS_WRAPPER_GROUP_NAME, STATUS_JAVA_GROUP_NAME);
  protected static final Pattern STATUS_LABELS_PATTERN = compile(STATUS_LABELS);
  private static final int DEFAULT_TIMEOUT = 30000;

  private static final String MULE_HOME_VARIABLE = "MULE_HOME";
  private static final String MULE_APP_VARIABLE = "MULE_APP";
  private static final String MULE_APP_LONG_VARIABLE = "MULE_APP_LONG";
  protected static final String MULE_SERVICE_NAME = "mule";
  protected static final String MULE_EE_SERVICE_NAME = "mule_ee";

  protected final String muleHome;
  protected final String muleAppName;
  protected final String muleAppLongName;
  protected final String muleBin;

  private final String amcSetup;

  protected final int timeout;
  protected Map<String, String> testEnvVars;

  public AbstractOSController(String muleHome, int timeout) {
    this.muleHome = muleHome;
    this.muleBin = getMuleBin();
    this.amcSetup = getAmcSetupBin();
    this.timeout = timeout != 0 ? timeout : DEFAULT_TIMEOUT;
    this.muleAppName = null;
    this.muleAppLongName = null;
  }

  protected abstract String getAmcSetupBin();

  public AbstractOSController(String muleHome, int timeout, String locationSuffix) {
    this.muleHome = muleHome;
    this.muleBin = getMuleBin();
    this.amcSetup = getAmcSetupBin();
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

  public int installAgent(String... args) {
    CommandLine commandLine = new CommandLine(amcSetup);
    commandLine.addArguments(args, false);
    File workingDir = new File(amcSetup).getParentFile();
    return runSync(commandLine, null, workingDir);
  }

  protected int runSync(String command, String... args) {
    CommandLine commandLine = new CommandLine(muleBin);
    commandLine.addArgument(command);
    commandLine.addArguments(args, false);
    return runSync(commandLine, null);
  }

  protected int runSync(CommandLine commandLine, ExecuteStreamHandler streamHandler) {
    return runSync(commandLine, streamHandler, null);
  }

  private int runSync(CommandLine commandLine, ExecuteStreamHandler streamHandler, File workingDirectory) {
    Map<String, String> newEnv = copyEnvironmentVariables();
    return executeSyncCommand(commandLine, newEnv, streamHandler, timeout, workingDirectory);
  }

  protected Process runAsync(CommandLine commandLine, ExecuteStreamHandler streamHandler) {
    Map<String, String> newEnv = copyEnvironmentVariables();
    return executeAsyncCommand(commandLine, newEnv, streamHandler);
  }

  private int executeSyncCommand(CommandLine commandLine, Map<String, String> newEnv, ExecuteStreamHandler streamHandler,
                                 int timeout, File workingDirectory) {
    Executor executor = new DefaultExecutor();
    ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
    executor.setWatchdog(watchdog);

    if (Objects.nonNull(workingDirectory)) {
      executor.setWorkingDirectory(workingDirectory);
    }

    setStreamHandler(executor, streamHandler);
    return doExecution(executor, commandLine, newEnv);
  }

  private Process executeAsyncCommand(CommandLine commandLine, Map<String, String> newEnv, ExecuteStreamHandler streamHandler) {
    ProcessCapturingExecutor executor = new ProcessCapturingExecutor();

    // We don't want to set up a watchdog in this mode because the process is allowed to last as long as the test lasts
    // The timeout specified in the constructor of this class is sized for short-lived processes like "mule start", not the mule
    // process itself.

    setStreamHandler(executor, streamHandler);
    doAsyncExecution(executor, commandLine, newEnv);
    return executor.getProcess();
  }

  private void setStreamHandler(Executor executor, ExecuteStreamHandler streamHandler) {
    if (streamHandler == null) {
      // TODO W-14142832: review if we should use a NullOutputStream for improved performance
      streamHandler = new PumpStreamHandler(new ByteArrayOutputStream());
    }
    executor.setStreamHandler(streamHandler);
  }

  protected int doExecution(Executor executor, CommandLine commandLine, Map<String, String> env) {
    String maskedCommandLine = getMaskedCommandLineForLogging(commandLine);

    try {
      logger.info("Executing: {}", maskedCommandLine);
      return executor.execute(commandLine, env);
    } catch (ExecuteException e) {
      logger.error("Error executing {}", maskedCommandLine);
      return e.getExitValue();
    } catch (Exception e) {
      throw new MuleControllerException(format("Error executing [%s]", maskedCommandLine), e);
    }
  }

  private void doAsyncExecution(Executor executor, CommandLine commandLine, Map<String, String> env) {
    String maskedCommandLine = getMaskedCommandLineForLogging(commandLine);

    try {
      logger.info("Executing: {}", maskedCommandLine);
      executor.execute(commandLine, env, new DefaultExecuteResultHandler());
    } catch (ExecuteException e) {
      logger.error("Error executing {}", maskedCommandLine);
    } catch (Exception e) {
      throw new MuleControllerException(format("Error executing [%s]", maskedCommandLine), e);
    }
  }

  private String getMaskedCommandLineForLogging(CommandLine commandLine) {
    final StringJoiner paramsJoiner = new StringJoiner(" ");
    for (String cmdArg : commandLine.toStrings()) {
      paramsJoiner.add(cmdArg.replaceAll("(?<=\\.password=)(.*)", "****"));
    }
    return paramsJoiner.toString();
  }

  protected Map<String, String> copyEnvironmentVariables() {
    Map<String, String> newEnv = new HashMap<>(System.getenv());

    if (this.testEnvVars != null) {
      newEnv.putAll(this.testEnvVars);
    }

    newEnv.put(MULE_HOME_VARIABLE, muleHome);
    if (isNotEmpty(muleAppName) && isNotEmpty(muleAppLongName)) {
      newEnv.put(MULE_APP_VARIABLE, muleAppName);
      newEnv.put(MULE_APP_LONG_VARIABLE, muleAppLongName);
    }

    return newEnv;
  }

  private static class ProcessCapturingExecutor extends DefaultExecutor {

    private final CompletableFuture<Process> processFuture = new CompletableFuture<>();

    @Override
    protected Process launch(CommandLine command, Map<String, String> env, File dir) throws IOException {
      Process process = super.launch(command, env, dir);
      processFuture.complete(process);
      return process;
    }

    public Process getProcess() {
      try {
        return processFuture.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new MuleControllerException(e.getCause());
      }
    }
  }
}
