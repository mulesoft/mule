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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
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
import org.mule.module.launcher.domain.Domain;
import org.mule.module.launcher.domain.MuleDomainClassLoaderRepository;
import org.mule.module.launcher.domain.TestDomainFactory;
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
    private static final String INVALID_DOMAIN_BUNDLE_PATH = "/invalid-domain-bundle.zip";
    private static final String INVALID_DOMAIN_BUNDLE = "invalid-domain-bundle";
    private static final String DUMMY_APP = "dummy-app";
    private static final String DUMMY_DOMAIN_BUNDLE_ZIP_PATH = "/dummy-domain-bundle.zip";
    private static final String DUMMY_DOMAIN_BUNDLE = "dummy-domain-bundle";

    protected File muleHome;
    protected File appsDir;
    protected File domainsDir;
    protected MuleDeploymentService deploymentService;
    protected DeploymentListener applicationDeploymentListener;
    protected DeploymentListener domainDeploymentListener;

    @Rule
    public SystemProperty changeChangeInterval = new SystemProperty(DeploymentDirectoryWatcher.CHANGE_CHECK_INTERVAL_PROPERTY, "10");

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // set up some mule home structure
        final String tmpDir = System.getProperty("java.io.tmpdir");
        muleHome = new File(tmpDir, getClass().getSimpleName() + System.currentTimeMillis());
        appsDir = new File(muleHome, "apps");
        appsDir.mkdirs();
        domainsDir = new File(muleHome, "domains");
        domainsDir.mkdirs();
        System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, muleHome.getCanonicalPath());

        new File(muleHome, "lib/shared/default").mkdirs();

        applicationDeploymentListener = mock(DeploymentListener.class);
        domainDeploymentListener = mock(DeploymentListener.class);
        deploymentService = new MuleDeploymentService(new MulePluginClassLoaderManager());
        deploymentService.addDeploymentListener(applicationDeploymentListener);
        deploymentService.addDomainDeploymentListener(domainDeploymentListener);
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
    public void deploysAppZipOnStartup() throws Exception
    {
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);

        // just assert no privileged entries were put in the registry
        final Application app = findApp(DUMMY_APP, 1);

        final MuleRegistry registry = getMuleRegistry(app);

        // mule-app.properties from the zip archive must have loaded properly
        assertEquals("mule-app.properties should have been loaded.", "someValue", registry.get("myCustomProp"));
    }

    @Test
    public void deploysAppZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);

        // just assert no privileged entries were put in the registry
        final Application app = findApp(DUMMY_APP, 1);
        final MuleRegistry registry = getMuleRegistry(app);

        // mule-app.properties from the zip archive must have loaded properly
        assertEquals("mule-app.properties should have been loaded.", "someValue", registry.get("myCustomProp"));
    }

    @Test
    public void deploysBrokenAppZipOnStartup() throws Exception
    {
        addPackedAppFromResource("/broken-app.zip", "brokenApp.zip");

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, "brokenApp");

        assertAppsDir(new String[] {"brokenApp.zip"}, NONE, true);

        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "brokenApp.zip", new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenAppZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/broken-app.zip", "brokenApp.zip");

        assertDeploymentFailure(applicationDeploymentListener, "brokenApp");

        assertAppsDir(new String[] {"brokenApp.zip"}, NONE, true);

        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "brokenApp.zip", new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void redeploysAppZipDeployedOnStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        addPackedAppFromResource("/dummy-app.zip");

        assertUndeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
    }

    @Test
    public void redeploysAppZipDeployedAfterStartup() throws Exception
    {
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        addPackedAppFromResource("/dummy-app.zip");

        assertUndeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
    }

    @Test
    public void deploysExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
    }

    @Test
    public void deploysPackagedAppOnStartupWhenExplodedAppIsAlsoPresent() throws Exception
    {
        addExplodedAppFromResource("/dummy-app.zip");
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        addPackedAppFromResource("/empty-app.zip");

        assertDeploymentSuccess(applicationDeploymentListener, "empty-app");

        // Checks that dummy app was deployed just once
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void deploysExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
    }

    @Test
    public void deploysInvalidExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource("/dummy-app.zip", "app with spaces");

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {"app with spaces"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
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

        assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {"app with spaces"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
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
        assertDeploymentFailure(applicationDeploymentListener, "app with spaces", atLeast(1));

        addExplodedAppFromResource("/dummy-app.zip");
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        addExplodedAppFromResource("/empty-app.zip");
        assertDeploymentSuccess(applicationDeploymentListener, "empty-app");

        // After three update cycles should have only one deployment failure notification for the broken app
        assertDeploymentFailure(applicationDeploymentListener, "app with spaces");
    }

    @Test
    public void deploysBrokenExplodedAppOnStartup() throws Exception
    {
        final URL url = getClass().getResource("/incompleteApp.zip");
        assertNotNull("Test app file not found " + url, url);

        addExplodedAppFromResource("/incompleteApp.zip");

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {"incompleteApp"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
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

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {"incompleteApp"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
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

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + "/dummy-app", "mule-config.xml");
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void redeploysExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + "/dummy-app", "mule-config.xml");
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void redeploysBrokenExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource("/incompleteApp.zip");

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {"incompleteApp"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + "/incompleteApp", "mule-config.xml");
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");
    }

    @Test
    public void redeploysBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {"incompleteApp"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + "/incompleteApp", "mule-config.xml");
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");
    }

    @Test
    public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentOnStartup() throws IOException, URISyntaxException
    {
        addExplodedAppFromResource("/dummy-app.zip", DUMMY_APP);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + "/dummy-app", "mule-config.xml");
        URL url = getClass().getResource("/broken-config.xml");
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentFailure(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentAfterStartup() throws IOException, URISyntaxException
    {
        deploymentService.start();

        addExplodedAppFromResource("/dummy-app.zip", DUMMY_APP);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + "/dummy-app", "mule-config.xml");
        URL url = getClass().getResource("/broken-config.xml");
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentFailure(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void redeploysFixedAppAfterBrokenExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource("/incompleteApp.zip", "incompleteApp");

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + "/incompleteApp", "mule-config.xml");
        URL url = getClass().getResource("/empty-config.xml");
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);
        assertDeploymentSuccess(applicationDeploymentListener, "incompleteApp");

        addPackedAppFromResource("/dummy-app.zip");
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained("incompleteApp");
    }

    @Test
    public void redeploysFixedAppAfterBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource("/incompleteApp.zip", "incompleteApp");


        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + "/incompleteApp", "mule-config.xml");
        URL url = getClass().getResource("/empty-config.xml");
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentSuccess(applicationDeploymentListener, "incompleteApp");

        addPackedAppFromResource("/dummy-app.zip");
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained("incompleteApp");
    }

    @Test
    public void redeploysZipAppOnConfigChanges() throws Exception
    {
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + "/dummy-app", "mule-config.xml");
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertUndeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
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

    @Test
    public void deploysInvalidZipAppOnStartup() throws Exception
    {
        addPackedAppFromResource("/empty-app.zip", "app with spaces.zip");

        deploymentService.start();
        assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

        // zip stays intact, no app dir created
        assertAppsDir(new String[] {"app with spaces.zip"}, NONE, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
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

        assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

        // zip stays intact, no app dir created
        assertAppsDir(new String[] {"app with spaces.zip"}, NONE, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
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

        assertDeploymentSuccess(applicationDeploymentListener, "empty-app.zip");
        reset(applicationDeploymentListener);

        assertAppsDir(NONE, new String[] {"empty-app.zip"}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        // Checks that the empty-app.zip folder is not processed as a zip file
        assertNoDeploymentInvoked(applicationDeploymentListener);
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

        assertDeploymentSuccess(applicationDeploymentListener, "1");
        assertDeploymentSuccess(applicationDeploymentListener, "2");
        assertDeploymentSuccess(applicationDeploymentListener, "3");
        assertAppsDir(NONE, new String[] {"1", "2", "3"}, true);

        // When apps are passed as -app app1:app2:app3 the startup order matters
        List<Application> applications = deploymentService.getApplications();
        assertNotNull(applications);
        assertEquals(3, applications.size());
        assertEquals("3", applications.get(0).getArtifactName());
        assertEquals("1", applications.get(1).getArtifactName());
        assertEquals("2", applications.get(2).getArtifactName());
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

        assertDeploymentSuccess(applicationDeploymentListener, "1");
        assertDeploymentSuccess(applicationDeploymentListener, "2");
        assertDeploymentSuccess(applicationDeploymentListener, "3");

        assertAppsDir(NONE, new String[] {"1", "2", "3"}, true);

        // When apps are passed as -app app1:app2:app3 the startup order matters
        List<Application> applications = deploymentService.getApplications();
        assertNotNull(applications);
        assertEquals(3, applications.size());
        assertEquals("3", applications.get(0).getArtifactName());
        assertEquals("1", applications.get(1).getArtifactName());
        assertEquals("2", applications.get(2).getArtifactName());
    }

    @Test
    public void deploysAppJustOnce() throws Exception
    {
        addPackedAppFromResource("/dummy-app.zip");

        Map<String, Object> startupOptions = new HashMap<String, Object>();
        startupOptions.put("app", "dummy-app:dummy-app:dummy-app");
        StartupContext.get().setStartupOptions(startupOptions);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);

        List<Application> applications = deploymentService.getApplications();
        assertEquals(1, applications.size());
    }

    @Test
    public void tracksAppConfigUpdateTime() throws Exception
    {
        addExplodedAppFromResource("/dummy-app.zip");

        // Sets a modification time in the future
        File appFolder = new File(appsDir.getPath(), DUMMY_APP);
        File configFile = new File(appFolder, "mule-config.xml");
        configFile.setLastModified(System.currentTimeMillis() + ONE_HOUR_IN_MILLISECONDS);

        deploymentService.start();
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        reset(applicationDeploymentListener);

        assertNoDeploymentInvoked(applicationDeploymentListener);
    }

    @Test
    public void redeployedFailedAppAfterTouched() throws Exception
    {
        addExplodedAppFromResource("/dummy-app.zip");

        File appFolder = new File(appsDir.getPath(), DUMMY_APP);

        File configFile = new File(appFolder, "mule-config.xml");
        FileUtils.writeStringToFile(configFile, "you shall not pass");

        deploymentService.start();
        assertDeploymentFailure(applicationDeploymentListener, DUMMY_APP);
        reset(applicationDeploymentListener);

        URL url = getClass().getResource("/empty-config.xml");
        FileUtils.copyFile(new File(url.toURI()), configFile);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void receivesMuleContextDeploymentNotifications() throws Exception
    {
        // NOTE: need an integration test like this because DefaultMuleApplication
        // class cannot be unit tested.
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertMuleContextCreated(applicationDeploymentListener, DUMMY_APP);
        assertMuleContextInitialized(applicationDeploymentListener, DUMMY_APP);
        assertMuleContextConfigured(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void undeploysStoppedApp() throws Exception
    {
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        final Application app = findApp(DUMMY_APP, 1);
        app.stop();

        deploymentService.undeploy(app);
    }

    @Test
    public void undeploysApplicationRemovingAnchorFile() throws Exception
    {
        addPackedAppFromResource("/dummy-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        assertTrue("Unable to remove anchor file", removeAppAnchorFile(DUMMY_APP));

        assertUndeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void undeploysAppCompletelyEvenOnStoppingException() throws Exception
    {
        addPackedAppFromResource("/empty-app.zip");

        TestApplicationFactory appFactory = new TestApplicationFactory(new MuleApplicationClassLoaderFactory(new MuleDomainClassLoaderRepository()));
        appFactory.setFailOnStopApplication(true);

        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, "empty-app");

        assertTrue("Unable to remove anchor file", removeAppAnchorFile("empty-app"));

        assertUndeploymentSuccess(applicationDeploymentListener, "empty-app");

        assertAppFolderIsDeleted("empty-app");
    }

    @Test
    public void undeploysAppCompletelyEvenOnDisposingException() throws Exception
    {
        addPackedAppFromResource("/empty-app.zip");

        TestApplicationFactory appFactory = new TestApplicationFactory(new MuleApplicationClassLoaderFactory(new MuleDomainClassLoaderRepository()));
        appFactory.setFailOnDisposeApplication(true);
        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, "empty-app");

        assertTrue("Unable to remove anchor file", removeAppAnchorFile("empty-app"));

        assertUndeploymentSuccess(applicationDeploymentListener, "empty-app");

        assertAppFolderIsDeleted("empty-app");
    }

    @Test
    public void deploysIncompleteZipAppOnStartup() throws Exception
    {
        addPackedAppFromResource("/incompleteApp.zip");

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained("incompleteApp");
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void deploysIncompleteZipAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained("incompleteApp");
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void mantainsAppFolderOnExplodedAppDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained("incompleteApp");
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysZipAppAfterDeploymentErrorOnStartup() throws Exception
    {
        addPackedAppFromResource("/incompleteApp.zip");

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/empty-app.zip", "incompleteApp.zip");
        assertDeploymentSuccess(applicationDeploymentListener, "incompleteApp");

        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieApplications().size());
    }

    @Test
    public void redeploysZipAppAfterDeploymentErrorAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/empty-app.zip", "incompleteApp.zip");
        assertDeploymentSuccess(applicationDeploymentListener, "incompleteApp");

        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieApplications().size());
    }

    @Test
    public void redeploysInvalidZipAppAfterSuccessfulDeploymentOnStartup() throws IOException
    {
        addPackedAppFromResource("/empty-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, "empty-app");

        addPackedAppFromResource("/incompleteApp.zip", "empty-app.zip");

        assertDeploymentFailure(applicationDeploymentListener, "empty-app");

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "empty-app", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterSuccessfulDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedAppFromResource("/empty-app.zip");
        assertDeploymentSuccess(applicationDeploymentListener, "empty-app");

        addPackedAppFromResource("/incompleteApp.zip", "empty-app.zip");
        assertDeploymentFailure(applicationDeploymentListener, "empty-app");

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "empty-app", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterFailedDeploymentOnStartup() throws IOException
    {
        addPackedAppFromResource("/incompleteApp.zip");

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        reset(applicationDeploymentListener);

        addPackedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterFailedDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedAppFromResource("/incompleteApp.zip");
        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        reset(applicationDeploymentListener);

        addPackedAppFromResource("/incompleteApp.zip");
        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteApp", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysExplodedAppAfterDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource("/incompleteApp.zip");

        assertDeploymentFailure(applicationDeploymentListener, "incompleteApp");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource("/dummy-app.zip");

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Redeploys a fixed version for incompleteApp
        addExplodedAppFromResource("/empty-app.zip", "incompleteApp");

        assertDeploymentSuccess(applicationDeploymentListener, "incompleteApp");
        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieApplications().size());
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
        }).when(applicationDeploymentListener).onDeploymentStart("empty-app");

        deploymentServiceThread.start();

        assertDeploymentSuccess(applicationDeploymentListener, "empty-app");

        assertFalse("Able to lock deployment service during start", lockedFromClient[0]);
    }

    @Test
    public void deploysDomainZipOnStartup() throws Exception
    {
        addPackedDomainFromResource("/dummy-domain.zip");

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        assertDomainDir(NONE, new String[] {"dummy-domain"}, true);

        final Domain domain = findADomain("dummy-domain", 1);
        assertNotNull(domain);
        assertNull(domain.getMuleContext());
    }


    @Test
    public void deploysExplodedDomainBundleOnStartup() throws Exception
    {
        addExplodedDomainFromResource(DUMMY_DOMAIN_BUNDLE_ZIP_PATH);

        deploymentService.start();

        deploysDomainBundle();
    }

    @Test
    public void deploysExplodedDomainBundleAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource(DUMMY_DOMAIN_BUNDLE_ZIP_PATH);

        deploysDomainBundle();
    }

    @Test
    public void deploysDomainBundleZipOnStartup() throws Exception
    {
        addPackedDomainFromResource(DUMMY_DOMAIN_BUNDLE_ZIP_PATH);

        deploymentService.start();

        deploysDomainBundle();
    }

    @Test
    public void deploysDomainBundleZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(DUMMY_DOMAIN_BUNDLE_ZIP_PATH);

        deploysDomainBundle();
    }

    private void deploysDomainBundle()
    {
        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN_BUNDLE);

        assertDomainDir(NONE, new String[] {DUMMY_DOMAIN_BUNDLE}, true);

        final Domain domain = findADomain(DUMMY_DOMAIN_BUNDLE, 1);
        assertNotNull(domain);
        assertNull(domain.getMuleContext());

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);

        final Application app = findApp(DUMMY_APP, 1);
        assertNotNull(app);
    }

    @Test
    public void deploysInvalidExplodedDomainBundleOnStartup() throws Exception
    {
        addExplodedDomainFromResource(INVALID_DOMAIN_BUNDLE_PATH);

        deploymentService.start();

        deploysInvalidDomainBundleZip();
    }

    @Test
    public void deploysInvalidExplodedDomainBundleAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource(INVALID_DOMAIN_BUNDLE_PATH);

        deploysInvalidDomainBundleZip();
    }

    @Test
    public void deploysInvalidDomainBundleZipOnStartup() throws Exception
    {
        addPackedDomainFromResource(INVALID_DOMAIN_BUNDLE_PATH);

        deploymentService.start();

        deploysInvalidDomainBundleZip();
    }

    @Test
    public void deploysInvalidDomainBundleZipAfterStartup() throws Exception
    {
        addPackedDomainFromResource(INVALID_DOMAIN_BUNDLE_PATH);

        deploymentService.start();

        deploysInvalidDomainBundleZip();
    }

    private void deploysInvalidDomainBundleZip()
    {
        assertDeploymentFailure(domainDeploymentListener, INVALID_DOMAIN_BUNDLE);

        assertDomainDir(NONE, new String[] {INVALID_DOMAIN_BUNDLE}, true);

        assertAppsDir(NONE, new String[] {}, true);
    }

    @Test
    public void deploysDomainZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource("/dummy-domain.zip");

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        assertDomainDir(NONE, new String[] {"dummy-domain"}, true);

        final Domain domain = findADomain("dummy-domain", 1);
        assertNotNull(domain);
        assertNull(domain.getMuleContext());

    }

    @Test
    public void deploysBrokenDomainZipOnStartup() throws Exception
    {
        addPackedDomainFromResource("/broken-domain.zip", "brokenDomain.zip");

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, "brokenDomain");

        assertDomainDir(new String[] {"brokenDomain.zip"}, NONE, true);

        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as domain", "brokenDomain.zip", new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenDomainZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource("/broken-domain.zip", "brokenDomain.zip");

        assertDeploymentFailure(domainDeploymentListener, "brokenDomain");

        assertDomainDir(new String[] {"brokenDomain.zip"}, NONE, true);

        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as domain.", "brokenDomain.zip", new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void redeploysDomainZipDeployedOnStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource("/dummy-domain.zip");

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        assertDomainDir(NONE, new String[] {"dummy-domain"}, true);
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());

        reset(domainDeploymentListener);

        addPackedDomainFromResource("/dummy-domain.zip");

        assertUndeploymentSuccess(domainDeploymentListener, "dummy-domain");
        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());
        assertDomainDir(NONE, new String[] {"dummy-domain"}, true);
    }

    @Test
    public void redeploysDomainZipDeployedAfterStartup() throws Exception
    {
        addPackedDomainFromResource("/dummy-domain.zip");

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        assertDomainDir(NONE, new String[] {"dummy-domain"}, true);
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());

        reset(domainDeploymentListener);

        addPackedDomainFromResource("/dummy-domain.zip");

        assertUndeploymentSuccess(domainDeploymentListener, "dummy-domain");
        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());
        assertDomainDir(NONE, new String[] {"dummy-domain"}, true);
    }

    @Test
    public void deploysExplodedDomainOnStartup() throws Exception
    {
        addExplodedDomainFromResource("/dummy-domain.zip");

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");
        assertDomainDir(NONE, new String[] {"dummy-domain"}, true);
    }

    @Test
    public void deploysPackagedDomainOnStartupWhenExplodedDomainIsAlsoPresent() throws Exception
    {
        addExplodedDomainFromResource("/dummy-domain.zip");
        addPackedDomainFromResource("/dummy-domain.zip");

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        addExplodedDomainFromResource("/empty-domain.zip");

        assertDeploymentSuccess(domainDeploymentListener, "empty-domain");

        // Checks that dummy app was deployed just once
        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");
    }

    @Test
    public void deploysExplodedDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource("/dummy-domain.zip");

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");
        assertDomainDir(NONE, new String[] {"dummy-domain"}, true);
    }

    @Test
    public void deploysInvalidExplodedDomainOnStartup() throws Exception
    {
        addExplodedDomainFromResource("/dummy-domain.zip", "domain with spaces");

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, "domain with spaces");

        // Maintains app dir created
        assertDomainDir(NONE, new String[] {"domain with spaces"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        // Spaces are converted to %20 is returned by java file api :/
        String domainName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
        assertEquals("Wrong URL tagged as zombie.", "domain with spaces", domainName);
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysInvalidExplodedDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource("/dummy-domain.zip", "domain with spaces");

        assertDeploymentFailure(domainDeploymentListener, "domain with spaces");

        // Maintains app dir created
        assertDomainDir(NONE, new String[] {"domain with spaces"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        // Spaces are converted to %20 is returned by java file api :/
        String appName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
        assertEquals("Wrong URL tagged as zombie.", "domain with spaces", appName);
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysInvalidExplodedDomainOnlyOnce() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource("/dummy-domain.zip", "domain with spaces");
        assertDeploymentFailure(domainDeploymentListener, "domain with spaces", atLeast(1));

        addExplodedDomainFromResource("/dummy-domain.zip");
        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        addExplodedDomainFromResource("/empty-domain.zip");
        assertDeploymentSuccess(domainDeploymentListener, "empty-domain");

        // After three update cycles should have only one deployment failure notification for the broken app
        assertDeploymentFailure(domainDeploymentListener, "domain with spaces");
    }

    @Test
    public void deploysBrokenExplodedDomainOnStartup() throws Exception
    {
        final URL url = getClass().getResource("/incompleteDomain.zip");
        assertNotNull("Test app file not found " + url, url);

        addExplodedDomainFromResource("/incompleteDomain.zip");

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        // Maintains app dir created
        assertDomainDir(NONE, new String[] {"incompleteDomain"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteDomain", new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenExplodedDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource("/incompleteDomain.zip");

        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        // Maintains app dir created
        assertDomainDir(NONE, new String[] {"incompleteDomain"}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteDomain", new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void receivesDomainMuleContextDeploymentNotifications() throws Exception
    {
        // NOTE: need an integration test like this because DefaultMuleApplication
        // class cannot be unit tested.
        addPackedDomainFromResource("/http-shared-domain.zip");

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, "http-shared-domain");
        assertMuleContextCreated(domainDeploymentListener, "http-shared-domain");
        assertMuleContextInitialized(domainDeploymentListener, "http-shared-domain");
        assertMuleContextConfigured(domainDeploymentListener, "http-shared-domain");
    }

    @Test
    public void undeploysStoppedDomain() throws Exception
    {
        addPackedDomainFromResource("/dummy-domain.zip");

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");
        final Domain domain = findADomain("dummy-domain", 1);
        domain.stop();

        deploymentService.undeploy(domain);
    }

    @Test
    public void undeploysDomainRemovingAnchorFile() throws Exception
    {
        addPackedDomainFromResource("/dummy-domain.zip");

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        assertTrue("Unable to remove anchor file", removeDomainAnchorFile("dummy-domain"));

        assertUndeploymentSuccess(domainDeploymentListener, "dummy-domain");
    }

    @Test
    public void undeploysDomainCompletelyEvenOnStoppingException() throws Exception
    {
        addPackedDomainFromResource("/empty-domain.zip");

        TestDomainFactory testDomainFactory = new TestDomainFactory(new MuleDomainClassLoaderRepository());
        testDomainFactory.setFailOnStopApplication();

        deploymentService.setDomainFactory(testDomainFactory);
        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, "empty-domain");

        assertTrue("Unable to remove anchor file", removeDomainAnchorFile("empty-domain"));

        assertUndeploymentSuccess(domainDeploymentListener, "empty-domain");

        assertAppFolderIsDeleted("empty-domain");
    }

    @Test
    public void undeploysDomainCompletelyEvenOnDisposingException() throws Exception
    {
        addPackedDomainFromResource("/empty-domain.zip");

        TestDomainFactory testDomainFactory = new TestDomainFactory(new MuleDomainClassLoaderRepository());
        testDomainFactory.setFailOnDisposeApplication();
        deploymentService.setDomainFactory(testDomainFactory);
        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, "empty-domain");

        assertTrue("Unable to remove anchor file", removeDomainAnchorFile("empty-domain"));

        assertUndeploymentSuccess(domainDeploymentListener, "empty-domain");

        assertAppFolderIsDeleted("empty-domain");
    }

    @Test
    public void deploysIncompleteZipDomainOnStartup() throws Exception
    {
        addPackedDomainFromResource("/incompleteDomain.zip");

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource("/dummy-domain.zip");

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained("incompleteDomain");
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteDomain", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void deploysIncompleteZipDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource("/incompleteDomain.zip");

        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource("/dummy-domain.zip");

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained("incompleteDomain");
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteDomain", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void mantainsDomainFolderOnExplodedAppDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource("/incompleteDomain.zip");

        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource("/dummy-domain.zip");

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained("incompleteDomain");
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteDomain", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysZipDomainAfterDeploymentErrorOnStartup() throws Exception
    {
        addPackedDomainFromResource("/incompleteDomain.zip");

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource("/dummy-domain.zip");

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource("/empty-domain.zip", "incompleteDomain.zip");
        assertDeploymentSuccess(domainDeploymentListener, "incompleteDomain");

        assertEquals("Failed domain still appears as zombie after a successful redeploy", 0, deploymentService.getZombieDomains().size());
    }

    @Test
    public void redeploysZipDomainAfterDeploymentErrorAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource("/incompleteDomain.zip");

        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource("/dummy-domain.zip");

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource("/empty-domain.zip", "incompleteDomain.zip");
        assertDeploymentSuccess(domainDeploymentListener, "incompleteDomain");

        assertEquals("Failed domain still appears as zombie after a successful redeploy", 0, deploymentService.getZombieDomains().size());
    }

    @Test
    public void redeploysInvalidZipDomainAfterSuccessfulDeploymentOnStartup() throws IOException
    {
        addPackedDomainFromResource("/empty-domain.zip");

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, "empty-domain");

        addPackedDomainFromResource("/incompleteDomain.zip", "empty-domain.zip");

        assertDeploymentFailure(domainDeploymentListener, "empty-domain");

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "empty-domain", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipDomainAfterSuccessfulDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedDomainFromResource("/empty-domain.zip");
        assertDeploymentSuccess(domainDeploymentListener, "empty-domain");

        addPackedDomainFromResource("/incompleteDomain.zip", "empty-domain.zip");
        assertDeploymentFailure(domainDeploymentListener, "empty-domain");

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "empty-domain", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipDomainAfterFailedDeploymentOnStartup() throws IOException
    {
        addPackedDomainFromResource("/incompleteDomain.zip");

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        reset(domainDeploymentListener);

        addPackedDomainFromResource("/incompleteDomain.zip");

        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteDomain", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipDomainAfterFailedDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedDomainFromResource("/incompleteDomain.zip");
        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        reset(domainDeploymentListener);

        addPackedDomainFromResource("/incompleteDomain.zip");
        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "incompleteDomain", new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysExplodedDomainAfterDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource("/incompleteDomain.zip");

        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource("/dummy-domain.zip");

        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        // Redeploys a fixed version for incompleteDomain
        addExplodedDomainFromResource("/empty-domain.zip", "incompleteDomain");

        assertDeploymentSuccess(domainDeploymentListener, "incompleteDomain");
        assertEquals("Failed domain still appears as zombie after a successful redeploy", 0, deploymentService.getZombieDomains().size());
    }

    public void doBrokenAppArchiveTest() throws Exception
    {
        addPackedAppFromResource("/broken-app.zip");

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, "broken-app");
        reset(applicationDeploymentListener);

        // let the file system's write-behind cache commit the delete operation?
        Thread.sleep(1000);

        // zip stays intact, no app dir created
        assertAppsDir(new String[] {"broken-app.zip"}, NONE, true);
        // don't assert dir contents, we want to check internal deployer state next
        assertAppsDir(NONE, new String[] {DUMMY_APP}, false);
        assertEquals("No apps should have been registered with Mule.", 0, deploymentService.getApplications().size());
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "broken-app.zip", new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        // Checks that the invalid zip was not deployed again
        try
        {
            assertDeploymentFailure(applicationDeploymentListener, "broken-app.zip");
            fail("Install was invoked again for the broken application file");
        }
        catch (AssertionError expected)
        {
        }
    }

    public void doBrokenDomainArchiveTest() throws Exception
    {
        addPackedDomainFromResource("/broken-domain.zip");

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, "broken-domain");
        reset(domainDeploymentListener);

        // let the file system's write-behind cache commit the delete operation?
        Thread.sleep(1000);

        // zip stays intact, no app dir created
        assertDomainDir(new String[] {"broken-domain.zip"}, NONE, true);
        // don't assert dir contents, we want to check internal deployer state next
        assertDomainDir(NONE, new String[] {"dummy-domain"}, false);
        assertEquals("No domains should have been registered with Mule.", 0, deploymentService.getDomains().size());
        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "broken-domain.zip", new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        // Checks that the invalid zip was not deployed again
        try
        {
            assertDeploymentFailure(domainDeploymentListener, "broken-domain.zip");
            fail("Install was invoked again for the broken doamin file");
        }
        catch (AssertionError expected)
        {
        }
    }

    private void assertDeploymentSuccess(final DeploymentListener listener, final String artifactName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                try
                {
                    verify(listener, times(1)).onDeploymentSuccess(artifactName);
                    return true;
                }
                catch (AssertionError e)
                {
                    return false;
                }
            }

            public String describeFailure()
            {
                return "Failed to deploy application: " + artifactName;
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
                return "Failed to undeployArtifact application: " + appName;
            }
        });
    }

    private MuleRegistry getMuleRegistry(Application app)
    {
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(app.getArtifactClassLoader().getClassLoader());
            return app.getMuleContext().getRegistry();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    private void assertDeploymentFailure(final DeploymentListener listener, final String artifactName)
    {
        assertDeploymentFailure(listener, artifactName, times(1));
    }

    private void assertDeploymentFailure(final DeploymentListener listener, final String artifactName, final VerificationMode mode)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                try
                {
                    verify(listener, mode).onDeploymentFailure(eq(artifactName), any(Throwable.class));
                    return true;
                }
                catch (AssertionError e)
                {
                    return false;
                }
            }

            public String describeFailure()
            {
                return "Application deployment was supposed to fail for: " + artifactName;
            }
        });
    }

    private void assertNoDeploymentInvoked(final DeploymentListener deploymentListener)
    {
        //TODO(pablo.kraan): look for a better way to test this
        boolean invoked;
        Prober prober = new PollingProber(DeploymentDirectoryWatcher.DEFAULT_CHANGES_CHECK_INTERVAL_MS * 2, 100);
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

    /**
     * Find a deployed app, performing some basic assertions.
     */
    private Domain findADomain(final String domainName, int totalAppsExpected)
    {
        // list all apps to validate total count
        final List<Domain> apps = deploymentService.getDomains();
        assertNotNull(apps);
        assertEquals(totalAppsExpected, apps.size());
        final Domain domain = deploymentService.findDomain(domainName);
        assertNotNull(domain);
        return domain;
    }

    private void assertAppsDir(String[] expectedZips, String[] expectedApps, boolean performValidation)
    {
        assertArtifactDir(appsDir, expectedZips, expectedApps, performValidation);
    }

    private void assertDomainDir(String[] expectedZips, String[] expectedDomains, boolean performValidation)
    {
        assertArtifactDir(domainsDir, expectedZips, expectedDomains, performValidation);
    }

    private void assertArtifactDir(File artifactDir, String[] expectedZips, String[] expectedArtifacts, boolean performValidation)
    {
        final String[] actualZips = artifactDir.list(MuleDeploymentService.ZIP_ARTIFACT_FILTER);
        if (performValidation)
        {
            assertArrayEquals("Invalid Mule artifact archives set", expectedZips, actualZips);
        }
        final String[] actualArtifacts = artifactDir.list(DirectoryFileFilter.DIRECTORY);
        if (performValidation)
        {
            assertTrue("Invalid Mule exploded artifact set",
                       CollectionUtils.isEqualCollection(Arrays.asList(expectedArtifacts), Arrays.asList(actualArtifacts)));
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
        addPackedArtifactFromResource(appsDir, resource, targetName);
    }

    /**
     * Copies a given app archive to the apps folder for deployment.
     */
    private void addPackedDomainFromResource(String resource) throws IOException
    {
        addPackedDomainFromResource(resource, null);
    }

    private void addPackedDomainFromResource(String resource, String targetName) throws IOException
    {
        addPackedArtifactFromResource(domainsDir, resource, targetName);
    }

    private void addPackedArtifactFromResource(File targetDir, String resource, String targetName) throws IOException
    {
        URL url = getClass().getResource(resource);
        assertNotNull("Test resource not found: " + url, url);
        addArchive(targetDir, url, targetName);
    }

    /**
     * Copies a given app archive with a given target name to the apps folder for deployment
     */
    private void addAppArchive(URL url, String targetFile) throws IOException
    {
        addArchive(appsDir, url, targetFile);
    }

    /**
     * Copies a given app archive with a given target name to the apps folder for deployment
     */
    private void addDomainArchive(URL url, String targetFile) throws IOException
    {
        addArchive(domainsDir, url, targetFile);
    }

    private void addArchive(File outputDir ,URL url, String targetFile) throws IOException
    {
        ReentrantLock lock = deploymentService.getLock();

        lock.lock();
        try
        {
            // copy is not atomic, copy to a temp file and rename instead (rename is atomic)
            final String tempFileName = new File((targetFile == null ? url.getFile() : targetFile) + ".part").getName();
            final File tempFile = new File(outputDir, tempFileName);
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

    private void addExplodedDomainFromResource(String resource) throws IOException, URISyntaxException
    {
        addExplodedDomainFromResource(resource, null);
    }

    private void addExplodedDomainFromResource(String resource, String domainName) throws IOException, URISyntaxException
    {
        addExplodedArtifactFromResource(resource, domainName, "mule-domain-config.xml", domainsDir);
    }

    private void addExplodedAppFromResource(String resource, String appName) throws IOException, URISyntaxException
    {
        addExplodedArtifactFromResource(resource, appName, "mule-config.xml", appsDir);
    }

    private void addExplodedArtifactFromResource(String resource, String artifactName, String configFileName, File destinationDir) throws IOException, URISyntaxException
    {
        URL url = getClass().getResource(resource);
        assertNotNull("Test resource not found: " + url, url);

        String artifactFolder = artifactName;
        if (artifactFolder == null)
        {
            File file = new File(url.getFile());
            int index = file.getName().lastIndexOf(".");

            if (index > 0)
            {
                artifactFolder = file.getName().substring(0, index);
            }
            else
            {
                artifactFolder = file.getName();
            }
        }

        addExplodedArtifact(url, artifactFolder, configFileName, destinationDir);
    }

    /**
     * Copies a given app archive with a given target name to the apps folder for deployment
     */
    private void addExplodedArtifact(URL url, String artifactName, String configFileName, File destinationDir) throws IOException, URISyntaxException
    {
        ReentrantLock lock = deploymentService.getLock();

        lock.lock();
        try
        {
            File tempFolder = new File(muleHome, artifactName);
            FileUtils.unzip(new File(url.toURI()), tempFolder);

            // Under some platforms, file.lastModified is managed at second level, not milliseconds.
            // Need to update the config file lastModified ere to ensure that is different from previous value
            File configFile = new File(tempFolder, configFileName);
            if (configFile.exists())
            {
                configFile.setLastModified(System.currentTimeMillis() + 1000);
            }

            File appFolder = new File(destinationDir, artifactName);

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
     * @param appName name of application to undeployArtifact
     * @return true if anchor file was deleted, false otherwise
     */
    private boolean removeAppAnchorFile(String appName)
    {
        String anchorFileName = appName + MuleDeploymentService.ARTIFACT_ANCHOR_SUFFIX;
        File anchorFile = new File(appsDir, anchorFileName);
        return anchorFile.delete();
    }

    /**
     * Removes a given domain anchor file in order to start application undeployment
     *
     * @param domainName name of application to undeployArtifact
     * @return true if anchor file was deleted, false otherwise
     */
    private boolean removeDomainAnchorFile(String domainName)
    {
        String anchorFileName = domainName + MuleDeploymentService.ARTIFACT_ANCHOR_SUFFIX;
        File anchorFile = new File(domainsDir, anchorFileName);
        return anchorFile.delete();
    }

    private boolean removeAnchorFile(String artifactName, File artifactDir)
    {
        String anchorFileName = artifactName + MuleDeploymentService.ARTIFACT_ANCHOR_SUFFIX;
        File anchorFile = new File(artifactDir, anchorFileName);
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
        assetArtifactFolderIsMaintained(appName, appsDir);
    }

    private void assertDomainFolderIsMaintained(String domainName)
    {
        assetArtifactFolderIsMaintained(domainName, domainsDir);
    }

    private void assetArtifactFolderIsMaintained(String artifactName, File artifactDir)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        File appFolder = new File(artifactDir, artifactName);
        prober.check(new FileExists(appFolder));
    }
}
