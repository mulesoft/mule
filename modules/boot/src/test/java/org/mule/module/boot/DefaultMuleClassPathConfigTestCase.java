/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.boot;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

public class DefaultMuleClassPathConfigTestCase extends AbstractMuleTestCase
{
    private File currentTestFolder;
    private File muleHome;
    private File muleBase;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        File tempDir = SystemUtils.getJavaIoTmpDir();
        long now = System.currentTimeMillis();
        currentTestFolder = new File(tempDir, "mule_test_delete_me_" + now);

        muleHome = new File(currentTestFolder, "mule_home");
        muleBase = new File(currentTestFolder, "mule_base");
    }

    /**
     * $MULE_BASE/lib/user folder should come before $MULE_HOME/lib/user. Note this
     * test checks folder only, not the jars. See
     * http://mule.mulesource.org/jira/browse/MULE-1311 for more details.
     */
    public void testMuleBaseUserFolderOverridesMuleHome() throws Exception
    {
        try
        {
            assertTrue("Couldn't create test Mule home folder.", muleHome.mkdirs());
            assertTrue("Couldn't create test Mule base folder.", muleBase.mkdirs());

            DefaultMuleClassPathConfig cp = new DefaultMuleClassPathConfig(muleHome, muleBase);
            List<URL> urls = cp.getURLs();
            assertNotNull("Urls shouldn't be null.", urls);
            assertFalse("Urls shouldn't be empty.", urls.isEmpty());

            URL muleBaseUserFolder = new File(muleBase, DefaultMuleClassPathConfig.USER_DIR)
                .getAbsoluteFile().toURI().toURL();
            String expectedMuleBaseUserFolder = muleBaseUserFolder.toExternalForm();
            String firstUrl = (urls.get(0)).toExternalForm();
            assertEquals("$MULE_BASE/lib/user must come first.", expectedMuleBaseUserFolder, firstUrl);
        }
        finally
        {
            // tearDown() may be too late for these calls
            FileUtils.deleteTree(currentTestFolder);
        }
    }

    public void testScanningMuleAppsDir() throws Exception
    {
        try
        {
            assertTrue("Couldn't create test MULE_HOME folder", muleHome.mkdirs());
            muleBase = muleHome;
            
            File appsDir = new File(muleHome, "apps");
            assertTrue("Couldn't create apps folder", appsDir.mkdirs());
            
            createSampleAppStructure(appsDir);
            
            DefaultMuleClassPathConfig cp = new DefaultMuleClassPathConfig(muleHome, muleBase);
            List<URL> urls = cp.getURLs();
            assertNotNull(urls);
            assertClasspathContainsAppsDirAndJars(urls);
        }
        finally
        {
            // tearDown() may be too late for these calls
            FileUtils.deleteTree(currentTestFolder);
        }
    }

    private void createSampleAppStructure(File appsDir) throws Exception
    {
        File loanbrokerDir = new File(appsDir, "mule-example-loanbroker-esb");
        assertTrue("Couldn't create app folder", loanbrokerDir.mkdirs());

        File libDir = new File(loanbrokerDir, "lib");
        assertTrue("Couldn't create lib dir for sample app", libDir.mkdirs());
        
        File sampleJar = new File(libDir, "mule-example-loanbroker-esb.jar");
        FileUtils.touch(sampleJar);
    }

    private void assertClasspathContainsAppsDirAndJars(List<URL> urls)
    {
        boolean appDirFound = false;
        boolean jarFound = false;
        for (URL url : urls)
        {
            if (url.getPath().endsWith("apps/mule-example-loanbroker-esb/"))
            {
                appDirFound = true;
            }
            if (url.getPath().endsWith("mule-example-loanbroker-esb.jar"))
            {
                jarFound = true;
            }
        }
        
        assertTrue("App dir not found on classpath", appDirFound);
        assertTrue("App's lib jars not found on classpath", jarFound);
    }
}
