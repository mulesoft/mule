/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.process;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static org.apache.commons.io.FileUtils.copyDirectoryToDirectory;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.mule.runtime.core.util.FileUtils.newFile;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

public abstract class Controller {

  protected static final String ANCHOR_SUFFIX = "-anchor.txt";
  private static final IOFileFilter ANCHOR_FILTER = FileFilterUtils.suffixFileFilter(ANCHOR_SUFFIX);
  protected static final String STATUS = "Mule Enterprise Edition is running \\(([0-9]+)\\)\\.";
  protected static final Pattern STATUS_PATTERN = Pattern.compile(STATUS);
  private static final int DEFAULT_TIMEOUT = 30000;
  private static final String MULE_HOME_VARIABLE = "MULE_HOME";
  private static final String DOMAIN_DEPLOY_ERROR = "Error deploying domain %s.";
  private static final String ANCHOR_DELETE_ERROR = "Could not delete anchor file [%s] when stopping Mule Runtime.";
  private static final String ADD_LIBRARY_ERROR = "Error copying jar file [%s] to lib directory [%s].";
  private static final int IS_RUNNING_STATUS_CODE = 0;
  private static final Pattern pattern = Pattern.compile("wrapper\\.java\\.additional\\.(\\d*)=");
  protected String muleHome;
  protected String muleBin;
  protected File domainsDir;
  protected File appsDir;
  protected File libsDir;
  protected Path wrapperConf;
  protected int timeout;

  public Controller(String muleHome, int timeout) {
    this.muleHome = muleHome;
    this.muleBin = getMuleBin();
    this.domainsDir = new File(muleHome + "/domains");
    this.appsDir = new File(muleHome + "/apps/");
    this.libsDir = new File(muleHome + "/lib/user");
    this.wrapperConf = Paths.get(muleHome + "/conf/wrapper.conf");
    this.timeout = timeout != 0 ? timeout : DEFAULT_TIMEOUT;
  }

  public abstract String getMuleBin();

  public void start(String[] args) {
    int error = runSync("start", args);
    if (error != 0) {
      throw new MuleControllerException("The mule instance couldn't be started");
    }
  }

  public void stop(String[] args) {
    int error = runSync("stop", args);
    verify(error == 0, "The mule instance couldn't be stopped");
    deleteAnchors();
  }

  public abstract int status(String... args);

  public abstract int getProcessId();

  public void restart(String[] args) {
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
    commandLine.addArguments(args);
    DefaultExecutor executor = new DefaultExecutor();
    ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
    executor.setWatchdog(watchdog);
    executor.setStreamHandler(new PumpStreamHandler());
    return doExecution(executor, commandLine, newEnv);
  }

  protected int doExecution(DefaultExecutor executor, CommandLine commandLine, Map<Object, Object> env) {
    try {
      return executor.execute(commandLine, env);
    } catch (ExecuteException e) {
      return e.getExitValue();
    } catch (Exception e) {
      throw new MuleControllerException("Error executing [" + commandLine.getExecutable() + " " + commandLine.getArguments()
          + "]", e);
    }
  }

  protected Map<Object, Object> copyEnvironmentVariables() {
    Map<String, String> env = System.getenv();
    Map<Object, Object> newEnv = new HashMap<Object, Object>();
    for (Map.Entry<String, String> it : env.entrySet()) {
      newEnv.put(it.getKey(), it.getValue());
    }
    newEnv.put(MULE_HOME_VARIABLE, muleHome);
    return newEnv;
  }


  protected void verify(boolean condition, String message, Object... args) {
    if (!condition) {
      throw new MuleControllerException(String.format(message, args));
    }
  }

  protected void deployDomain(String domain) {
    File domainFile = new File(domain);
    verify(domainFile.exists(), "Domain does not exist: %s", domain);
    try {
      if (domainFile.isDirectory()) {
        copyDirectoryToDirectory(domainFile, this.domainsDir);
      } else {
        copyFileToDirectory(domainFile, this.domainsDir);
      }
    } catch (IOException e) {
      throw new MuleControllerException(String.format(DOMAIN_DEPLOY_ERROR, domain), e);
    }
  }

