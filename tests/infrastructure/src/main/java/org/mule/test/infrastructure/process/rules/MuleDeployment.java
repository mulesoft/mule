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
import static java.lang.System.getProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.process.MuleProcessController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
 */
public class MuleDeployment extends MuleInstallation {

  private static final long POLL_DELAY_MILLIS = 1000;
  private static final String DEFAULT_DEPLOYMENT_TIMEOUT = "60000";
  private static final boolean STOP_ON_EXIT = parseBoolean(getProperty("mule.test.stopOnExit", "true"));
  private static Logger logger = LoggerFactory.getLogger(MuleDeployment.class);
  private static PollingProber prober;
  private int deploymentTimeout = parseInt(getProperty("mule.test.deployment.timeout", DEFAULT_DEPLOYMENT_TIMEOUT));
  private List<String> applications = new ArrayList<>();
  private List<String> domains = new ArrayList<>();
  private List<String> libraries = new ArrayList<>();
  private MuleProcessController mule;
  private Map<String, String> properties = new HashMap<>();

  public static class Builder {

    MuleDeployment deployment;

    Builder() {
      deployment = new MuleDeployment();
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
     * Specifies a Map of system properties to be passed in the command line when starting Mule.
     * 
     * @param properties
     * @return
     */
    public Builder withProperties(Map<String, String> properties) {
      if (deployment.properties.size() != 0) {
        throw new IllegalStateException("Properties map already has properties defined. Can't overwrite all properties");
      }
      deployment.properties = properties;
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
     * Specifies domains or domain-bundles to be deployed to the domains folder.
     * 
     * @param domains
     * @return
     */
    public Builder withDomains(String... domains) {
      Collections.addAll(deployment.domains, domains);
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

  private MuleDeployment() {
    super();
  }

  private String[] toArray(Map<String, String> map) {
    List<String> values = new ArrayList<>();
    map.forEach((key, value) -> values.add(key + "=" + value));
    return values.toArray(new String[0]);
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    testname = description.getTestClass().getSimpleName();
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
      logger.info("Redeploying domains");
      domains.forEach((domain) -> redeployDomain(domain));
      logger.info("Redeploying applications");
      applications.forEach((application) -> redeploy(application));
      logger.info("Redeployment successful");
    } else {
      libraries.forEach((library) -> mule.addLibrary(new File(library)));
      domains.forEach((domain) -> mule.deployDomain(domain));
      applications.forEach((application) -> mule.deploy(application));
      mule.start(toArray(properties));
      logger.info("Starting Mule Server");
      domains.forEach((domain) -> checkDomainIsDeployed(getName(domain)));
      applications.forEach((application) -> checkAppIsDeployed(getName(application)));
      logger.info("Deployment successful");
    }
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
      if (mule.isRunning()) {
        logger.info("Stopping Mule Server");
        mule.stop();
      }
      super.after();
    }
  }

}
