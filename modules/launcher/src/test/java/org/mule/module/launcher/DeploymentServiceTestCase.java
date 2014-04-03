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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.MuleRegistry;
import org.mule.config.StartupContext;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationStatus;
import org.mule.module.launcher.application.MuleApplicationClassLoaderFactory;
import org.mule.module.launcher.application.TestApplicationFactory;
import org.mule.module.launcher.domain.Domain;
import org.mule.module.launcher.domain.MuleDomainClassLoaderRepository;
import org.mule.module.launcher.domain.TestDomainFactory;
import org.mule.module.launcher.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
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
    private static final String EMPTY_APP_CONFIG_XML = "/empty-config.xml";
    private static final String BAD_APP_CONFIG_XML = "/bad-app-config.xml";

    //APP constants
    private static final ArtifactDescriptor dummyAppDescriptor = new ArtifactDescriptor("dummy-app", "/dummy-app.zip", "/dummy-app", null, null);
    private static final ArtifactDescriptor emptyAppDescriptor = new ArtifactDescriptor("empty-app", "/empty-app.zip", null, "empty-app.zip", null);
    private static final ArtifactDescriptor brokenAppDescriptor = new ArtifactDescriptor("broken-app", "/broken-app.zip", null, "brokenApp.zip", null);
    private static final ArtifactDescriptor brokenAppWithFunkyNameDescriptor = new ArtifactDescriptor("broken-app+", "/broken-app+.zip", null, "brokenApp+.zip", null);
    private static final ArtifactDescriptor incompleteAppDescriptor = new ArtifactDescriptor("incompleteApp", "/incompleteApp.zip", "/incompleteApp", "incompleteApp.zip", null);
    private static final ArtifactDescriptor waitAppDescriptor = new ArtifactDescriptor("wait-app", "/wait-app.zip", "/wait-app", "wait-app.zip", "mule-config.xml");

    //Domain constants
    private static final ArtifactDescriptor brokenDomainDescriptor = new ArtifactDescriptor("brokenDomain", "/broken-domain.zip", null, "brokenDomain.zip", "/broken-config.xml");
    private static final ArtifactDescriptor dummyDomainDescriptor = new ArtifactDescriptor("dummy-domain", "/dummy-domain.zip", "/dummy-domain", null, "mule-domain-config.xml");
    private static final ArtifactDescriptor dummyDomainApp1Descriptor = new ArtifactDescriptor("dummy-domain-app1", "/dummy-domain-app1.zip", null, null, null);
    private static final ArtifactDescriptor dummyDomainApp2Descriptor = new ArtifactDescriptor("dummy-domain-app2", "/dummy-domain-app2.zip", "/dummy-domain-app2", null, null);
    private static final ArtifactDescriptor dummyDomainApp3Descriptor = new ArtifactDescriptor("dummy-domain-app3", "/dummy-domain-app3.zip", "/dummy-domain-app3", null, null);
    private static final ArtifactDescriptor dummyDomainBundleDescriptor = new ArtifactDescriptor("dummy-domain-bundle", "/dummy-domain-bundle.zip", null, null, null);
    private static final ArtifactDescriptor emptyDomainDescriptor = new ArtifactDescriptor("empty-domain", "/empty-domain.zip", null, "empty-domain.zip", EMPTY_APP_CONFIG_XML);
    private static final ArtifactDescriptor incompleteDomainDescriptor = new ArtifactDescriptor("incompleteDomain", "/incompleteDomain.zip", null, "incompleteDomain.zip", null);
    private static final ArtifactDescriptor invalidDomainBundle = new ArtifactDescriptor("invalid-domain-bundle", "/invalid-domain-bundle.zip", null, null, null);
    private static final ArtifactDescriptor httpSharedDomainBundle = new ArtifactDescriptor("http-shared-domain", "/http-shared-domain.zip", null, null, null);
    private static final ArtifactDescriptor waitDomainDescriptor = new ArtifactDescriptor("wait-domain", "/wait-domain.zip", "/wait-domain", "wait-domain.zip", "mule-domain-config.xml");


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
        muleHome = getWorkingDirectory();
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
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);
        assertApplicationAnchorFileExists(dummyAppDescriptor.id);

        // just assert no privileged entries were put in the registry
        final Application app = findApp(dummyAppDescriptor.id, 1);
        final MuleRegistry registry = getMuleRegistry(app);

        // mule-app.properties from the zip archive must have loaded properly
        assertEquals("mule-app.properties should have been loaded.", "someValue", registry.get("myCustomProp"));
    }

    @Test
    public void deploysExplodedAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception
    {
        Action deployExplodedWaitAppAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                addExplodedAppFromResource(waitAppDescriptor.zipPath);
            }
        };
        deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployExplodedWaitAppAction);
    }

    @Test
    public void deploysPackagedAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception
    {
        Action deployPackagedWaitAppAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                addPackedAppFromResource(waitAppDescriptor.zipPath);
            }
        };
        deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployPackagedWaitAppAction);
    }

    @Test
    public void deploysAppZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);
        assertApplicationAnchorFileExists(dummyAppDescriptor.id);

        // just assert no privileged entries were put in the registry
        final Application app = findApp(dummyAppDescriptor.id, 1);
        final MuleRegistry registry = getMuleRegistry(app);

        // mule-app.properties from the zip archive must have loaded properly
        assertEquals("mule-app.properties should have been loaded.", "someValue", registry.get("myCustomProp"));
    }

    @Test
    public void deploysBrokenAppZipOnStartup() throws Exception
    {
        addPackedAppFromResource(brokenAppDescriptor.zipPath, brokenAppDescriptor.targetPath);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, "brokenApp");

        assertAppsDir(new String[] {"brokenApp.zip"}, NONE, true);

        assertApplicationAnchorFileDoesNotExists(brokenAppDescriptor.id);

        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
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
        addPackedAppFromResource(brokenAppWithFunkyNameDescriptor.zipPath, brokenAppWithFunkyNameDescriptor.targetPath);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, "brokenApp+");

        assertAppsDir(new String[] {"brokenApp+.zip"}, NONE, true);

        assertApplicationAnchorFileDoesNotExists(brokenAppDescriptor.id);

        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "brokenApp+.zip", new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(applicationDeploymentListener);

        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertDeploymentFailure(applicationDeploymentListener, "brokenApp+", never());

        addPackedAppFromResource(emptyAppDescriptor.zipPath);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppDescriptor.id);
        assertDeploymentFailure(applicationDeploymentListener, "brokenApp+", never());
    }

    @Test
    public void deploysBrokenAppZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource(brokenAppDescriptor.zipPath, "brokenApp.zip");

        assertDeploymentFailure(applicationDeploymentListener, "brokenApp");

        assertAppsDir(new String[] {"brokenApp.zip"}, NONE, true);

        assertApplicationAnchorFileDoesNotExists(brokenAppDescriptor.id);

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

        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        assertUndeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);
    }

    @Test
    public void redeploysAppZipDeployedAfterStartup() throws Exception
    {
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        assertUndeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);
    }

    @Test
    public void deploysExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource(dummyAppDescriptor.zipPath);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);
        assertApplicationAnchorFileExists(dummyAppDescriptor.id);
    }

    @Test
    public void deploysPackagedAppOnStartupWhenExplodedAppIsAlsoPresent() throws Exception
    {
        addExplodedAppFromResource(dummyAppDescriptor.zipPath);
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        addPackedAppFromResource(emptyAppDescriptor.zipPath);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppDescriptor.id);

        // Checks that dummy app was deployed just once
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
    }

    @Test
    public void deploysExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource(dummyAppDescriptor.zipPath);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);
        assertApplicationAnchorFileExists(dummyAppDescriptor.id);
    }

    @Test
    public void deploysInvalidExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource(dummyAppDescriptor.zipPath, "app with spaces");

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

        addExplodedAppFromResource(dummyAppDescriptor.zipPath, "app with spaces");

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

        addExplodedAppFromResource(dummyAppDescriptor.zipPath, "app with spaces");
        assertDeploymentFailure(applicationDeploymentListener, "app with spaces", atLeast(1));

        addExplodedAppFromResource(dummyAppDescriptor.zipPath);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        addExplodedAppFromResource(emptyAppDescriptor.zipPath);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppDescriptor.id);

        // After three update cycles should have only one deployment failure notification for the broken app
        assertDeploymentFailure(applicationDeploymentListener, "app with spaces");
    }

    @Test
    public void deploysBrokenExplodedAppOnStartup() throws Exception
    {
        final URL url = getClass().getResource(incompleteAppDescriptor.zipPath);
        assertNotNull("Test app file not found " + url, url);

        addExplodedAppFromResource(incompleteAppDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        assertApplicationAnchorFileDoesNotExists(incompleteAppDescriptor.id);

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {incompleteAppDescriptor.id}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource(incompleteAppDescriptor.zipPath);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        assertApplicationAnchorFileDoesNotExists(incompleteAppDescriptor.id);

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {incompleteAppDescriptor.id}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void redeploysExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource(dummyAppDescriptor.zipPath);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + dummyAppDescriptor.path, MULE_CONFIG_XML_FILE);
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
    }

    @Test
    public void redeploysExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource(dummyAppDescriptor.zipPath);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + dummyAppDescriptor.path, MULE_CONFIG_XML_FILE);
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
    }

    @Test
    public void redeploysBrokenExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource(incompleteAppDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {incompleteAppDescriptor.id}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + incompleteAppDescriptor.path, MULE_CONFIG_XML_FILE);
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);
    }

    @Test
    public void redeploysBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource(incompleteAppDescriptor.zipPath);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {incompleteAppDescriptor.id}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + incompleteAppDescriptor.path, MULE_CONFIG_XML_FILE);
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);
    }

    @Test
    public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentOnStartup() throws IOException, URISyntaxException
    {
        addExplodedAppFromResource(dummyAppDescriptor.zipPath, dummyAppDescriptor.id);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + dummyAppDescriptor.path, MULE_CONFIG_XML_FILE);
        URL url = getClass().getResource(brokenDomainDescriptor.configFilePath);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentFailure(applicationDeploymentListener, dummyAppDescriptor.id);
        assertStatus(dummyAppDescriptor.id, ApplicationStatus.DEPLOYMENT_FAILED);
    }

    @Test
    public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentAfterStartup() throws IOException, URISyntaxException
    {
        deploymentService.start();

        addExplodedAppFromResource(dummyAppDescriptor.zipPath, dummyAppDescriptor.id);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + dummyAppDescriptor.path, MULE_CONFIG_XML_FILE);
        URL url = getClass().getResource(brokenDomainDescriptor.configFilePath);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentFailure(applicationDeploymentListener, dummyAppDescriptor.id);
        assertStatus(dummyAppDescriptor.id, ApplicationStatus.DEPLOYMENT_FAILED);
    }

    @Test
    public void redeploysFixedAppAfterBrokenExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromResource(incompleteAppDescriptor.zipPath, incompleteAppDescriptor.id);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + incompleteAppDescriptor.path, MULE_CONFIG_XML_FILE);
        URL url = getClass().getResource(emptyDomainDescriptor.configFilePath);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppDescriptor.id);

        addPackedAppFromResource(dummyAppDescriptor.zipPath);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(incompleteAppDescriptor.id);
    }

    @Test
    public void redeploysFixedAppAfterBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromResource(incompleteAppDescriptor.zipPath, incompleteAppDescriptor.id);


        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + incompleteAppDescriptor.path, MULE_CONFIG_XML_FILE);
        URL url = getClass().getResource(emptyDomainDescriptor.configFilePath);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppDescriptor.id);

        addPackedAppFromResource(dummyAppDescriptor.zipPath);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(incompleteAppDescriptor.id);
    }

    @Test
    public void redeploysZipAppOnConfigChanges() throws Exception
    {
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + dummyAppDescriptor.path, MULE_CONFIG_XML_FILE);
        configFile.setLastModified(configFile.lastModified() + 1000);

        assertUndeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);
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
        startupOptions.put("app", brokenAppDescriptor.id);
        StartupContext.get().setStartupOptions(startupOptions);

        doBrokenAppArchiveTest();
    }

    @Test
    public void deploysInvalidZipAppOnStartup() throws Exception
    {
        addPackedAppFromResource(emptyAppDescriptor.zipPath, "app with spaces.zip");

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

        addPackedAppFromResource(emptyAppDescriptor.zipPath, "app with spaces.zip");

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
        addPackedAppFromResource(emptyAppDescriptor.zipPath, "empty-app.zip.zip");

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppDescriptor.targetPath);
        reset(applicationDeploymentListener);

        assertAppsDir(NONE, new String[] {emptyAppDescriptor.targetPath}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        // Checks that the empty-app.zip folder is not processed as a zip file
        assertNoDeploymentInvoked(applicationDeploymentListener);
    }

    @Test
    public void deploysPackedAppsInOrderWhenAppArgumentIsUsed() throws Exception
    {
        addPackedAppFromResource(emptyAppDescriptor.zipPath, "1.zip");
        addPackedAppFromResource(emptyAppDescriptor.zipPath, "2.zip");
        addPackedAppFromResource(emptyAppDescriptor.zipPath, "3.zip");

        Map<String, Object> startupOptions = new HashMap<String, Object>();
        startupOptions.put("app", "3:1:2");
        StartupContext.get().setStartupOptions(startupOptions);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, "1");
        assertApplicationDeploymentSuccess(applicationDeploymentListener, "2");
        assertApplicationDeploymentSuccess(applicationDeploymentListener, "3");
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
        addExplodedAppFromResource(emptyAppDescriptor.zipPath, "1");
        addExplodedAppFromResource(emptyAppDescriptor.zipPath, "2");
        addExplodedAppFromResource(emptyAppDescriptor.zipPath, "3");

        Map<String, Object> startupOptions = new HashMap<String, Object>();
        startupOptions.put("app", "3:1:2");
        StartupContext.get().setStartupOptions(startupOptions);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, "1");
        assertApplicationDeploymentSuccess(applicationDeploymentListener, "2");
        assertApplicationDeploymentSuccess(applicationDeploymentListener, "3");

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
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        Map<String, Object> startupOptions = new HashMap<String, Object>();
        startupOptions.put("app", "dummy-app:dummy-app:dummy-app");
        StartupContext.get().setStartupOptions(startupOptions);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);

        List<Application> applications = deploymentService.getApplications();
        assertEquals(1, applications.size());
    }

    @Test
    public void tracksAppConfigUpdateTime() throws Exception
    {
        addExplodedAppFromResource(dummyAppDescriptor.zipPath);

        // Sets a modification time in the future
        File appFolder = new File(appsDir.getPath(), dummyAppDescriptor.id);
        File configFile = new File(appFolder, MULE_CONFIG_XML_FILE);
        configFile.setLastModified(System.currentTimeMillis() + ONE_HOUR_IN_MILLISECONDS);

        deploymentService.start();
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        reset(applicationDeploymentListener);

        assertNoDeploymentInvoked(applicationDeploymentListener);
    }

    @Test
    public void redeployedFailedAppAfterTouched() throws Exception
    {
        addExplodedAppFromResource(dummyAppDescriptor.zipPath);

        File appFolder = new File(appsDir.getPath(), dummyAppDescriptor.id);

        File configFile = new File(appFolder, MULE_CONFIG_XML_FILE);
        FileUtils.writeStringToFile(configFile, "you shall not pass");

        deploymentService.start();
        assertDeploymentFailure(applicationDeploymentListener, dummyAppDescriptor.id);
        reset(applicationDeploymentListener);

        URL url = getClass().getResource(emptyDomainDescriptor.configFilePath);
        FileUtils.copyFile(new File(url.toURI()), configFile);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
    }

    @Test
    public void receivesMuleContextDeploymentNotifications() throws Exception
    {
        // NOTE: need an integration test like this because DefaultMuleApplication
        // class cannot be unit tested.
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertMuleContextCreated(applicationDeploymentListener, dummyAppDescriptor.id);
        assertMuleContextInitialized(applicationDeploymentListener, dummyAppDescriptor.id);
        assertMuleContextConfigured(applicationDeploymentListener, dummyAppDescriptor.id);
    }

    @Test
    public void undeploysStoppedApp() throws Exception
    {
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        final Application app = findApp(dummyAppDescriptor.id, 1);
        app.stop();
        assertStatus(app, ApplicationStatus.STOPPED);

        deploymentService.undeploy(app);
    }

    @Test
    public void undeploysApplicationRemovingAnchorFile() throws Exception
    {
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        Application app = findApp(dummyAppDescriptor.id, 1);

        assertTrue("Unable to remove anchor file", removeAppAnchorFile(dummyAppDescriptor.id));

        assertUndeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertStatus(app, ApplicationStatus.DESTROYED);
    }

    @Test
    public void undeploysAppCompletelyEvenOnStoppingException() throws Exception
    {
        addPackedAppFromResource(emptyAppDescriptor.zipPath);

        TestApplicationFactory appFactory = new TestApplicationFactory(new MuleApplicationClassLoaderFactory(new MuleDomainClassLoaderRepository(), new DefaultNativeLibraryFinderFactory()));
        appFactory.setFailOnStopApplication(true);

        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppDescriptor.id);
        Application app = findApp(emptyAppDescriptor.id, 1);

        assertTrue("Unable to remove anchor file", removeAppAnchorFile(emptyAppDescriptor.id));

        assertUndeploymentSuccess(applicationDeploymentListener, emptyAppDescriptor.id);

        assertAppFolderIsDeleted(emptyAppDescriptor.id);
        assertStatus(app, ApplicationStatus.DESTROYED);
    }

    @Test
    public void undeploysAppCompletelyEvenOnDisposingException() throws Exception
    {
        addPackedAppFromResource(emptyAppDescriptor.zipPath);

        TestApplicationFactory appFactory = new TestApplicationFactory(new MuleApplicationClassLoaderFactory(new MuleDomainClassLoaderRepository(), new DefaultNativeLibraryFinderFactory()));
        appFactory.setFailOnDisposeApplication(true);
        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppDescriptor.id);
        Application app = findApp(emptyAppDescriptor.id, 1);

        assertTrue("Unable to remove anchor file", removeAppAnchorFile(emptyAppDescriptor.id));

        assertUndeploymentSuccess(applicationDeploymentListener, emptyAppDescriptor.id);
        assertStatus(app, ApplicationStatus.STOPPED);
        assertAppFolderIsDeleted(emptyAppDescriptor.id);
    }

    @Test
    public void deploysIncompleteZipAppOnStartup() throws Exception
    {
        addPackedAppFromResource(incompleteAppDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(incompleteAppDescriptor.id);
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void deploysIncompleteZipAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource(incompleteAppDescriptor.zipPath);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(incompleteAppDescriptor.id);
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void mantainsAppFolderOnExplodedAppDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource(incompleteAppDescriptor.zipPath);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(incompleteAppDescriptor.id);
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysZipAppAfterDeploymentErrorOnStartup() throws Exception
    {
        addPackedAppFromResource(incompleteAppDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(emptyAppDescriptor.zipPath, incompleteAppDescriptor.targetPath);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppDescriptor.id);

        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieApplications().size());
    }

    @Test
    public void redeploysZipAppAfterDeploymentErrorAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource(incompleteAppDescriptor.zipPath);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(emptyAppDescriptor.zipPath, incompleteAppDescriptor.targetPath);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppDescriptor.id);

        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieApplications().size());
    }

    @Test
    public void redeploysInvalidZipAppAfterSuccessfulDeploymentOnStartup() throws IOException
    {
        addPackedAppFromResource(emptyAppDescriptor.zipPath);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppDescriptor.id);

        addPackedAppFromResource(incompleteAppDescriptor.zipPath, emptyAppDescriptor.targetPath);

        assertDeploymentFailure(applicationDeploymentListener, emptyAppDescriptor.id);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", emptyAppDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterSuccessfulDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedAppFromResource(emptyAppDescriptor.zipPath);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppDescriptor.id);

        addPackedAppFromResource(incompleteAppDescriptor.zipPath, emptyAppDescriptor.targetPath);
        assertDeploymentFailure(applicationDeploymentListener, emptyAppDescriptor.id);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", emptyAppDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterFailedDeploymentOnStartup() throws IOException
    {
        addPackedAppFromResource(incompleteAppDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        reset(applicationDeploymentListener);

        addPackedAppFromResource(incompleteAppDescriptor.zipPath);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterFailedDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedAppFromResource(incompleteAppDescriptor.zipPath);
        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        reset(applicationDeploymentListener);

        addPackedAppFromResource(incompleteAppDescriptor.zipPath);
        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysExplodedAppAfterDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedAppFromResource(incompleteAppDescriptor.zipPath);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromResource(dummyAppDescriptor.zipPath);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);

        // Redeploys a fixed version for incompleteApp
        addExplodedAppFromResource(emptyAppDescriptor.zipPath, incompleteAppDescriptor.id);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppDescriptor.id);
        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieApplications().size());
    }

    @Test
    public void synchronizesDeploymentOnStart() throws Exception
    {
        addPackedAppFromResource(emptyAppDescriptor.zipPath);

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
        }).when(applicationDeploymentListener).onDeploymentStart(emptyAppDescriptor.id);

        deploymentServiceThread.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppDescriptor.id);

        assertFalse("Able to lock deployment service during start", lockedFromClient[0]);
    }

    @Test
    public void deploysDomainZipOnStartup() throws Exception
    {
        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        assertDomainDir(NONE, new String[] {dummyDomainDescriptor.id}, true);

        final Domain domain = findADomain(dummyDomainDescriptor.id, 1);
        assertNotNull(domain);
        assertNotNull(domain.getMuleContext());
        assertDomainAnchorFileExists(dummyDomainDescriptor.id);
    }

    @Test
    public void deploysPackagedDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception
    {
        Action deployPackagedWaitDomainAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                addPackedDomainFromResource(waitDomainDescriptor.zipPath);
            }
        };
        deploysDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployPackagedWaitDomainAction);
    }

    @Test
    public void deploysExplodedDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception
    {
        Action deployExplodedWaitDomainAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                addExplodedDomainFromResource(waitDomainDescriptor.zipPath);
            }
        };
        deploysDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployExplodedWaitDomainAction);
    }

    @Test
    public void deploysExplodedDomainBundleOnStartup() throws Exception
    {
        addExplodedDomainFromResource(dummyDomainBundleDescriptor.zipPath);

        deploymentService.start();

        deploysDomainBundle();
    }

    @Test
    public void deploysExplodedDomainBundleAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource(dummyDomainBundleDescriptor.zipPath);

        deploysDomainBundle();
    }

    @Test
    public void deploysDomainBundleZipOnStartup() throws Exception
    {
        addPackedDomainFromResource(dummyDomainBundleDescriptor.zipPath);

        deploymentService.start();

        deploysDomainBundle();
    }

    @Test
    public void deploysDomainBundleZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(dummyDomainBundleDescriptor.zipPath);

        deploysDomainBundle();
    }

    private void deploysDomainBundle()
    {
        assertDeploymentSuccess(domainDeploymentListener, dummyDomainBundleDescriptor.id);

        assertDomainDir(NONE, new String[] {dummyDomainBundleDescriptor.id}, true);

        final Domain domain = findADomain(dummyDomainBundleDescriptor.id, 1);
        assertNotNull(domain);
        assertNull(domain.getMuleContext());

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptor.id);
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, true);

        final Application app = findApp(dummyAppDescriptor.id, 1);
        assertNotNull(app);
    }

    @Test
    public void deploysInvalidExplodedDomainBundleOnStartup() throws Exception
    {
        addExplodedDomainFromResource(invalidDomainBundle.zipPath);

        deploymentService.start();

        deploysInvalidDomainBundleZip();
    }

    @Test
    public void deploysInvalidExplodedDomainBundleAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource(invalidDomainBundle.zipPath);

        deploysInvalidDomainBundleZip();
    }

    @Test
    public void deploysInvalidDomainBundleZipOnStartup() throws Exception
    {
        addPackedDomainFromResource(invalidDomainBundle.zipPath);

        deploymentService.start();

        deploysInvalidDomainBundleZip();
    }

    @Test
    public void deploysInvalidDomainBundleZipAfterStartup() throws Exception
    {
        addPackedDomainFromResource(invalidDomainBundle.zipPath);

        deploymentService.start();

        deploysInvalidDomainBundleZip();
    }

    private void deploysInvalidDomainBundleZip()
    {
        assertDeploymentFailure(domainDeploymentListener, invalidDomainBundle.id);

        assertDomainDir(NONE, new String[] {invalidDomainBundle.id}, true);

        assertAppsDir(NONE, new String[] {}, true);
    }

    @Test
    public void deploysDomainZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        assertDomainDir(NONE, new String[] {dummyDomainDescriptor.id}, true);

        final Domain domain = findADomain(dummyDomainDescriptor.id, 1);
        assertNotNull(domain);
        assertNotNull(domain.getMuleContext());
        assertDomainAnchorFileExists(dummyDomainDescriptor.id);
    }

    @Test
    public void deploysBrokenDomainZipOnStartup() throws Exception
    {
        addPackedDomainFromResource(brokenDomainDescriptor.zipPath, brokenDomainDescriptor.targetPath);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, brokenDomainDescriptor.id);

        assertDomainDir(new String[] {brokenDomainDescriptor.targetPath}, NONE, true);

        assertDomainAnchorFileDoesNotExists(brokenDomainDescriptor.id);

        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as domain", brokenDomainDescriptor.targetPath, new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenDomainZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(brokenDomainDescriptor.zipPath, brokenDomainDescriptor.targetPath);

        assertDeploymentFailure(domainDeploymentListener, brokenDomainDescriptor.id);

        assertDomainDir(new String[] {brokenDomainDescriptor.targetPath}, NONE, true);

        assertDomainAnchorFileDoesNotExists(brokenDomainDescriptor.id);

        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as domain.", brokenDomainDescriptor.targetPath, new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void redeploysDomainZipDeployedOnStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        assertDomainDir(NONE, new String[] {dummyDomainDescriptor.id}, true);
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());

        reset(domainDeploymentListener);

        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        assertUndeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);
        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());
        assertDomainDir(NONE, new String[] {dummyDomainDescriptor.id}, true);
    }

    @Test
    public void redeploysDomainZipDeployedAfterStartup() throws Exception
    {
        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        assertDomainDir(NONE, new String[] {dummyDomainDescriptor.id}, true);
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());

        reset(domainDeploymentListener);

        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        assertUndeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);
        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());
        assertDomainDir(NONE, new String[] {dummyDomainDescriptor.id}, true);
    }

    @Test
    public void deploysExplodedDomainOnStartup() throws Exception
    {
        addExplodedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);
        assertDomainDir(NONE, new String[] {dummyDomainDescriptor.id}, true);
        assertDomainAnchorFileExists(dummyDomainDescriptor.id);
    }

    @Test
    public void deploysPackagedDomainOnStartupWhenExplodedDomainIsAlsoPresent() throws Exception
    {
        addExplodedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);
        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        addExplodedDomainFromResource(emptyDomainDescriptor.zipPath);

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainDescriptor.id);

        // Checks that dummy app was deployed just once
        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);
    }

    @Test
    public void deploysExplodedDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);
        assertDomainDir(NONE, new String[] {dummyDomainDescriptor.id}, true);
        assertDomainAnchorFileExists(dummyDomainDescriptor.id);
    }

    @Test
    public void deploysInvalidExplodedDomainOnStartup() throws Exception
    {
        addExplodedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath, "domain with spaces");

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

        addExplodedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath, "domain with spaces");

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

        addExplodedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath, "domain with spaces");
        assertDeploymentFailure(domainDeploymentListener, "domain with spaces", atLeast(1));

        addExplodedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);
        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        addExplodedDomainFromResource(emptyDomainDescriptor.zipPath);
        assertDeploymentSuccess(domainDeploymentListener, emptyDomainDescriptor.id);

        // After three update cycles should have only one deployment failure notification for the broken app
        assertDeploymentFailure(domainDeploymentListener, "domain with spaces");
    }

    @Test
    public void deploysBrokenExplodedDomainOnStartup() throws Exception
    {
        final URL url = getClass().getResource(incompleteDomainDescriptor.zipPath);
        assertNotNull("Test app file not found " + url, url);

        addExplodedDomainFromResource(incompleteDomainDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainDescriptor.id);

        // Maintains app dir created
        assertDomainDir(NONE, new String[] {incompleteDomainDescriptor.id}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenExplodedDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource(incompleteDomainDescriptor.zipPath);

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainDescriptor.id);

        // Maintains app dir created
        assertDomainDir(NONE, new String[] {incompleteDomainDescriptor.id}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void receivesDomainMuleContextDeploymentNotifications() throws Exception
    {
        // NOTE: need an integration test like this because DefaultMuleApplication
        // class cannot be unit tested.
        addPackedDomainFromResource(httpSharedDomainBundle.zipPath);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, httpSharedDomainBundle.id);
        assertMuleContextCreated(domainDeploymentListener, httpSharedDomainBundle.id);
        assertMuleContextInitialized(domainDeploymentListener, httpSharedDomainBundle.id);
        assertMuleContextConfigured(domainDeploymentListener, httpSharedDomainBundle.id);
    }

    @Test
    public void undeploysStoppedDomain() throws Exception
    {
        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);
        final Domain domain = findADomain(dummyDomainDescriptor.id, 1);
        domain.stop();

        deploymentService.undeploy(domain);
    }

    @Test
    public void undeploysDomainRemovingAnchorFile() throws Exception
    {
        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        assertTrue("Unable to remove anchor file", removeDomainAnchorFile(dummyDomainDescriptor.id));

        assertUndeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);
    }

    @Test
    public void undeploysDomainAndDomainsApps() throws Exception
    {
        doDomainUndeployAndVerifyAppsAreUndeployed(new Action()
        {
            @Override
            public void perform()
            {
                Domain domain = findADomain(dummyDomainDescriptor.id, 1);
                deploymentService.undeploy(domain);
            }
        });
    }

    @Test
    public void undeploysDomainAndDomainsAppsRemovingAnchorFile() throws Exception
    {
        doDomainUndeployAndVerifyAppsAreUndeployed(createUndeployDummyDomainAction());
    }

    @Test
    public void undeployDomainDoesNotDeployAllApplications() throws Exception
    {
        addPackedAppFromResource(emptyAppDescriptor.zipPath);

        doDomainUndeployAndVerifyAppsAreUndeployed(createUndeployDummyDomainAction());

        assertThat(findApp(emptyAppDescriptor.id, 1), notNullValue());
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
        addPackedDomainFromResource(emptyDomainDescriptor.zipPath);

        TestDomainFactory testDomainFactory = new TestDomainFactory(new MuleDomainClassLoaderRepository());
        testDomainFactory.setFailOnStopApplication();

        deploymentService.setDomainFactory(testDomainFactory);
        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainDescriptor.id);

        assertTrue("Unable to remove anchor file", removeDomainAnchorFile(emptyDomainDescriptor.id));

        assertUndeploymentSuccess(domainDeploymentListener, emptyDomainDescriptor.id);

        assertAppFolderIsDeleted(emptyDomainDescriptor.id);
    }

    @Test
    public void undeploysDomainCompletelyEvenOnDisposingException() throws Exception
    {
        addPackedDomainFromResource(emptyDomainDescriptor.zipPath);

        TestDomainFactory testDomainFactory = new TestDomainFactory(new MuleDomainClassLoaderRepository());
        testDomainFactory.setFailOnDisposeApplication();
        deploymentService.setDomainFactory(testDomainFactory);
        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainDescriptor.id);

        assertTrue("Unable to remove anchor file", removeDomainAnchorFile(emptyDomainDescriptor.id));

        assertUndeploymentSuccess(domainDeploymentListener, emptyDomainDescriptor.id);

        assertAppFolderIsDeleted(emptyDomainDescriptor.id);
    }

    @Test
    public void deploysIncompleteZipDomainOnStartup() throws Exception
    {
        addPackedDomainFromResource(incompleteDomainDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained(incompleteDomainDescriptor.id);
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void deploysIncompleteZipDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(incompleteDomainDescriptor.zipPath);

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained(incompleteDomainDescriptor.id);
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void mantainsDomainFolderOnExplodedAppDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(incompleteDomainDescriptor.zipPath);

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained(incompleteDomainDescriptor.id);
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysZipDomainAfterDeploymentErrorOnStartup() throws Exception
    {
        addPackedDomainFromResource(incompleteDomainDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(emptyDomainDescriptor.zipPath, incompleteDomainDescriptor.targetPath);
        assertDeploymentSuccess(domainDeploymentListener, incompleteDomainDescriptor.id);

        assertEquals("Failed domain still appears as zombie after a successful redeploy", 0, deploymentService.getZombieDomains().size());
    }

    @Test
    public void redeploysZipDomainAfterDeploymentErrorAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(incompleteDomainDescriptor.zipPath);

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(emptyDomainDescriptor.zipPath, incompleteDomainDescriptor.targetPath);
        assertDeploymentSuccess(domainDeploymentListener, incompleteDomainDescriptor.id);

        assertEquals("Failed domain still appears as zombie after a successful redeploy", 0, deploymentService.getZombieDomains().size());
    }

    @Test
    public void redeploysInvalidZipDomainAfterSuccessfulDeploymentOnStartup() throws IOException
    {
        addPackedDomainFromResource(emptyDomainDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainDescriptor.id);

        addPackedDomainFromResource(incompleteDomainDescriptor.zipPath, emptyDomainDescriptor.targetPath);

        assertDeploymentFailure(domainDeploymentListener, emptyDomainDescriptor.id);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", emptyDomainDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipDomainAfterSuccessfulDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedDomainFromResource(emptyDomainDescriptor.zipPath);
        assertDeploymentSuccess(domainDeploymentListener, emptyDomainDescriptor.id);

        addPackedDomainFromResource(incompleteDomainDescriptor.zipPath, emptyDomainDescriptor.targetPath);
        assertDeploymentFailure(domainDeploymentListener, emptyDomainDescriptor.id);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", emptyDomainDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipDomainAfterFailedDeploymentOnStartup() throws IOException
    {
        addPackedDomainFromResource(incompleteDomainDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainDescriptor.id);

        reset(domainDeploymentListener);

        addPackedDomainFromResource(incompleteDomainDescriptor.zipPath);

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainDescriptor.id);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipDomainAfterFailedDeploymentAfterStartup() throws IOException
    {
        deploymentService.start();

        addPackedDomainFromResource(incompleteDomainDescriptor.zipPath);
        assertDeploymentFailure(domainDeploymentListener, incompleteDomainDescriptor.id);

        reset(domainDeploymentListener);

        addPackedDomainFromResource(incompleteDomainDescriptor.zipPath);
        assertDeploymentFailure(domainDeploymentListener, incompleteDomainDescriptor.id);

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainDescriptor.id, new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysExplodedDomainAfterDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(incompleteDomainDescriptor.zipPath);

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainDescriptor.id);

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromResource(DeploymentServiceTestCase.dummyDomainDescriptor.zipPath);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        // Redeploys a fixed version for incompleteDomain
        addExplodedDomainFromResource(emptyDomainDescriptor.zipPath, incompleteDomainDescriptor.id);

        assertDeploymentSuccess(domainDeploymentListener, incompleteDomainDescriptor.id);
        assertEquals("Failed domain still appears as zombie after a successful redeploy", 0, deploymentService.getZombieDomains().size());
    }

    private Action createUndeployDummyDomainAction()
    {
        return new Action()
        {
            @Override
            public void perform()
            {
                removeDomainAnchorFile(dummyDomainDescriptor.id);
            }
        };
    }

    private void doDomainUndeployAndVerifyAppsAreUndeployed(Action undeployAction) throws Exception
    {
        deploymentService.start();

        addPackedDomainFromResource(dummyDomainDescriptor.zipPath);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        addPackedAppFromResource(dummyDomainApp1Descriptor.zipPath);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1Descriptor.id);

        addPackedAppFromResource(dummyDomainApp2Descriptor.zipPath);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2Descriptor.id);

        undeployAction.perform();

        assertDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1Descriptor.id);
        assertDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2Descriptor.id);
        assertUndeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);
    }


    @Test
    public void redeploysFixedDomainAfterBrokenExplodedDomainOnStartup() throws Exception
    {
        addExplodedDomainFromResource("/incompleteDomain.zip", "incompleteDomain");

        deploymentService.start();

        doRedeployFixedDomainAfterBrokenDomain();
    }

    @Test
    public void redeploysFixedDomainAfterBrokenExplodedDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource("/incompleteDomain.zip", "incompleteDomain");

        doRedeployFixedDomainAfterBrokenDomain();
    }

    @Test
    public void redeploysDomainAndItsApplications() throws Exception
    {
        addExplodedDomainFromResource(dummyDomainDescriptor.zipPath, dummyDomainDescriptor.id);

        addExplodedAppFromResource(dummyDomainApp1Descriptor.zipPath, dummyDomainApp1Descriptor.id);
        addExplodedAppFromResource(dummyDomainApp2Descriptor.zipPath, dummyDomainApp2Descriptor.id);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1Descriptor.id);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2Descriptor.id);

        reset(domainDeploymentListener);
        reset(applicationDeploymentListener);

        doRedeployDummyDomainByChangingConfigFileWithGoodOne();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1Descriptor.id);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2Descriptor.id);
    }

    @Test
    public void redeploysDomainAndFails() throws Exception
    {
        addExplodedDomainFromResource(dummyDomainDescriptor.zipPath, dummyDomainDescriptor.id);

        addExplodedAppFromResource(dummyDomainApp1Descriptor.zipPath, dummyDomainApp1Descriptor.id);
        addExplodedAppFromResource(dummyDomainApp2Descriptor.zipPath, dummyDomainApp2Descriptor.id);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1Descriptor.id);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2Descriptor.id);

        reset(domainDeploymentListener);
        reset(applicationDeploymentListener);

        doRedeployDummyDomainByChangingConfigFileWithBadOne();

        assertDeploymentFailure(domainDeploymentListener, dummyDomainDescriptor.id);

        assertNoDeploymentInvoked(applicationDeploymentListener);
    }

    @Test
    public void redeploysDomainWithOneApplicationFailedOnFirstDeployment() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource(dummyDomainDescriptor.zipPath, dummyDomainDescriptor.id);

        addExplodedAppFromResource(dummyDomainApp1Descriptor.zipPath, dummyDomainApp1Descriptor.id);
        addExplodedAppFromResource(dummyDomainApp2Descriptor.zipPath, dummyDomainApp2Descriptor.id);
        addExplodedAppFromResource(dummyDomainApp3Descriptor.zipPath, dummyDomainApp3Descriptor.id);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1Descriptor.id);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2Descriptor.id);
        assertDeploymentFailure(applicationDeploymentListener, dummyDomainApp3Descriptor.id);

        reset(domainDeploymentListener);
        reset(applicationDeploymentListener);

        deploymentService.getLock().lock();
        try
        {
            doRedeployDummyDomainByChangingConfigFileWithGoodOne();
            doRedeployAppByChangingConfigFileWithGoodOne(dummyDomainApp3Descriptor.path);
        }
        finally
        {
            deploymentService.getLock().unlock();
        }

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1Descriptor.id);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2Descriptor.id);
        assertDeploymentSuccess(applicationDeploymentListener, dummyDomainApp3Descriptor.id);
    }

    @Test
    public void redeploysDomainWithOneApplicationFailedAfterRedeployment() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromResource(dummyDomainDescriptor.zipPath, dummyDomainDescriptor.id);

        addExplodedAppFromResource(dummyDomainApp1Descriptor.zipPath, dummyDomainApp1Descriptor.id);
        addExplodedAppFromResource(dummyDomainApp2Descriptor.zipPath, dummyDomainApp2Descriptor.id);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1Descriptor.id);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2Descriptor.id);

        reset(domainDeploymentListener);
        reset(applicationDeploymentListener);

        deploymentService.getLock().lock();
        try
        {
            doRedeployDummyDomainByChangingConfigFileWithGoodOne();
            doRedeployAppByChangingConfigFileWithBadOne(dummyDomainApp2Descriptor.path);
        }
        finally
        {
            deploymentService.getLock().unlock();
        }

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainDescriptor.id);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1Descriptor.id);
        assertDeploymentFailure(applicationDeploymentListener, dummyDomainApp2Descriptor.id);
    }

    private void doRedeployAppByChangingConfigFileWithGoodOne(String applicationPath) throws Exception
    {
        doRedeployAppByChangingConfigFile(applicationPath, EMPTY_APP_CONFIG_XML);
    }

    private void doRedeployAppByChangingConfigFileWithBadOne(String applicationPath) throws Exception
    {
        doRedeployAppByChangingConfigFile(applicationPath, BAD_APP_CONFIG_XML);
    }

    private void doRedeployAppByChangingConfigFile(String applicationPath, String configFile) throws Exception
    {
        File originalConfigFile = new File(appsDir + applicationPath, MULE_CONFIG_XML_FILE);
        URL url = getClass().getResource(configFile);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);
    }

    private void doRedeployDummyDomainByChangingConfigFileWithGoodOne() throws URISyntaxException, IOException
    {
        doRedeployDummyDomainByChangingConfigFile("/empty-domain-config.xml");
    }

    private void doRedeployDummyDomainByChangingConfigFileWithBadOne() throws URISyntaxException, IOException
    {
        doRedeployDummyDomainByChangingConfigFile("/bad-domain-config.xml");
    }

    private void doRedeployDummyDomainByChangingConfigFile(String configFile) throws URISyntaxException, IOException
    {
        File originalConfigFile = new File(domainsDir + dummyDomainDescriptor.path, dummyDomainDescriptor.configFilePath);
        URL url = getClass().getResource(configFile);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);
    }

    private void doRedeployFixedDomainAfterBrokenDomain() throws URISyntaxException, IOException
    {
        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        reset(domainDeploymentListener);

        File originalConfigFile = new File(domainsDir + "/incompleteDomain", "mule-domain-config.xml");
        URL url = getClass().getResource("/empty-domain-config.xml");
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);
        assertDeploymentSuccess(domainDeploymentListener, "incompleteDomain");

        addPackedDomainFromResource("/dummy-domain.zip");
        assertDeploymentSuccess(domainDeploymentListener, "dummy-domain");

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained("incompleteDomain");
    }

    public void doBrokenAppArchiveTest() throws Exception
    {
        addPackedAppFromResource(brokenAppDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, brokenAppDescriptor.id);
        reset(applicationDeploymentListener);

        // let the file system's write-behind cache commit the delete operation?
        Thread.sleep(1000);

        // zip stays intact, no app dir created
        assertAppsDir(new String[] {"broken-app.zip"}, NONE, true);
        // don't assert dir contents, we want to check internal deployer state next
        assertAppsDir(NONE, new String[] {dummyAppDescriptor.id}, false);
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
        addPackedDomainFromResource(brokenDomainDescriptor.zipPath);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, "broken-domain");
        reset(domainDeploymentListener);

        // let the file system's write-behind cache commit the delete operation?
        Thread.sleep(1000);

        // zip stays intact, no app dir created
        assertDomainDir(new String[] {"broken-domain.zip"}, NONE, true);
        // don't assert dir contents, we want to check internal deployer state next
        assertDomainDir(NONE, new String[] {dummyDomainDescriptor.id}, false);
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

    private void deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(Action deployArtifactAction) throws Exception
    {
        Action verifyAnchorFileDoesNotExistsAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                assertApplicationAnchorFileDoesNotExists(waitAppDescriptor.id);
            }
        };
        Action verifyDeploymentSuccessfulAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                assertApplicationDeploymentSuccess(applicationDeploymentListener, waitAppDescriptor.id);
            }
        };
        Action verifyAnchorFileExistsAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                assertApplicationAnchorFileExists(waitAppDescriptor.id);
            }
        };
        deploysArtifactAndVerifyAnchorFileCreatedWhenDeploymentEnds(deployArtifactAction, verifyAnchorFileDoesNotExistsAction, verifyDeploymentSuccessfulAction, verifyAnchorFileExistsAction);
    }

    private void deploysDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(Action deployArtifactAction) throws Exception
    {
        Action verifyAnchorFileDoesNotExists = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                assertDomainAnchorFileDoesNotExists(waitDomainDescriptor.id);
            }
        };
        Action verifyDeploymentSuccessful = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                assertDeploymentSuccess(domainDeploymentListener, waitDomainDescriptor.id);
            }
        };
        Action verifyAnchorFileExists = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                assertDomainAnchorFileExists(waitDomainDescriptor.id);
            }
        };
        deploysArtifactAndVerifyAnchorFileCreatedWhenDeploymentEnds(deployArtifactAction, verifyAnchorFileDoesNotExists, verifyDeploymentSuccessful, verifyAnchorFileExists);
    }

    private void deploysArtifactAndVerifyAnchorFileCreatedWhenDeploymentEnds(Action deployArtifactAction,
                                                                             Action verifyAnchorFileDoesNotExistsAction,
                                                                             Action verifyDeploymentSuccessfulAction,
                                                                             Action verifyAnchorFileExistsAction
    ) throws Exception
    {
        WaitComponent.reset();
        deploymentService.start();
        deployArtifactAction.perform();
        try
        {
            if (!WaitComponent.componentInitializedLatch.await(DEPLOYMENT_TIMEOUT, TimeUnit.MILLISECONDS))
            {
                fail("WaitComponent should be initilaized already. Probably app deployment failed");
            }
            verifyAnchorFileDoesNotExistsAction.perform();
        }
        finally
        {
            WaitComponent.waitLatch.release();
        }
        verifyDeploymentSuccessfulAction.perform();
        verifyAnchorFileExistsAction.perform();
    }

    private void assertApplicationDeploymentSuccess(DeploymentListener listener, String artifactName)
    {
        assertDeploymentSuccess(listener, artifactName);
        assertStatus(artifactName, ApplicationStatus.STARTED);
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

    private void assertStatus(String appName, ApplicationStatus status)
    {
        assertStatus(appName, status, -1);
    }

    private void assertStatus(String appName, ApplicationStatus status, int expectedApps)
    {
        Application app = findApp(appName, expectedApps);
        assertThat(app, notNullValue());
        assertStatus(app, status);
    }

    private void assertStatus(final Application application, final ApplicationStatus status)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new JUnitProbe()
        {
            @Override
            protected boolean test() throws Exception
            {
                assertThat(application.getStatus(), is(status));
                return true;
            }

            @Override
            public String describeFailure()
            {
                return String.format("Application %s was expected to be in status %s but was %s instead",
                                     application.getArtifactName(), status.name(), application.getStatus().name());
            }
        });

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

        if (totalAppsExpected >= 0)
        {
            assertEquals(totalAppsExpected, apps.size());
        }

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
        File anchorFile = getArtifactAnchorFile(appName, appsDir);
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
        File anchorFile = getArtifactAnchorFile(domainName, domainsDir);
        return anchorFile.delete();
    }

    private void assertApplicationAnchorFileExists(String applicationName)
    {
        assertThat(getArtifactAnchorFile(applicationName, appsDir).exists(), is(true));
    }

    private void assertApplicationAnchorFileDoesNotExists(String applicationName)
    {
        assertThat(getArtifactAnchorFile(applicationName, appsDir).exists(), is(false));
    }

    private void assertDomainAnchorFileDoesNotExists(String domainName)
    {
        assertThat(getArtifactAnchorFile(domainName, domainsDir).exists(), is(false));
    }

    private void assertDomainAnchorFileExists(String domainName)
    {
        assertThat(getArtifactAnchorFile(domainName, domainsDir).exists(), is(true));
    }

    private File getArtifactAnchorFile(String artifactName, File artifactDir)
    {
        String anchorFileName = artifactName + MuleDeploymentService.ARTIFACT_ANCHOR_SUFFIX;
        return new File(artifactDir, anchorFileName);
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
     * Allows to execute custom actions before or after executing logic or checking preconditions / verifications.
     */
    private interface Action
    {
        void perform() throws Exception;
    }

    public static class ArtifactDescriptor
    {

        public String id;
        public String zipPath;
        public String path;
        public String targetPath;
        public String configFilePath;

        public ArtifactDescriptor(String id, String zipPath, String path, String targetPath, String configFilePath)
        {
            this.id = id;
            this.zipPath = zipPath;
            this.path = path;
            this.targetPath = targetPath;
            this.configFilePath = configFilePath;
        }
    }

    public static class WaitComponent implements Initialisable
    {

        public static Latch componentInitializedLatch = new Latch();
        public static Latch waitLatch = new Latch();

        @Override
        public void initialise() throws InitialisationException
        {
            try
            {
                componentInitializedLatch.release();
                waitLatch.await();
            }
            catch (InterruptedException e)
            {
                throw new InitialisationException(e, this);
            }
        }

        public static void reset()
        {
            componentInitializedLatch = new Latch();
            waitLatch = new Latch();
        }
    }

}
