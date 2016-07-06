/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.process.AppDeploymentProbe;
import org.mule.test.infrastructure.process.DomainDeploymentProbe;
import org.mule.test.infrastructure.process.MuleProcessController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit rule to deploy a Mule application for testing in a local Mule server. Usage:
 * <p>
 * <pre>
 * public class MuleApplicationTestCase {
 *  &#064;ClassRule
 *  public static MuleDeployment deployment = builder().withApplication(&quot;/path/to/application.zip&quot;).withProperty("-M-Dproperty", "value").timeout(120).deploy();
 *
 *  &#064;Test
 *  public void useApplication() throws IOException {
 *      // App is deployed
 *      // This code exercises the application
 *  }
 * }
 * </pre>
 */
public class MuleDeployment extends MuleInstallation
{

    private static final long POLL_DELAY_MILLIS = 1000;
    private static final String DEFAULT_DEPLOYMENT_TIMEOUT = "60000";
    private static Logger logger = LoggerFactory.getLogger(MuleDeployment.class);
    private static PollingProber prober;
    private int deploymentTimeout = parseInt(getProperty("mule.test.deployment.timeout", DEFAULT_DEPLOYMENT_TIMEOUT));
    private List<String> applications = new ArrayList<>();
    private List<String> domains = new ArrayList<>();
    private MuleProcessController mule;
    private Map<String, String> properties = new HashMap<>();

    public static class Builder
    {

        MuleDeployment deployment;

        Builder()
        {
            deployment = new MuleDeployment();
        }

        public MuleDeployment deploy()
        {
            return deployment;
        }

        public Builder withProperties(Map<String, String> properties)
        {
            if (deployment.properties.size() != 0)
            {
                throw new IllegalStateException("Properties map already has properties defined. Can't overwrite all properties");
            }
            deployment.properties = properties;
            return this;
        }

        public Builder timeout(int seconds)
        {
            deployment.deploymentTimeout = seconds * 1000;
            return this;
        }

        public Builder withProperty(String property, String value)
        {
            deployment.properties.put(property, value);
            return this;
        }

        public Builder withApplication(String application)
        {
            deployment.applications.add(application);
            return this;
        }

        public Builder withApplications(List<String> applications)
        {
            for (String application : applications)
            {
                deployment.applications.add(application);
            }
            return this;
        }

        public Builder withDomain(String domain)
        {
            deployment.domains.add(domain);
            return this;
        }

        public Builder withDomains(List<String> domains)
        {
            for (String domain : domains)
            {
                deployment.domains.add(domain);
            }
            return this;
        }

    }

    public static MuleDeployment.Builder builder()
    {
        return new Builder();
    }

    private MuleDeployment()
    {
        super();
    }

    private String[] toArray(Map<String, String> map)
    {
        List<String> values = new ArrayList<>();
        map.forEach((key, value) -> values.add(key + "=" + value));
        return values.toArray(new String[0]);
    }

    @Override
    public Statement apply(final Statement base, final Description description)
    {
        testname = description.getTestClass().getSimpleName();
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                try
                {
                    before();
                    base.evaluate();
                }
                finally
                {
                    after();
                }
            }
        };
    }

    protected void before() throws Throwable
    {
        super.before();
        prober = new PollingProber(deploymentTimeout, POLL_DELAY_MILLIS);
        mule = new MuleProcessController(getMuleHome());
        domains.forEach((domain) -> mule.deployDomain(domain));
        applications.forEach((application) -> mule.deploy(application));
        mule.start(toArray(properties));
        logger.info("Starting Mule Server");
        domains.forEach((domain) -> prober.check(DomainDeploymentProbe.isDeployed(mule, getName(domain))));
        applications.forEach((application) -> prober.check(AppDeploymentProbe.isDeployed(mule, getName(application))));
        logger.info("Deployment successful");
    }

    private String getName(String application)
    {
        return removeExtension(FilenameUtils.getName(application));
    }

    protected void after()
    {
        if (mule.isRunning())
        {
            logger.info("Stopping Mule Server");
            mule.stop();
        }
        super.after();
    }

}
