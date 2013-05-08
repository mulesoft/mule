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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleContext;
import org.mule.api.component.JavaComponent;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.registry.MuleRegistry;
import org.mule.config.StartupContext;
import org.mule.construct.SimpleService;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationWrapper;
import org.mule.module.launcher.application.PriviledgedMuleApplication;
import org.mule.module.launcher.application.TestApplicationFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.tck.probe.file.FileDoesNotExists;
import org.mule.tck.probe.file.FileExists;
import org.mule.util.CollectionUtils;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.Latch;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.junit.Test;

public class DeploymentServiceTestCase extends AbstractMuleContextTestCase
{

    protected static final int LATCH_TIMEOUT = 20000;
    protected static final String[] NONE = new String[0];

    protected File muleHome;
    protected File appsDir;
    protected DeploymentService deploymentService;
    // these latches are re-created during the test, thus need to be declared volatile
    protected volatile Latch deployLatch;
    protected volatile Latch installLatch;
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
        installLatch = new Latch();
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

        // this is a complex classloader setup and we can't reproduce standalone Mule 100%,
        // so trick the next test method into thinking it's the first run, otherwise
        // app resets CCL ref to null and breaks the next test
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
    }

    @Test
    public void testPriviledgedApp() throws Exception
    {
        final URL url = getClass().getResource("/priviledged-dummy-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));

        assertAppsDir(NONE, new String[] {"priviledged-dummy-app"}, true);

        final Application app = findApp("priviledged-dummy-app", 1);
        // now that we're sure it's the app we wanted, assert the registry has everything
        // a 'priviledged' app would have had
        final Object obj = app.getMuleContext().getRegistry().lookupObject(PriviledgedMuleApplication.REGISTRY_KEY_DEPLOYMENT_SERVICE);
        assertNotNull("Priviledged objects have not been registered", obj);
        assertTrue(((ApplicationWrapper) app).getDelegate() instanceof PriviledgedMuleApplication);
    }

    @Test
    public void testPriviledgedCrossAppAccess() throws Exception
    {
        URL url = getClass().getResource("/priviledged-dummy-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        url = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        deploymentService.start();

        // a basic latch isn't ideal here, as there are 2 apps to deploy
        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));

        assertAppsDir(NONE, new String[] {"dummy-app", "priviledged-dummy-app"}, true);

        final Application privApp = findApp("priviledged-dummy-app", 2);
        final Application dummyApp = findApp("dummy-app", 2);
        assertTrue(((ApplicationWrapper) privApp).getDelegate() instanceof PriviledgedMuleApplication);

        final MuleContext muleContext1 = privApp.getMuleContext();
        System.out.println("muleContext1 = " + muleContext1);
        assertNotSame(muleContext1, muleContext);
        assertNotSame(privApp.getDeploymentClassLoader(), dummyApp.getDeploymentClassLoader());
        final Collection<FlowConstruct> flowConstructs = dummyApp.getMuleContext().getRegistry().lookupObjects(FlowConstruct.class);
        assertFalse("No FlowConstructs found in the sibling app", flowConstructs.isEmpty());
        FlowConstruct fc = flowConstructs.iterator().next();
        assertTrue(fc instanceof SimpleService);
        SimpleService service = (SimpleService) fc;
        // note that we don't have this class available to this test directly
        Class<?> clazz = ((JavaComponent) service.getComponent()).getObjectType();
        assertEquals("Wrong component implementation class", "org.mule.module.launcher.EchoTest", clazz.getName());
    }

    @Test
    public void testDeployZipOnStartup() throws Exception
    {
        final URL url = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));

        assertAppsDir(NONE, new String[] {"dummy-app"}, true);

        // just assert no priviledged entries were put in the registry
        final Application app = findApp("dummy-app", 1);
        final MuleRegistry registry = app.getMuleContext().getRegistry();
        final Object obj = registry.lookupObject(PriviledgedMuleApplication.REGISTRY_KEY_DEPLOYMENT_SERVICE);
        assertNull(obj);
        assertFalse(((ApplicationWrapper) app).getDelegate() instanceof PriviledgedMuleApplication);

        // mule-app.properties from the zip archive must have loaded properly
        assertEquals("mule-app.properties should have been loaded.", "someValue", registry.get("myCustomProp"));
    }

    @Test
    public void testUpdateAppViaZip() throws Exception
    {
        final URL url = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        // set up a new deployment latch (can't reuse the old one)
        deployLatch = new Latch();
        addAppArchive(url);
        assertTrue("Undeploy never invoked", undeployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[]{"dummy-app"}, true);
    }

    @Test
    public void testBrokenAppArchiveWithoutArgument() throws Exception
    {
        doBrokenAppArchiveTest();
    }

    @Test
    public void testBrokenAppArchiveAsArgument() throws Exception
    {
        Map<String, Object> startupOptions = new HashMap<String, Object>();
        startupOptions.put("app", "broken-app");
        StartupContext.get().setStartupOptions(startupOptions);

        doBrokenAppArchiveTest();
    }

    public void doBrokenAppArchiveTest() throws Exception
    {
        final URL url = getClass().getResource("/broken-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        deploymentService.start();

        assertTrue("Install never invoked", installLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));

        // Resets the latch to detect a new attempt to deploy the zip file
        installLatch = new Latch();

        // let the file system's write-behind cache commit the delete operation?
        Thread.sleep(1000);

        // zip stays intact, no app dir created
        assertAppsDir(new String[] {"broken-app.zip"}, NONE, true);
        // don't assert dir contents, we want to check internal deployer state next
        assertAppsDir(NONE, new String[] {"dummy-app"}, false);
        assertEquals("No apps should have been registered with Mule.", 0, deploymentService.getApplications().size());
        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "broken-app.zip", new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        // Checks that the invalid zip was not deployed again
        assertFalse("Install was invoked again for the broken application file", installLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testBrokenAppName() throws Exception
    {
        final URL url = getClass().getResource("/empty-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url, "app with spaces.zip");

        try
        {
            deploymentService.start();
        }
        catch (DeploymentInitException e)
        {
            assertTrue(e.getMessage().contains("may not contain spaces"));
        }

        // zip stays intact, no app dir created
        // %20 is returned by java file api :/
        assertAppsDir(new String[] {"app with spaces.zip"}, NONE, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        // Spaces are converted to %20 is returned by java file api :/
        String appName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
        assertEquals("Wrong URL tagged as zombie.", "app with spaces.zip",  appName);
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }
    
    @Test
    public void testConfigurableWorkingDirectoryApp() throws Exception
    {
        final URL url1 = getClass().getResource("/configurableApp.zip");
        assertNotNull("Test app file not found " + url1, url1);
        addAppArchive(url1);
        
        final URL url2 = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found " + url2, url2);
        addAppArchive(url2);

        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        deployLatch.release();

        assertAppsDir(NONE, new String[] {"configurableApp", "dummy-app"}, true);

        // just assert no priviledged entries were put in the registry
        final Application configurableApp = findApp("configurableApp", 2);
        final Application dummyApp = findApp("dummy-app", 2);
        
        final MuleRegistry registry1 = configurableApp.getMuleContext().getRegistry();
        final Object obj1 = registry1.lookupObject(PriviledgedMuleApplication.REGISTRY_KEY_DEPLOYMENT_SERVICE);
        assertNull(obj1);
        assertFalse(((ApplicationWrapper) configurableApp).getDelegate() instanceof PriviledgedMuleApplication);
        assertEquals("mule-app.properties should have been loaded.", "someValue", registry1.get("myCustomProp"));
        assertTrue(configurableApp.getMuleContext().getConfiguration().getWorkingDirectory().endsWith(".appT/configurableApp"));

        final MuleRegistry registry2 = dummyApp.getMuleContext().getRegistry();
        final Object obj2 = registry2.lookupObject(PriviledgedMuleApplication.REGISTRY_KEY_DEPLOYMENT_SERVICE);
        assertNull(obj2);
        assertFalse(((ApplicationWrapper) dummyApp).getDelegate() instanceof PriviledgedMuleApplication);
        assertEquals("mule-app.properties should have been loaded.", "someValue", registry2.get("myCustomProp"));
        assertTrue(dummyApp.getMuleContext().getConfiguration().getWorkingDirectory().endsWith(".mule/dummy-app"));
    }

    @Test
    public void testDeployAppNameWithZipSuffix() throws Exception
    {
        final URL url = getClass().getResource("/empty-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url, "empty-app.zip.zip");

        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        deployLatch = new Latch();
        assertAppsDir(NONE, new String[] {"empty-app.zip"}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        // Checks that the empty-app.zip folder is not processed as a zip file
        assertNoDeploymentInvoked();
    }

    @Test
    public void testDeployAsArgumentStartupOrder() throws Exception
    {
        final URL url = getClass().getResource("/empty-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url, "1.zip");
        addAppArchive(url, "2.zip");
        addAppArchive(url, "3.zip");

        Map<String, Object> startupOptions = new HashMap<String, Object>();
        startupOptions.put("app", "3:1:2");
        StartupContext.get().setStartupOptions(startupOptions);

        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));

        assertAppsDir(NONE, new String[] {"1", "2", "3"}, true);

        // When apps are passed as -app app1:app2:app3 the startup order matters
        List<Application> applications = deploymentService.getApplications();
        assertNotNull(applications);
        assertEquals(3, applications.size());
        assertEquals("3", applications.get(0).getAppName());
        assertEquals("1", applications.get(1).getAppName());
        assertEquals("2", applications.get(2).getAppName());
    }

    @Test
    public void testDeploysAppJustOnce() throws Exception
    {
        final URL url = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        Map<String, Object> startupOptions = new HashMap<String, Object>();
        startupOptions.put("app", "dummy-app:dummy-app:dummy-app");
        StartupContext.get().setStartupOptions(startupOptions);

        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);

        List<Application> applications = deploymentService.getApplications();
        assertEquals(1, applications.size());
    }

    @Test
    public void undeploysApplicationRemovingAnchorFile() throws Exception
    {
        final URL url = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found " + url, url);

        addAppArchive(url);
        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);

        assertTrue("Unable to remove anchor file", removeAnchorFile("dummy-app"));

        assertTrue("Undeploy never invoked", undeployLatch.await(2 * LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals("Application has not been properly unregistered from Mule", 0, deploymentService.getApplications().size());
    }

    @Test
    public void undeploysAppCompletelyEvenOnStoppingException() throws Exception
    {
        final URL url = getClass().getResource("/empty-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        TestApplicationFactory appFactory = new TestApplicationFactory(deploymentService);
        appFactory.setFailOnStopApplication(true);

        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertAppsDir(NONE, new String[] {"empty-app"}, true);

        deployLatch = new Latch();
        assertTrue("Unable to remove anchor file", removeAnchorFile("empty-app"));

        assertTrue("Undeploy never invoked", undeployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals("Application has not been properly unregistered from Mule", 0, deploymentService.getApplications().size());

        Prober prober = new PollingProber(LATCH_TIMEOUT, 100);
        File appFolder = new File(appsDir, "empty-app");
        prober.check(new FileDoesNotExists(appFolder));
    }

    @Test
    public void undeploysAppCompletelyEvenOnDisposingException() throws Exception
    {
        final URL url = getClass().getResource("/empty-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        TestApplicationFactory appFactory = new TestApplicationFactory(deploymentService);
        appFactory.setFailOnDisposeApplication(true);
        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertAppsDir(NONE, new String[] {"empty-app"}, true);

        deployLatch = new Latch();
        assertTrue("Unable to remove anchor file", removeAnchorFile("empty-app"));

        assertTrue("Undeploy never invoked", undeployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals("Application has not been properly unregistered from Mule", 0, deploymentService.getApplications().size());

        Prober prober = new PollingProber(LATCH_TIMEOUT, 100);
        File appFolder = new File(appsDir, "empty-app");
        prober.check(new FileDoesNotExists(appFolder));
    }

    @Test
    public void mantainsAppFolderOnDeploymentError() throws Exception
    {
        final URL url = getClass().getResource("/incompleteApp.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url, "incompleteApp.zip");

        deploymentService.start();

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        final URL extraApp = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found " + extraApp, extraApp);
        addAppArchive(extraApp);
        assertTrue("Deployer never invoked", deployLatch.await(LATCH_TIMEOUT, TimeUnit.MILLISECONDS));
        assertAppsDir(NONE, new String[] {"incompleteApp", "dummy-app"}, true);
    }

    private void assertNoDeploymentInvoked()
    {
        //TODO(pablo.kraan): look for a better way to test this
        boolean invoked;
        Prober prober = new PollingProber(DeploymentService.DEFAULT_CHANGES_CHECK_INTERVAL_MS * 2, 100);
        try
        {
            prober.check(new Probe()
            {
                public boolean isSatisfied()
                {
                    return deploymentService.getZombieMap().size() == 1;
                }

                public String describeFailure()
                {
                    return "App was never redeployed";
                }
            });

            invoked = true;
        }
        catch (AssertionError e)
        {
           invoked = false;
        }

        assertFalse("Deployer was invoked", invoked);
    }

    /**
     * Find a deployed app, performing some basic assertions.
     */
    private Application findApp(final String appName, int totalAppsExpected)
    {
        // list all apps to validate total count
        final List<Application> apps = deploymentService.getApplications();
        assertNotNull(apps);
        assertEquals(totalAppsExpected, apps.size());
        final Application app = deploymentService.findApplication(appName);
        assertNotNull(app);
        return app;
    }

    private void assertAppsDir(String[] expectedZips, String[] expectedApps, boolean performValidation)
    {
        final String[] actualZips = appsDir.list(DeploymentService.ZIP_APPS_FILTER);
        if (performValidation) {
            assertArrayEquals("Invalid Mule application archives set", expectedZips, actualZips);
        }
        final String[] actualApps = appsDir.list(DirectoryFileFilter.DIRECTORY);
        if (performValidation) {
            assertTrue("Invalid Mule exploded applications set",
                       CollectionUtils.isEqualCollection(Arrays.asList(expectedApps), Arrays.asList(actualApps)));
        }
    }

    /**
     * Copies a given app archive to the apps folder for deployment.
     */
    private void addAppArchive(URL url) throws IOException
    {
        addAppArchive(url, null);
    }

    /**
     * Copies a given app archive with a given target name to the apps folder for deployment
     */
    private void addAppArchive(URL url, String targetFile) throws IOException
    {
        // copy is not atomic, copy to a temp file and rename instead (rename is atomic)
        final String tempFileName = new File((targetFile == null ? url.getFile() : targetFile) + ".part").getName();
        final File tempFile = new File(appsDir, tempFileName);
        FileUtils.copyURLToFile(url, tempFile);
        tempFile.renameTo(new File(StringUtils.removeEnd(tempFile.getAbsolutePath(), ".part")));
    }

    /**
     * Removes a given application anchor file in order to start application undeployment
     * @param appName name of application to undeploy
     * @return true if anchor file was deleted, false otherwise
     */
    private boolean removeAnchorFile(String appName)
    {
        String anchorFileName = appName + DeploymentService.APP_ANCHOR_SUFFIX;
        File anchorFile = new File(appsDir, anchorFileName);

        return anchorFile.delete();
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
            installLatch.release();
            System.out.println("DeploymentServiceTestCase$TestDeployer.installFromAppDir");
            return delegate.installFromAppDir(packedMuleAppFileName);
        }

        public Application installFrom(URL url) throws IOException
        {
            installLatch.release();
            System.out.println("DeploymentServiceTestCase$TestDeployer.installFrom");
            return delegate.installFrom(url);
        }

    }
}
