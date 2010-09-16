/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import org.mule.api.config.MuleProperties;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationWrapper;
import org.mule.module.launcher.application.PriviledgedMuleApplication;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.Latch;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import static org.junit.Assert.assertArrayEquals;

/**
 *
 */
public class DeploymentServiceTestCase extends AbstractMuleTestCase
{

    protected static final int LATCH_TIMEOUT = 10000;
    protected static final String[] NONE = new String[0];

    protected File muleHome;
    protected File appsDir;
    protected DeploymentService deploymentService;
    // these latches are re-created during the test, thus need to be declared volatile
    protected volatile Latch deployLatch;
    protected volatile Latch undeployLatch;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // set up some mule home structure
        final String tmpDir = System.getProperty("java.io.tmpdir");
        muleHome = new File(tmpDir, getClass().getSimpleName() + System.currentTimeMillis());
        appsDir = new File(muleHome, "apps");
        appsDir.mkdirs();
        System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, muleHome.getCanonicalPath());

        new File(muleHome, "lib/shared/default").mkdirs();

        deploymentService = new DeploymentService();
        deploymentService.setDeployer(new TestDeployer());
        deployLatch = new Latch();
        undeployLatch = new Latch();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        // comment out the deletion to analyze results after test is done
        FileUtils.deleteTree(muleHome);
        if (deploymentService != null)
        {
            deploymentService.stop();
        }
        super.doTearDown();
    }

    public void testPriviledgedApp() throws Exception
    {
        final URL url = getClass().getResource("/priviledged-dummy-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));

        assertAppsDir(NONE, new String[] {"priviledged-dummy-app"});

        final Application app = findApp("priviledged-dummy-app");
        // now that we're sure it's the app we wanted, assert the registry has everything
        // a 'priviledged' app would have had
        // TODO some constants for priviledged registry key names
        final Object obj = app.getMuleContext().getRegistry().lookupObject(PriviledgedMuleApplication.REGISTRY_KEY_DEPLOYMENT_SERVICE);
        assertNotNull("Priviledged objects have not been registered", obj);
        assertTrue(((ApplicationWrapper) app).getDelegate() instanceof PriviledgedMuleApplication);
    }

    public void testDeployZipOnStartup() throws Exception
    {
        final URL url = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found", url);
        addAppArchive(url);

        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));

        assertAppsDir(NONE, new String[] {"dummy-app"});

        // just assert no priviledged entries were put in the registry
        final Application app = findApp("dummy-app");
        final Object obj = app.getMuleContext().getRegistry().lookupObject(PriviledgedMuleApplication.REGISTRY_KEY_DEPLOYMENT_SERVICE);
        assertNull(obj);
        assertFalse(((ApplicationWrapper) app).getDelegate() instanceof PriviledgedMuleApplication);
    }

    public void testUpdateAppViaZip() throws Exception
    {
        final URL url = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found", url);
        addAppArchive(url);

        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertAppsDir(NONE, new String[] {"dummy-app"});
        // set up a new deployment latch (can't reuse the old one)
        deployLatch = new Latch();
        addAppArchive(url);
        assertTrue("Undeploy never invoked", undeployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertAppsDir(NONE, new String[] {"dummy-app"});
    }

    /**
     * Find a deployed app, performing some basic assertions.
     */
    private Application findApp(final String appName)
    {
        final List<Application> apps = deploymentService.getApplications();
        assertNotNull(apps);
        assertEquals(1, apps.size());
        final Application app = apps.get(0);
        assertNotNull(app);
        assertEquals("Wrong application name", appName, app.getAppName());
        return app;
    }

    private void assertAppsDir(String[] expectedZips, String[] expectedApps)
    {
        final String[] actualZips = appsDir.list(new SuffixFileFilter(".zip"));
        assertArrayEquals("Invalid Mule application archives set", expectedZips, actualZips);
        final String[] actualApps = appsDir.list(DirectoryFileFilter.DIRECTORY);
        assertArrayEquals("Invalid Mule exploded applications set", expectedApps, actualApps);
    }

    /**
     * Copies a given app archive to the apps folder for deployment.
     */
    private void addAppArchive(URL url) throws IOException
    {
        // copy is not atomic, copy to a temp file and rename instead (rename is atomic)
        final String tempFileName = new File(url.getFile() + ".part").getName();
        final File tempFile = new File(appsDir, tempFileName);
        FileUtils.copyURLToFile(url, tempFile);
        tempFile.renameTo(new File(StringUtils.removeEnd(tempFile.getAbsolutePath(), ".part")));
    }


    private class TestDeployer implements MuleDeployer
    {
        MuleDeployer delegate = new DefaultMuleDeployer(deploymentService);

        public void deploy(Application app)
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.deploy");
            delegate.deploy(app);
            deployLatch.release();
        }

        public void undeploy(Application app)
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.undeploy");
            delegate.undeploy(app);
            undeployLatch.release();
        }

        public Application installFromAppDir(String packedMuleAppFileName) throws IOException
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.installFromAppDir");
            return delegate.installFromAppDir(packedMuleAppFileName);
        }

        public Application installFrom(URL url) throws IOException
        {
            System.out.println("DeploymentServiceTestCase$TestDeployer.installFrom");
            return delegate.installFrom(url);
        }

    }
}
