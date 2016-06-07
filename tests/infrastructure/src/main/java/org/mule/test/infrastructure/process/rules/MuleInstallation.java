/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static org.apache.commons.io.FileUtils.*;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang.StringUtils;
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
    private static final int BUFFER = 2048;
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
        testname = description.getClassName();
        return super.apply(base, description);
    }

    @Override
    protected void before() throws Throwable
    {
        unzip(distribution, WORKING_DIRECTORY);
    }

    @Override
    protected void after()
    {
        File logs = new File(muleHome, "logs");
        File dest = new File(testname + ".logs");
        deleteQuietly(dest);
        String deleteOnExit = getProperty("mule.test.deleteOnExit");
        if (StringUtils.isEmpty(deleteOnExit) || parseBoolean(deleteOnExit))
        {
            try
            {
                moveDirectory(logs, dest);
                deleteDirectory(muleHome);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Couldn't delete directory [" + muleHome + "], delete it manually.", e);
            }
        }
    }

    private void unzip(File file, File destDir) throws IOException
    {
        ZipFile zip = new ZipFile(file);
        Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
        ZipEntry root = zipFileEntries.nextElement();
        muleHome = new File(destDir, root.getName());
        muleHome.mkdirs();
        chmodRwx(muleHome);
        while (zipFileEntries.hasMoreElements())
        {
            ZipEntry entry = zipFileEntries.nextElement();
            File destFile = new File(entry.getName());
            if (entry.isDirectory())
            {
                destFile.mkdir();
            }
            else
            {
                copyInputStreamToFile(zip.getInputStream(entry), destFile);
                chmodRwx(destFile);
            }
        }
    }

    private void chmodRwx(File destFile)
    {
        destFile.setExecutable(true, false);
        destFile.setWritable(true, false);
        destFile.setReadable(true, false);
    }

}
