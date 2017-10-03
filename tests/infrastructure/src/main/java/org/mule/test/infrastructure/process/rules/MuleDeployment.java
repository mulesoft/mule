/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import static com.jayway.awaitility.Awaitility.await;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.mule.runtime.module.repository.internal.RepositoryServiceFactory.MULE_REMOTE_REPOSITORIES_PROPERTY;
import static org.mule.test.infrastructure.process.MuleStatusProbe.isNotRunning;
import org.mule.runtime.core.privileged.util.MapUtils;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.process.MuleProcessController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.qameta.allure.Attachment;
import org.apache.commons.io.FilenameUtils;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit rule to deploy a Mule application for testing in a local Mule server. Usage:
 * <p>
 * 
 * <pre>
 * public class MuleApplicationTestCase {
 * 
 *   &#064;ClassRule
 *   public static MuleDeployment deployment =
 *       builder().withApplications(&quot;/path/to/application.zip&quot;).withProperty("-M-Dproperty", "value").timeout(120).deploy();
 *
 *   &#064;Test
 *   public void useApplication() throws IOException {
 *     // App is deployed
 *     // This code exercises the application
 *   }
 * }
 * </pre>
 *
 * <table>
 * <tr>
 * <td><b>System Property</b></td>
 * <td><b>Default Value</b></td>
 * <td><b>Description</b></td>
 * </tr>
 * <tr>
 * <td>mule.test.deleteOnExit</td>
 * <td>true</td>
 * <td>When false, keeps the used Mule Server under target/server/TEST_NAME</td>
 * </tr>
 * <tr>
 * <td>mule.test.stopOnExit</td>
 * <td>true</td>
 * <td>When false, keeps the used Mule Server running</td>
 * </tr>
 * <tr>
 * <td>mule.test.deployment.timeout</td>
 * <td>60000</td>
 * <td>Timeout for starting Mule (in milliseconds)</td>
 * </tr>
 * <tr>
 * <td>mule.test.debug</td>
 * <td>false</td>
 * <td>Mule server wait for remote debugger attachment.</td>
 * </tr>
 * </table>
 */
public class MuleDeployment extends MuleInstallation {

  private static final long POLL_DELAY_MILLIS = 1000;
  private static final String DEFAULT_DEPLOYMENT_TIMEOUT = "60000";
  private static final boolean STOP_ON_EXIT = parseBoolean(getProperty("mule.test.stopOnExit", "true"));
  private static final boolean DEBUGGING_ENABLED = parseBoolean(getProperty("mule.test.debug", "false"));
  private static final String REMOTE_REPOSITORIES = getProperty("mule.test.repositories", "");
  private static final String DEBUG_PORT = "5005";
  private static Logger logger = LoggerFactory.getLogger(MuleDeployment.class);
  private static PollingProber prober;
  private String locationSuffix = "";
  private int deploymentTimeout = parseInt(getProperty("mule.test.deployment.timeout", DEFAULT_DEPLOYMENT_TIMEOUT));
  private List<String> applications = new ArrayList<>();
  private List<String> domains = new ArrayList<>();
  private List<String> domainBundles = new ArrayList<>();
  private List<String> libraries = new ArrayList<>();
  private MuleProcessController mule;
  private Map<String, String> properties = new HashMap<>();
  private Map<String, Supplier<String>> propertiesUsingLambdas = new HashMap<>();
  private List<String> parameters = new ArrayList<>();

  private final Thread shutdownHookThread = new Thread(this::after);

  public static class Builder {

    MuleDeployment deployment;

    Builder() {
      deployment = new MuleDeployment();
    }

    Builder(String zippedDistribution) {
      deployment = new MuleDeployment(zippedDistribution);
    }

    /**
     * Deploys and starts the Mule instance with the specified configuration.
     * 
     * @return
     */
    public MuleDeployment deploy() {
      return deployment;
    }

    /**
     * Specifies the deployment timeout for each deployed artifact.
     * 
     * @param seconds
     * @return
     */
    public Builder timeout(int seconds) {
      deployment.deploymentTimeout = seconds * 1000;
      return this;
    }

    /**
     * Specifies a system property to be passed in the command line when starting Mule.
     * 
     * @param property
     * @param value
     * @return
     */
    public Builder withProperty(String property, String value) {
      deployment.properties.put(property, value);
      return this;
    }

