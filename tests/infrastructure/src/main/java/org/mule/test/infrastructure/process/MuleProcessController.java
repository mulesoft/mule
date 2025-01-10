/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;

import static java.lang.Boolean.getBoolean;
import static java.lang.System.getProperties;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;

public class MuleProcessController {

  public static final String MULE_PARAM_PROPERTY_PREFIX = SYSTEM_PROPERTY_PREFIX + "test.muleParam.";
  private static final String DEBUG_WRAPER_PROPERTY = SYSTEM_PROPERTY_PREFIX + "test.wrapperDebug";

  private static final Logger LOGGER_WRAPPER = getLogger(MuleProcessController.class.getName() + ".wrapper");

  private static final int DEFAULT_TIMEOUT = 60000;

  private final Controller controller;

  public MuleProcessController(String muleHome) {
    this(muleHome, DEFAULT_TIMEOUT);
  }

  public MuleProcessController(String muleHome, int timeout) {
    AbstractOSController osSpecificController =
        IS_OS_WINDOWS ? new WindowsController(muleHome, timeout) : new UnixController(muleHome, timeout);
    controller = buildController(muleHome, osSpecificController);
  }

  public MuleProcessController(String muleHome, String locationSuffix) {
    AbstractOSController osSpecificController =
        IS_OS_WINDOWS ? new WindowsController(muleHome, DEFAULT_TIMEOUT, locationSuffix)
            : new UnixController(muleHome, DEFAULT_TIMEOUT);
    controller = buildController(muleHome, osSpecificController);
  }

  protected Controller buildController(String muleHome, AbstractOSController osSpecificController) {
    return new Controller(osSpecificController, muleHome);
  }

  public boolean isRunning() {
    return getController().isRunning();
  }

  public int installAgent(String consoleURI, String serverName, String token) {
    return getController().installAgent(serverName, consoleURI, token);
  }

  public void start(String... baseArgs) {
    String[] args = baseArgs;

    final Map<String, String> additionalMuleParamsProperties = getProperties().entrySet()
        .stream()
        .filter(e -> e.getKey().toString().startsWith(MULE_PARAM_PROPERTY_PREFIX))
        .collect(toMap(e -> e.getKey().toString().substring(MULE_PARAM_PROPERTY_PREFIX.length()),
                       e -> e.getValue().toString()));

    if (LOGGER_WRAPPER.isDebugEnabled() || getBoolean(DEBUG_WRAPER_PROPERTY)) {
      additionalMuleParamsProperties.put("wrapperDebug", "");
    }

    if (!additionalMuleParamsProperties.isEmpty()) {
      args = concat(Stream.of(args),
                    additionalMuleParamsProperties.entrySet()
                        .stream()
                        .map(e -> e.getKey() + (isEmpty(e.getValue()) ? "" : "=" + e.getValue())))
                            .toArray(String[]::new);
    }

    getController().start(args);
  }

  public void stop(String... args) {
    getController().stop(args);
  }

  public int status(String... args) {
    return getController().status(args);
  }

  public int getProcessId() {
    return getController().getProcessId();
  }

  public void restart(String... args) {
    getController().restart(args);
  }

  public void deploy(String path) {
    getController().deploy(path);
  }

  /**
   * Triggers a redeploy of the application but touching the application descriptor file.
   * <p/>
   * Clients should expect this method to return before the redeploy actually being done.
   *
   * @param application the application to redeploy
   */
  public void redeploy(String application) {
    getController().redeploy(application);
  }

  public boolean isDeployed(String appName) {
    return getController().isDeployed(appName);
  }

  public boolean wasRemoved(String appName) {
    return getController().wasRemoved(appName);
  }

  public File getArtifactInternalRepository(String artifactName) {
    return getController().getArtifactInternalRepository(artifactName);
  }

  public File getRuntimeInternalRepository() {
    return getController().getRuntimeInternalRepository();
  }

  public boolean isDomainDeployed(String domainName) {
    return getController().isDomainDeployed(domainName);
  }

  public void undeploy(String application) {
    getController().undeploy(application);
  }

  public void undeployDomain(String domain) {
    getController().undeployDomain(domain);
  }

  public void undeployAll() {
    getController().undeployAll();
  }

  public void installLicense(String path) {
    getController().installLicense(path);
  }

  public void uninstallLicense() {
    getController().uninstallLicense();
  }

  public void verifyLicenseDetails(String path) {
    getController().verifyLicenseDetails(path);
  }

  public void addLibrary(File jar) {
    getController().addLibrary(jar);
  }

  public void deployDomain(String domain) {
    getController().deployDomain(domain);
  }

  public void deployDomainBundle(String domain) {
    getController().deployDomainBundle(domain);
  }

  public File getLog() {
    return getController().getLog();
  }

  public File getLog(String appName) {
    return getController().getLog(appName);
  }

  public void addConfProperty(String value) {
    getController().addConfProperty(value);
  }

  public void useLog4jConfigFile(File log4jFile) {
    getController().useLog4jConfigFile(log4jFile);
  }

  protected Controller getController() {
    return controller;
  }


  /**
   * Set the environment variables with the corresponding value before running the tests. This should be called with null after
   * running the tests.
   *
   * @param testEnvVars map of environment variables and their values
   */
  public void setTestEnvVars(Map<String, String> testEnvVars) {
    getController().setTestEnvVars(testEnvVars);
  }
}
