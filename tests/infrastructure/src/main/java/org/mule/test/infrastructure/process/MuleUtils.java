/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.FileUtils;

public class MuleUtils {

  private static final long TIMEOUT = 1800000;
  private String muleHome;
  private String populateM2repoCommand;

  public MuleUtils(String muleHome) {
    this.muleHome = muleHome;
    this.populateM2repoCommand = muleHome + "/bin/populate_m2_repo";
  }

  public void populateM2Repo(String repo) throws IOException {
    File repository = new File(repo);
    if (!repository.exists()) {
      FileUtils.forceMkdir(repository);
    }
    if (!repository.isDirectory()) {
      throw new IllegalArgumentException("Repository should be a directory.");
    }
    executeCommand(populateM2repoCommand + " " + repo, "MULE_HOME=" + muleHome);
  }

  public static int executeCommand(String command, String... envVars) throws IOException {
    CommandLine cmdLine = CommandLine.parse(command);
    DefaultExecutor executor = new DefaultExecutor();
    Map<String, String> env = addEnvProperties(envVars);
    ExecuteWatchdog watchDog = new ExecuteWatchdog(TIMEOUT);
    executor.setWatchdog(watchDog);
    executor.setStreamHandler(new PumpStreamHandler());
    int result = executor.execute(cmdLine, env);
    if (executor.isFailure(result)) {
      if (watchDog.killedProcess()) {
        throw new RuntimeException("Reached timeout while running: " + cmdLine);
      }
      throw new RuntimeException("Process failed with return code [" + result + "]: " + cmdLine);
    }
    return result;
  }

  private static Map<String, String> addEnvProperties(String[] envVars) throws IOException {
    @SuppressWarnings("unchecked")
    Map<String, String> env = EnvironmentUtils.getProcEnvironment();
    for (String envVar : envVars) {
      EnvironmentUtils.addVariableToEnvironment(env, envVar);
    }
    return env;
  }

}
