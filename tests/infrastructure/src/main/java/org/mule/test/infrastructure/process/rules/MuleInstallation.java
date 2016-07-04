/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.moveDirectory;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This is a JUnit rule to install Mule Runtime during tests. Usage:
 *
 * <pre>
 * public static class MuleRuntimeInstallationTest {
 *  &#064;Rule
 *  public MuleInstallation installation = new MuleInstallation(&quot;/path/to/packed/distribution.zip&quot;);
 *
 *  &#064;Test
 *  public void usingMuleRuntime() throws IOException {
 *      String muleHomePath = installation.getMuleHome();
 *      MuleProcessController mule = new MuleProcessController(muleHomePath);
 *      mule.start();
 *     }
 * }
 * </pre>
 *
 */
public class MuleInstallation extends ExternalResource
{

    private static final File WORKING_DIRECTORY = new File(getProperty("user.dir"));
    private static final String DELETE_ON_EXIT = getProperty("mule.test.deleteOnExit");
    private File distribution;
    private File muleHome;
    private String testname;

    public MuleInstallation(String zippedDistribution)
    {
        distribution = new File(zippedDistribution);
        if (!distribution.exists())
        {
            throw new IllegalArgumentException("Packed distribution not found: " + distribution);
        }
    }

    public String getMuleHome()
    {
        return muleHome.getAbsolutePath();
    }

    @Override
    public Statement apply(final Statement base, final Description description)
    {
        testname = description.getTestClass().getSimpleName();
        return super.apply(base, description);
    }

    @Override
    protected void before() throws Throwable
    {
        muleHome = new DistroUnzipper(distribution, WORKING_DIRECTORY).unzip().muleHome();
    }

    @Override
    protected void after()
    {
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

}
