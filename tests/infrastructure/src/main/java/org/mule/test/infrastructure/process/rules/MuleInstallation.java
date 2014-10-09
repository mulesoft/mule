/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import static java.lang.System.getProperty;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.junit.rules.ExternalResource;

/**
 * This is a JUnit rule to install Mule ESB during tests. Usage:
 *
 * <pre>
 * public static class MuleEsbInstallationTest {
 *  &#064;Rule
 *  public MuleInstallation installation = new MuleInstallation(&quot;/path/to/packed/distribution.zip&quot;);
 *
 *  &#064;Test
 *  public void usingMuleEsb() throws IOException {
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
    protected void before() throws Throwable
    {
        unzip(distribution, WORKING_DIRECTORY);
    }

    @Override
    protected void after()
    {
        try
        {
            FileUtils.deleteDirectory(muleHome);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Couldn't delete directory [" + muleHome + "], delete it manually.", e);
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
                saveToFile(zip.getInputStream(entry), destFile);
            }
        }
    }

    private void saveToFile(InputStream stream, File destFile) throws IOException
    {
        int currentByte;
        byte data[] = new byte[BUFFER];
        BufferedInputStream is = new BufferedInputStream(stream);
        FileOutputStream fos = new FileOutputStream(destFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER);
        while ((currentByte = is.read(data, 0, BUFFER)) != -1)
        {
            bos.write(data, 0, currentByte);
        }
        bos.flush();
        bos.close();
        is.close();
        chmodRwx(destFile);
    }

    private void chmodRwx(File destFile)
    {
        assert destFile.setExecutable(true, false);
        assert destFile.setWritable(true, false);
        assert destFile.setReadable(true, false);
    }

}
