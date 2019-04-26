/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.apache.commons.io.filefilter.FileFileFilter.FILE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsArrayContainingInAnyOrder.arrayContainingInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.module.launcher.MuleDeploymentService.PARALLEL_DEPLOYMENT_PROPERTY;
import static org.mule.module.launcher.descriptor.PropertiesDescriptorParser.PROPERTY_CONFIG_RESOURCES;
import static org.mule.module.launcher.domain.Domain.DOMAIN_CONFIG_FILE_LOCATION;
import static org.mule.util.FileUtils.deleteFile;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.Registry;
import org.mule.config.StartupContext;
import org.mule.module.http.internal.listener.DefaultHttpListenerConfig;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationStatus;
import org.mule.module.launcher.application.MuleApplicationClassLoaderFactory;
import org.mule.module.launcher.application.TestApplicationFactory;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.module.launcher.artifact.ShutdownListener;
import org.mule.module.launcher.builder.ApplicationFileBuilder;
import org.mule.module.launcher.builder.ApplicationPluginFileBuilder;
import org.mule.module.launcher.builder.DomainFileBuilder;
import org.mule.module.launcher.builder.TestArtifactDescriptor;
import org.mule.module.launcher.domain.Domain;
import org.mule.module.launcher.domain.DomainFactory;
import org.mule.module.launcher.domain.MuleDomainClassLoaderRepository;
import org.mule.module.launcher.domain.TestDomainFactory;
import org.mule.module.launcher.listener.TestDeploymentListener;
import org.mule.module.launcher.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.module.launcher.util.ObservableList;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.tck.probe.file.FileDoesNotExists;
import org.mule.tck.probe.file.FileExists;
import org.mule.util.CollectionUtils;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.Latch;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

@RunWith(Parameterized.class)
public class DeploymentServiceTestCase extends AbstractMuleContextTestCase
{

    private static final String PORT_PROPERTY_NAME = "port";
    private static final String SYSTEM_PROPERTY_PORT = "9999";
            
    private static final int FILE_TIMESTAMP_PRECISION_MILLIS = 1000;
    protected static final int DEPLOYMENT_TIMEOUT = 10000;
    protected static final String[] NONE = new String[0];
    protected static final int ONE_HOUR_IN_MILLISECONDS = 3600000;

    // Resources
    private static final String MULE_CONFIG_XML_FILE = "mule-config.xml";
    private static final String EMPTY_APP_CONFIG_XML = "/empty-config.xml";
    private static final String BAD_APP_CONFIG_XML = "/bad-app-config.xml";
    private static final String BROKEN_CONFIG_XML = "/broken-config.xml";
    private static final String EMPTY_DOMAIN_CONFIG_XML = "/empty-domain-config.xml";

