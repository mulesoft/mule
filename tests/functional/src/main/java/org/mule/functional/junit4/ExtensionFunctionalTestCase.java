/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.util.IOUtils.getResourceAsUrl;
import static org.springframework.util.ReflectionUtils.findMethod;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.util.ArrayUtils;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * Base test class for {@link FunctionalTestCase}s
 * that make use of components generated through the extensions API.
 * <p/>
 * The value added by this class in comparison to a traditional
 * {@link FunctionalTestCase} is that before creating
 * the {@link MuleContext}, it creates a {@link ExtensionManager}
 * and automatically registers extensions pointed by the {@link #getDescribers()}
 * or {@link #getAnnotatedExtensionClasses()} methods.
 * <p/>
 * Once extensions are registered, a {@link ResourcesGenerator} is used to automatically
 * generate any backing resources needed (XSD schemas, spring bundles, etc).
 * <p/>
 * In this way, the user experience is greatly simplified when running the test
 * either through an IDE or build tool such as maven or gradle.
 * <p/>
 * Since this class extends {@link FunctionalTestCase}, a new {@link MuleContext}
 * is created per each test. That also means that a new {@link ExtensionManager}
 * is created per test.
 *
 * @since 3.7.0
 */
public abstract class ExtensionFunctionalTestCase extends FunctionalTestCase
{

    private ExtensionManagerAdapter extensionManager;

    /**
     * Implement this method to limit the amount of extensions
     * initialised by providing the {@link Describer}s for
     * the extensions that you actually want to use for this test.
     * Returning a {@code null} or empty array will cause the
     * {@link #getAnnotatedExtensionClasses()} method to be considered.
     * Default implementation of this method returns {@code null}
     */
    protected Describer[] getDescribers()
    {
        return null;
    }

    /**
     * Implement this method to limit the amount of extensions
     * initialised by providing the annotated classes which define
     * the extensions that you actually want to use for this test.
     * Returning a {@code null} or empty array forces the
     * {@link ExtensionManager} to perform a full classpath discovery.
     * Default implementation of this method returns {@code null}.
     * This method will only be considered if {@link #getDescribers()}
     * returns {@code null}
     */
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return null;
    }

    /**
     * Adds a {@link ConfigurationBuilder} that sets the {@link #extensionManager}
     * into the {@link #muleContext}. This {@link ConfigurationBuilder} is set
     * as the first element of the {@code builders} {@link List}
     *
     * @param builders the list of {@link ConfigurationBuilder}s that will be used to initialise the {@link #muleContext}
     */
    @Override
    protected final void addBuilders(List<ConfigurationBuilder> builders)
    {
        super.addBuilders(builders);
        builders.add(0, new AbstractConfigurationBuilder()
        {
            @Override
            protected void doConfigure(MuleContext muleContext) throws Exception
            {
                createExtensionsManager(muleContext);
            }
        });
    }

    private void createExtensionsManager(MuleContext muleContext) throws Exception
    {
        extensionManager = new DefaultExtensionManager();
        File generatedResourcesDirectory = getGenerationTargetDirectory();

        ((DefaultMuleContext) muleContext).setExtensionManager(extensionManager);
        initialiseIfNeeded(extensionManager, muleContext);

        ExtensionsTestInfrastructureDiscoverer extensionsTestInfrastructureDiscoverer = new ExtensionsTestInfrastructureDiscoverer(extensionManager, generatedResourcesDirectory);
        List<GeneratedResource> generatedResources = extensionsTestInfrastructureDiscoverer.discoverExtensions(getDescribers(), getAnnotatedExtensionClasses());
        generateResourcesAndAddToClasspath(generatedResourcesDirectory, generatedResources);
    }

    private void generateResourcesAndAddToClasspath(File generatedResourcesDirectory, List<GeneratedResource> resources) throws Exception
    {
        ClassLoader cl = getClass().getClassLoader();
        Method method = findMethod(cl.getClass(), "addURL", URL.class);
        method.setAccessible(true);

        for (GeneratedResource resource : resources)
        {
            URL generatedResourceURL = new File(generatedResourcesDirectory, resource.getPath()).toURI().toURL();
            method.invoke(cl, generatedResourceURL);
        }
    }

    private File getGenerationTargetDirectory()
    {
        URL url = getResourceAsUrl(getEffectiveConfigFile(), getClass(), true, true);
        File targetDirectory = new File(FileUtils.toFile(url).getParentFile(), "META-INF");

        if (!targetDirectory.exists() && !targetDirectory.mkdir())
        {
            throw new RuntimeException("Could not create target directory " + targetDirectory.getAbsolutePath());
        }

        return targetDirectory;
    }

    private String getEffectiveConfigFile()
    {
        String configFile = getConfigFile();
        if (configFile != null)
        {
            return configFile;
        }

        configFile = getConfigFileFromSplittable(getConfigurationResources());
        if (configFile != null)
        {
            return configFile;
        }

        configFile = getConfigFileFromSplittable(getConfigResources());
        if (configFile != null)
        {
            return configFile;
        }

        String[] configFiles = getConfigFiles();
        if (!ArrayUtils.isEmpty(configFiles))
        {
            return configFiles[0].trim();
        }

        throw new IllegalArgumentException("No valid config file was specified");
    }

    private String getConfigFileFromSplittable(String configFile)
    {
        if (configFile != null)
        {
            return configFile.split(",")[0].trim();
        }

        return null;
    }

}
