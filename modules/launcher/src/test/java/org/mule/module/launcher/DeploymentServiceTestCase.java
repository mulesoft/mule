/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.hamcrest.core.Is;
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

    private static final String MULE_CONFIG_XML_FILE = "mule-config.xml";

    //APP constants
    private static final String DUMMY_APP = "dummy-app";
    private static final String DUMMY_APP_PATH = "/dummy-app";
    private static final String DUMMY_APP_ZIP_PATH = "/dummy-app.zip";
    private static final String EMPTY_APP = "empty-app";
    private static final String EMPTY_APP_TARGET_NAME = "empty-app.zip";
    private static final String EMPTY_APP_ZIP_PATH = "/empty-app.zip";
    private static final String BROKEN_APP = "broken-app";
    private static final String BROKEN_APP_TARGET_NAME = "brokenApp.zip";
    private static final String BROKEN_APP_ZIP_PATH = "/broken-app.zip";
    private static final String INCOMPLETE_APP = "incompleteApp";
    private static final String INCOMPLETE_APP_PATH = "/incompleteApp";
    private static final String INCOMPLETE_APP_TARGET_NAME = "incompleteApp.zip";
    private static final String INCOMPLETE_APP_ZIP_PATH = "/incompleteApp.zip";

    //Domain constants
    private static final String BROKEN_DOMAIN = "brokenDomain";
    private static final String BROKEN_DOMAIN_TARGET_PATH = "brokenDomain.zip";
    private static final String BROKEN_DOMAIN_ZIP_PATH = "/broken-domain.zip";
    private static final String BROKEN_CONFIG_XML_FILE_PATH = "/broken-config.xml";
    private static final String DUMMY_DOMAIN = "dummy-domain";
    private static final String DUMMY_DOMAIN_ZIP_PATH = "/dummy-domain.zip";
    private static final String DUMMY_DOMAIN_APP1 = "dummy-domain-app1";
    private static final String DUMMY_DOMAIN_APP1_ZIP_PATH = "/dummy-domain-app1.zip";
    private static final String DUMMY_DOMAIN_APP2 = "dummy-domain-app2";
    private static final String DUMMY_DOMAIN_APP2_ZIP_PATH = "/dummy-domain-app2.zip";
    private static final String DUMMY_DOMAIN_BUNDLE = "dummy-domain-bundle";
    private static final String DUMMY_DOMAIN_BUNDLE_ZIP_PATH = "/dummy-domain-bundle.zip";
    private static final String EMPTY_DOMAIN = "empty-domain";
    private static final String EMPTY_DOMAIN_TARGET_PATH = "empty-domain.zip";
    private static final String EMPTY_DOMAIN_ZIP_PATH = "/empty-domain.zip";
    private static final String EMPTY_CONFIG_XML_FILE_PATH = "/empty-config.xml";
    private static final String INCOMPLETE_DOMAIN = "incompleteDomain";
    private static final String INCOMPLETE_DOMAIN_TARGET_PATH = "incompleteDomain.zip";
    private static final String INCOMPLETE_DOMAIN_ZIP_PATH = "/incompleteDomain.zip";
    private static final String INVALID_DOMAIN_BUNDLE = "invalid-domain-bundle";
    private static final String INVALID_DOMAIN_BUNDLE_ZIP_PATH = "/invalid-domain-bundle.zip";
    private static final String HTTP_SHARED_DOMAIN_ZIP_PATH = "/http-shared-domain.zip";
    private static final String HTTP_SHARED_DOMAIN = "http-shared-domain";

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
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

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

        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

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
        addPackedAppFromResource(BROKEN_APP_ZIP_PATH, BROKEN_APP_TARGET_NAME);

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

        addPackedAppFromResource(BROKEN_APP_ZIP_PATH, "brokenApp.zip");

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

        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        assertUndeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
    }

    @Test
    public void redeploysAppZipDeployedAfterStartup() throws Exception
    {
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        assertUndeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
    }

    @Test
    public void deploysExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
    }

    @Test
    public void deploysPackagedAppOnStartupWhenExplodedAppIsAlsoPresent() throws Exception
    {
        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH);
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        addPackedAppFromResource(EMPTY_APP_ZIP_PATH);

        assertDeploymentSuccess(applicationDeploymentListener, EMPTY_APP);

        // Checks that dummy app was deployed just once
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void deploysExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
    }

    @Test
    public void deploysInvalidExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH, "app with spaces");

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

        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH, "app with spaces");

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

        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH, "app with spaces");
        assertDeploymentFailure(applicationDeploymentListener, "app with spaces", atLeast(1));

        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH);
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        addExplodedAppFromResource(EMPTY_APP_ZIP_PATH);
        assertDeploymentSuccess(applicationDeploymentListener, EMPTY_APP);

        // After three update cycles should have only one deployment failure notification for the broken app
        assertDeploymentFailure(applicationDeploymentListener, "app with spaces");
    }

    @Test
    public void deploysBrokenExplodedAppOnStartup() throws Exception
    {
        final URL url = getClass().getResource(INCOMPLETE_APP_ZIP_PATH);
        assertNotNull("Test app file not found " + url, url);

        addExplodedAppFromResource(INCOMPLETE_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {INCOMPLETE_APP}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_APP, new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource(INCOMPLETE_APP_ZIP_PATH);

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {INCOMPLETE_APP}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_APP, new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void redeploysExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + DUMMY_APP_PATH, MULE_CONFIG_XML_FILE);
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void redeploysExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + DUMMY_APP_PATH, MULE_CONFIG_XML_FILE);
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void redeploysBrokenExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource(INCOMPLETE_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {INCOMPLETE_APP}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_APP, new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + INCOMPLETE_APP_PATH, MULE_CONFIG_XML_FILE);
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);
    }

    @Test
    public void redeploysBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource(INCOMPLETE_APP_ZIP_PATH);

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {INCOMPLETE_APP}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_APP, new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + INCOMPLETE_APP_PATH, MULE_CONFIG_XML_FILE);
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);
    }

    @Test
    public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentOnStartup() throws IOException, URISyntaxException
    {
        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH, DUMMY_APP);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + DUMMY_APP_PATH, MULE_CONFIG_XML_FILE);
        URL url = getClass().getResource(BROKEN_CONFIG_XML_FILE_PATH);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentFailure(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentAfterStartup() throws IOException, URISyntaxException
    {
        deploymentService.start();

        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH, DUMMY_APP);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + DUMMY_APP_PATH, MULE_CONFIG_XML_FILE);
        URL url = getClass().getResource(BROKEN_CONFIG_XML_FILE_PATH);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentFailure(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void redeploysFixedAppAfterBrokenExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource(INCOMPLETE_APP_ZIP_PATH, INCOMPLETE_APP);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + INCOMPLETE_APP_PATH, MULE_CONFIG_XML_FILE);
        URL url = getClass().getResource(EMPTY_CONFIG_XML_FILE_PATH);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);
        assertDeploymentSuccess(applicationDeploymentListener, INCOMPLETE_APP);

        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(INCOMPLETE_APP);
    }

    @Test
    public void redeploysFixedAppAfterBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource(INCOMPLETE_APP_ZIP_PATH, INCOMPLETE_APP);


        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + INCOMPLETE_APP_PATH, MULE_CONFIG_XML_FILE);
        URL url = getClass().getResource(EMPTY_CONFIG_XML_FILE_PATH);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentSuccess(applicationDeploymentListener, INCOMPLETE_APP);

        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(INCOMPLETE_APP);
    }

    @Test
    public void redeploysZipAppOnConfigChanges() throws Exception
    {
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        assertAppsDir(NONE, new String[] {DUMMY_APP}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + DUMMY_APP_PATH, MULE_CONFIG_XML_FILE);
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
        startupOptions.put("app", BROKEN_APP);
        StartupContext.get().setStartupOptions(startupOptions);

        doBrokenAppArchiveTest();
    }

    @Test
    public void deploysInvalidZipAppOnStartup() throws Exception
    {
        addPackedAppFromResource(EMPTY_APP_ZIP_PATH, "app with spaces.zip");

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

        addPackedAppFromResource(EMPTY_APP_ZIP_PATH, "app with spaces.zip");

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
        addPackedAppFromResource(EMPTY_APP_ZIP_PATH, "empty-app.zip.zip");

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, EMPTY_APP_TARGET_NAME);
        reset(applicationDeploymentListener);

        assertAppsDir(NONE, new String[] {EMPTY_APP_TARGET_NAME}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        // Checks that the empty-app.zip folder is not processed as a zip file
        assertNoDeploymentInvoked(applicationDeploymentListener);
    }

    @Test
    public void deploysPackedAppsInOrderWhenAppArgumentIsUsed() throws Exception
    {
        addPackedAppFromResource(EMPTY_APP_ZIP_PATH, "1.zip");
        addPackedAppFromResource(EMPTY_APP_ZIP_PATH, "2.zip");
        addPackedAppFromResource(EMPTY_APP_ZIP_PATH, "3.zip");

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
        addExplodedAppFromResource(EMPTY_APP_ZIP_PATH, "1");
        addExplodedAppFromResource(EMPTY_APP_ZIP_PATH, "2");
        addExplodedAppFromResource(EMPTY_APP_ZIP_PATH, "3");

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
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

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
        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH);

        // Sets a modification time in the future
        File appFolder = new File(appsDir.getPath(), DUMMY_APP);
        File configFile = new File(appFolder, MULE_CONFIG_XML_FILE);
        configFile.setLastModified(System.currentTimeMillis() + ONE_HOUR_IN_MILLISECONDS);

        deploymentService.start();
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        reset(applicationDeploymentListener);

        assertNoDeploymentInvoked(applicationDeploymentListener);
    }

    @Test
    public void redeployedFailedAppAfterTouched() throws Exception
    {
        addExplodedAppFromResource(DUMMY_APP_ZIP_PATH);

        File appFolder = new File(appsDir.getPath(), DUMMY_APP);

        File configFile = new File(appFolder, MULE_CONFIG_XML_FILE);
        FileUtils.writeStringToFile(configFile, "you shall not pass");

        deploymentService.start();
        assertDeploymentFailure(applicationDeploymentListener, DUMMY_APP);
        reset(applicationDeploymentListener);

        URL url = getClass().getResource(EMPTY_CONFIG_XML_FILE_PATH);
        FileUtils.copyFile(new File(url.toURI()), configFile);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void receivesMuleContextDeploymentNotifications() throws Exception
    {
        // NOTE: need an integration test like this because DefaultMuleApplication
        // class cannot be unit tested.
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        assertMuleContextCreated(applicationDeploymentListener, DUMMY_APP);
        assertMuleContextInitialized(applicationDeploymentListener, DUMMY_APP);
        assertMuleContextConfigured(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void undeploysStoppedApp() throws Exception
    {
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
        final Application app = findApp(DUMMY_APP, 1);
        app.stop();

        deploymentService.undeploy(app);
    }

    @Test
    public void undeploysApplicationRemovingAnchorFile() throws Exception
    {
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        assertTrue("Unable to remove anchor file", removeAppAnchorFile(DUMMY_APP));

        assertUndeploymentSuccess(applicationDeploymentListener, DUMMY_APP);
    }

    @Test
    public void undeploysAppCompletelyEvenOnStoppingException() throws Exception
    {
        addPackedAppFromResource(EMPTY_APP_ZIP_PATH);

        TestApplicationFactory appFactory = new TestApplicationFactory(new MuleApplicationClassLoaderFactory(new MuleDomainClassLoaderRepository()));
        appFactory.setFailOnStopApplication(true);

        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, EMPTY_APP);

        assertTrue("Unable to remove anchor file", removeAppAnchorFile(EMPTY_APP));

        assertUndeploymentSuccess(applicationDeploymentListener, EMPTY_APP);

        assertAppFolderIsDeleted(EMPTY_APP);
    }

    @Test
    public void undeploysAppCompletelyEvenOnDisposingException() throws Exception
    {
        addPackedAppFromResource(EMPTY_APP_ZIP_PATH);

        TestApplicationFactory appFactory = new TestApplicationFactory(new MuleApplicationClassLoaderFactory(new MuleDomainClassLoaderRepository()));
        appFactory.setFailOnDisposeApplication(true);
        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, EMPTY_APP);

        assertTrue("Unable to remove anchor file", removeAppAnchorFile(EMPTY_APP));

        assertUndeploymentSuccess(applicationDeploymentListener, EMPTY_APP);

        assertAppFolderIsDeleted(EMPTY_APP);
    }

    @Test
    public void deploysIncompleteZipAppOnStartup() throws Exception
    {
        addPackedAppFromResource(INCOMPLETE_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(INCOMPLETE_APP);
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_APP, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void deploysIncompleteZipAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource(INCOMPLETE_APP_ZIP_PATH);

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(INCOMPLETE_APP);
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_APP, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void mantainsAppFolderOnExplodedAppDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource(INCOMPLETE_APP_ZIP_PATH);

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(INCOMPLETE_APP);
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_APP, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysZipAppAfterDeploymentErrorOnStartup() throws Exception
    {
        addPackedAppFromResource(INCOMPLETE_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(EMPTY_APP_ZIP_PATH, INCOMPLETE_APP_TARGET_NAME);
        assertDeploymentSuccess(applicationDeploymentListener, INCOMPLETE_APP);

        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieApplications().size());
    }

    @Test
    public void redeploysZipAppAfterDeploymentErrorAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource(INCOMPLETE_APP_ZIP_PATH);

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(EMPTY_APP_ZIP_PATH, INCOMPLETE_APP_TARGET_NAME);
        assertDeploymentSuccess(applicationDeploymentListener, INCOMPLETE_APP);

        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieApplications().size());
    }

    @Test
    public void redeploysInvalidZipAppAfterSuccessfulDeploymentOnStartup() throws IOException
    {
        addPackedAppFromResource(EMPTY_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, EMPTY_APP);

        addPackedAppFromResource(INCOMPLETE_APP_ZIP_PATH, EMPTY_APP_TARGET_NAME);

        assertDeploymentFailure(applicationDeploymentListener, EMPTY_APP);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", EMPTY_APP, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterSuccessfulDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedAppFromResource(EMPTY_APP_ZIP_PATH);
        assertDeploymentSuccess(applicationDeploymentListener, EMPTY_APP);

        addPackedAppFromResource(INCOMPLETE_APP_ZIP_PATH, EMPTY_APP_TARGET_NAME);
        assertDeploymentFailure(applicationDeploymentListener, EMPTY_APP);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", EMPTY_APP, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterFailedDeploymentOnStartup() throws IOException
    {
        addPackedAppFromResource(INCOMPLETE_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        reset(applicationDeploymentListener);

        addPackedAppFromResource(INCOMPLETE_APP_ZIP_PATH);

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_APP, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterFailedDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedAppFromResource(INCOMPLETE_APP_ZIP_PATH);
        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        reset(applicationDeploymentListener);

        addPackedAppFromResource(INCOMPLETE_APP_ZIP_PATH);
        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_APP, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysExplodedAppAfterDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource(INCOMPLETE_APP_ZIP_PATH);

        assertDeploymentFailure(applicationDeploymentListener, INCOMPLETE_APP);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(DUMMY_APP_ZIP_PATH);

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_APP);

        // Redeploys a fixed version for incompleteApp
        addExplodedAppFromResource(EMPTY_APP_ZIP_PATH, INCOMPLETE_APP);

        assertDeploymentSuccess(applicationDeploymentListener, INCOMPLETE_APP);
        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieApplications().size());
    }

    @Test
    public void synchronizesDeploymentOnStart() throws Exception
    {
        addPackedAppFromResource(EMPTY_APP_ZIP_PATH);

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
        }).when(applicationDeploymentListener).onDeploymentStart(EMPTY_APP);

        deploymentServiceThread.start();

        assertDeploymentSuccess(applicationDeploymentListener, EMPTY_APP);

        assertFalse("Able to lock deployment service during start", lockedFromClient[0]);
    }

    @Test
    public void deploysDomainZipOnStartup() throws Exception
    {
        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        assertDomainDir(NONE, new String[] {DUMMY_DOMAIN}, true);

        final Domain domain = findADomain(DUMMY_DOMAIN, 1);
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
        addExplodedDomainFromResource(INVALID_DOMAIN_BUNDLE_ZIP_PATH);

        deploymentService.start();

        deploysInvalidDomainBundleZip();
    }

    @Test
    public void deploysInvalidExplodedDomainBundleAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource(INVALID_DOMAIN_BUNDLE_ZIP_PATH);

        deploysInvalidDomainBundleZip();
    }

    @Test
    public void deploysInvalidDomainBundleZipOnStartup() throws Exception
    {
        addPackedDomainFromResource(INVALID_DOMAIN_BUNDLE_ZIP_PATH);

        deploymentService.start();

        deploysInvalidDomainBundleZip();
    }

    @Test
    public void deploysInvalidDomainBundleZipAfterStartup() throws Exception
    {
        addPackedDomainFromResource(INVALID_DOMAIN_BUNDLE_ZIP_PATH);

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

        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        assertDomainDir(NONE, new String[] {DUMMY_DOMAIN}, true);

        final Domain domain = findADomain(DUMMY_DOMAIN, 1);
        assertNotNull(domain);
        assertNull(domain.getMuleContext());

    }

    @Test
    public void deploysBrokenDomainZipOnStartup() throws Exception
    {
        addPackedDomainFromResource(BROKEN_DOMAIN_ZIP_PATH, BROKEN_DOMAIN_TARGET_PATH);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, BROKEN_DOMAIN);

        assertDomainDir(new String[] {BROKEN_DOMAIN_TARGET_PATH}, NONE, true);

        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as domain", BROKEN_DOMAIN_TARGET_PATH, new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenDomainZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(BROKEN_DOMAIN_ZIP_PATH, BROKEN_DOMAIN_TARGET_PATH);

        assertDeploymentFailure(domainDeploymentListener, BROKEN_DOMAIN);

        assertDomainDir(new String[] {BROKEN_DOMAIN_TARGET_PATH}, NONE, true);

        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as domain.", BROKEN_DOMAIN_TARGET_PATH, new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void redeploysDomainZipDeployedOnStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        assertDomainDir(NONE, new String[] {DUMMY_DOMAIN}, true);
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());

        reset(domainDeploymentListener);

        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        assertUndeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);
        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());
        assertDomainDir(NONE, new String[] {DUMMY_DOMAIN}, true);
    }

    @Test
    public void redeploysDomainZipDeployedAfterStartup() throws Exception
    {
        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        assertDomainDir(NONE, new String[] {DUMMY_DOMAIN}, true);
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());

        reset(domainDeploymentListener);

        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        assertUndeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);
        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());
        assertDomainDir(NONE, new String[] {DUMMY_DOMAIN}, true);
    }

    @Test
    public void deploysExplodedDomainOnStartup() throws Exception
    {
        addExplodedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);
        assertDomainDir(NONE, new String[] {DUMMY_DOMAIN}, true);
    }

    @Test
    public void deploysPackagedDomainOnStartupWhenExplodedDomainIsAlsoPresent() throws Exception
    {
        addExplodedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);
        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        addExplodedDomainFromResource(EMPTY_DOMAIN_ZIP_PATH);

        assertDeploymentSuccess(domainDeploymentListener, EMPTY_DOMAIN);

        // Checks that dummy app was deployed just once
        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);
    }

    @Test
    public void deploysExplodedDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);
        assertDomainDir(NONE, new String[] {DUMMY_DOMAIN}, true);
    }

    @Test
    public void deploysInvalidExplodedDomainOnStartup() throws Exception
    {
        addExplodedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH, "domain with spaces");

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

        addExplodedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH, "domain with spaces");

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

        addExplodedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH, "domain with spaces");
        assertDeploymentFailure(domainDeploymentListener, "domain with spaces", atLeast(1));

        addExplodedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);
        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        addExplodedDomainFromResource(EMPTY_DOMAIN_ZIP_PATH);
        assertDeploymentSuccess(domainDeploymentListener, EMPTY_DOMAIN);

        // After three update cycles should have only one deployment failure notification for the broken app
        assertDeploymentFailure(domainDeploymentListener, "domain with spaces");
    }

    @Test
    public void deploysBrokenExplodedDomainOnStartup() throws Exception
    {
        final URL url = getClass().getResource(INCOMPLETE_DOMAIN_ZIP_PATH);
        assertNotNull("Test app file not found " + url, url);

        addExplodedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, INCOMPLETE_DOMAIN);

        // Maintains app dir created
        assertDomainDir(NONE, new String[] {INCOMPLETE_DOMAIN}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_DOMAIN, new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenExplodedDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH);

        assertDeploymentFailure(domainDeploymentListener, INCOMPLETE_DOMAIN);

        // Maintains app dir created
        assertDomainDir(NONE, new String[] {INCOMPLETE_DOMAIN}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_DOMAIN, new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void receivesDomainMuleContextDeploymentNotifications() throws Exception
    {
        // NOTE: need an integration test like this because DefaultMuleApplication
        // class cannot be unit tested.
        addPackedDomainFromResource(HTTP_SHARED_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, HTTP_SHARED_DOMAIN);
        assertMuleContextCreated(domainDeploymentListener, HTTP_SHARED_DOMAIN);
        assertMuleContextInitialized(domainDeploymentListener, HTTP_SHARED_DOMAIN);
        assertMuleContextConfigured(domainDeploymentListener, HTTP_SHARED_DOMAIN);
    }

    @Test
    public void undeploysStoppedDomain() throws Exception
    {
        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);
        final Domain domain = findADomain(DUMMY_DOMAIN, 1);
        domain.stop();

        deploymentService.undeploy(domain);
    }

    @Test
    public void undeploysDomainRemovingAnchorFile() throws Exception
    {
        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        assertTrue("Unable to remove anchor file", removeDomainAnchorFile(DUMMY_DOMAIN));

        assertUndeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);
    }

    @Test
    public void undeploysDomainAndDomainsApps() throws Exception
    {
        undeployDomainAndVerifyAppsAreUndeployed(new Action()
        {
            @Override
            public void perform()
            {
                Domain domain = findADomain(DUMMY_DOMAIN, 1);
                deploymentService.undeploy(domain);
            }
        });
    }

    @Test
    public void undeploysDomainAndDomainsAppsRemovingAnchorFile() throws Exception
    {
        undeployDomainAndVerifyAppsAreUndeployed(createUndeployDummyDomainAction());
    }

    @Test
    public void undeployDomainDoesNotDeployAllApplications() throws Exception
    {
        addPackedAppFromResource(EMPTY_APP_ZIP_PATH);

        undeployDomainAndVerifyAppsAreUndeployed(createUndeployDummyDomainAction());

        assertThat(findApp(EMPTY_APP, 1), notNullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void findDomainApplicationsWillNullDomainFails()
    {
        deploymentService.findDomainApplications(null);
    }

    @Test
    public void findDomainApplicationsWillNonExistentDomainReturnsEmptyCollection()
    {
        Collection<Application> domainApplications = deploymentService.findDomainApplications("");
        assertThat(domainApplications, notNullValue());
        assertThat(domainApplications.isEmpty(), is(true));
    }

    @Test
    public void undeploysDomainCompletelyEvenOnStoppingException() throws Exception
    {
        addPackedDomainFromResource(EMPTY_DOMAIN_ZIP_PATH);

        TestDomainFactory testDomainFactory = new TestDomainFactory(new MuleDomainClassLoaderRepository());
        testDomainFactory.setFailOnStopApplication();

        deploymentService.setDomainFactory(testDomainFactory);
        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, EMPTY_DOMAIN);

        assertTrue("Unable to remove anchor file", removeDomainAnchorFile(EMPTY_DOMAIN));

        assertUndeploymentSuccess(domainDeploymentListener, EMPTY_DOMAIN);

        assertAppFolderIsDeleted(EMPTY_DOMAIN);
    }

    @Test
    public void undeploysDomainCompletelyEvenOnDisposingException() throws Exception
    {
        addPackedDomainFromResource(EMPTY_DOMAIN_ZIP_PATH);

        TestDomainFactory testDomainFactory = new TestDomainFactory(new MuleDomainClassLoaderRepository());
        testDomainFactory.setFailOnDisposeApplication();
        deploymentService.setDomainFactory(testDomainFactory);
        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, EMPTY_DOMAIN);

        assertTrue("Unable to remove anchor file", removeDomainAnchorFile(EMPTY_DOMAIN));

        assertUndeploymentSuccess(domainDeploymentListener, EMPTY_DOMAIN);

        assertAppFolderIsDeleted(EMPTY_DOMAIN);
    }

    @Test
    public void deploysIncompleteZipDomainOnStartup() throws Exception
    {
        addPackedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, INCOMPLETE_DOMAIN);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained(INCOMPLETE_DOMAIN);
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_DOMAIN, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void deploysIncompleteZipDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH);

        assertDeploymentFailure(domainDeploymentListener, INCOMPLETE_DOMAIN);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained(INCOMPLETE_DOMAIN);
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_DOMAIN, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void mantainsDomainFolderOnExplodedAppDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH);

        assertDeploymentFailure(domainDeploymentListener, INCOMPLETE_DOMAIN);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained(INCOMPLETE_DOMAIN);
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_DOMAIN, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysZipDomainAfterDeploymentErrorOnStartup() throws Exception
    {
        addPackedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, INCOMPLETE_DOMAIN);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(EMPTY_DOMAIN_ZIP_PATH, INCOMPLETE_DOMAIN_TARGET_PATH);
        assertDeploymentSuccess(domainDeploymentListener, INCOMPLETE_DOMAIN);

        assertEquals("Failed domain still appears as zombie after a successful redeploy", 0, deploymentService.getZombieDomains().size());
    }

    @Test
    public void redeploysZipDomainAfterDeploymentErrorAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH);

        assertDeploymentFailure(domainDeploymentListener, INCOMPLETE_DOMAIN);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(EMPTY_DOMAIN_ZIP_PATH, INCOMPLETE_DOMAIN_TARGET_PATH);
        assertDeploymentSuccess(domainDeploymentListener, INCOMPLETE_DOMAIN);

        assertEquals("Failed domain still appears as zombie after a successful redeploy", 0, deploymentService.getZombieDomains().size());
    }

    @Test
    public void redeploysInvalidZipDomainAfterSuccessfulDeploymentOnStartup() throws IOException
    {
        addPackedDomainFromResource(EMPTY_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, EMPTY_DOMAIN);

        addPackedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH, EMPTY_DOMAIN_TARGET_PATH);

        assertDeploymentFailure(domainDeploymentListener, EMPTY_DOMAIN);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", EMPTY_DOMAIN, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipDomainAfterSuccessfulDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedDomainFromResource(EMPTY_DOMAIN_ZIP_PATH);
        assertDeploymentSuccess(domainDeploymentListener, EMPTY_DOMAIN);

        addPackedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH, EMPTY_DOMAIN_TARGET_PATH);
        assertDeploymentFailure(domainDeploymentListener, EMPTY_DOMAIN);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", EMPTY_DOMAIN, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipDomainAfterFailedDeploymentOnStartup() throws IOException
    {
        addPackedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, INCOMPLETE_DOMAIN);

        reset(domainDeploymentListener);

        addPackedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH);

        assertDeploymentFailure(domainDeploymentListener, INCOMPLETE_DOMAIN);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_DOMAIN, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipDomainAfterFailedDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH);
        assertDeploymentFailure(domainDeploymentListener, INCOMPLETE_DOMAIN);

        reset(domainDeploymentListener);

        addPackedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH);
        assertDeploymentFailure(domainDeploymentListener, INCOMPLETE_DOMAIN);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", INCOMPLETE_DOMAIN, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysExplodedDomainAfterDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(INCOMPLETE_DOMAIN_ZIP_PATH);

        assertDeploymentFailure(domainDeploymentListener, INCOMPLETE_DOMAIN);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(DeploymentServiceTestCase.DUMMY_DOMAIN_ZIP_PATH);

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        // Redeploys a fixed version for incompleteDomain
        addExplodedDomainFromResource(EMPTY_DOMAIN_ZIP_PATH, INCOMPLETE_DOMAIN);

        assertDeploymentSuccess(domainDeploymentListener, INCOMPLETE_DOMAIN);
        assertEquals("Failed domain still appears as zombie after a successful redeploy", 0, deploymentService.getZombieDomains().size());
    }

    private Action createUndeployDummyDomainAction()
    {
        return new Action()
        {
            @Override
            public void perform()
            {
                removeDomainAnchorFile(DUMMY_DOMAIN);
            }
        };
    }

    private void undeployDomainAndVerifyAppsAreUndeployed(Action undeployAction) throws IOException
    {
        deploymentService.start();

        addPackedDomainFromResource(DUMMY_DOMAIN_ZIP_PATH);

        assertDeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);

        addPackedAppFromResource(DUMMY_DOMAIN_APP1_ZIP_PATH);
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_DOMAIN_APP1);

        addPackedAppFromResource(DUMMY_DOMAIN_APP2_ZIP_PATH);
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_DOMAIN_APP2);

        undeployAction.perform();

        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_DOMAIN_APP1);
        assertDeploymentSuccess(applicationDeploymentListener, DUMMY_DOMAIN_APP2);
        assertUndeploymentSuccess(domainDeploymentListener, DUMMY_DOMAIN);
    }

    public void doBrokenAppArchiveTest() throws Exception
    {
        addPackedAppFromResource(BROKEN_APP_ZIP_PATH);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, BROKEN_APP);
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
        addPackedDomainFromResource(BROKEN_DOMAIN_ZIP_PATH);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, "broken-domain");
        reset(domainDeploymentListener);

        // let the file system's write-behind cache commit the delete operation?
        Thread.sleep(1000);

        // zip stays intact, no app dir created
        assertDomainDir(new String[] {"broken-domain.zip"}, NONE, true);
        // don't assert dir contents, we want to check internal deployer state next
        assertDomainDir(NONE, new String[] {DUMMY_DOMAIN}, false);
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
     * Find a deployed domain, performing some basic assertions.
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
        addExplodedArtifactFromResource(resource, appName, MULE_CONFIG_XML_FILE, appsDir);
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

    /**
     * Inteface for executing generic actions in the middle of a test case.
     *
     * Allows to execute custom actions before or after executing logic or checking preconditions / verficitations.
     */
    private interface Action
    {
        void perform();
    }
}
