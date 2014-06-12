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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.registry.MuleRegistry;
import org.mule.config.StartupContext;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.MuleApplicationClassLoaderFactory;
import org.mule.module.launcher.application.TestApplicationFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

public class DeploymentServiceTestCase extends AbstractMuleContextTestCase
{

    protected static final int DEPLOYMENT_TIMEOUT = 10000;
    protected static final String[] NONE = new String[0];
    protected static final int ONE_HOUR_IN_MILLISECONDS = 3600000;

    protected File muleHome;
    protected File appsDir;
    protected MuleDeploymentService deploymentService;
    protected DeploymentListener deploymentListener;

    @Rule
    public SystemProperty changeChangeInterval = new SystemProperty(MuleDeploymentService.CHANGE_CHECK_INTERVAL_PROPERTY, "10");

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
        deploymentService = new MuleDeploymentService(new MulePluginClassLoaderManager());
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
    public void deploysZipOnStartup() throws Exception
    {
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        assertAppsDir(NONE, new String[] {"dummy-app"}, true);

        // just assert no privileged entries were put in the registry
        final Application app = findApp("dummy-app", 1);
        final MuleRegistry registry = app.getMuleContext().getRegistry();

        // mule-app.properties from the zip archive must have loaded properly
        assertEquals("mule-app.properties should have been loaded.", "someValue", registry.get("myCustomProp"));
    }

