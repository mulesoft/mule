/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import static org.mule.runtime.core.api.util.StringUtils.isEmpty;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

/**
 *
 */
public class UnixController extends AbstractOSController {

  public UnixController(String muleHome, int timeout) {
    super(muleHome, timeout);
  }

  @Override
  public String getMuleBin() {
    return muleHome + "/bin/mule";
  }

  @Override
  public int getProcessId() {
    Map<String, String> newEnv = this.copyEnvironmentVariables();
    DefaultExecutor executor = new DefaultExecutor();
    ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
    executor.setWatchdog(watchdog);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
    executor.setStreamHandler(streamHandler);
    if (this.doExecution(executor, new CommandLine(this.muleBin).addArgument(STATUS_CMD), newEnv) == 0) {
      Matcher matcher = STATUS_LABELS_PATTERN.matcher(outputStream.toString());
      if (matcher.find() && !isEmpty(matcher.group(STATUS_PID_GROUP_NAME))) {
        return parseInt(matcher.group(STATUS_PID_GROUP_NAME));
      } else {
        throw new MuleControllerException("bin/mule status didn't return the expected pattern: " + STATUS_LABELS);
      }
    } else {
      throw new MuleControllerException("Mule Runtime is not running");
    }
  }

  @Override
  public MuleProcessStatus getProcessesStatus() {
    Map<String, String> newEnv = this.copyEnvironmentVariables();
    DefaultExecutor executor = new DefaultExecutor();
    ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
    executor.setWatchdog(watchdog);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
    executor.setStreamHandler(streamHandler);

    if (this.doExecution(executor, new CommandLine(this.muleBin).addArgument(STATUS_CMD), newEnv) == 0) {
      Matcher matcher = STATUS_LABELS_PATTERN.matcher(outputStream.toString());
      if (matcher.find() && !isEmpty(matcher.group(STATUS_WRAPPER_GROUP_NAME))
          && !isEmpty(matcher.group(STATUS_JAVA_GROUP_NAME))) {
        return MuleProcessStatus.valueOf(format("%s_%s", matcher.group(STATUS_WRAPPER_GROUP_NAME),
                                                matcher.group(STATUS_JAVA_GROUP_NAME)));
      } else {
        throw new MuleControllerException("bin/mule status didn't return the expected pattern: " + STATUS_LABELS);
      }
    } else {
      throw new MuleControllerException("Mule Runtime is not running");
    }
  }

  @Override
  public int status(String... args) {
    return runSync(STATUS_CMD, args);
  }

  public void resolveSchedulerConfig() {
    // if (0 != runSync(RESOLVE_SCHEDULER_CONFIG)) {
    // throw new MuleControllerException("Could not resolve scheduler configuration.");
    // }

    Map<String, String> newEnv = this.copyEnvironmentVariables();
    DefaultExecutor executor = new DefaultExecutor();
    ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
    executor.setWatchdog(watchdog);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
    executor.setStreamHandler(streamHandler);

    if (this.doExecution(executor, new CommandLine(this.muleBin).addArgument(RESOLVE_SCHEDULER_CONFIG_CMD), newEnv) == 0) {
      System.out.println(outputStream);
      // Matcher matcher = STATUS_LABELS_PATTERN.matcher(outputStream.toString());
      // if (matcher.find() && !isEmpty(matcher.group(STATUS_WRAPPER_GROUP_NAME))
      // && !isEmpty(matcher.group(STATUS_JAVA_GROUP_NAME))) {
      // return MuleProcessStatus.valueOf(format("%s_%s", matcher.group(STATUS_WRAPPER_GROUP_NAME),
      // matcher.group(STATUS_JAVA_GROUP_NAME)));
      // } else {
      // throw new MuleControllerException("bin/mule resolve-scheduler-config didn't return the expected pattern: " +
      // STATUS_LABELS);
      // }
    } else {
      throw new MuleControllerException("Could not resolve scheduler configuration.");
    }
  }
}
