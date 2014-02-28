/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mule.MuleCoreExtension;
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DeploymentServiceTestCase extends AbstractMuleContextTestCase
{
    protected static final int DEPLOYMENT_TIMEOUT = 20000;
    protected static final String[] NONE = new String[0];
    protected static final int ONE_HOUR_IN_MILLISECONDS = 3600000;

    protected File muleHome;
    protected File appsDir;
    protected DeploymentService deploymentService;
    protected DeploymentListener deploymentListener;

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

        deploymentListener = mock(DeploymentListener.class);
        deploymentService = new DeploymentService(new HashMap<Class<? extends MuleCoreExtension>, MuleCoreExtension>());
        deploymentService.addDeploymentListener(deploymentListener);
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

        assertDeploymentSuccess(deploymentListener, "priviledged-dummy-app");

        assertAppsDir(NONE, new String[] {"priviledged-dummy-app"}, true);

        final Application app = findApp("priviledged-dummy-app", 1);
        // now that we're sure it's the app we wanted, assert the registry has everything
        // a 'privileged' app would have had
        final Object obj = app.getMuleContext().getRegistry().lookupObject(PriviledgedMuleApplication.REGISTRY_KEY_DEPLOYMENT_SERVICE);
        assertNotNull("Privileged objects have not been registered", obj);
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

        assertDeploymentSuccess(deploymentListener, "priviledged-dummy-app");
        assertDeploymentSuccess(deploymentListener, "dummy-app");

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

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        assertAppsDir(NONE, new String[] {"dummy-app"}, true);

        // just assert no privileged entries were put in the registry
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

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        assertAppsDir(NONE, new String[] {"dummy-app"}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(deploymentListener);
        addAppArchive(url);

        assertUndeploymentSuccess(deploymentListener, "dummy-app");
        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);
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

        assertDeploymentFailure(deploymentListener, "broken-app.zip");
        reset(deploymentListener);

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
        try
        {
            assertDeploymentFailure(deploymentListener, "broken-app.zip");
            fail("Install was invoked again for the broken application file");
        }
        catch (AssertionError expected)
        {
        }
    }

    @Test
    public void testBrokenAppName() throws Exception
    {
        final URL url = getClass().getResource("/empty-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url, "app with spaces.zip");

        deploymentService.start();
        assertDeploymentFailure(deploymentListener, "app with spaces.zip");

        // zip stays intact, no app dir created
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
    public void testDeployAppNameWithZipSuffix() throws Exception
    {
        final URL url = getClass().getResource("/empty-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url, "empty-app.zip.zip");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "empty-app.zip");
        reset(deploymentListener);

        assertAppsDir(NONE, new String[] {"empty-app.zip"}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        // Checks that the empty-app.zip folder is not processed as a zip file
        assertNoDeploymentInvoked(deploymentListener);
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

        assertDeploymentSuccess(deploymentListener, "1");
        assertDeploymentSuccess(deploymentListener, "2");
        assertDeploymentSuccess(deploymentListener, "3");
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

        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);

        List<Application> applications = deploymentService.getApplications();
        assertEquals(1, applications.size());
    }

    @Test
    public void testTracksAppConfigUpdateTime() throws Exception
    {
        final URL url = getClass().getResource("/dummy-app.zip");
        File appFolder = new File(appsDir.getPath(), "dummy-app");
        FileUtils.unzip(new File(url.toURI()), appFolder);

        // Sets a modification time in the future
        File configFile = new File(appFolder, "mule-config.xml");
        configFile.setLastModified(System.currentTimeMillis() + ONE_HOUR_IN_MILLISECONDS);

        deploymentService.start();
        assertDeploymentSuccess(deploymentListener, "dummy-app");
        reset(deploymentListener);

        assertNoDeploymentInvoked(deploymentListener);
    }

    @Test
    public void undeploysStoppedApp() throws Exception
    {
        final URL url = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "dummy-app");
        final Application app = findApp("dummy-app", 1);
        app.stop();

        deploymentService.undeploy(app);
    }

    @Test
    public void undeploysApplicationRemovingAnchorFile() throws Exception
    {
        final URL url = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found " + url, url);

        addAppArchive(url);
        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        assertTrue("Unable to remove anchor file", removeAnchorFile("dummy-app"));

        assertUndeploymentSuccess(deploymentListener, "dummy-app");
    }

    @Test
    public void undeploysAppCompletelyEvenOnStoppingException() throws Exception
    {
        final URL url = getClass().getResource("/empty-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        TestApplicationFactory appFactory = new TestApplicationFactory(deploymentService, Collections.EMPTY_MAP);
        appFactory.setFailOnStopApplication(true);

        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "empty-app");

        assertTrue("Unable to remove anchor file", removeAnchorFile("empty-app"));

        assertUndeploymentSuccess(deploymentListener, "empty-app");

        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        File appFolder = new File(appsDir, "empty-app");
        prober.check(new FileDoesNotExists(appFolder));
    }

    @Test
    public void undeploysAppCompletelyEvenOnDisposingException() throws Exception
    {
        final URL url = getClass().getResource("/empty-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url);

        TestApplicationFactory appFactory = new TestApplicationFactory(deploymentService, Collections.EMPTY_MAP);
        appFactory.setFailOnDisposeApplication(true);
        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "empty-app");

        assertTrue("Unable to remove anchor file", removeAnchorFile("empty-app"));

        assertUndeploymentSuccess(deploymentListener, "empty-app");

        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
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
        assertDeploymentFailure(deploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        final URL extraApp = getClass().getResource("/dummy-app.zip");
        assertNotNull("Test app file not found " + extraApp, extraApp);
        addAppArchive(extraApp);
        assertDeploymentSuccess(deploymentListener, "dummy-app");

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained("incompleteApp");
    }

    @Test
    public void synchronizesDeploymentOnStart() throws Exception
    {
        final URL url = getClass().getResource("/empty-app.zip");
        assertNotNull("Test app file not found " + url, url);
        addAppArchive(url, "empty-app.zip");

        Thread deploymentServiceThread = new Thread(new Runnable()
        {
            public void run()
            {
                deploymentService.start();
            }
        });

        final boolean[] lockedFromClient = new boolean[1];

        Mockito.doAnswer(new Answer()
        {
            public Object answer(InvocationOnMock invocation) throws Throwable
            {

                Thread deploymentClientThread = new Thread(new Runnable()
                {
                    public void run()
                    {
                        ReentrantLock deploymentLock = deploymentService.getLock();

                        try
                        {
                            try
                            {
                                lockedFromClient[0] = deploymentLock.tryLock(1000, TimeUnit.MILLISECONDS);
                            }
                            catch (InterruptedException e)
                            {
                                // Ignore
                            }
                        }
                        finally
                        {
                            if (deploymentLock.isHeldByCurrentThread())
                            {
                                deploymentLock.unlock();
                            }
                        }
                    }
                });

                deploymentClientThread.start();
                deploymentClientThread.join();

                return null;
            }
        }).when(deploymentListener).onDeploymentStart("empty-app");

        deploymentServiceThread.start();

        assertDeploymentSuccess(deploymentListener, "empty-app");

        assertFalse("Able to lock deployment service during start", lockedFromClient[0]);
    }

    private void assertDeploymentSuccess(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                try{
                    verify(listener, times(1)).onDeploymentSuccess(appName);
                    return true;
                }
                catch (AssertionError e)
                {
                    return false;
                }
            }

            public String describeFailure()
            {
                return "Failed to deploy application: " + appName;
            }
        });
    }


    private void assertUndeploymentSuccess(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                try{
                    verify(listener, times(1)).onUndeploymentSuccess(appName);
                    return true;
                }
                catch (AssertionError e)
                {
                    return false;
                }
            }

            public String describeFailure()
            {
                return "Failed to undeploy application: " + appName;
            }
        });
    }

    private void assertDeploymentFailure(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                try{
                    verify(listener, times(1)).onDeploymentFailure(eq(appName), any(Throwable.class));
                    return true;
                }
                catch (AssertionError e)
                {
                    return false;
                }
            }

            public String describeFailure()
            {
                return "Application deployment was supposed to fail for: " + appName;
            }
        });
    }

    private void assertNoDeploymentInvoked(final DeploymentListener deploymentListener)
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
                    try
                    {
                        verify(deploymentListener, times(1)).onDeploymentStart(any(String.class));
                        return true;
                    }
                    catch (AssertionError e)
                    {
                        return false;
                    }
                }

                public String describeFailure()
                {
                    return "No deployment has started";
                }
            });

            invoked = true;
        }
        catch (AssertionError e)
        {
            invoked = false;
        }

        assertFalse("A deployment was started", invoked);
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

    private void assertAppFolderIsDeleted(String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        File appFolder = new File(appsDir, appName);
        prober.check(new FileDoesNotExists(appFolder));
    }

    private void assertAppFolderIsMaintained(String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        File appFolder = new File(appsDir, appName);
        prober.check(new FileExists(appFolder));
    }
}
