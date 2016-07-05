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
import static org.mule.test.infrastructure.process.AppDeploymentProbe.isDeployed;
import org.mule.tck.probe.PollingProber;
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
 *  public static MuleDeployment deployment = application(&quot;/path/to/application.zip&quot;).withProperty("-M-Dproperty", "value").timeout(120).deploy();
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
    private static Logger logger = LoggerFactory.getLogger(MuleDeployment.class);
    private static final String DEFAULT_DEPLOYMENT_TIMEOUT = "60000";
    private int deploymentTimeout = parseInt(getProperty("mule.test.deployment.timeout", DEFAULT_DEPLOYMENT_TIMEOUT));
    private String application;
    private MuleProcessController mule;
    private Map<String, String> properties = new HashMap<>();

    public static class Builder
    {

        MuleDeployment deployment;

        Builder(String application)
        {
            deployment = new MuleDeployment();
            deployment.application = application;
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
    }

    public static MuleDeployment.Builder application(String application)
    {
        return new Builder(application);
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
        mule = new MuleProcessController(getMuleHome());
        mule.deploy(application);
        mule.start(toArray(properties));
        logger.info("Starting Mule Server");
        new PollingProber(deploymentTimeout, POLL_DELAY_MILLIS).check(isDeployed(mule, getName(application)));
        logger.info("Application deployed");
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