  protected void addLibrary(File jar) {
    verify(jar.exists(), "Jar file does not exist: %s", jar);
    verify("jar".equals(FilenameUtils.getExtension(jar.getAbsolutePath())), "Library [%s] don't have .jar extension.", jar);
    verify(jar.canRead(), "Cannot read jar file: %s", jar);
    verify(libsDir.canWrite(), "Cannot write on lib dir: %", libsDir);
    try {
      copyFileToDirectory(jar, libsDir);
    } catch (IOException e) {
      throw new MuleControllerException(String.format(ADD_LIBRARY_ERROR, jar, libsDir), e);
    }
  }

  protected void deleteAnchors() {
    @SuppressWarnings("unchecked")
    Collection<File> anchors = listFiles(appsDir, ANCHOR_FILTER, null);
    for (File anchor : anchors) {
      try {
        forceDelete(anchor);
      } catch (IOException e) {
        throw new MuleControllerException(String.format(ANCHOR_DELETE_ERROR, anchor), e);
      }
    }
  }


  public void deploy(String path) {
    File app = new File(path);
    verify(app.exists(), "File does not exists: %s", app);
    verify(app.canRead(), "Cannot read file: %s", app);
    try {
      if (app.isFile()) {
        copyFileToDirectory(app, appsDir);
      } else {
        copyDirectoryToDirectory(app, appsDir);
      }
    } catch (IOException e) {
      throw new MuleControllerException("Could not deploy app [" + path + "] to [" + appsDir + "]", e);
    }
  }


  public boolean isRunning() {
    return IS_RUNNING_STATUS_CODE == status();
  }

  public void undeploy(String application) {
    if (!new File(appsDir, application + ANCHOR_SUFFIX).exists()) {
      throw new MuleControllerException("Couldn't undeploy application [" + application + "]. Application is not deployed");
    }
    if (!new File(appsDir, application + ANCHOR_SUFFIX).delete()) {
      throw new MuleControllerException("Couldn't undeploy application [" + application + "]");
    }
  }

  public void undeployDomain(String domain) {
    if (!new File(domainsDir, domain + ANCHOR_SUFFIX).exists()) {
      new MuleControllerException("Couldn't undeploy domain [" + domain + "]. Domain is not deployed");
    }
    if (!new File(domainsDir, domain + ANCHOR_SUFFIX).delete()) {
      new MuleControllerException("Couldn't undeploy domain [" + domain + "]");
    }
  }

  public void undeployAll() {
    for (File file : appsDir.listFiles()) {
      try {
        forceDelete(file);
      } catch (IOException e) {
        throw new MuleControllerException("Could not delete directory [" + file.getAbsolutePath() + "]", e);
      }
    }
  }

  public void installLicense(String path) {
    this.runSync(null, "--installLicense", path);
  }

  public void uninstallLicense() {
    this.runSync(null, "-unInstallLicense");
  }

  protected boolean isDeployed(String appName) {
    return new File(appsDir, appName + ANCHOR_SUFFIX).exists();
  }

  protected boolean isDomainDeployed(String domainName) {
    return new File(domainsDir, domainName + ANCHOR_SUFFIX).exists();
  }

  public File getLog() {
    File logEE = newFile(muleHome + "/logs/mule_ee.log");
    File logCE = newFile(muleHome + "/logs/mule.log");
    if (logCE.exists() && logCE.isFile()) {
      return logCE;
    }
    if (logEE.exists() && logEE.isFile()) {
      return logEE;
    }
    throw new MuleControllerException(String.format("There is no mule log available at %s/logs/", muleHome));
  }

  public File getLog(String appName) {
    File log = newFile(String.format("%s/logs/mule-app-%s.log", muleHome, appName));
    if (log.exists() && log.isFile()) {
      return log;
    }
    throw new MuleControllerException(String.format("There is no mule log available at %s/logs/", muleHome));
  }

  public void addConfProperty(String value) {
    try {
      int maxOrder = getMaxPropertyOrder(wrapperConf);
      String line = format("wrapper.java.additional.%d=%s\n", maxOrder + 1, value);
      Files.write(wrapperConf, line.getBytes(UTF_8), APPEND);
    } catch (IOException e) {
      throw new UncheckedIOException("Couldn't add wrapper.conf property", e);
    }
  }

  private int getOrderNumber(String line) {
    Matcher matcher = pattern.matcher(line);
    matcher.find();
    return parseInt(matcher.group(1));
  }

  private Integer getMaxPropertyOrder(Path path) throws IOException {
    return Files.lines(path)
        .filter(line -> pattern.matcher(line).find())
        .map(line -> getOrderNumber(line))
        .max(Integer::compare).get();
  }

}