    @Parameterized.Parameters(name =  "Parallel: {0}")
    public static List<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]
                {
                        {false},
                        {true},
                });
    }

    // Application plugin file builders
    private final ApplicationPluginFileBuilder echoPlugin = new ApplicationPluginFileBuilder("echoPlugin").usingLibrary("lib/echo-test.jar");
    private final ApplicationPluginFileBuilder echoPluginWithoutLib1 = new ApplicationPluginFileBuilder("echoPlugin1").containingClass("org/foo/Plugin1Echo.clazz");

    // Application file builders
    private final ApplicationFileBuilder emptyAppFileBuilder = new ApplicationFileBuilder("empty-app").definedBy("empty-config.xml");
    private final ApplicationFileBuilder springPropertyAppFileBuilder = new ApplicationFileBuilder("property-app").definedBy("app-properties-config.xml");
    private final ApplicationFileBuilder dummyAppDescriptorFileBuilder = new ApplicationFileBuilder("dummy-app").definedBy("dummy-app-config.xml").configuredWith("myCustomProp", "someValue").containingClass("org/mule/module/launcher/EchoTest.clazz");
    private final ApplicationFileBuilder dummyCascadingPropsAppDescriptorFileBuilder = new ApplicationFileBuilder("dummy-app").definedBy("dummy-app-with-cascading-props.xml");
    private final ApplicationFileBuilder dummyAppDescriptorWithPropsFileBuilder = new ApplicationFileBuilder("dummy-app-with-props").definedBy("dummy-app-with-props.xml").configuredWith("myCustomProp", "someValue").containingClass("org/mule/module/launcher/EchoTest.clazz");
    private final ApplicationFileBuilder dummyAppDescriptorFileBuilderWithUpperCaseInExtension =            new ApplicationFileBuilder("dummy-app", true).definedBy("dummy-app-config.xml").configuredWith("myCustomProp", "someValue").containingClass("org/mule/module/launcher/EchoTest.clazz");
    private final ApplicationFileBuilder waitAppFileBuilder = new ApplicationFileBuilder("wait-app").definedBy("wait-app-config.xml");
    private final ApplicationFileBuilder brokenAppFileBuilder = new ApplicationFileBuilder("broken-app").corrupted();
    private final ApplicationFileBuilder incompleteAppFileBuilder = new ApplicationFileBuilder("incomplete-app").definedBy("incomplete-app-config.xml");
    private final ApplicationFileBuilder echoPluginAppFileBuilder = new ApplicationFileBuilder("dummyWithEchoPlugin").definedBy("app-with-echo-plugin-config.xml").containingPlugin(echoPlugin);
    private final ApplicationFileBuilder sharedLibPluginAppFileBuilder = new ApplicationFileBuilder("shared-plugin-lib-app").definedBy("app-with-echo1-plugin-config.xml").containingPlugin(echoPluginWithoutLib1).sharingLibrary("lib/bar-1.0.jar");
    private final ApplicationFileBuilder brokenAppWithFunkyNameAppFileBuilder = new ApplicationFileBuilder("broken-app+", brokenAppFileBuilder);
    private final ApplicationFileBuilder dummyDomainApp1FileBuilder = new ApplicationFileBuilder("dummy-domain-app1").definedBy("empty-config.xml").deployedWith("domain", "dummy-domain");
    private final ApplicationFileBuilder dummyDomainApp2FileBuilder = new ApplicationFileBuilder("dummy-domain-app2").definedBy("empty-config.xml").deployedWith("domain", "dummy-domain");
    private final ApplicationFileBuilder dummyDomainApp3FileBuilder = new ApplicationFileBuilder("dummy-domain-app3").definedBy("bad-app-config.xml").deployedWith("domain", "dummy-domain");
    private final ApplicationFileBuilder httpAAppFileBuilder = new ApplicationFileBuilder("shared-http-app-a").definedBy("shared-http-a-app-config.xml").deployedWith("domain", "shared-http-domain");
    private final ApplicationFileBuilder httpBAppFileBuilder = new ApplicationFileBuilder("shared-http-app-b").definedBy("shared-http-b-app-config.xml").deployedWith("domain", "shared-http-domain");
    private final ApplicationFileBuilder badConfigAppFileBuilder = new ApplicationFileBuilder("bad-config-app").definedBy("bad-app-config.xml");

    // Domain file builders
    private final DomainFileBuilder brokenDomainFileBuilder = new DomainFileBuilder("brokenDomain").corrupted();
    private final DomainFileBuilder emptyDomainFileBuilder = new DomainFileBuilder("empty-domain").definedBy("empty-domain-config.xml");
    private final DomainFileBuilder waitDomainFileBuilder = new DomainFileBuilder("wait-domain").definedBy("wait-domain-config.xml");
    private final DomainFileBuilder incompleteDomainFileBuilder = new DomainFileBuilder("incompleteDomain").definedBy("incomplete-domain-config.xml");
    private final DomainFileBuilder invalidDomainBundleFileBuilder = new DomainFileBuilder("invalid-domain-bundle").definedBy("incomplete-domain-config.xml").containing(emptyAppFileBuilder);
    private final DomainFileBuilder dummyDomainBundleFileBuilder = new DomainFileBuilder("dummy-domain-bundle").containing(new ApplicationFileBuilder(dummyAppDescriptorFileBuilder).deployedWith("domain", "dummy-domain-bundle"));
    private final DomainFileBuilder dummyDomainFileBuilder = new DomainFileBuilder("dummy-domain").definedBy("empty-domain-config.xml");
    private final DomainFileBuilder dummyUndeployableDomainFileBuilder = new DomainFileBuilder("dummy-undeployable-domain").definedBy("empty-domain-config.xml").deployedWith("redeployment.enabled", "false");
    private final DomainFileBuilder sharedHttpDomainFileBuilder = new DomainFileBuilder("shared-http-domain").definedBy("shared-http-domain-config.xml");
    private final DomainFileBuilder sharedHttpBundleDomainFileBuilder = new DomainFileBuilder("shared-http-domain").definedBy("shared-http-domain-config.xml").containing(httpAAppFileBuilder).containing(httpBAppFileBuilder);

    private final boolean parallelDeployment;
    protected File muleHome;
    protected File appsDir;
    protected File domainsDir;
    protected TestMuleDeploymentService deploymentService;
    protected DeploymentListener applicationDeploymentListener;
    protected DeploymentListener domainDeploymentListener;
    protected TestDeploymentListener testDeploymentListener = new TestDeploymentListener();
    protected TestDeploymentListener testDomainDeploymentListener = new TestDeploymentListener();
    private static Latch undeployLatch = new Latch();


    @Rule
    public SystemProperty changeChangeInterval = new SystemProperty(DeploymentDirectoryWatcher.CHANGE_CHECK_INTERVAL_PROPERTY, "10");

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Rule
    public DynamicPort httpPortAlternative = new DynamicPort("httpPortAlternative");

    @Rule
    public DynamicPort deploymentPropertiesPort = new DynamicPort("deploymentPropertiesPort");

    @Rule
    public DynamicPort deploymentPropertiesOverriddenPort = new DynamicPort("deploymentPropertiesOverriddenPort");

    public DeploymentServiceTestCase(boolean parallelDeployment)
    {
        this.parallelDeployment = parallelDeployment;
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        if (parallelDeployment)
        {
            System.setProperty(PARALLEL_DEPLOYMENT_PROPERTY, "");
        }

        // set up some mule home structure
        final String tmpDir = System.getProperty("java.io.tmpdir");
        muleHome = new File(new File(tmpDir, "mule home"), getClass().getSimpleName() + System.currentTimeMillis());
        appsDir = new File(muleHome, "apps");
        appsDir.mkdirs();
        domainsDir = new File(muleHome, "domains");
        domainsDir.mkdirs();
        System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, muleHome.getCanonicalPath());

        new File(muleHome, "lib/shared/default").mkdirs();

        applicationDeploymentListener = mock(DeploymentListener.class);
        domainDeploymentListener = mock(DeploymentListener.class);
        deploymentService = new TestMuleDeploymentService(new MulePluginClassLoaderManager());
        deploymentService.addDeploymentListener(applicationDeploymentListener);
        deploymentService.addDomainDeploymentListener(domainDeploymentListener);
        deploymentService.addDeploymentListener(testDeploymentListener);
        deploymentService.addDomainDeploymentListener(testDomainDeploymentListener);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        // comment out the deletion to analyze results after test is done
        if (deploymentService != null)
        {
            deploymentService.stop();
        }
        FileUtils.deleteTree(muleHome);
        super.doTearDown();

        // this is a complex classloader setup and we can't reproduce standalone Mule 100%,
        // so trick the next test method into thinking it's the first run, otherwise
        // app resets CCL ref to null and breaks the next test
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());

        if (parallelDeployment)
        {
            System.clearProperty(PARALLEL_DEPLOYMENT_PROPERTY);
        }
    }

    @Test
    public void deploysAppZipOnStartup() throws Exception
    {
        deployAfterStartUp(dummyAppDescriptorFileBuilder);
    }

    @Test
    public void extensionManagerPresent() throws Exception
    {
        addPackedAppFromBuilder(emptyAppFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        final Application app = findApp(emptyAppFileBuilder.getId(), 1);
        assertThat(app.getMuleContext().getExtensionManager(), is(notNullValue()));
    }

    @Test
    public void appHomePropertyIsPresent() throws Exception
    {
        addExplodedAppFromBuilder(springPropertyAppFileBuilder);

        deploymentService.start();
        assertApplicationDeploymentSuccess(applicationDeploymentListener, springPropertyAppFileBuilder.getId());

        final Application app = findApp(springPropertyAppFileBuilder.getId(), 1);
        final MuleRegistry registry = getMuleRegistry(app);

        Map<String, Object> appProperties = registry.get("appProperties");
        assertThat(appProperties, is(notNullValue()));

        String appHome = (String) appProperties.get("appHome");
        assertThat(new File(appHome).exists(), is(true));
    }

    @Test
    public void deploysExplodedAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception
    {
        Action deployExplodedWaitAppAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                addExplodedAppFromBuilder(waitAppFileBuilder);
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
                addPackedAppFromBuilder(waitAppFileBuilder);
            }
        };
        deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployPackagedWaitAppAction);
    }

    @Test
    public void deploysAppZipWithExtensionUpperCaseAfterStartup() throws Exception
    {
        deployAfterStartUp(dummyAppDescriptorFileBuilderWithUpperCaseInExtension);
    }


    @Test
    public void deploysAppZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
        assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
        assertApplicationAnchorFileExists(dummyAppDescriptorFileBuilder.getId());

        // just assert no privileged entries were put in the registry
        final Application app = findApp(dummyAppDescriptorFileBuilder.getId(), 1);
        final MuleRegistry registry = getMuleRegistry(app);

        // mule-app.properties from the zip archive must have loaded properly
        assertEquals("mule-app.properties should have been loaded.", "someValue", registry.get("myCustomProp"));
    }

    @Test
    public void deploysBrokenAppZipOnStartup() throws Exception
    {
        addPackedAppFromBuilder(brokenAppFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, brokenAppFileBuilder.getId());

        assertAppsDir(new String[] {brokenAppFileBuilder.getDeployedPath()}, NONE, true);

        assertApplicationAnchorFileDoesNotExists(brokenAppFileBuilder.getId());

        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", brokenAppFileBuilder.getDeployedPath(), new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    /**
     * This tests deploys a broken app which name has a weird character.
     * It verifies that after failing deploying that app, it doesn't try to do it
     * again, which is a behavior than can be seen in some file systems due to
     * path handling issues
     */
    @Test
    public void doesNotRetriesBrokenAppWithFunkyName() throws Exception
    {
        addPackedAppFromBuilder(brokenAppWithFunkyNameAppFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, brokenAppWithFunkyNameAppFileBuilder.getId());
        assertAppsDir(new String[] {brokenAppWithFunkyNameAppFileBuilder.getDeployedPath()}, NONE, true);
        assertApplicationAnchorFileDoesNotExists(brokenAppWithFunkyNameAppFileBuilder.getId());

        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", brokenAppWithFunkyNameAppFileBuilder.getDeployedPath(), new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(applicationDeploymentListener);

        addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
        assertDeploymentFailure(applicationDeploymentListener, brokenAppWithFunkyNameAppFileBuilder.getId(), never());

        addPackedAppFromBuilder(emptyAppFileBuilder);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertDeploymentFailure(applicationDeploymentListener, brokenAppWithFunkyNameAppFileBuilder.getId(), never());
    }

    @Test
    public void deploysBrokenAppZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromBuilder(brokenAppFileBuilder);

        assertDeploymentFailure(applicationDeploymentListener, "broken-app");

        assertAppsDir(new String[] {"broken-app.zip"}, NONE, true);

        assertApplicationAnchorFileDoesNotExists(brokenAppFileBuilder.getId());

        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", "broken-app.zip", new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void redeploysAppZipDeployedOnStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromBuilder(emptyAppFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        addPackedAppFromBuilder(emptyAppFileBuilder);

        assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    }

    @Test
    public void removesPreviousAppFolderOnRedeploy() throws Exception {
        deploymentService.start();

        addPackedAppFromBuilder(emptyAppFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        ApplicationFileBuilder emptyAppFileBuilder =
          new ApplicationFileBuilder("empty-app").usingResource("empty-config.xml", "empty-config.xml")
            .deployedWith(PROPERTY_CONFIG_RESOURCES, "empty-config.xml");

        addPackedAppFromBuilder(emptyAppFileBuilder);

        assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        assertApplicationFiles(emptyAppFileBuilder.getId(), new String[] {"empty-config.xml", "mule-deploy.properties"});
    }

    @Test
    public void removesPreviousAppFolderOnStart() throws Exception {
        addExplodedAppFromBuilder(emptyAppFileBuilder);

        ApplicationFileBuilder emptyAppFileBuilder =
          new ApplicationFileBuilder("empty-app").usingResource("empty-config.xml", "empty-config.xml")
            .deployedWith(PROPERTY_CONFIG_RESOURCES, "empty-config.xml");

        addPackedAppFromBuilder(emptyAppFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);

        assertApplicationFiles(emptyAppFileBuilder.getId(), new String[] {"empty-config.xml", "mule-deploy.properties"});
    }

    @Test
    public void redeploysAppZipDeployedAfterStartup() throws Exception
    {
        addPackedAppFromBuilder(emptyAppFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        addPackedAppFromBuilder(emptyAppFileBuilder);

        assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    }

    @Test
    public void deploysExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromBuilder(emptyAppFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
        assertApplicationAnchorFileExists(emptyAppFileBuilder.getId());
    }

    @Test
    public void deploysPackagedAppOnStartupWhenExplodedAppIsAlsoPresent() throws Exception
    {
        addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);
        addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
        addPackedAppFromBuilder(emptyAppFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        // Checks that dummy app was deployed just once
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    }

    @Test
    public void deploysExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromBuilder(emptyAppFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
        assertApplicationAnchorFileExists(emptyAppFileBuilder.getId());
    }

    @Test
    public void deploysInvalidExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromBuilder(emptyAppFileBuilder, "app with spaces");

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

        addExplodedAppFromBuilder(emptyAppFileBuilder, "app with spaces");

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

        addExplodedAppFromBuilder(emptyAppFileBuilder, "app with spaces");
        assertDeploymentFailure(applicationDeploymentListener, "app with spaces", times(1));

        addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

        addExplodedAppFromBuilder(emptyAppFileBuilder);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        // After three update cycles should have only one deployment failure notification for the broken app
        assertDeploymentFailure(applicationDeploymentListener, "app with spaces");
    }

    @Test
    public void deploysBrokenExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromBuilder(incompleteAppFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        assertApplicationAnchorFileDoesNotExists(incompleteAppFileBuilder.getId());

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromBuilder(incompleteAppFileBuilder);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        assertApplicationAnchorFileDoesNotExists(incompleteAppFileBuilder.getId());

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void redeploysExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
        assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
        configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    }

    @Test
    public void redeploysExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromBuilder(emptyAppFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + "/" + emptyAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
        assertThat("Configuration file does not exists", configFile.exists(), is(true));
        assertThat("Could not update last updated time in configuration file", configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS), is(true));

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    }

    @Test
    public void redeploysBrokenExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromBuilder(incompleteAppFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(applicationDeploymentListener);

        final ReentrantLock lock = deploymentService.getLock();
        lock.lock();
        try
        {
            File configFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
            assertThat(configFile.exists(), is(true));
            configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);
        }
        finally
        {
            lock.unlock();
        }
        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());
    }

    @Test
    public void redeploysBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromBuilder(incompleteAppFileBuilder);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        // Maintains app dir created
        assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
        assertThat(configFile.exists(), is(true));
        configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());
    }

    @Test
    public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentOnStartup() throws Exception
    {
        addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

        assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
        URL url = getClass().getResource(BROKEN_CONFIG_XML);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentFailure(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
        assertStatus(dummyAppDescriptorFileBuilder.getId(), ApplicationStatus.DEPLOYMENT_FAILED);
    }

    @Test
    public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
        assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
        assertThat(originalConfigFile.exists(), is(true));
        URL url = getClass().getResource(BROKEN_CONFIG_XML);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertDeploymentFailure(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
        assertStatus(dummyAppDescriptorFileBuilder.getId(), ApplicationStatus.DEPLOYMENT_FAILED);
    }

    @Test
    public void redeploysFixedAppAfterBrokenExplodedAppOnStartup() throws Exception
    {
        addExplodedAppFromBuilder(incompleteAppFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
        assertThat(originalConfigFile.exists(), is(true));
        URL url = getClass().getResource(EMPTY_DOMAIN_CONFIG_XML);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        addPackedAppFromBuilder(emptyAppFileBuilder);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
    }

    @Test
    public void redeploysFixedAppAfterBrokenExplodedAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedAppFromBuilder(incompleteAppFileBuilder);


        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
        URL url = getClass().getResource(EMPTY_DOMAIN_CONFIG_XML);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        addPackedAppFromBuilder(emptyAppFileBuilder);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
    }

    @Test
    public void deployDomainWithDeploymentProperties() throws Exception
    {
        Properties deploymentProperties = new Properties();
        deploymentProperties.put("httpPort", httpPortAlternative.getValue());
        deploymentService.deployDomain(sharedHttpDomainFileBuilder.getArtifactFile().toURI().toURL(), deploymentProperties);        
        DefaultHttpListenerConfig httpListenerConfig = testDomainDeploymentListener.getMuleContext().getRegistry().get("http-listener-config");        
        assertThat(httpListenerConfig.getPort(), equalTo(httpPortAlternative.getNumber()));
        assertPropertyValue(testDomainDeploymentListener, "httpPort", httpPortAlternative.getValue());
        
        // Redeploys without deployment properties (remains the same, as it takes the deployment properties from the persisted file)
        deploymentService.redeployDomain(sharedHttpDomainFileBuilder.getId());
        httpListenerConfig = testDomainDeploymentListener.getMuleContext().getRegistry().get("http-listener-config");
        assertThat(httpListenerConfig.getPort(), equalTo(httpPortAlternative.getNumber()));
        assertPropertyValue(testDomainDeploymentListener, "httpPort", httpPortAlternative.getValue());
        
        // Redeploy with new deployment properties
        deploymentProperties.put("httpPort", httpPort.getValue());
        deploymentService.redeployDomain(sharedHttpDomainFileBuilder.getId(), deploymentProperties);
        httpListenerConfig = testDomainDeploymentListener.getMuleContext().getRegistry().get("http-listener-config");
        assertThat(httpListenerConfig.getPort(), equalTo(httpPort.getNumber()));
        assertPropertyValue(testDomainDeploymentListener, "httpPort", httpPort.getValue());
    }
    
    @Test
    public void redeployModifiedDomainAndRedeployFailedApps() throws Exception
    {
        addExplodedDomainFromBuilder(sharedHttpBundleDomainFileBuilder);

        //change shared http config name to use a wrong name
        File domainConfigFile = new File(domainsDir + "/" + sharedHttpBundleDomainFileBuilder.getDeployedPath(), DOMAIN_CONFIG_FILE_LOCATION);
        String correctDomainConfigContent = IOUtils.toString(new FileInputStream(domainConfigFile));
        String wrongDomainFileContext = correctDomainConfigContent.replace("http-listener-config", "http-listener-config-wrong");
        FileUtils.copyInputStreamToFile(new ByteArrayInputStream(wrongDomainFileContext.getBytes()), domainConfigFile);
        long firstFileTimestamp = domainConfigFile.lastModified();

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, sharedHttpDomainFileBuilder.getId());
        assertDeploymentFailure(applicationDeploymentListener, httpAAppFileBuilder.getId());
        assertDeploymentFailure(applicationDeploymentListener, httpBAppFileBuilder.getId());

        reset(applicationDeploymentListener);
        reset(domainDeploymentListener);

        FileUtils.copyInputStreamToFile(new ByteArrayInputStream(correctDomainConfigContent.getBytes()), domainConfigFile);
        alterTimestampIfNeeded(domainConfigFile, firstFileTimestamp);

        assertDeploymentSuccess(domainDeploymentListener, sharedHttpDomainFileBuilder.getId());
        assertDeploymentSuccess(applicationDeploymentListener, httpAAppFileBuilder.getId());
        assertDeploymentSuccess(applicationDeploymentListener, httpBAppFileBuilder.getId());
    }

    @Test
    public void redeploysZipAppOnConfigChanges() throws Exception
    {
        addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

        assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        reset(applicationDeploymentListener);

        File configFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
        configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);

        assertUndeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
        assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
    }

    @Test
    public void removesZombieFilesAfterFailedAppIsDeleted() throws Exception
    {
        final String appName = "bad-config-app";

        addPackedAppFromBuilder(badConfigAppFileBuilder);
        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, appName);
        assertAppsDir(new String[] {}, new String[] {appName}, true);

        final Map<URL, Long> startZombieMap = deploymentService.getZombieApplications();
        assertEquals("Should be a zombie file for the app's broken XML config", 1, startZombieMap.size());

        final Application app = findApp(badConfigAppFileBuilder.getId(), 1);
        assertStatus(app, ApplicationStatus.DEPLOYMENT_FAILED);
        assertApplicationAnchorFileDoesNotExists(app.getArtifactName());

        reset(applicationDeploymentListener);
        org.apache.commons.io.FileUtils.deleteDirectory(new File(appsDir, app.getArtifactName()));
        assertAppFolderIsDeleted(appName);
        assertAtLeastOneUndeploymentSuccess(applicationDeploymentListener, appName);

        final Map<URL, Long> endZombieMap = deploymentService.getZombieApplications();
        assertEquals("Should not be any more zombie files present", 0, endZombieMap.size());
    }

    @Test
    public void brokenAppArchiveWithoutArgument() throws Exception
    {
        doBrokenAppArchiveTest();
    }

    @Test
    public void brokenAppArchiveAsArgument() throws Exception
    {
        Map<String, Object> startupOptions = new HashMap<>();
        startupOptions.put("app", brokenAppFileBuilder.getId());
        StartupContext.get().setStartupOptions(startupOptions);

        doBrokenAppArchiveTest();
    }

    @Test
    public void deploysInvalidZipAppOnStartup() throws Exception
    {
        addPackedAppFromBuilder(emptyAppFileBuilder, "app with spaces.zip");

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

        addPackedAppFromBuilder(emptyAppFileBuilder, "app with spaces.zip");

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
    public void deployAppNameWithZipSuffix() throws Exception
    {
        final ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("empty-app.zip", emptyAppFileBuilder);
        addPackedAppFromBuilder(applicationFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
        reset(applicationDeploymentListener);

        assertAppsDir(NONE, new String[] {applicationFileBuilder.getDeployedPath()}, true);
        assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

        // Checks that the empty-app.zip folder is not processed as a zip file
        assertNoDeploymentInvoked(applicationDeploymentListener);
    }

    @Test
    public void deploysPackedAppsInOrderWhenAppArgumentIsUsed() throws Exception
    {
        assumeThat(parallelDeployment, is(false));

        addPackedAppFromBuilder(emptyAppFileBuilder, "1.zip");
        addPackedAppFromBuilder(emptyAppFileBuilder, "2.zip");
        addPackedAppFromBuilder(emptyAppFileBuilder, "3.zip");

        Map<String, Object> startupOptions = new HashMap<>();
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
        assumeThat(parallelDeployment, is(false));

        addExplodedAppFromBuilder(emptyAppFileBuilder, "1");
        addExplodedAppFromBuilder(emptyAppFileBuilder, "2");
        addExplodedAppFromBuilder(emptyAppFileBuilder, "3");

        Map<String, Object> startupOptions = new HashMap<>();
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
        addPackedAppFromBuilder(emptyAppFileBuilder);

        Map<String, Object> startupOptions = new HashMap<>();
        startupOptions.put("app", "empty-app:empty-app:empty-app");
        StartupContext.get().setStartupOptions(startupOptions);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);

        List<Application> applications = deploymentService.getApplications();
        assertEquals(1, applications.size());
    }

    @Test
    public void tracksAppConfigUpdateTime() throws Exception
    {
        addExplodedAppFromBuilder(emptyAppFileBuilder);

        // Sets a modification time in the future
        File appFolder = new File(appsDir.getPath(), emptyAppFileBuilder.getId());
        File configFile = new File(appFolder, MULE_CONFIG_XML_FILE);
        configFile.setLastModified(System.currentTimeMillis() + ONE_HOUR_IN_MILLISECONDS);

        deploymentService.start();
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        reset(applicationDeploymentListener);

        assertNoDeploymentInvoked(applicationDeploymentListener);
    }

    @Test
    public void redeployedFailedAppAfterTouched() throws Exception
    {
        addExplodedAppFromBuilder(emptyAppFileBuilder);

        File appFolder = new File(appsDir.getPath(), emptyAppFileBuilder.getId());

        File configFile = new File(appFolder, MULE_CONFIG_XML_FILE);
        FileUtils.writeStringToFile(configFile, "you shall not pass");

        deploymentService.start();
        assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());
        reset(applicationDeploymentListener);

        URL url = getClass().getResource(EMPTY_DOMAIN_CONFIG_XML);
        FileUtils.copyFile(new File(url.toURI()), configFile);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    }

    @Test
    public void receivesMuleContextDeploymentNotifications() throws Exception
    {
        // NOTE: need an integration test like this because DefaultMuleApplication
        // class cannot be unit tested.
        addPackedAppFromBuilder(emptyAppFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertMuleContextCreated(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertMuleContextInitialized(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertMuleContextConfigured(applicationDeploymentListener, emptyAppFileBuilder.getId());
    }

    @Test
    public void undeploysStoppedApp() throws Exception
    {
        addPackedAppFromBuilder(emptyAppFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        final Application app = findApp(emptyAppFileBuilder.getId(), 1);
        app.stop();
        assertStatus(app, ApplicationStatus.STOPPED);

        deploymentService.undeploy(app);
    }

    @Test
    public void undeploysApplicationRemovingAnchorFile() throws Exception
    {
        addPackedAppFromBuilder(emptyAppFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        Application app = findApp(emptyAppFileBuilder.getId(), 1);

        assertTrue("Unable to remove anchor file", removeAppAnchorFile(emptyAppFileBuilder.getId()));

        assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertStatus(app, ApplicationStatus.DESTROYED);
    }

    @Test
    public void undeploysAppCompletelyEvenOnStoppingException() throws Exception
    {
        addPackedAppFromBuilder(emptyAppFileBuilder);

        TestApplicationFactory appFactory = new TestApplicationFactory(new MuleApplicationClassLoaderFactory(new MuleDomainClassLoaderRepository(), new DefaultNativeLibraryFinderFactory()));
        appFactory.setFailOnStopApplication(true);

        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        Application app = findApp(emptyAppFileBuilder.getId(), 1);

        assertTrue("Unable to remove anchor file", removeAppAnchorFile(emptyAppFileBuilder.getId()));

        assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        assertAppFolderIsDeleted(emptyAppFileBuilder.getId());
        assertStatus(app, ApplicationStatus.DESTROYED);
    }

    @Test
    public void undeploysAppCompletelyEvenOnDisposingException() throws Exception
    {
        addPackedAppFromBuilder(emptyAppFileBuilder);

        TestApplicationFactory appFactory = new TestApplicationFactory(new MuleApplicationClassLoaderFactory(new MuleDomainClassLoaderRepository(), new DefaultNativeLibraryFinderFactory()));
        appFactory.setFailOnDisposeApplication(true);
        deploymentService.setAppFactory(appFactory);
        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        Application app = findApp(emptyAppFileBuilder.getId(), 1);

        assertTrue("Unable to remove anchor file", removeAppAnchorFile(emptyAppFileBuilder.getId()));

        assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertStatus(app, ApplicationStatus.STOPPED);
        assertAppFolderIsDeleted(emptyAppFileBuilder.getId());
    }

    @Test
    public void deploysIncompleteZipAppOnStartup() throws Exception
    {
        addPackedAppFromBuilder(incompleteAppFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromBuilder(emptyAppFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void deploysIncompleteZipAppAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromBuilder(incompleteAppFileBuilder);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromBuilder(emptyAppFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void mantainsAppFolderOnExplodedAppDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedAppFromBuilder(incompleteAppFileBuilder);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromBuilder(emptyAppFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        // Check that the failed application folder is still there
        assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysZipAppAfterDeploymentErrorOnStartup() throws Exception
    {
        addPackedAppFromBuilder(incompleteAppFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromBuilder(emptyAppFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromBuilder(emptyAppFileBuilder, incompleteAppFileBuilder.getZipPath());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieApplications().size());
    }

    @Test
    public void redeploysZipAppAfterDeploymentErrorAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromBuilder(incompleteAppFileBuilder);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromBuilder(emptyAppFileBuilder);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromBuilder(emptyAppFileBuilder, incompleteAppFileBuilder.getZipPath());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieApplications().size());
    }

    @Test
    public void redeploysInvalidZipAppAfterSuccessfulDeploymentOnStartup() throws Exception
    {
        addPackedAppFromBuilder(emptyAppFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        addPackedAppFromBuilder(incompleteAppFileBuilder, emptyAppFileBuilder.getZipPath());

        assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", emptyAppFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterSuccessfulDeploymentAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromBuilder(emptyAppFileBuilder);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        addPackedAppFromBuilder(incompleteAppFileBuilder, emptyAppFileBuilder.getZipPath());
        assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", emptyAppFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterFailedDeploymentOnStartup() throws Exception
    {
        addPackedAppFromBuilder(incompleteAppFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        reset(applicationDeploymentListener);

        addPackedAppFromBuilder(incompleteAppFileBuilder);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipAppAfterFailedDeploymentAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedAppFromBuilder(incompleteAppFileBuilder);
        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        reset(applicationDeploymentListener);

        addPackedAppFromBuilder(incompleteAppFileBuilder);
        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysExplodedAppAfterDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedAppFromBuilder(incompleteAppFileBuilder);

        assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedAppFromBuilder(emptyAppFileBuilder);

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        // Redeploys a fixed version for incompleteApp
        addExplodedAppFromBuilder(emptyAppFileBuilder, incompleteAppFileBuilder.getId());

        assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());
        assertEquals("Failed app still appears as zombie after a successful redeploy", 0, deploymentService.getZombieApplications().size());
    }

    @Test
    public void deploysAppZipWithPlugin() throws Exception
    {
        addPackedAppFromBuilder(echoPluginAppFileBuilder);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, echoPluginAppFileBuilder.getId());
    }

    @Test
    public void undeploysAppWithPlugin() throws Exception
    {
        addPackedAppFromBuilder(echoPluginAppFileBuilder);

        deploymentService.start();

        assertDeploymentSuccess(applicationDeploymentListener, echoPluginAppFileBuilder.getId());
        final Application app = findApp(echoPluginAppFileBuilder.getId(), 1);

        // As this app has a plugin, the tmp directory must exist
        assertApplicationTmpFileExists(app.getArtifactName());

        // Remove the anchor file so undeployment starts
        assertTrue("Unable to remove anchor file", removeAppAnchorFile(echoPluginAppFileBuilder.getId()));

        assertUndeploymentSuccess(applicationDeploymentListener, echoPluginAppFileBuilder.getId());
        assertStatus(app, ApplicationStatus.DESTROYED);

        // Check the tmp directory was effectively removed
        assertApplicationTmpFileDoesNotExists(app.getArtifactName());
    }

    @Test
    public void deploysAppWithPluginSharedLibrary() throws Exception
    {
        addPackedAppFromBuilder(sharedLibPluginAppFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, sharedLibPluginAppFileBuilder.getId());
        assertAppsDir(NONE, new String[] {sharedLibPluginAppFileBuilder.getId()}, true);
        assertApplicationAnchorFileExists(sharedLibPluginAppFileBuilder.getId());
    }

    @Test
    public void synchronizesDeploymentOnStart() throws Exception
    {
        addPackedAppFromBuilder(emptyAppFileBuilder);

        Thread deploymentServiceThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                deploymentService.start();
            }
        });

        final boolean[] lockedFromClient = new boolean[1];

        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {

                Thread deploymentClientThread = new Thread(new Runnable()
                {
                    @Override
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
        }).when(applicationDeploymentListener).onDeploymentStart(emptyAppFileBuilder.getId());

        deploymentServiceThread.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        assertFalse("Able to lock deployment service during start", lockedFromClient[0]);
    }

    @Test
    public void propertiesAreOverridenByDeploymentProperties() throws Exception
    {
        System.setProperty(PORT_PROPERTY_NAME, SYSTEM_PROPERTY_PORT);
        Properties deploymentProperties = new Properties();
        deploymentProperties.put(PORT_PROPERTY_NAME, deploymentPropertiesPort.getValue());
        deploymentService.deploy(dummyAppDescriptorWithPropsFileBuilder.getArtifactFile().toURI().toURL(), deploymentProperties); 
        assertMuleContextCreated(applicationDeploymentListener, dummyAppDescriptorWithPropsFileBuilder.getId());
        assertPropertyValue(testDeploymentListener, PORT_PROPERTY_NAME, deploymentPropertiesPort.getValue());
        DefaultHttpListenerConfig httpListenerConfig = testDeploymentListener.getMuleContext().getRegistry().get("listenerConfig");        
        assertThat(httpListenerConfig.getPort(), equalTo(deploymentPropertiesPort.getNumber()));
    }
    
    @Test
    public void propertiesInSpringFileAreResolvedIntoDeploymentProperties() throws Exception
    {
        Properties deploymentPropertiees = new Properties();
        deploymentPropertiees.put("someValue", "DUMMY_VALUE");
        deploymentService.deploy(springPropertyAppFileBuilder.getArtifactFile().toURI().toURL(), deploymentPropertiees);
        assertDeploymentPropertiesFileAreOverriden(testDeploymentListener, "APP_HOME", "DUMMY_VALUE");
    }

    @Test
    public void noPropsAreOverridenOnDeployThenTheyAreOverridenWhenRedeployWithNewPropertiesSet() throws Exception
    {
        deploymentService.deploy(dummyCascadingPropsAppDescriptorFileBuilder.getArtifactFile().toURI().toURL());
        assertPropertyValue(testDeploymentListener, "prop1", "value1");                

        Properties deploymentProperties = new Properties();
        deploymentProperties.put("prop1", "DUMMY_VALUE");
        deploymentService.redeploy(dummyCascadingPropsAppDescriptorFileBuilder.getId(), deploymentProperties);
        assertPropertyValue(testDeploymentListener, "prop1", "DUMMY_VALUE");        
    }
    
    @Test
    public void propsAreMantainedInRedeployThenChangedWhenNewPropertiesAreSet() throws Exception
    {
        Properties deploymentProperties = new Properties();
        deploymentProperties.put("prop1", "DUMMY_VALUE");
        deploymentService.deploy(dummyCascadingPropsAppDescriptorFileBuilder.getArtifactFile().toURI().toURL(), deploymentProperties);
        assertPropertyValue(testDeploymentListener, "prop1", "DUMMY_VALUE");        

        deploymentService.redeploy(dummyCascadingPropsAppDescriptorFileBuilder.getId());
        assertPropertyValue(testDeploymentListener, "prop1", "DUMMY_VALUE");
        
        deploymentProperties.clear();
        deploymentService.redeploy(dummyCascadingPropsAppDescriptorFileBuilder.getId(), deploymentProperties);
        assertPropertyValue(testDeploymentListener, "prop1", "value1");        
    }
    
    @Test
    public void cascadingPropsAreOverridenIntoConfigurationThenTakesGlobalpropertyOnRedeployWithDeploymentProperties() throws Exception
    {
        Properties deploymentProperties = new Properties();
        deploymentProperties.put("prop1", "DUMMY_VALUE");
        deploymentService.deploy(dummyCascadingPropsAppDescriptorFileBuilder.getArtifactFile().toURI().toURL(), deploymentProperties);
        assertPropertyValue(testDeploymentListener, "prop1", "DUMMY_VALUE");        

        deploymentProperties.clear();
        deploymentService.redeploy(dummyCascadingPropsAppDescriptorFileBuilder.getId(), deploymentProperties);
        assertPropertyValue(testDeploymentListener, "prop1", "value1");        
    }

    
    @Test
    public void propertiesInFileAreOverriddenIntoDeploymentProperties() throws Exception
    {
        Properties deploymentProperties = new Properties();
        deploymentProperties.put("myCustomProp", "DUMMY_VALUE");
        deploymentService.deploy(dummyAppDescriptorFileBuilder.getArtifactFile().toURI().toURL(), deploymentProperties);
        assertPropertyValue(testDeploymentListener, "myCustomProp", "DUMMY_VALUE");
    }

    @Test
    public void applicationsWithTheSameSystemPropertiesOverridesDeploymentPropertiesSeparately() throws Exception
    {
        System.setProperty("myCustomProp", "WRONG_VALUE");
        Properties deploymentProperties1 = new Properties();
        deploymentProperties1.put("myCustomProp", "DUMMY_VALUE");
        deploymentService.deploy(dummyAppDescriptorFileBuilder.getArtifactFile().toURI().toURL(), deploymentProperties1);
        MuleContext muleContextApp1 = testDeploymentListener.getMuleContext();

        Properties deploymentProperties2 = new Properties();
        deploymentProperties2.put("port", deploymentPropertiesPort.getValue());
        deploymentProperties2.put("myCustomProp", "DUMMY_VALUE2");
        deploymentService.deploy(dummyAppDescriptorWithPropsFileBuilder.getArtifactFile().toURI().toURL(), deploymentProperties2);
        MuleContext muleContextApp2 = testDeploymentListener.getMuleContext();
        
        assertThat((String) muleContextApp1.getRegistry().get("myCustomProp"), equalTo("DUMMY_VALUE"));
        assertThat((String) muleContextApp2.getRegistry().get("myCustomProp"), equalTo("DUMMY_VALUE2"));
    }
    
    private void assertPropertyValue(final TestDeploymentListener listener, final String propertyName, final String propertyValue)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new JUnitProbe()
        {
            @Override
            public boolean test()
            {
                Registry registry = listener.getMuleContext().getRegistry();
                return registry.get(propertyName).equals(propertyValue);
            }

            @Override
            public String describeFailure()
            {
                return "Properties were not overriden by the deployment properties";
            }
        });
    }
    private void assertDeploymentPropertiesFileAreOverriden(final TestDeploymentListener listener, final String oropertyName, String propertyValue)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new JUnitProbe()
        {
            @Override
            public boolean test()
            {
                Registry registry = listener.getMuleContext().getRegistry();
                Map<String, String> properties = registry.get("appProperties");
                return properties.get("myCustomProp").equals("DUMMY_VALUE");
            }

            @Override
            public String describeFailure()
            {
                return "File properties were not overriden by the deployment properties";
            }
        });
    }

    @Test
    public void propertiesAreChangedInRedeployByDeploymentProperties() throws Exception
    {
        System.setProperty(PORT_PROPERTY_NAME, SYSTEM_PROPERTY_PORT);
        Properties deploymentProperties = new Properties();
        deploymentProperties.put(PORT_PROPERTY_NAME, deploymentPropertiesPort.getValue());
        deploymentService.deploy(dummyAppDescriptorWithPropsFileBuilder.getArtifactFile().toURI().toURL(), deploymentProperties); 
        assertMuleContextCreated(applicationDeploymentListener, dummyAppDescriptorWithPropsFileBuilder.getId());
        assertPropertyValue(testDeploymentListener, PORT_PROPERTY_NAME, deploymentPropertiesPort.getValue());
        deploymentProperties.put(PORT_PROPERTY_NAME, deploymentPropertiesOverriddenPort.getValue());
        deploymentService.redeploy(testDeploymentListener.getArtifactName(), deploymentProperties);
        assertPropertyValue(testDeploymentListener, PORT_PROPERTY_NAME, deploymentPropertiesOverriddenPort.getValue());
    }

    @Test
    public void synchronizesAppDeployFromClient() throws Exception
    {
        final Action action = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                deploymentService.deploy(dummyAppDescriptorFileBuilder.getArtifactFile().toURI().toURL());
            }
        };

        final Action assertAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                verify(applicationDeploymentListener, never()).onDeploymentStart(dummyAppDescriptorFileBuilder.getId());
            }
        };
        doSynchronizedAppDeploymentActionTest(action, assertAction);
    }

    @Test
    public void synchronizesAppUndeployFromClient() throws Exception
    {
        final Action action = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                deploymentService.undeploy(emptyAppFileBuilder.getId());
            }
        };

        final Action assertAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                verify(applicationDeploymentListener, never()).onUndeploymentStart(emptyAppFileBuilder.getId());
            }
        };
        doSynchronizedAppDeploymentActionTest(action, assertAction);
    }

    @Test
    public void synchronizesAppRedeployFromClient() throws Exception
    {
        final Action action = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                // Clears notification from first deployment
                reset(applicationDeploymentListener);
                deploymentService.redeploy(emptyAppFileBuilder.getId());
            }
        };

        final Action assertAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                verify(applicationDeploymentListener, never()).onDeploymentStart(emptyAppFileBuilder.getId());
            }
        };
        doSynchronizedAppDeploymentActionTest(action, assertAction);
    }

    private void doSynchronizedAppDeploymentActionTest(final Action deploymentAction, final Action assertAction) throws Exception
    {
        addPackedAppFromBuilder(emptyAppFileBuilder);

        doSynchronizedArtifactDeploymentActionTest(deploymentAction, assertAction, applicationDeploymentListener, emptyAppFileBuilder.getId());
    }

    @Test
    public void deploysMultipleAppsZipOnStartup() throws Exception
    {
        final int totalApps = 20;

        for (int i = 1; i <= totalApps; i++)
        {
            addExplodedAppFromBuilder(emptyAppFileBuilder, Integer.toString(i));
        }

        deploymentService.start();

        for (int i = 1; i <= totalApps; i++)
        {
            assertDeploymentSuccess(applicationDeploymentListener, Integer.toString(i));
        }
    }

    @Test
    public void deploysDomainZipOnStartup() throws Exception
    {
        addPackedDomainFromBuilder(emptyDomainFileBuilder);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        assertDomainDir(NONE, new String[] {emptyDomainFileBuilder.getId()}, true);

        final Domain domain = findADomain(emptyDomainFileBuilder.getId(), 1);
        assertNotNull(domain);
        assertNotNull(domain.getMuleContext());
        assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
    }

    @Test
    public void deploysPackagedDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception
    {
        Action deployPackagedWaitDomainAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                addPackedDomainFromBuilder(waitDomainFileBuilder);
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
                addExplodedDomainFromBuilder(waitDomainFileBuilder);
            }
        };
        deploysDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployExplodedWaitDomainAction);
    }

    @Test
    public void deploysExplodedDomainBundleOnStartup() throws Exception
    {
        addExplodedDomainFromBuilder(dummyDomainBundleFileBuilder);

        deploymentService.start();

        deploysDomainBundle();
    }

    @Test
    public void deploysExplodedDomainBundleAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromBuilder(dummyDomainBundleFileBuilder);

        deploysDomainBundle();
    }

    @Test
    public void deploysDomainBundleZipOnStartup() throws Exception
    {
        addPackedDomainFromBuilder(dummyDomainBundleFileBuilder);

        deploymentService.start();

        deploysDomainBundle();
    }

    @Test
    public void deploysDomainBundleZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromBuilder(dummyDomainBundleFileBuilder);

        deploysDomainBundle();
    }

    private void deploysDomainBundle()
    {
        assertDeploymentSuccess(domainDeploymentListener, dummyDomainBundleFileBuilder.getId());

        assertDomainDir(NONE, new String[] {dummyDomainBundleFileBuilder.getId()}, true);

        final Domain domain = findADomain(dummyDomainBundleFileBuilder.getId(), 1);
        assertNotNull(domain);
        assertNull(domain.getMuleContext());

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
        assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

        final Application app = findApp(dummyAppDescriptorFileBuilder.getId(), 1);
        assertNotNull(app);
    }

    @Test
    public void deploysInvalidExplodedDomainBundleOnStartup() throws Exception
    {
        addExplodedDomainFromBuilder(invalidDomainBundleFileBuilder);

        deploymentService.start();

        deploysInvalidDomainBundleZip();
    }

    @Test
    public void deploysInvalidExplodedDomainBundleAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromBuilder(invalidDomainBundleFileBuilder);

        deploysInvalidDomainBundleZip();
    }

    @Test
    public void deploysInvalidDomainBundleZipOnStartup() throws Exception
    {
        addPackedDomainFromBuilder(invalidDomainBundleFileBuilder);

        deploymentService.start();

        deploysInvalidDomainBundleZip();
    }

    @Test
    public void deploysInvalidDomainBundleZipAfterStartup() throws Exception
    {
        addPackedDomainFromBuilder(invalidDomainBundleFileBuilder);

        deploymentService.start();

        deploysInvalidDomainBundleZip();
    }

    private void deploysInvalidDomainBundleZip()
    {
        assertDeploymentFailure(domainDeploymentListener, invalidDomainBundleFileBuilder.getId());

        assertDomainDir(NONE, new String[] {invalidDomainBundleFileBuilder.getId()}, true);

        assertAppsDir(NONE, new String[] {}, true);
    }

    @Test
    public void deploysDomainZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromBuilder(emptyDomainFileBuilder);

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        assertDomainDir(NONE, new String[] {emptyDomainFileBuilder.getId()}, true);

        final Domain domain = findADomain(emptyDomainFileBuilder.getId(), 1);
        assertNotNull(domain);
        assertNotNull(domain.getMuleContext());
        assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
    }

    @Test
    public void deploysBrokenDomainZipOnStartup() throws Exception
    {
        addPackedDomainFromBuilder(brokenDomainFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, brokenDomainFileBuilder.getId());

        assertDomainDir(new String[] {brokenDomainFileBuilder.getDeployedPath()}, NONE, true);

        assertDomainAnchorFileDoesNotExists(brokenDomainFileBuilder.getId());

        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as domain", brokenDomainFileBuilder.getDeployedPath(), new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenDomainZipAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromBuilder(brokenDomainFileBuilder);

        assertDeploymentFailure(domainDeploymentListener, brokenDomainFileBuilder.getId());

        assertDomainDir(new String[] {brokenDomainFileBuilder.getDeployedPath()}, NONE, true);

        assertDomainAnchorFileDoesNotExists(brokenDomainFileBuilder.getId());

        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as domain.", brokenDomainFileBuilder.getDeployedPath(), new File(zombie.getKey().getFile()).getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void redeploysDomainZipDeployedOnStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromBuilder(emptyAppFileBuilder);
        File dummyDomainFile = new File(domainsDir, emptyAppFileBuilder.getZipPath());
        long firstFileTimestamp = dummyDomainFile.lastModified();

        assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

        assertDomainDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());

        reset(domainDeploymentListener);

        addPackedDomainFromBuilder(emptyAppFileBuilder);
        alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

        assertUndeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());
        assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());
        assertDomainDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    }

    @Test
    public void redeployedDomainsAreDifferent() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromBuilder(emptyAppFileBuilder);
        File dummyDomainFile = new File(domainsDir, emptyAppFileBuilder.getZipPath());
        long firstFileTimestamp = dummyDomainFile.lastModified();

        assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());
        Domain firstDomain = deploymentService.getDomains().get(0);

        reset(domainDeploymentListener);

        addPackedDomainFromBuilder(emptyAppFileBuilder);
        alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

        assertUndeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());
        assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());
        Domain secondDomain = deploymentService.getDomains().get(0);

        assertNotSame(firstDomain, secondDomain);
    }

    @Test
    public void redeploysDomainZipRefreshesApps() throws Exception
    {
        addPackedDomainFromBuilder(dummyDomainFileBuilder);
        File dummyDomainFile = new File(domainsDir, dummyDomainFileBuilder.getZipPath());
        long firstFileTimestamp = dummyDomainFile.lastModified();

        addPackedAppFromBuilder(dummyDomainApp1FileBuilder);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());

        reset(domainDeploymentListener);
        reset(applicationDeploymentListener);

        addPackedDomainFromBuilder(dummyDomainFileBuilder);
        alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

        assertUndeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
        assertUndeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    }

    @Test
    public void redeploysDomainZipDeployedAfterStartup() throws Exception
    {
        addPackedDomainFromBuilder(dummyDomainFileBuilder);
        File dummyDomainFile = new File(domainsDir, dummyDomainFileBuilder.getZipPath());
        long firstFileTimestamp = dummyDomainFile.lastModified();

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

        assertDomainDir(NONE, new String[] {dummyDomainFileBuilder.getId()}, true);
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());

        reset(domainDeploymentListener);

        addPackedDomainFromBuilder(dummyDomainFileBuilder);
        alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

        assertUndeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
        assertEquals("Domain has not been properly registered with Mule", 1, deploymentService.getDomains().size());
        assertDomainDir(NONE, new String[] {dummyDomainFileBuilder.getId()}, true);
    }


    @Test
    public void applicationBundledWithinDomainNotRemovedAfterFullDeploy() throws Exception
    {
        resetUndeployLatch();
        dummyDomainBundleFileBuilder.containing(emptyAppFileBuilder);
        dummyDomainBundleFileBuilder.definedBy("empty-domain-config.xml");
        emptyAppFileBuilder.deployedWith("domain", "dummy-domain-bundle");
        addPackedDomainFromBuilder(dummyDomainBundleFileBuilder);

        deploymentService.start();

        doRedeployBrokenDomainAfterFixedDomain();
    }

    protected void alterTimestampIfNeeded(File file, long firstTimestamp)
    {
        if (!file.exists())
        {
            throw new IllegalArgumentException("File does not exists: " + file.getAbsolutePath());
        }
        if (firstTimestamp == file.lastModified())
        {
            // File systems only have second precision. If both file writes happen during the same second, the last
            // change will be ignored by the directory scanner.
            assertThat(file.setLastModified(file.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS), is(true));
        }
    }

    @Test
    public void deploysExplodedDomainOnStartup() throws Exception
    {
        addExplodedDomainFromBuilder(emptyDomainFileBuilder);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
        assertDomainDir(NONE, new String[] {emptyDomainFileBuilder.getId()}, true);
        assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
    }

    @Test
    public void deploysPackagedDomainOnStartupWhenExplodedDomainIsAlsoPresent() throws Exception
    {
        addExplodedDomainFromBuilder(emptyDomainFileBuilder);
        addPackedDomainFromBuilder(emptyDomainFileBuilder);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        addExplodedDomainFromBuilder(emptyDomainFileBuilder);

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        // Checks that dummy app was deployed just once
        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    }

    @Test
    public void deploysExplodedDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromBuilder(emptyDomainFileBuilder);

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
        assertDomainDir(NONE, new String[] {emptyDomainFileBuilder.getId()}, true);
        assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
    }

    @Test
    public void deploysInvalidExplodedDomainOnStartup() throws Exception
    {
        addExplodedDomainFromBuilder(emptyDomainFileBuilder, "domain with spaces");

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

        addExplodedDomainFromBuilder(emptyDomainFileBuilder, "domain with spaces");

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

        addExplodedDomainFromBuilder(emptyDomainFileBuilder, "domain with spaces");
        assertDeploymentFailure(domainDeploymentListener, "domain with spaces", times(1));

        addExplodedDomainFromBuilder(emptyDomainFileBuilder);
        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        addExplodedDomainFromBuilder(emptyAppFileBuilder, "empty2-domain");
        assertDeploymentSuccess(domainDeploymentListener, "empty2-domain");

        // After three update cycles should have only one deployment failure notification for the broken app
        assertDeploymentFailure(domainDeploymentListener, "domain with spaces");
    }

    @Test
    public void deploysBrokenExplodedDomainOnStartup() throws Exception
    {
        addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        // Maintains app dir created
        assertDomainDir(NONE, new String[] {incompleteDomainFileBuilder.getId()}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void deploysBrokenExplodedDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        // Maintains app dir created
        assertDomainDir(NONE, new String[] {incompleteDomainFileBuilder.getId()}, true);
        final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
        assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
    }

    @Test
    public void receivesDomainMuleContextDeploymentNotifications() throws Exception
    {
        // NOTE: need an integration test like this because DefaultMuleApplication
        // class cannot be unit tested.
        addPackedDomainFromBuilder(sharedHttpDomainFileBuilder);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, sharedHttpDomainFileBuilder.getId());
        assertMuleContextCreated(domainDeploymentListener, sharedHttpDomainFileBuilder.getId());
        assertMuleContextInitialized(domainDeploymentListener, sharedHttpDomainFileBuilder.getId());
        assertMuleContextConfigured(domainDeploymentListener, sharedHttpDomainFileBuilder.getId());
    }

    @Test
    public void undeploysStoppedDomain() throws Exception
    {
        addPackedDomainFromBuilder(emptyAppFileBuilder);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());
        final Domain domain = findADomain(emptyAppFileBuilder.getId(), 1);
        domain.stop();

        deploymentService.undeploy(domain);
    }

    @Test
    public void undeploysDomainRemovingAnchorFile() throws Exception
    {
        addPackedDomainFromBuilder(emptyAppFileBuilder);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

        assertTrue("Unable to remove anchor file", removeDomainAnchorFile(emptyAppFileBuilder.getId()));

        assertUndeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());
    }

    @Test
    public void undeploysDomainAndDomainsApps() throws Exception
    {
        doDomainUndeployAndVerifyAppsAreUndeployed(new Action()
        {
            @Override
            public void perform()
            {
                Domain domain = findADomain(dummyDomainFileBuilder.getId(), 1);
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
        addPackedAppFromBuilder(emptyAppFileBuilder);

        doDomainUndeployAndVerifyAppsAreUndeployed(createUndeployDummyDomainAction());

        assertThat(findApp(emptyAppFileBuilder.getId(), 1), notNullValue());
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
        addPackedDomainFromBuilder(emptyDomainFileBuilder);

        TestDomainFactory testDomainFactory = new TestDomainFactory(new MuleDomainClassLoaderRepository());
        testDomainFactory.setFailOnStopApplication();

        deploymentService.setDomainFactory(testDomainFactory);
        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        assertTrue("Unable to remove anchor file", removeDomainAnchorFile(emptyDomainFileBuilder.getId()));

        assertUndeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        assertAppFolderIsDeleted(emptyDomainFileBuilder.getId());
    }

    @Test
    public void undeploysDomainCompletelyEvenOnDisposingException() throws Exception
    {
        addPackedDomainFromBuilder(emptyDomainFileBuilder);

        TestDomainFactory testDomainFactory = new TestDomainFactory(new MuleDomainClassLoaderRepository());
        testDomainFactory.setFailOnDisposeApplication();
        deploymentService.setDomainFactory(testDomainFactory);
        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        assertTrue("Unable to remove anchor file", removeDomainAnchorFile(emptyDomainFileBuilder.getId()));

        assertUndeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        assertAppFolderIsDeleted(emptyDomainFileBuilder.getId());
    }

    @Test
    public void deploysIncompleteZipDomainOnStartup() throws Exception
    {
        addPackedDomainFromBuilder(incompleteDomainFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromBuilder(emptyDomainFileBuilder);

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained(incompleteDomainFileBuilder.getId());
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void deploysIncompleteZipDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromBuilder(incompleteDomainFileBuilder);

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromBuilder(emptyDomainFileBuilder);

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained(incompleteDomainFileBuilder.getId());
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void mantainsDomainFolderOnExplodedAppDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromBuilder(incompleteDomainFileBuilder);

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromBuilder(emptyAppFileBuilder);

        assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained(incompleteDomainFileBuilder.getId());
        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysZipDomainAfterDeploymentErrorOnStartup() throws Exception
    {
        addPackedDomainFromBuilder(incompleteDomainFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromBuilder(emptyAppFileBuilder);

        assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromBuilder(emptyDomainFileBuilder, incompleteDomainFileBuilder.getZipPath());
        assertDeploymentSuccess(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        assertEquals("Failed domain still appears as zombie after a successful redeploy", 0, deploymentService.getZombieDomains().size());
    }

    @Test
    public void redeploysZipDomainAfterDeploymentErrorAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromBuilder(incompleteDomainFileBuilder);

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromBuilder(dummyDomainFileBuilder);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromBuilder(emptyDomainFileBuilder, incompleteDomainFileBuilder.getZipPath());
        assertDeploymentSuccess(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        assertEquals("Failed domain still appears as zombie after a successful redeploy", 0, deploymentService.getZombieDomains().size());
    }

    @Test
    public void refreshDomainClassloaderAfterRedeployment() throws Exception
    {
        deploymentService.start();

        // Deploy domain and apps and wait until success
        addPackedDomainFromBuilder(sharedHttpDomainFileBuilder);
        addPackedAppFromBuilder(httpAAppFileBuilder);
        addPackedAppFromBuilder(httpBAppFileBuilder);
        assertDeploymentSuccess(domainDeploymentListener, sharedHttpDomainFileBuilder.getId());
        assertDeploymentSuccess(applicationDeploymentListener, httpAAppFileBuilder.getId());
        assertDeploymentSuccess(applicationDeploymentListener, httpBAppFileBuilder.getId());

        // Ensure resources are registered at domain's registry
        Domain domain = findADomain(sharedHttpDomainFileBuilder.getId(), 1);
        assertThat(domain.getMuleContext().getRegistry().get("http-listener-config"), not(is(nullValue())));

        ArtifactClassLoader initialArtifactClassLoader = domain.getArtifactClassLoader();

        reset(domainDeploymentListener);
        reset(applicationDeploymentListener);

        // Force redeployment by touching the domain's config file
        File domainFolder = new File(domainsDir.getPath(), sharedHttpDomainFileBuilder.getId());
        File configFile = new File(domainFolder, sharedHttpDomainFileBuilder.getConfigFile());
        long firstFileTimestamp = configFile.lastModified();
        FileUtils.touch(configFile);
        alterTimestampIfNeeded(configFile, firstFileTimestamp);

        assertUndeploymentSuccess(applicationDeploymentListener, httpAAppFileBuilder.getId());
        assertUndeploymentSuccess(applicationDeploymentListener, httpBAppFileBuilder.getId());
        assertUndeploymentSuccess(domainDeploymentListener, sharedHttpDomainFileBuilder.getId());

        assertDeploymentSuccess(domainDeploymentListener, sharedHttpDomainFileBuilder.getId());
        assertDeploymentSuccess(applicationDeploymentListener, httpAAppFileBuilder.getId());
        assertDeploymentSuccess(applicationDeploymentListener, httpBAppFileBuilder.getId());

        domain = findADomain(sharedHttpDomainFileBuilder.getId(), 1);
        ArtifactClassLoader artifactClassLoaderAfterRedeployment = domain.getArtifactClassLoader();

        // Ensure that after redeployment the domain's class loader has changed
        assertThat(artifactClassLoaderAfterRedeployment, not(sameInstance(initialArtifactClassLoader)));

        // Undeploy domain and apps
        removeAppAnchorFile(httpAAppFileBuilder.getId());
        removeAppAnchorFile(httpBAppFileBuilder.getId());
        removeDomainAnchorFile(sharedHttpDomainFileBuilder.getId());
        assertUndeploymentSuccess(applicationDeploymentListener, httpAAppFileBuilder.getId());
        assertUndeploymentSuccess(applicationDeploymentListener, httpBAppFileBuilder.getId());
        assertUndeploymentSuccess(domainDeploymentListener, sharedHttpDomainFileBuilder.getId());
    }

    @Test
    public void redeploysInvalidZipDomainAfterSuccessfulDeploymentOnStartup() throws Exception
    {
        addPackedDomainFromBuilder(emptyDomainFileBuilder);

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        addPackedDomainFromBuilder(incompleteDomainFileBuilder, emptyDomainFileBuilder.getZipPath());

        assertDeploymentFailure(domainDeploymentListener, emptyDomainFileBuilder.getId());

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", emptyDomainFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipDomainAfterSuccessfulDeploymentAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromBuilder(emptyDomainFileBuilder);
        assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

        addPackedDomainFromBuilder(incompleteDomainFileBuilder, emptyDomainFileBuilder.getZipPath());
        assertDeploymentFailure(domainDeploymentListener, emptyDomainFileBuilder.getId());

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", emptyDomainFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipDomainAfterFailedDeploymentOnStartup() throws Exception
    {
        addPackedDomainFromBuilder(incompleteDomainFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        reset(domainDeploymentListener);

        addPackedDomainFromBuilder(incompleteDomainFileBuilder);

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysInvalidZipDomainAfterFailedDeploymentAfterStartup() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromBuilder(incompleteDomainFileBuilder);
        assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        reset(domainDeploymentListener);

        addPackedDomainFromBuilder(incompleteDomainFileBuilder);
        assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(), new File(zombie.getKey().getFile()).getParentFile().getName());
    }

    @Test
    public void redeploysExplodedDomainAfterDeploymentError() throws Exception
    {
        deploymentService.start();

        addPackedDomainFromBuilder(incompleteDomainFileBuilder);

        assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

        // Deploys another app to confirm that DeploymentService has execute the updater thread
        addPackedDomainFromBuilder(emptyAppFileBuilder);

        assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

        // Redeploys a fixed version for incompleteDomain
        addExplodedDomainFromBuilder(emptyDomainFileBuilder, incompleteDomainFileBuilder.getId());

        assertDeploymentSuccess(domainDeploymentListener, incompleteDomainFileBuilder.getId());
        assertEquals("Failed domain still appears as zombie after a successful redeploy", 0, deploymentService.getZombieDomains().size());
    }


    @Test
    public void deployFailsWhenMissingFile() throws Exception
    {
        addExplodedAppFromBuilder(emptyAppFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        reset(applicationDeploymentListener);

        File originalConfigFile = new File(appsDir + "/" + emptyAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
        FileUtils.forceDelete(originalConfigFile);

        assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());
        assertStatus(emptyAppFileBuilder.getId(), ApplicationStatus.DEPLOYMENT_FAILED);
    }

    private Action createUndeployDummyDomainAction()
    {
        return new Action()
        {
            @Override
            public void perform()
            {
                removeDomainAnchorFile(dummyDomainFileBuilder.getId());
            }
        };
    }

    private void doDomainUndeployAndVerifyAppsAreUndeployed(Action undeployAction) throws Exception
    {
        deploymentService.start();

        addPackedDomainFromBuilder(dummyDomainFileBuilder);

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

        addPackedAppFromBuilder(dummyDomainApp1FileBuilder);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());

        addPackedAppFromBuilder(dummyDomainApp2FileBuilder);
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

        deploymentService.getLock().lock();
        try
        {
            undeployAction.perform();
        }
        finally
        {
            deploymentService.getLock().unlock();
        }

        assertUndeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
        assertUndeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
        assertUndeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    }

    @Test
    public void redeploysFixedDomainAfterBrokenExplodedDomainOnStartup() throws Exception
    {
        addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

        deploymentService.start();

        doRedeployFixedDomainAfterBrokenDomain();
    }

    @Test
    public void redeploysFixedDomainAfterBrokenExplodedDomainAfterStartup() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

        doRedeployFixedDomainAfterBrokenDomain();
    }

    @Test
    public void redeploysDomainAndItsApplications() throws Exception
    {
        addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

        addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
        addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

        reset(domainDeploymentListener);
        reset(applicationDeploymentListener);

        doRedeployDummyDomainByChangingConfigFileWithGoodOne();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
    }

    @Test
    public void doesNotRedeployDomainWithRedeploymentDisabled() throws Exception
    {
        addExplodedDomainFromBuilder(dummyUndeployableDomainFileBuilder, dummyUndeployableDomainFileBuilder.getId());
        addPackedAppFromBuilder(emptyAppFileBuilder, "empty-app.zip");

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyUndeployableDomainFileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

        reset(domainDeploymentListener);
        reset(applicationDeploymentListener);

        //change domain and app since once the app redeploys we can check the domain did not
        doRedeployDomainByChangingConfigFileWithGoodOne(dummyUndeployableDomainFileBuilder);
        doRedeployAppByChangingConfigFileWithGoodOne(emptyAppFileBuilder.getDeployedPath());

        assertDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
        verify(domainDeploymentListener, never()).onDeploymentSuccess(dummyUndeployableDomainFileBuilder.getId());
    }

    @Test
    public void redeploysDomainAndFails() throws Exception
    {
        addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

        addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
        addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());

        deploymentService.start();

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

        reset(domainDeploymentListener);
        reset(applicationDeploymentListener);

        doRedeployDummyDomainByChangingConfigFileWithBadOne();

        assertDeploymentFailure(domainDeploymentListener, dummyDomainFileBuilder.getId());

        assertNoDeploymentInvoked(applicationDeploymentListener);
    }

    @Test
    public void redeploysDomainWithOneApplicationFailedOnFirstDeployment() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

        addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
        addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());
        addExplodedAppFromBuilder(dummyDomainApp3FileBuilder, dummyDomainApp3FileBuilder.getId());

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
        assertDeploymentFailure(applicationDeploymentListener, dummyDomainApp3FileBuilder.getId());

        reset(domainDeploymentListener);
        reset(applicationDeploymentListener);

        deploymentService.getLock().lock();
        try
        {
            doRedeployDummyDomainByChangingConfigFileWithGoodOne();
            doRedeployAppByChangingConfigFileWithGoodOne(dummyDomainApp3FileBuilder.getDeployedPath());
        }
        finally
        {
            deploymentService.getLock().unlock();
        }

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
        assertDeploymentSuccess(applicationDeploymentListener, dummyDomainApp3FileBuilder.getId());
    }

    @Test
    public void redeploysDomainWithOneApplicationFailedAfterRedeployment() throws Exception
    {
        deploymentService.start();

        addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

        addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
        addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

        reset(domainDeploymentListener);
        reset(applicationDeploymentListener);

        deploymentService.getLock().lock();
        try
        {
            doRedeployDummyDomainByChangingConfigFileWithGoodOne();
            doRedeployAppByChangingConfigFileWithBadOne(dummyDomainApp2FileBuilder.getDeployedPath());
        }
        finally
        {
            deploymentService.getLock().unlock();
        }

        assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
        assertDeploymentFailure(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
    }

    @Test
    public void synchronizesDomainDeployFromClient() throws Exception
    {
        final Action action = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                deploymentService.deployDomain(dummyDomainFileBuilder.getArtifactFile().toURI().toURL());
            }
        };

        final Action assertAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                verify(domainDeploymentListener, never()).onDeploymentStart(dummyDomainFileBuilder.getId());
            }
        };
        doSynchronizedDomainDeploymentActionTest(action, assertAction);
    }

    @Test
    public void synchronizesDomainUndeployFromClient() throws Exception
    {
        final Action action = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                deploymentService.undeployDomain(emptyDomainFileBuilder.getId());
            }
        };

        final Action assertAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                verify(domainDeploymentListener, never()).onUndeploymentStart(emptyDomainFileBuilder.getId());
            }
        };
        doSynchronizedDomainDeploymentActionTest(action, assertAction);
    }

    @Test
    public void synchronizesDomainRedeployFromClient() throws Exception
    {
        final Action action = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                // Clears notification from first deployment
                reset(domainDeploymentListener);
                deploymentService.redeployDomain(emptyDomainFileBuilder.getId());
            }
        };

        final Action assertAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                verify(domainDeploymentListener, never()).onDeploymentStart(emptyDomainFileBuilder.getId());
            }
        };
        doSynchronizedDomainDeploymentActionTest(action, assertAction);
    }

    private void doSynchronizedDomainDeploymentActionTest(final Action deploymentAction, final Action assertAction) throws Exception
    {
        addPackedDomainFromBuilder(emptyDomainFileBuilder);
        final DeploymentListener domainDeploymentListener = this.domainDeploymentListener;
        final String artifactId = emptyDomainFileBuilder.getId();

        doSynchronizedArtifactDeploymentActionTest(deploymentAction, assertAction, domainDeploymentListener, artifactId);
    }

    private void doSynchronizedArtifactDeploymentActionTest(final Action deploymentAction, final Action assertAction, DeploymentListener domainDeploymentListener, String artifactId)
    {
        Thread deploymentServiceThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                deploymentService.start();
            }
        });

        final boolean[] deployedFromClient = new boolean[1];

        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {

                Thread deploymentClientThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            deploymentAction.perform();
                        }
                        catch (Exception e)
                        {
                            // Ignore
                        }
                    }
                });

                deploymentClientThread.start();
                deploymentClientThread.join();
                try
                {
                    assertAction.perform();
                }
                catch (AssertionError e)
                {
                    deployedFromClient[0] = true;
                }

                return null;
            }
        }).when(domainDeploymentListener).onDeploymentStart(artifactId);

        deploymentServiceThread.start();

        assertDeploymentSuccess(domainDeploymentListener, artifactId);

        assertFalse("Able to perform a deployment action while another deployment operation was in progress", deployedFromClient[0]);
    }

    private void doRedeployAppByChangingConfigFileWithGoodOne(String applicationPath) throws Exception
    {
        changeConfigFile(applicationPath, EMPTY_APP_CONFIG_XML);
    }

    private void doRedeployAppByChangingConfigFileWithBadOne(String applicationPath) throws Exception
    {
        changeConfigFile(applicationPath, BAD_APP_CONFIG_XML);
    }

    private void changeConfigFile(String applicationPath, String configFile) throws Exception
    {
        File originalConfigFile = new File(new File(appsDir, applicationPath), MULE_CONFIG_XML_FILE);
        assertThat("Original config file doe snot exists: " + originalConfigFile, originalConfigFile.exists(), is(true));
        URL url = getClass().getResource(configFile);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);
    }

    private void doRedeployDummyDomainByChangingConfigFileWithGoodOne() throws URISyntaxException, IOException
    {
        doRedeployDomainByChangingConfigFile("/empty-domain-config.xml", dummyDomainFileBuilder);
    }

    private void doRedeployDomainByChangingConfigFileWithGoodOne(DomainFileBuilder domain) throws URISyntaxException, IOException
    {
        doRedeployDomainByChangingConfigFile("/empty-domain-config.xml", domain);
    }

    private void doRedeployDummyDomainByChangingConfigFileWithBadOne() throws URISyntaxException, IOException
    {
        doRedeployDomainByChangingConfigFile("/bad-domain-config.xml", dummyDomainFileBuilder);
    }

    private void doRedeployDomainByChangingConfigFile(String configFile, DomainFileBuilder domain) throws URISyntaxException, IOException
    {
        File originalConfigFile = new File(new File(domainsDir, domain.getDeployedPath()), domain.getConfigFile());
        assertThat("Cannot find domain config file: " + originalConfigFile, originalConfigFile.exists(), is(true));
        URL url = getClass().getResource(configFile);
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);
    }

    private void doRedeployFixedDomainAfterBrokenDomain() throws Exception
    {
        assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

        reset(domainDeploymentListener);

        File originalConfigFile = new File(domainsDir + "/incompleteDomain", DOMAIN_CONFIG_FILE_LOCATION);
        URL url = getClass().getResource("/empty-domain-config.xml");
        File newConfigFile = new File(url.toURI());
        FileUtils.copyFile(newConfigFile, originalConfigFile);
        assertDeploymentSuccess(domainDeploymentListener, "incompleteDomain");

        addPackedDomainFromBuilder(emptyAppFileBuilder);
        assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

        // Check that the failed application folder is still there
        assertDomainFolderIsMaintained("incompleteDomain");
    }

    /**
     * After a successful deploy using the {@link DeploymentServiceTestCase#domainDeploymentListener},
     * this method deploys a domain zip with the same name and a wrong configuration.
     * Applications dependant of the domain should not be deleted after this failure full redeploy.
     */
    private void doRedeployBrokenDomainAfterFixedDomain() throws Exception
    {
        assertDeploymentSuccess(domainDeploymentListener, dummyDomainBundleFileBuilder.getId());
        assertApplicationAnchorFileExists(emptyAppFileBuilder.getId());

        reset(domainDeploymentListener);

        DomainFileBuilder dummyDomainBundleFileBuilderFullDeploy = new DomainFileBuilder("dummy-domain-bundle").containing(emptyAppFileBuilder);

        dummyDomainBundleFileBuilderFullDeploy.definedBy("incomplete-domain-config.xml");
        addPackedDomainFromBuilder(dummyDomainBundleFileBuilderFullDeploy);

        assertDeploymentFailure(domainDeploymentListener, dummyDomainBundleFileBuilderFullDeploy.getId());

        undeployLatch.await();

        assertApplicationAnchorFileExists(emptyAppFileBuilder.getId());
        Application dependantApplication = deploymentService.getApplications().get(0);
        assertThat(dependantApplication.getMuleContext(), is(nullValue()));
    }

    public void doBrokenAppArchiveTest() throws Exception
    {
        addPackedAppFromBuilder(brokenAppFileBuilder);

        deploymentService.start();

        assertDeploymentFailure(applicationDeploymentListener, brokenAppFileBuilder.getId());
        reset(applicationDeploymentListener);

        // let the file system's write-behind cache commit the delete operation?
        Thread.sleep(FILE_TIMESTAMP_PRECISION_MILLIS);

        // zip stays intact, no app dir created
        assertAppsDir(new String[] {brokenAppFileBuilder.getDeployedPath()}, NONE, true);
        // don't assert dir contents, we want to check internal deployer state next
        assertAppsDir(NONE, new String[] {brokenAppFileBuilder.getId()}, false);
        assertEquals("No apps should have been registered with Mule.", 0, deploymentService.getApplications().size());
        final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
        assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
        final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
        assertEquals("Wrong URL tagged as zombie.", brokenAppFileBuilder.getDeployedPath(), new File(zombie.getKey().getFile()).getName());
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

    private void deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(Action deployArtifactAction) throws Exception
    {
        Action verifyAnchorFileDoesNotExistsAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                assertApplicationAnchorFileDoesNotExists(waitAppFileBuilder.getId());
            }
        };
        Action verifyDeploymentSuccessfulAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                assertApplicationDeploymentSuccess(applicationDeploymentListener, waitAppFileBuilder.getId());
            }
        };
        Action verifyAnchorFileExistsAction = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                assertApplicationAnchorFileExists(waitAppFileBuilder.getId());
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
                assertDomainAnchorFileDoesNotExists(waitDomainFileBuilder.getId());
            }
        };
        Action verifyDeploymentSuccessful = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                assertDeploymentSuccess(domainDeploymentListener, waitDomainFileBuilder.getId());
            }
        };
        Action verifyAnchorFileExists = new Action()
        {
            @Override
            public void perform() throws Exception
            {
                assertDomainAnchorFileExists(waitDomainFileBuilder.getId());
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
        prober.check(new JUnitProbe()
        {
            @Override
            protected boolean test() throws Exception
            {
                verify(listener, times(1)).onDeploymentSuccess(artifactName);
                return true;
            }

            @Override
            public String describeFailure()
            {
                return "Failed to deploy application: " + artifactName + System.lineSeparator() + super.describeFailure();
            }
        });
    }

    private void assertMuleContextCreated(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new JUnitProbe()
        {
            @Override
            public boolean test()
            {
                verify(listener, times(1)).onMuleContextCreated(eq(appName), any(MuleContext.class));
                return true;
            }

            @Override
            public String describeFailure()
            {
                return String.format("Did not received notification '%s' for app '%s'", "onMuleContextCreated", appName) + System.lineSeparator() + super.describeFailure();
            }
        });
    }

    private void assertMuleContextInitialized(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new JUnitProbe()
        {
            @Override
            public boolean test()
            {
                verify(listener, times(1)).onMuleContextInitialised(eq(appName), any(MuleContext.class));
                return true;
            }

            @Override
            public String describeFailure()
            {
                return String.format("Did not received notification '%s' for app '%s'", "onMuleContextInitialised", appName) + System.lineSeparator() + super.describeFailure();
            }
        });
    }

    private void assertMuleContextConfigured(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new JUnitProbe()
        {
            @Override
            public boolean test()
            {
                verify(listener, times(1)).onMuleContextConfigured(eq(appName), any(MuleContext.class));
                return true;
            }

            @Override
            public String describeFailure()
            {
                return String.format("Did not received notification '%s' for app '%s'", "onMuleContextConfigured", appName) + System.lineSeparator() + super.describeFailure();
            }
        });
    }

    private void assertUndeploymentSuccess(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new JUnitProbe()
        {
            @Override
            public boolean test()
            {
                verify(listener, times(1)).onUndeploymentSuccess(appName);
                return true;
            }

            @Override
            public String describeFailure()
            {
                return "Failed to undeploy artifact: " + appName + System.lineSeparator() + super.describeFailure();
            }
        });
    }

    private void assertAtLeastOneUndeploymentSuccess(final DeploymentListener listener, final String appName)
    {
        Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
        prober.check(new JUnitProbe()
        {
            @Override
            public boolean test()
            {
                verify(listener, atLeastOnce()).onUndeploymentSuccess(appName);
                return true;
            }

            @Override
            public String describeFailure()
            {
                return "Failed to undeploy artifact: " + appName + System.lineSeparator() + super.describeFailure();
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
        prober.check(new JUnitProbe()
        {

            @Override
            public boolean test()
            {
                verify(listener, mode).onDeploymentFailure(eq(artifactName), any(Throwable.class));
                return true;
            }

            @Override
            public String describeFailure()
            {
                return "Application deployment was supposed to fail for: " + artifactName + super.describeFailure();
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
                @Override
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

                @Override
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

    private void addPackedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception
    {
        addPackedAppFromBuilder(artifactFileBuilder, null);
    }

    private void addPackedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder, String targetName) throws Exception
    {
        addPackedAppArchive(artifactFileBuilder, targetName);
    }

    private void addPackedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception
    {
        addPackedDomainFromBuilder(artifactFileBuilder, null);
    }

    private void addPackedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder, String targetName) throws Exception
    {
        addArchive(domainsDir, artifactFileBuilder.getArtifactFile().toURI().toURL(), targetName);
    }

    /**
     * Copies a given app archive with a given target name to the apps folder for deployment
     */
    private void addPackedAppArchive(TestArtifactDescriptor artifactFileBuilder, String targetFile) throws Exception
    {
        addArchive(appsDir, artifactFileBuilder.getArtifactFile().toURI().toURL(), targetFile);
    }

    private void addArchive(File outputDir, URL url, String targetFile) throws Exception
    {
        ReentrantLock lock = deploymentService.getLock();

        lock.lock();
        try
        {
            // copy is not atomic, copy to a temp file and rename instead (rename is atomic)
            final String tempFileName = new File((targetFile == null ? url.getFile() : targetFile) + ".part").getName();
            final File tempFile = new File(outputDir, tempFileName);
            FileUtils.copyURLToFile(url, tempFile);
            final File destFile = new File(StringUtils.removeEnd(tempFile.getAbsolutePath(), ".part"));
            tempFile.renameTo(destFile);
            assertThat("File does not exists: " + destFile.getAbsolutePath(), destFile.exists(), is(true));
        }
        finally
        {
            lock.unlock();
        }
    }

    private void addExplodedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception
    {
        addExplodedAppFromBuilder(artifactFileBuilder, null);
    }

    private void addExplodedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder, String appName) throws Exception
    {
        addExplodedArtifactFromBuilder(artifactFileBuilder, appName, MULE_CONFIG_XML_FILE, appsDir);
    }

    private void addExplodedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception
    {
        addExplodedDomainFromBuilder(artifactFileBuilder, null);
    }

    private void addExplodedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder, String appName) throws Exception
    {
        addExplodedArtifactFromBuilder(artifactFileBuilder, appName, DOMAIN_CONFIG_FILE_LOCATION, domainsDir);
    }

    private void addExplodedArtifactFromBuilder(TestArtifactDescriptor artifactFileBuilder, String artifactName, String configFileName, File destinationDir) throws Exception
    {
        addExplodedArtifactFromUrl(artifactFileBuilder.getArtifactFile().toURI().toURL(), artifactName, configFileName, destinationDir);
    }

    private void addExplodedArtifactFromUrl(URL resource, String artifactName, String configFileName, File destinationDir) throws Exception, URISyntaxException
    {
        assertNotNull("Resource URL cannot be null", resource);

        String artifactFolder = artifactName;
        if (artifactFolder == null)
        {
            File file = new File(resource.getFile());
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

        addExplodedArtifact(resource, artifactFolder, configFileName, destinationDir);
    }

    /**
     * Copies a given app archive with a given target name to the apps folder for deployment
     */
    private void addExplodedArtifact(URL url, String artifactName, String configFileName, File destinationDir) throws Exception, URISyntaxException
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
                configFile.setLastModified(System.currentTimeMillis() + FILE_TIMESTAMP_PRECISION_MILLIS);
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
        return deleteFile(anchorFile);
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
        return deleteFile(anchorFile);
    }

    private void assertApplicationAnchorFileExists(String applicationName)
    {
        assertThat(getArtifactAnchorFile(applicationName, appsDir).exists(), is(true));
    }

    private void assertApplicationTmpFileExists(String applicationName)
    {
        assertThat(getApplicationTmpFile(applicationName).exists(), is(true));
    }

    private void assertApplicationTmpFileDoesNotExists(String applicationName)
    {
        assertThat(getApplicationTmpFile(applicationName).exists(), is(false));
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

    private File getApplicationTmpFile(String applicationName)
    {
        return MuleFoldersUtil.getAppTempFolder(applicationName);
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

    private void assertApplicationFiles(String appName, String[] expectedFiles) {
        assertArtifactConfigs(new File(appsDir, appName), expectedFiles);
    }

    private void assertArtifactConfigs(File artifactDir, String[] expectedFiles) {
        final String[] artifactFiles = artifactDir.list(FILE);

        assertThat(expectedFiles, arrayContainingInAnyOrder(artifactFiles));
    }

    /**
     * Allows to execute custom actions before or after executing logic or checking preconditions / verifications.
     */
    private interface Action
    {
        void perform() throws Exception;
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

    private void deployAfterStartUp(ApplicationFileBuilder applicationFileBuilder) throws Exception
    {
        addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

        deploymentService.start();

        assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
        assertAppsDir(NONE, new String[] {applicationFileBuilder.getId()}, true);
        assertApplicationAnchorFileExists(applicationFileBuilder.getId());

        // just assert no privileged entries were put in the registry
        final Application app = findApp(applicationFileBuilder.getId(), 1);
        final MuleRegistry registry = getMuleRegistry(app);

        // mule-app.properties from the zip archive must have loaded properly
        assertThat("mule-app.properties should have been loaded.", (String) registry.get("myCustomProp"), equalTo("someValue"));
    }

    private void resetUndeployLatch ()
    {
        undeployLatch = new Latch();
    }

    private static class TestMuleDeploymentService extends MuleDeploymentService
    {
        TestMuleDeploymentService(PluginClassLoaderManager pluginClassLoaderManager)
        {
            super(pluginClassLoaderManager);
        }

        @Override
        protected DomainArchiveDeployer createDomainArchiveDeployer(DomainFactory domainFactory, ObservableList<Domain> domains, DefaultArchiveDeployer<Application> applicationDeployer)
        {
            return new TestDomainArchiveDeployer(
                    new DefaultArchiveDeployer<>(new DefaultArtifactDeployer<Domain>(), domainFactory, domains,
                                                 new DomainDeploymentTemplate(applicationDeployer, this)),
                    applicationDeployer, this);
        }
    }

    private static class TestDomainArchiveDeployer extends DomainArchiveDeployer
    {
        TestDomainArchiveDeployer(ArchiveDeployer<Domain> domainDeployer, ArchiveDeployer<Application> applicationDeployer, DeploymentService deploymentService)
        {
            super(domainDeployer, applicationDeployer, deploymentService);
        }

        @Override
        public void undeployArtifact(String artifactId)
        {
            super.undeployArtifact(artifactId);
            undeployLatch.countDown();
        }
    }
    
    @Test
    public void shutdownListenerInvoked() throws Exception
    {
        deploymentService.start();
        TestShutdownListener shutdownListener = new TestShutdownListener();
        deploymentService.addShutdownListener(shutdownListener);
        deploymentService.stop();
        assertThat(shutdownListener.isInvoked(), is(true));
    }
    
    public static class TestShutdownListener implements ShutdownListener
    {
        boolean invoked = false;
        
        @Override
        public void execute()
        {
            invoked = true;
        }
        
        public boolean isInvoked()
        {
            return invoked;
        }
    }
}
