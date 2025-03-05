/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import static org.mule.runtime.core.api.util.FileUtils.copyFile;
import static org.mule.runtime.core.api.util.FileUtils.newFile;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION;
import static org.mule.test.infrastructure.process.AbstractOSController.MULE_EE_SERVICE_NAME;
import static org.mule.test.infrastructure.process.AbstractOSController.MULE_SERVICE_NAME;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;

import static org.apache.commons.io.FileUtils.copyDirectoryToDirectory;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Controller {

  private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);

  protected static final String ANCHOR_SUFFIX = "-anchor.txt";
  private static final IOFileFilter ANCHOR_FILTER = suffixFileFilter(ANCHOR_SUFFIX);
  private static final String DOMAIN_DEPLOY_ERROR = "Error deploying domain %s.";
  private static final String DOMAIN_BUNDLE_DEPLOY_ERROR = "Error deploying domain bundle %s.";
  private static final String ANCHOR_DELETE_ERROR = "Could not delete anchor file [%s] when stopping Mule Runtime.";
  private static final String ADD_LIBRARY_ERROR = "Error copying jar file [%s] to lib directory [%s].";
  private static final int IS_RUNNING_STATUS_CODE = 0;
  private static final Pattern pattern = compile("wrapper\\.java\\.additional\\.(\\d*)=");

  private final AbstractOSController osSpecificController;

  protected File serverPluginsDir;
  protected File domainsDir;
  protected File appsDir;
  protected File libsDir;
  protected File internalRepository;
  protected Path wrapperConf;

  public Controller(AbstractOSController osSpecificController, String muleHome) {
    this.osSpecificController = osSpecificController;
    this.serverPluginsDir = new File(muleHome + "/server-plugins");
    this.domainsDir = new File(muleHome + "/domains");
    this.appsDir = new File(muleHome + "/apps/");
    this.libsDir = new File(muleHome + "/lib/user");
    this.internalRepository = new File(muleHome, "repository");
    this.wrapperConf = Paths.get(muleHome + "/conf/wrapper.conf");
  }

  public String getMuleBin() {
    return osSpecificController.getMuleBin();
  }

  public void start(String... args) {
    checkRepositoryLocationAndUpdateInternalRepoPropertyIfPresent(args);
    try {
      osSpecificController.start(args);
    } catch (MuleControllerException mce) {
      try {
        printLog();
      } catch (IOException ioe) {
        mce.addSuppressed(ioe);
      }

      throw mce;
    }
  }

  public void printLog() throws IOException {
    final File muleLogFile = getLog();
    if (muleLogFile.exists()) {
      final StringBuilder muleLogBuilder = new StringBuilder();
      muleLogBuilder.append(muleLogFile.getName() + ":" + lineSeparator());
      muleLogBuilder.append("============" + lineSeparator());
      try (BufferedReader br = new BufferedReader(new FileReader(muleLogFile))) {
        String line;
        while ((line = br.readLine()) != null) {
          muleLogBuilder.append(" > " + line + lineSeparator());
        }
      }
      LOGGER.info("{}", muleLogBuilder);
    } else {
      LOGGER.warn("Log ({}) not available.", muleLogFile.toString());
    }
  }

  protected void checkRepositoryLocationAndUpdateInternalRepoPropertyIfPresent(String... args) {
    Optional<String> repoVar =
        asList(args).stream().filter(arg -> arg.contains("-M-DmuleRuntimeConfig.maven.repositoryLocation=")).findFirst();
    if (repoVar.isPresent()) {
      this.internalRepository = new File(repoVar.get().split("=")[1]);
    }
  }

  public int stop(String... args) {
    int error = osSpecificController.stop(args);
    verify(error == 0, "The mule instance couldn't be stopped");
    deleteAnchors();
    return error;
  }

  public int status(String... args) {
    return osSpecificController.status(args);
  }

  public int getProcessId() {
    return osSpecificController.getProcessId();
  }

  public void restart(String... args) {
    osSpecificController.restart(args);
  }

  protected void verify(boolean condition, String message, Object... args) {
    if (!condition) {
      throw new MuleControllerException(format(message, args));
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
      throw new MuleControllerException(format(DOMAIN_DEPLOY_ERROR, domain), e);
    }
  }

  protected void deployDomainBundle(String domainBundle) {
    File domainBundleFile = new File(domainBundle);
    verify(domainBundleFile.exists(), "Domain bundle does not exist: %s", domainBundle);
    try {
      copyFileToDirectory(domainBundleFile, this.domainsDir);
    } catch (IOException e) {
      throw new MuleControllerException(format(DOMAIN_BUNDLE_DEPLOY_ERROR, domainBundle), e);
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
      throw new MuleControllerException(format(ADD_LIBRARY_ERROR, jar, libsDir), e);
    }
  }

  protected void deleteAnchors() {
    @SuppressWarnings("unchecked")
    Collection<File> anchors = listFiles(appsDir, ANCHOR_FILTER, null);
    for (File anchor : anchors) {
      try {
        forceDelete(anchor);
      } catch (IOException e) {
        throw new MuleControllerException(format(ANCHOR_DELETE_ERROR, anchor), e);
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
    validateApplicationExists(application);
    if (!new File(appsDir, application + ANCHOR_SUFFIX).delete()) {
      throw new MuleControllerException("Couldn't undeploy application [" + application + "]");
    }
  }

  private void validateApplicationExists(String application) {
    if (!new File(appsDir, application + ANCHOR_SUFFIX).exists()) {
      throw new MuleControllerException("Couldn't undeploy application [" + application + "]. Application is not deployed");
    }
  }

  /**
   * Triggers a redeploy of the application but touching the application descriptor file.
   * <p/>
   * Clients should expect this method to return before the redeploy actually being done.
   *
   * @param application the application to redeploy
   */
  public void redeploy(String application) {
    validateApplicationExists(application);
    File descriptor = new File(new File(appsDir, application), MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION);
    descriptor.setLastModified(System.currentTimeMillis());
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
    if (0 != osSpecificController.runSync(null, "--installLicense", path, "-M-client")) {
      throw new MuleControllerException("Could not install license " + path);
    }
  }

  public void uninstallLicense() {
    if (0 != osSpecificController.runSync(null, "--unInstallLicense", "-M-client")) {
      throw new MuleControllerException("Could not uninstall license");
    }
  }

  public void verifyLicenseDetails(String path) {
    if (0 != osSpecificController.runSync(null, "--verifyLicenseDetails", path, "-M-client")) {
      throw new MuleControllerException("Could not validate license " + path);
    }
  }

  protected boolean isDeployed(String appName) {
    return new File(appsDir, appName + ANCHOR_SUFFIX).exists();
  }

  protected boolean wasRemoved(String appName) {
    return !new File(appsDir, appName + ANCHOR_SUFFIX).exists() && !new File(appsDir, appName).exists();
  }

  protected boolean isDomainDeployed(String domainName) {
    return new File(domainsDir, domainName + ANCHOR_SUFFIX).exists();
  }

  /**
   * @param artifactName
   * @return the directory of the internal repository for the artifact with the given name.
   */
  protected File getArtifactInternalRepository(String artifactName) {
    return new File(new File(appsDir, artifactName), "repository");
  }

  /**
   * @return the directory of the internal repository for the Mule runtime.
   */
  protected File getRuntimeInternalRepository() {
    return this.internalRepository;
  }

  public File getLog() {
    String muleHome = osSpecificController.getMuleHome();
    List<String> serviceNames = asList(MULE_SERVICE_NAME, MULE_EE_SERVICE_NAME, osSpecificController.getMuleAppName());

    return serviceNames.stream().map(s -> Paths.get(muleHome, "logs", s + ".log").toFile())
        .filter(f -> f.exists() && f.isFile())
        .findFirst()
        .orElseThrow(() -> new MuleControllerException(format("There is no mule log available at %s/logs/", muleHome)));
  }

  public File getLog(String appName) {
    File log = newFile(format("%s/logs/mule-app-%s.log", osSpecificController.getMuleHome(), appName));
    if (log.exists() && log.isFile()) {
      return log;
    }
    throw new MuleControllerException(format("There is no app log available at %s/logs/mule-app-%s",
                                             osSpecificController.getMuleHome(), appName));
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

  public void useLog4jConfigFile(File log4jFile) {
    File currentRuntimeLog4jConfigFile = Paths.get(osSpecificController.getMuleHome(), "conf", "log4j2.xml").toFile();
    try {
      copyFile(log4jFile, currentRuntimeLog4jConfigFile, false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void setTestEnvVars(Map<String, String> testEnvVars) {
    osSpecificController.setTestEnvVars(testEnvVars);
  }
}
