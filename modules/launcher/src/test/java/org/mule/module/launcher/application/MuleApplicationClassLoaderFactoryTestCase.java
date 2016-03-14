/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.application;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.launcher.MuleFoldersUtil.getAppClassesFolder;
import static org.mule.module.launcher.MuleFoldersUtil.getAppLibFolder;
import org.mule.api.config.MuleProperties;
import org.mule.module.artifact.classloader.CompositeClassLoader;
import org.mule.module.artifact.classloader.GoodCitizenClassLoader;
import org.mule.module.launcher.MuleApplicationClassLoader;
import org.mule.module.launcher.MuleFoldersUtil;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.domain.DomainClassLoaderRepository;
import org.mule.module.launcher.nativelib.NativeLibraryFinderFactory;
import org.mule.module.launcher.plugin.ApplicationPluginDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MuleApplicationClassLoaderFactoryTestCase extends AbstractMuleTestCase
{

    private static final String RESOURCE_IN_CLASSES_AND_JAR = "test-resource-1.txt";
    private static final String RESOURCE_JUST_IN_CLASSES = "test-resource-3.txt";
    private static final String RESOURCE_JUST_IN_DOMAIN = "test-resource-4.txt";

    private static final String DOMAIN_NAME = "test-domain";
    private static final String APP_NAME = "test-app";

    @Rule
    public TemporaryFolder tempMuleHome = new TemporaryFolder();

    private String previousMuleHome;

    private MuleSharedDomainClassLoader domainCL;

    private File jarFile;

    @Before
    public void createAppClassLoader() throws IOException
    {
        // Create directories structure
        previousMuleHome = System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, tempMuleHome.getRoot().getAbsolutePath());

        List<URL> urls = new LinkedList<>();

        File classesDir = getAppClassesFolder(APP_NAME);
        Assert.assertThat(classesDir.mkdirs(), is(true));
        // Add isolated resources in classes dir
        FileUtils.stringToFile(new File(classesDir, RESOURCE_IN_CLASSES_AND_JAR).getAbsolutePath(), "Some text");
        FileUtils.stringToFile(new File(classesDir, RESOURCE_JUST_IN_CLASSES).getAbsolutePath(), "Some text");
        urls.add(classesDir.toURI().toURL());

        // Add jar file with resources in lib dir
        File libDir = getAppLibFolder(APP_NAME);
        Assert.assertThat(libDir.mkdirs(), is(true));
        URL resourceSrcJarFile = Thread.currentThread().getContextClassLoader().getResource("test-jar-with-resources.jar");
        assertNotNull(resourceSrcJarFile);
        File srcJarFile = new File(resourceSrcJarFile.getFile());
        jarFile = new File(libDir, "test-jar-with-resources.jar");
        FileUtils.copyFile(srcJarFile, jarFile, false);
        urls.add(jarFile.toURI().toURL());

        // Add isolated resources in domain dir
        File domainDir = MuleFoldersUtil.getDomainFolder(DOMAIN_NAME);
        Assert.assertThat(domainDir.mkdirs(), is(true));
        FileUtils.stringToFile(new File(domainDir, RESOURCE_JUST_IN_DOMAIN).getAbsolutePath(), "Some text");

        // Create app class loader
        domainCL = new MuleSharedDomainClassLoader(DOMAIN_NAME, Thread.currentThread().getContextClassLoader());
    }

    @After
    public void cleanUp()
    {
        if (previousMuleHome != null)
        {
            System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, previousMuleHome);
        }
        FileUtils.deleteTree(tempMuleHome.getRoot());
    }

    @Test
    public void createClassLoaderWithNoPlugins() throws Exception
    {
        final DomainClassLoaderRepository domainClassLoaderRepository = mock(DomainClassLoaderRepository.class);
        when(domainClassLoaderRepository.getDomainClassLoader(DOMAIN_NAME)).thenReturn(domainCL);
        final NativeLibraryFinderFactory nativeLibraryFinderFactory = mock(NativeLibraryFinderFactory.class);
        MuleApplicationClassLoaderFactory classLoaderFactory = new MuleApplicationClassLoaderFactory(domainClassLoaderRepository, nativeLibraryFinderFactory);

        final ApplicationDescriptor descriptor = new ApplicationDescriptor();
        descriptor.setName(APP_NAME);
        descriptor.setDomain(DOMAIN_NAME);

        final MuleApplicationClassLoader artifactClassLoader = (MuleApplicationClassLoader) classLoaderFactory.create(descriptor);
        assertThat(artifactClassLoader.getURLs(), is(equalTo(artifactClassLoader.getURLs())));
        assertThat(artifactClassLoader.getParent(), is(domainCL));
    }

    @Test
    public void createClassLoaderWithPlugins() throws Exception
    {
        final DomainClassLoaderRepository domainClassLoaderRepository = mock(DomainClassLoaderRepository.class);
        when(domainClassLoaderRepository.getDomainClassLoader(DOMAIN_NAME)).thenReturn(domainCL);
        final NativeLibraryFinderFactory nativeLibraryFinderFactory = mock(NativeLibraryFinderFactory.class);
        MuleApplicationClassLoaderFactory classLoaderFactory = new MuleApplicationClassLoaderFactory(domainClassLoaderRepository, nativeLibraryFinderFactory);

        final ApplicationDescriptor descriptor = new ApplicationDescriptor();
        descriptor.setName(APP_NAME);
        descriptor.setDomain(DOMAIN_NAME);
        final Set<ApplicationPluginDescriptor> plugins = new HashSet<>();
        final ApplicationPluginDescriptor pluginDescriptor = new ApplicationPluginDescriptor();
        pluginDescriptor.setName("plugin1");
        plugins.add(pluginDescriptor);
        descriptor.setPlugins(plugins);

        final MuleApplicationClassLoader artifactClassLoader = (MuleApplicationClassLoader) classLoaderFactory.create(descriptor);
        assertThat(artifactClassLoader.getURLs(), is(equalTo(artifactClassLoader.getURLs())));
        assertThat(artifactClassLoader.getParent(), is(instanceOf(CompositeClassLoader.class)));
        assertThat(artifactClassLoader.getParent().getParent(), is(domainCL));
    }

    @Test
    public void createClassLoaderWithPluginsAndSharedPluginLibs() throws Exception
    {
        final DomainClassLoaderRepository domainClassLoaderRepository = mock(DomainClassLoaderRepository.class);
        when(domainClassLoaderRepository.getDomainClassLoader(DOMAIN_NAME)).thenReturn(domainCL);
        final NativeLibraryFinderFactory nativeLibraryFinderFactory = mock(NativeLibraryFinderFactory.class);
        MuleApplicationClassLoaderFactory classLoaderFactory = new MuleApplicationClassLoaderFactory(domainClassLoaderRepository, nativeLibraryFinderFactory);

        final ApplicationDescriptor descriptor = new ApplicationDescriptor();
        descriptor.setName(APP_NAME);
        descriptor.setDomain(DOMAIN_NAME);
        final Set<ApplicationPluginDescriptor> plugins = new HashSet<>();
        final ApplicationPluginDescriptor pluginDescriptor = new ApplicationPluginDescriptor();
        pluginDescriptor.setName("plugin1");
        plugins.add(pluginDescriptor);
        descriptor.setPlugins(plugins);
        descriptor.setSharedPluginLibs(new URL[] {jarFile.toURI().toURL()});

        final MuleApplicationClassLoader artifactClassLoader = (MuleApplicationClassLoader) classLoaderFactory.create(descriptor);
        assertThat(artifactClassLoader.getURLs(), is(equalTo(artifactClassLoader.getURLs())));
        assertThat(artifactClassLoader.getParent(), is(instanceOf(CompositeClassLoader.class)));
        assertThat(artifactClassLoader.getParent().getParent(), is(instanceOf(GoodCitizenClassLoader.class)));
        assertThat(artifactClassLoader.getParent().getParent().getParent(), is(domainCL));
    }
}