    @Test
    public void deploysZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);

        // just assert no privileged entries were put in the registry
        final Application app = findApp("dummy-app", 1);
        final MuleRegistry registry = app.getMuleContext().getRegistry();

        // mule-app.properties from the zip archive must have loaded properly
        assertEquals("mule-app.properties should have been loaded.", "someValue", registry.get("myCustomProp"));
    }

    @Test
    public void deploysBrokenZipOnStartup() throws Exception
    {
        addPackedAppFromResource("/broken-app.zip", "brokenApp.zip");

        deploymentService.start();

        assertDeploymentFailure(deploymentListener, "brokenApp");

        assertAppsDir(new String[] {"brokenApp.zip"}, NONE, true);

        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "brokenApp.zip", new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    /**
     * This tests deploys a broken app which name has a weird character.
     * It verifies that after failing deploying that app, it doesn't try to do it
     * again, which is a behavior than can be seen in some file systems due to
     * path handling issues
     */
    @Test
    public void dontRetryBrokenAppWithFunkyName() throws Exception
    {
        addPackedAppFromResource("/broken-app+.zip", "brokenApp+.zip");

        deploymentService.start();

        assertDeploymentFailure(deploymentListener, "brokenApp+");

        assertAppsDir(new String[] {"brokenApp+.zip"}, NONE, true);

        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "brokenApp+.zip", new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(deploymentListener);

        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertDeploymentFailure(deploymentListener, "brokenApp+", never());

        addPackedAppFromResource("/empty-app.zip");
        assertDeploymentSuccess(deploymentListener, "empty-app");
        assertDeploymentFailure(deploymentListener, "brokenApp+", never());
    }

    @Test
    public void deploysBrokenZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/broken-app.zip", "brokenApp.zip");

        assertDeploymentFailure(deploymentListener, "brokenApp");

        assertAppsDir(new String[] {"brokenApp.zip"}, NONE, true);

        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "brokenApp.zip", new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void redeploysZipDeployedOnStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        assertAppsDir(NONE, new String[] {"dummy-app"}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(deploymentListener);

        addPackedAppFromResource("/dummy-app.zip");

        assertUndeploymentSuccess(deploymentListener, "dummy-app");
        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);
    }

    @Test
    public void redeploysZipDeployedAfterStartup() throws Exception
    {
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        assertAppsDir(NONE, new String[] {"dummy-app"}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(deploymentListener);

        addPackedAppFromResource("/dummy-app.zip");

        assertUndeploymentSuccess(deploymentListener, "dummy-app");
        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);
    }

    @Test
    public void deploysExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);
    }

    @Test
    public void deploysPackagedAppOnStartupWhenExplodedAppIsAlsoPresent() throws Exception
    {
        addExplodedAppFromResource("/dummy-app.zip");
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        addPackedAppFromResource("/empty-app.zip");

        assertDeploymentSuccess(deploymentListener, "empty-app");

        // Checks that dummy app was deployed just once
        assertDeploymentSuccess(deploymentListener, "dummy-app");
    }

    @Test
    public void deploysExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);
    }

    @Test
    public void deploysInvalidExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource("/dummy-app.zip", "app with spaces");

        deploymentService.start();

        assertDeploymentFailure(deploymentListener, "app with spaces");

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {"app with spaces"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        // Spaces are converted to %20 is returned by java file api :/
        String appName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
        assertEquals("Wrong URL tagged as zombie.", "app with spaces", appName);
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysInvalidExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource("/dummy-app.zip", "app with spaces");

        assertDeploymentFailure(deploymentListener, "app with spaces");

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {"app with spaces"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        // Spaces are converted to %20 is returned by java file api :/
        String appName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
        assertEquals("Wrong URL tagged as zombie.", "app with spaces", appName);
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysInvalidExplodedOnlyOnce() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource("/dummy-app.zip", "app with spaces");
        assertDeploymentFailure(deploymentListener, "app with spaces", atLeast(1));

        addExplodedAppFromResource("/dummy-app.zip");
        assertDeploymentSuccess(deploymentListener, "dummy-app");

        addExplodedAppFromResource("/empty-app.zip");
        assertDeploymentSuccess(deploymentListener, "empty-app");

        // After three update cycles should have only one deployment failure notification for the broken app
        assertDeploymentFailure(deploymentListener, "app with spaces");
    }

    @Test
    public void deploysBrokenExplodedAppOnStartup() throws Exception
    {
        final URL url = getClass().getResource("/incompleteApp.zip");
        assertNotNull("Test app file not found " + url, url);

        addExplodedAppFromResource("/incompleteApp.zip");

        deploymentService.start();

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {"incompleteApp"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {"incompleteApp"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void redeploysExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);

        reset(deploymentListener);

        File configFile = new File(appsDir + "/dummy-app", "mule-config.xml");
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentSuccess(deploymentListener, "dummy-app");
    }

    @Test
    public void redeploysExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);

        reset(deploymentListener);

        File configFile = new File(appsDir + "/dummy-app", "mule-config.xml");
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentSuccess(deploymentListener, "dummy-app");
    }

    @Test
    public void redeploysBrokenExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource("/incompleteApp.zip");

        deploymentService.start();

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {"incompleteApp"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(deploymentListener);

        File configFile = new File(appsDir + "/incompleteApp", "mule-config.xml");
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentFailure(deploymentListener, "incompleteApp");
    }

    @Test
    public void redeploysBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {"incompleteApp"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(deploymentListener);

        File configFile = new File(appsDir + "/incompleteApp", "mule-config.xml");
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentFailure(deploymentListener, "incompleteApp");
    }

    @Test
    public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentOnStartup() throws IOException, URISyntaxException
    {
        addExplodedAppFromResource("/dummy-app.zip", "dummy-app");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);

        reset(deploymentListener);

        File originalConfigFile = new File(appsDir + "/dummy-app", "mule-config.xml");
        URL url = getClass().getResource("/broken-config.xml");
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentFailure(deploymentListener, "dummy-app");
    }

    @Test
    public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentAfterStartup() throws IOException, URISyntaxException
    {
        deploymentService.start();

        addExplodedAppFromResource("/dummy-app.zip", "dummy-app");

        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertAppsDir(NONE, new String[] {"dummy-app"}, true);

        reset(deploymentListener);

        File originalConfigFile = new File(appsDir + "/dummy-app", "mule-config.xml");
        URL url = getClass().getResource("/broken-config.xml");
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentFailure(deploymentListener, "dummy-app");
    }

    @Test
    public void redeploysFixedAppAfterBrokenExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource("/incompleteApp.zip", "incompleteApp");

        deploymentService.start();

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        reset(deploymentListener);

        File originalConfigFile = new File(appsDir + "/incompleteApp", "mule-config.xml");
        URL url = getClass().getResource("/empty-config.xml");
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);
        assertDeploymentSuccess(deploymentListener, "incompleteApp");

        addPackedAppFromResource("/dummy-app.zip");
        assertDeploymentSuccess(deploymentListener, "dummy-app");

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained("incompleteApp");
    }

    @Test
    public void redeploysFixedAppAfterBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource("/incompleteApp.zip", "incompleteApp");


        assertDeploymentFailure(deploymentListener, "incompleteApp");

        reset(deploymentListener);

        File originalConfigFile = new File(appsDir + "/incompleteApp", "mule-config.xml");
        URL url = getClass().getResource("/empty-config.xml");
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentSuccess(deploymentListener, "incompleteApp");

        addPackedAppFromResource("/dummy-app.zip");
        assertDeploymentSuccess(deploymentListener, "dummy-app");

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained("incompleteApp");
    }


    @Test
    public void redeploysZipAppOnConfigChanges() throws Exception
    {
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        assertAppsDir(NONE, new String[] {"dummy-app"}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(deploymentListener);

        File configFile = new File(appsDir + "/dummy-app", "mule-config.xml");
        configFile.setLastModified(configFile.lastModified() + 1000);

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
        addPackedAppFromResource("/broken-app.zip");

        deploymentService.start();

        assertDeploymentFailure(deploymentListener, "broken-app");
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
    public void deploysInvalidZipAppOnStartup() throws Exception
    {
        addPackedAppFromResource("/empty-app.zip", "app with spaces.zip");

        deploymentService.start();
        assertDeploymentFailure(deploymentListener, "app with spaces");

        // zip stays intact, no app dir created
        assertAppsDir(new String[] {"app with spaces.zip"}, NONE, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        // Spaces are converted to %20 is returned by java file api :/
        String appName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
        assertEquals("Wrong URL tagged as zombie.", "app with spaces.zip", appName);
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysInvalidZipAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/empty-app.zip", "app with spaces.zip");

        assertDeploymentFailure(deploymentListener, "app with spaces");

        // zip stays intact, no app dir created
        assertAppsDir(new String[] {"app with spaces.zip"}, NONE, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieMap();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        // Spaces are converted to %20 is returned by java file api :/
        String appName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
        assertEquals("Wrong URL tagged as zombie.", "app with spaces.zip", appName);
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void testDeployAppNameWithZipSuffix() throws Exception
    {
        addPackedAppFromResource("/empty-app.zip", "empty-app.zip.zip");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "empty-app.zip");
        reset(deploymentListener);

        assertAppsDir(NONE, new String[] {"empty-app.zip"}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        // Checks that the empty-app.zip folder is not processed as a zip file
        assertNoDeploymentInvoked(deploymentListener);
    }

    @Test
    public void deploysPackedAppsInOrderWhenAppArgumentIsUsed() throws Exception
    {
        addPackedAppFromResource("/empty-app.zip", "1.zip");
        addPackedAppFromResource("/empty-app.zip", "2.zip");
        addPackedAppFromResource("/empty-app.zip", "3.zip");

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
    public void deploysExplodedAppsInOrderWhenAppArgumentIsUsed() throws Exception
    {
        addExplodedAppFromResource("/empty-app.zip", "1");
        addExplodedAppFromResource("/empty-app.zip", "2");
        addExplodedAppFromResource("/empty-app.zip", "3");

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
    public void deploysAppJustOnce() throws Exception
    {
        addPackedAppFromResource("/dummy-app.zip");

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
    public void tracksAppConfigUpdateTime() throws Exception
    {
        addExplodedAppFromResource("/dummy-app.zip");

        // Sets a modification time in the future
        File appFolder = new File(appsDir.getPath(), "dummy-app");
        File configFile = new File(appFolder, "mule-config.xml");
        configFile.setLastModified(System.currentTimeMillis() + ONE_HOUR_IN_MILLISECONDS);

        deploymentService.start();
        assertDeploymentSuccess(deploymentListener, "dummy-app");
        reset(deploymentListener);

        assertNoDeploymentInvoked(deploymentListener);
    }

    @Test
    public void receivesMuleContextDeploymentNotifications() throws Exception
    {
        // NOTE: need an integration test like this because DefaultMuleApplication
        // class cannot be unit tested.
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "dummy-app");
        assertMuleContextCreated(deploymentListener, "dummy-app");
        assertMuleContextInitialized(deploymentListener, "dummy-app");
        assertMuleContextConfigured(deploymentListener, "dummy-app");
    }

    @Test
    public void undeploysStoppedApp() throws Exception
    {
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "dummy-app");
        final Application app = findApp("dummy-app", 1);
        app.stop();

        deploymentService.undeploy(app);
    }

    @Test
    public void undeploysApplicationRemovingAnchorFile() throws Exception
    {
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        assertTrue("Unable to remove anchor file", removeAnchorFile("dummy-app"));

        assertUndeploymentSuccess(deploymentListener, "dummy-app");
    }

    @Test
    public void undeploysAppCompletelyEvenOnStoppingException() throws Exception
    {
        addPackedAppFromResource("/empty-app.zip");

        TestApplicationFactory appFactory = new TestApplicationFactory(new MuleApplicationClassLoaderFactory());
        appFactory.setFailOnStopApplication(true);

        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "empty-app");

        assertTrue("Unable to remove anchor file", removeAnchorFile("empty-app"));

        assertUndeploymentSuccess(deploymentListener, "empty-app");

        assertAppFolderIsDeleted("empty-app");
    }

    @Test
    public void undeploysAppCompletelyEvenOnDisposingException() throws Exception
    {
        addPackedAppFromResource("/empty-app.zip");

        TestApplicationFactory appFactory = new TestApplicationFactory(new MuleApplicationClassLoaderFactory());
        appFactory.setFailOnDisposeApplication(true);
        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "empty-app");

        assertTrue("Unable to remove anchor file", removeAnchorFile("empty-app"));

        assertUndeploymentSuccess(deploymentListener, "empty-app");

        assertAppFolderIsDeleted("empty-app");
    }

    @Test
    public void deploysIncompleteZipAppOnStartup() throws Exception
    {
        addPackedAppFromResource("/incompleteApp.zip");

        deploymentService.start();

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained("incompleteApp");
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieMap().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void deploysIncompleteZipAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained("incompleteApp");
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieMap().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    //@Test
    //public void redeploysOkAfterIncompleteZipAppOnStartupWhenConfigIsUpdated() throws Exception
    //{
    //    addPackedAppFromResource("/incompleteApp.zip");
    //
    //    deploymentService.start();
    //
    //    assertDeploymentFailure(deploymentListener, "incompleteApp");
    //
    //
    //    File originalConfigFile = new File(appsDir + "/incompleteApp", "mule-config.xml");
    //    URL url = getClass().getResource("/empty-config.xml");
    //    File newConfigFile = new File(url.toURI());
    //    FileUtils.copyFile(newConfigFile, originalConfigFile);
    //
    //
    //
    //    // Deploys another app to confirm that DeploymentService has execute the updater thread
    //    addPackedAppFromResource("/dummy-app.zip");
    //
    //    assertDeploymentSuccess(deploymentListener, "dummy-app");
    //
    //    // Check that the failed application folder is still there
    //    assertAppFolderIsMaintained("broken-xml-app");
    //    final Map.Entry<URL, Long> zombie = deploymentService.getZombieMap().entrySet().iterator().next();
    //    assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
    //}

    @Test
    public void mantainsAppFolderOnExplodedAppDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained("incompleteApp");
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieMap().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysZipAppAfterDeploymentErrorOnStartup() throws Exception
    {
        addPackedAppFromResource("/incompleteApp.zip");

        deploymentService.start();

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/empty-app.zip", "incompleteApp.zip");
        assertDeploymentSuccess(deploymentListener, "incompleteApp");

        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieMap().size());
    }

    @Test
    public void redeploysZipAppAfterDeploymentErrorAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/empty-app.zip", "incompleteApp.zip");
        assertDeploymentSuccess(deploymentListener, "incompleteApp");

        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieMap().size());
    }


    @Test
    public void redeploysInvalidZipAppAfterSuccessfulDeploymentOnStartup() throws IOException
    {
        addPackedAppFromResource("/empty-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(deploymentListener, "empty-app");

        addPackedAppFromResource("/incompleteApp.zip", "empty-app.zip");

        assertDeploymentFailure(deploymentListener, "empty-app");

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieMap().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "empty-app", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterSuccessfulDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedAppFromResource("/empty-app.zip");
        assertDeploymentSuccess(deploymentListener, "empty-app");

        addPackedAppFromResource("/incompleteApp.zip", "empty-app.zip");
        assertDeploymentFailure(deploymentListener, "empty-app");

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieMap().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "empty-app", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterFailedDeploymentOnStartup() throws IOException
    {
        addPackedAppFromResource("/incompleteApp.zip");

        deploymentService.start();

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        reset(deploymentListener);

        addPackedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieMap().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterFailedDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedAppFromResource("/incompleteApp.zip");
        assertDeploymentFailure(deploymentListener, "incompleteApp");

        reset(deploymentListener);

        addPackedAppFromResource("/incompleteApp.zip");
        assertDeploymentFailure(deploymentListener, "incompleteApp");

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieMap().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysExplodedAppAfterDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(deploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(deploymentListener, "dummy-app");

        // Redeploys a fixed version for incompleteApp
        addExplodedAppFromResource("/empty-app.zip", "incompleteApp");

        assertDeploymentSuccess(deploymentListener, "incompleteApp");
        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieMap().size());
    }

    @Test
    public void synchronizesDeploymentOnStart() throws Exception
    {
        addPackedAppFromResource("/empty-app.zip");

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
                try
                {
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

    private void assertMuleContextCreated(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                try
                {
                    verify(listener, times(1)).onMuleContextCreated(eq(appName), any(MuleContext.class));
                    return true;
                }
                catch (AssertionError e)
                {
                    return false;
                }
            }

            public String describeFailure()
            {
                return String.format("Did not received notification '%s' for app '%s'", "onMuleContextCreated", appName);
            }
        });
    }

    private void assertMuleContextInitialized(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                try
                {
                    verify(listener, times(1)).onMuleContextInitialised(eq(appName), any(MuleContext.class));
                    return true;
                }
                catch (AssertionError e)
                {
                    return false;
                }
            }

            public String describeFailure()
            {
                return String.format("Did not received notification '%s' for app '%s'", "onMuleContextInitialised", appName);
            }
        });
    }

    private void assertMuleContextConfigured(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                try
                {
                    verify(listener, times(1)).onMuleContextConfigured(eq(appName), any(MuleContext.class));
                    return true;
                }
                catch (AssertionError e)
                {
                    return false;
                }
            }

            public String describeFailure()
            {
                return String.format("Did not received notification '%s' for app '%s'", "onMuleContextConfigured", appName);
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
                try
                {
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
        assertDeploymentFailure(listener, appName, times(1));
    }

    private void assertDeploymentFailure(final DeploymentListener listener, final String appName, final VerificationMode mode)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                try
                {
                    verify(listener, mode).onDeploymentFailure(eq(appName), any(Throwable.class));
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
        Prober prober = new PollingProber(MuleDeploymentService.DEFAULT_CHANGES_CHECK_INTERVAL_MS * 2, 100);
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
        final String[] actualZips = appsDir.list(MuleDeploymentService.ZIP_APPS_FILTER);
        if (performValidation)
        {
            assertArrayEquals("Invalid Mule application archives set", expectedZips, actualZips);
        }
        final String[] actualApps = appsDir.list(DirectoryFileFilter.DIRECTORY);
        if (performValidation)
        {
            assertTrue("Invalid Mule exploded applications set",
                       CollectionUtils.isEqualCollection(Arrays.asList(expectedApps), Arrays.asList(actualApps)));
        }
    }

    /**
     * Copies a given app archive to the apps folder for deployment.
     */
    private void addPackedAppFromResource(String resource) throws IOException
    {
        addPackedAppFromResource(resource, null);
    }

    private void addPackedAppFromResource(String resource, String targetName) throws IOException
    {
        URL url = getClass().getResource(resource);
        assertNotNull("Test resource not found: " + url, url);

        addAppArchive(url, targetName);
    }

    /**
     * Copies a given app archive with a given target name to the apps folder for deployment
     */
    private void addAppArchive(URL url, String targetFile) throws IOException
    {
        ReentrantLock lock = deploymentService.getLock();

        lock.lock();
        try
        {
            // copy is not atomic, copy to a temp file and rename instead (rename is atomic)
            final String tempFileName = new File((targetFile == null ? url.getFile() : targetFile) + ".part").getName();
            final File tempFile = new File(appsDir, tempFileName);
            FileUtils.copyURLToFile(url, tempFile);
            tempFile.renameTo(new File(StringUtils.removeEnd(tempFile.getAbsolutePath(), ".part")));
        }
        finally
        {
            lock.unlock();
        }
    }

    private void addExplodedAppFromResource(String resource) throws IOException, URISyntaxException
    {
        addExplodedAppFromResource(resource, null);
    }

    private void addExplodedAppFromResource(String resource, String appName) throws IOException, URISyntaxException
    {
        URL url = getClass().getResource(resource);
        assertNotNull("Test resource not found: " + url, url);

        String appFolder = appName;
        if (appFolder == null)
        {
            File file = new File(url.getFile());
            int index = file.getName().lastIndexOf(".");

            if (index > 0)
            {
                appFolder = file.getName().substring(0, index);
            }
            else
            {
                appFolder = file.getName();
            }
        }

        addExplodedApp(url, appFolder);
    }

    /**
     * Copies a given app archive with a given target name to the apps folder for deployment
     */
    private void addExplodedApp(URL url, String appName) throws IOException, URISyntaxException
    {
        ReentrantLock lock = deploymentService.getLock();

        lock.lock();
        try
        {
            File tempFolder = new File(muleHome, appName);
            FileUtils.unzip(new File(url.toURI()), tempFolder);

            // Under some platforms, file.lastModified is managed at second level, not milliseconds.
            // Need to update the config file lastModified ere to ensure that is different from previous value
            File configFile = new File(tempFolder, "mule-config.xml");
            if (configFile.exists())
            {
                configFile.setLastModified(System.currentTimeMillis() + 1000);
            }

            File appFolder = new File(appsDir, appName);

            if (appFolder.exists())
            {
                FileUtils.deleteTree(appFolder);
            }

            FileUtils.moveDirectory(tempFolder, appFolder);
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Removes a given application anchor file in order to start application undeployment
     *
     * @param appName name of application to undeploy
     * @return true if anchor file was deleted, false otherwise
     */
    private boolean removeAnchorFile(String appName)
    {
        String anchorFileName = appName + MuleDeploymentService.APP_ANCHOR_SUFFIX;
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