    /**
     * Specifies a system property to be passed in the command line when starting Mule that needs to be resolved in the before()
     * of the MuleDeployment rule.
     *
     * @param property
     * @param propertySupplier
     * @return
     */
    public Builder withPropertyUsingLambda(String property, Supplier<String> propertySupplier) {
      deployment.propertiesUsingLambdas.put(property, propertySupplier);
      return this;
    }

    /**
     * Specifies a Map of system properties to be passed in the command line when starting Mule.
     * 
     * @param properties
     * @return
     */
    public Builder withProperties(Map<String, String> properties) {
      deployment.properties.putAll(properties);
      return this;
    }

    /**
     * Specifies parameters to be passed in the command line when starting Mule. As opposed to a property, a parameter is
     * something that will be passed "as-is" and not in a key-value pair format.
     *
     * @param parameters
     * @return
     */
    public Builder withParameters(String... parameters) {
      Collections.addAll(deployment.parameters, parameters);
      return this;
    }

    /**
     * Specifies application folders or ZIP files to be deployed to the apps folder.
     * 
     * @param applications
     * @return
     */
    public Builder withApplications(String... applications) {
      Collections.addAll(deployment.applications, applications);
      return this;
    }

    /**
     * Configure the server location suffix.
     *
     * @param suffix
     * @return
     */
    public Builder locationSuffix(String suffix) {
      deployment.locationSuffix = suffix;
      return this;
    }

    /**
     * Specifies domains to be deployed to the domains folder.
     * 
     * @param domains
     * @return
     */
    public Builder withDomains(String... domains) {
      Collections.addAll(deployment.domains, domains);
      return this;
    }

    /**
     * Specifies domain-bundles to be deployed to the domains folder.
     *
     * @param domainBundles domain bundle full paths to deploy
     * @return same builder instance
     */
    public Builder withDomainBundles(String... domainBundles) {
      Collections.addAll(deployment.domainBundles, domainBundles);
      return this;
    }

    /**
     * Adds libraries to lib/user folder.
     * 
     * @param libraries
     * @return
     */
    public Builder withLibraries(String... libraries) {
      Collections.addAll(deployment.libraries, libraries);
      return this;
    }

  }

  public static MuleDeployment.Builder builder() {
    return new Builder();
  }

  public static MuleDeployment.Builder builder(String zippedDistribution) {
    return new Builder(zippedDistribution);
  }


  protected MuleDeployment() {
    super();
  }

  protected MuleDeployment(String zippedDistribution) {
    super(zippedDistribution);
  }

  private String[] toArray(Map<String, String> map) {
    List<String> values = new ArrayList<>();
    map.forEach((key, value) -> values.add(key + "=" + value));
    return values.toArray(new String[0]);
  }

