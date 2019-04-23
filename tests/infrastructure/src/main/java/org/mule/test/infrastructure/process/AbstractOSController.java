/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.process;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;

public abstract class AbstractOSController {

  private static Logger logger = getLogger(AbstractOSController.class);

  protected static final String STATUS =
      "Mule(\\sEnterprise Edition \\(standalone\\))? is running(\\s|:\\sPID:)?\\(?([0-9]+)\\)?.*";
  protected static final Pattern STATUS_PATTERN = Pattern.compile(STATUS);
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
  protected final int timeout;

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

  public void start(String... args) {
    int error = runSync("start", args);
    if (error != 0) {
      throw new MuleControllerException("The mule instance couldn't be started");
    }
  }

  public int stop(String... args) {
    return runSync("stop", args);
  }

  public abstract int status(String... args);

  public abstract int getProcessId();

  public String getMuleAppName() {
    return muleAppName;
  }

  public void restart(String... args) {
    int error = runSync("restart", args);
    if (error != 0) {
      throw new MuleControllerException("The mule instance couldn't be restarted");
    }
  }

  protected int runSync(String command, String... args) {
    Map<Object, Object> newEnv = copyEnvironmentVariables();
    return executeSyncCommand(command, args, newEnv, timeout);
  }

  private int executeSyncCommand(String command, String[] args, Map<Object, Object> newEnv, int timeout)
      throws MuleControllerException {
    CommandLine commandLine = new CommandLine(muleBin);
    commandLine.addArgument(command);
    commandLine.addArguments(args, false);
    DefaultExecutor executor = new DefaultExecutor();
    ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
    executor.setWatchdog(watchdog);
    executor.setStreamHandler(new PumpStreamHandler());
    return doExecution(executor, commandLine, newEnv);
  }

  protected int doExecution(DefaultExecutor executor, CommandLine commandLine, Map<Object, Object> env) {
    try {
      final StringJoiner paramsJoiner = new StringJoiner(" ");
      for (String cmdArg : commandLine.toStrings()) {
        paramsJoiner.add(cmdArg.replaceAll("(?<=\\.password=)(.*)", "****"));
      }

      logger.info("Executing: " + paramsJoiner.toString());
      return executor.execute(commandLine, env);
    } catch (ExecuteException e) {
      return e.getExitValue();
    } catch (Exception e) {
      throw new MuleControllerException("Error executing [" + commandLine.getExecutable() + " "
          + Arrays.toString(commandLine.getArguments())
          + "]", e);
    }
  }

  protected Map<Object, Object> copyEnvironmentVariables() {
    Map<String, String> env = System.getenv();
    Map<Object, Object> newEnv = new HashMap<>();
    for (Map.Entry<String, String> it : env.entrySet()) {
      newEnv.put(it.getKey(), it.getValue());
    }

    newEnv.put(MULE_HOME_VARIABLE, muleHome);
    if (isNotEmpty(muleAppName) && isNotEmpty(muleAppLongName)) {
      newEnv.put(MULE_APP_VARIABLE, muleAppName);
      newEnv.put(MULE_APP_LONG_VARIABLE, muleAppLongName);
    }

    return newEnv;
  }
}
