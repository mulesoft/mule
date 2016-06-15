/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.moveDirectory;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.test.infrastructure.process.AppDeploymentProbe.isDeployed;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.process.MuleProcessController;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit rule to deploy a Mule application for testing in a local Mule server. Usage:
 * <p>
 * <pre>
 * public static class MuleDeployment {
 *  &#064;Rule
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
public class MuleDeployment implements TestRule
{

    private static final String DISTRIBUTION_PROPERTY = "com.mulesoft.muleesb.distributions:mule-ee-distribution-standalone:zip";
    private static final long POLL_DELAY_MILLIS = 1000;
    private static Logger logger = LoggerFactory.getLogger(MuleDeployment.class);
    private static final File WORKING_DIRECTORY = new File(getProperty("user.dir"));
    private static final String DELETE_ON_EXIT = getProperty("mule.test.deleteOnExit");
    private static final int DEFAULT_DEPLOYMENT_TIMEOUT = 60000;
    private int deploymentTimeout = getProperty("mule.test.deployment.timeout") == null ? DEFAULT_DEPLOYMENT_TIMEOUT : parseInt(getProperty("mule.test.deployment.timeout"));
    private PollingProber prober = new PollingProber(deploymentTimeout, POLL_DELAY_MILLIS);
    private DistroUnzipper zipExtractor;
    private String application;
    private File muleHome;
    private String testname;
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
        String zippedDistribution = System.getProperty(DISTRIBUTION_PROPERTY);
        if (StringUtils.isEmpty(zippedDistribution))
        {
            logger.error("You must configure the location for Mule distribution in the system property: " + DISTRIBUTION_PROPERTY);
        }
        File distribution = new File(zippedDistribution);
        zipExtractor = new DistroUnzipper(distribution, WORKING_DIRECTORY);
        if (!distribution.exists())
        {
            throw new IllegalArgumentException("Packed distribution not found: " + distribution);
        }
    }

    public String[] getProperties()
    {
        String[] result = new String[properties.size()];
        Iterator<Map.Entry<String, String>> it = properties.entrySet().iterator();
        for (int i = 0; i < result.length; i++)
        {
            Map.Entry<String, String> entry = it.next();
            result[i] = entry.getKey() + "=" + entry.getValue();
        }
        return result;
    }

    public Statement apply(Statement base, Description description)
    {
        testname = description.getTestClass().getSimpleName();
        return statement(base);
    }

    private Statement statement(final Statement base)
    {
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
        muleHome = zipExtractor.unzip().muleHome();
        mule = new MuleProcessController(muleHome.getAbsolutePath());
        mule.deploy(application);
        mule.start(getProperties());
        prober.check(isDeployed(mule, getName(application)));
    }

    private String getName(String application)
    {
        return removeExtension(FilenameUtils.getName(application));
    }

    protected void after()
    {
        if (mule.isRunning())
        {
            mule.stop();
        }
        File dest = new File(new File("logs"), testname);
        deleteQuietly(dest);
        if (isEmpty(DELETE_ON_EXIT) || parseBoolean(DELETE_ON_EXIT))
        {
            try
            {
                File logs = new File(muleHome, "logs");
                moveDirectory(logs, dest);
                deleteDirectory(muleHome);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Couldn't delete directory [" + muleHome + "], delete it manually.", e);
            }
        }
    }

    public String getHome()
    {
        return muleHome.getAbsolutePath();
    }
}
