/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowsController extends AbstractOSController {

  protected static final String PID_WINDOWS = "(\\s)*PID (\\s)+ :(\\s)*([0-9])+";
  protected static final Pattern PID_PATTERN_WINDOWS = Pattern.compile(PID_WINDOWS);

  public WindowsController(String muleHome, int timeout) {
    super(muleHome, timeout);
  }

  public WindowsController(String muleHome, int defaultTimeout, String locationSuffix) {
    super(muleHome, defaultTimeout, locationSuffix);
  }

  @Override
  public String getMuleBin() {
    return muleHome + "/bin/mule.bat";
  }

  @Override
  public void start(String... args) {
    install(args);
    super.start(args);
    if (status() != 0) {
      throw new MuleControllerException("The mule instance couldn't be started");
    }
  }

  @Override
  public int stop(String... args) {
    final int returnCode = super.stop(args);
    int errorRemove = runSync("remove");
    if (errorRemove != 0 && errorRemove != 0x424) {
      throw new MuleControllerException("The mule instance couldn't be removed as a service");
    }
    return returnCode;
  }

  @Override
  public int getProcessId() {
    List<String> serviceNames = Arrays.asList("mule", "mule_ee", muleAppName);
    for (String serviceName : serviceNames) {
      String cmdOutput = executeCmd("sc queryex \"" + serviceName + "\" ");
      if (cmdOutput.contains("RUNNING")) {
        return getId(cmdOutput);
      }
    }
    throw new MuleControllerException("No mule instance is running");
  }

  private int getId(String str) {
    Matcher matcher = PID_PATTERN_WINDOWS.matcher(str);
    String result;
    if (matcher.find()) {
      result = matcher.group(0);
    } else {
      throw new MuleControllerException("PID pattern not recognized in " + str);
    }
    String[] resultArray = result.split(": ");
    return Integer.parseInt(resultArray[1]);
  }

  @Override
  public int status(String... args) {
    List<String> serviceNames = Arrays.asList("mule", "mule_ee", muleAppName);
    for (String serviceName : serviceNames) {
      boolean serviceRunning = executeCmd("sc queryex \"" + serviceName + "\" ").contains("RUNNING");
      if (serviceRunning) {
        return 0;
      }
    }
    return 1;
  }

  private String executeCmd(String cmd) {
    StringBuilder output = new StringBuilder();
    Process p;
    try {
      p = Runtime.getRuntime().exec(cmd);
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line = "";
      while ((line = reader.readLine()) != null) {
        output.append(line + "\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return output.toString();
  }

  @Override
  public void restart(String... args) {
    install(args);
    super.restart(args);
  }

  private void install(String... args) {
    int errorInstall = runSync("install", args);
    if (errorInstall != 0 && errorInstall != 0x431) {
      throw new MuleControllerException("The mule instance couldn't be installed as a service");
    }
  }
}