  private String[] toArray(List<String> parameters) {
    return parameters.toArray(new String[0]);
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    location = description.getTestClass().getSimpleName() + locationSuffix;
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        try {
          before();
          base.evaluate();
        } finally {
          after();
        }
      }
    };
  }

  protected void before() throws Throwable {
    super.before();
    prober = new PollingProber(deploymentTimeout, POLL_DELAY_MILLIS);
    mule = new MuleProcessController(getMuleHome());
    addShutdownHooks();
    try {
      doBefore();
    } catch (Error e) {
      logServerError(e);
    }
  }

  private void logServerError(Error e) throws IOException {
    logger.error("====================== Server log ===============================");
    Files.lines(mule.getLog().toPath()).forEach(logger::error);
    logger.error("=================================================================");
    logger.error("Cause: " + e.getMessage());
  }

  private void doBefore() {
    if (mule.isRunning()) {
      logger.warn("Mule Server was already running");
      libraries.forEach((library) -> mule.addLibrary(new File(library)));
      logger.info("Redeploying domain bundles");
      domainBundles.forEach((domainBundle) -> mule.deployDomainBundle(domainBundle));
      logger.info("Redeploying domains");
      domains.forEach((domain) -> redeployDomain(domain));
      logger.info("Redeploying applications");
      applications.forEach((application) -> redeploy(application));
      logger.info("Redeployment successful");
    } else {
      libraries.forEach((library) -> mule.addLibrary(new File(library)));
      domainBundles.forEach((domainBundle) -> mule.deployDomainBundle(domainBundle));
      domains.forEach((domain) -> mule.deployDomain(domain));
      applications.forEach((application) -> mule.deploy(application));
      logger.info("Starting Mule Server");
      if (DEBUGGING_ENABLED) {
        setupDebugging();
      }
      if (isNotEmpty(REMOTE_REPOSITORIES)) {
        mule.addConfProperty(format("-D%s=%s", MULE_REMOTE_REPOSITORIES_PROPERTY, REMOTE_REPOSITORIES));
      }
      resolvePropertiesUsingLambdas();
      mule.start(addAll(toArray(parameters), toArray(properties)));
      domains.forEach((domain) -> checkDomainIsDeployed(getName(domain)));
      applications.forEach((application) -> checkAppIsDeployed(getName(application)));
      logger.info("Deployment successful");
    }
  }

  private void resolvePropertiesUsingLambdas() {
    propertiesUsingLambdas
        .forEach((propertyName, propertySupplierLambda) -> properties.put(propertyName, propertySupplierLambda.get()));
  }

  private void setupDebugging() {
    mule.addConfProperty("-Xdebug");
    mule.addConfProperty("-Xnoagent");
    mule.addConfProperty("-Djava.compiler=NONE");
    mule.addConfProperty("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + DEBUG_PORT);
    logger.info("Listening for remote debugger to attach at port: " + DEBUG_PORT);
  }

  private void redeployDomain(String domain) {
    mule.undeployDomain(getName(domain));
    await("Domain " + domain + " is undeployed").atMost(deploymentTimeout, MILLISECONDS)
        .until(() -> !mule.isDomainDeployed(getName(domain)));
    mule.deployDomain(domain);
    await("Domain " + domain + " is deployed").atMost(deploymentTimeout, MILLISECONDS)
        .until(() -> mule.isDomainDeployed(getName(domain)));
  }

  private void redeploy(String application) {
    mule.undeploy(getName(application));
    await("Application " + application + " is undeployed").atMost(deploymentTimeout, MILLISECONDS)
        .until(() -> !mule.isDeployed(getName(application)));
    mule.deploy(application);
    await("Application " + application + " is deployed").atMost(deploymentTimeout, MILLISECONDS)
        .until(() -> mule.isDeployed(getName(application)));
  }


  private String getName(String application) {
    return removeExtension(FilenameUtils.getName(application));
  }

  private void checkAppIsDeployed(String appName) {
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        return mule.isDeployed(appName);
      }

      @Override
      public String describeFailure() {
        return "Application " + appName + " is not deployed after " + (deploymentTimeout / 1000) + " seconds.";
      }
    });
  }

  private void checkDomainIsDeployed(String domainName) {
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        return mule.isDomainDeployed(domainName);
      }

      @Override
      public String describeFailure() {
        return "Domain " + domainName + " is not deployed after " + (deploymentTimeout / 1000) + " seconds.";
      }
    });
  }

  protected void after() {
    if (STOP_ON_EXIT) {
      stopMule();
      super.after();
    }
    removeShutdownHooks();
  }

  private void stopMule() {
    if (mule.isRunning()) {
      logger.info("Stopping Mule Server");
      mule.stop();
      prober.check(isNotRunning(mule));
    }
  }

  private void addShutdownHooks() {
    Runtime.getRuntime().addShutdownHook(shutdownHookThread);
  }

  private void removeShutdownHooks() {
    Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
  }

  /***
   * Utility method to generate System properties attachment for the Allure report
   * 
   * @return a formatted String of the Mule system properties of the given instance.
   */
  @Attachment(value = "Properties")
  public String attachProperties() {
    return MapUtils.toString(properties, true).replaceAll("(?<=\\.password=)(.*)", "****");
  }

  /***
   * Utility method to generate Runtime server log attachment for the Allure report
   * 
   * @return a ByteArray representation of the log.
   */
  @Attachment(value = "Server log ", type = "text/plain", fileExtension = ".log")
  public byte[] attachServerLog() {
    try {
      return Files.readAllBytes(mule.getLog().toPath());
    } catch (IOException ignored) {

    }
    return null;
  }

  /***
   * Utility method to generate Application log attachment for the Allure report
   * 
   * @param appName the application name
   * @return a ByteArray representation of the log.
   */
  @Attachment(value = "Application {appName} log", type = "text/plain", fileExtension = ".log")
  public byte[] attachAppLog(String appName) {
    try {
      return Files.readAllBytes(mule.getLog(appName).toPath());
    } catch (IOException ignored) {

    }
    return null;
  }

  /**
   * @return the Mule instance controller.
   */
  public MuleProcessController getMuleProcessController() {
    return mule;
  }
}
