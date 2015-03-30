/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import org.mule.DefaultMuleContext;
import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.registry.SPIServiceRegistry;
import org.mule.api.registry.ServiceRegistry;
import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Describer;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.ExtensionFactory;
import org.mule.extension.resources.GenerableResource;
import org.mule.extension.resources.ResourcesGenerator;
import org.mule.extension.resources.spi.GenerableResourceContributor;
import org.mule.module.extension.internal.introspection.AnnotationsBasedDescriber;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.module.extension.internal.resources.AbstractResourcesGenerator;
import org.mule.util.ArrayUtils;
import org.mule.util.IOUtils;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
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
 * and automatically discovers extensions by delegating on
 * {@link ExtensionManager#discoverExtensions(ClassLoader)}.
 * <p/>
 * By default, standard extension discovery will be
 * performed by invoking {@link ExtensionManager#discoverExtensions(ClassLoader)}.
 * Although this behavior suits most use cases, it can be time consuming because of
 * all the classpath scanning and the overhead of initialising extensions that
 * are most likely not used in this tests. As the number of extensions available grows,
 * the problem gets worst. For those cases,  you can override the {@link #getDescribers()}
 * and specify which describers are to be used to initialise the extensions manager. In that way,
 * extensions discovery is skipped and you only initialise what you need.
 * <p/>
 * Once extensions are discovered and described,
 * a {@link ResourcesGenerator} is used to automatically
 * generate any backing resources needed (for example, XSD schemas, spring bundles,
 * service registration files, etc).
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
public abstract class ExtensionsFunctionalTestCase extends FunctionalTestCase
{

    private final ServiceRegistry serviceRegistry = new SPIServiceRegistry();
    private final ExtensionFactory extensionFactory = new DefaultExtensionFactory(serviceRegistry);
    private ExtensionManager extensionManager;
    private File generatedResourcesDirectory;


    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        super.doSetUpBeforeMuleContextCreation();
        createExtensionsManager();
    }

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

    @Override
    protected final void addBuilders(List<ConfigurationBuilder> builders)
    {
        super.addBuilders(builders);
        builders.add(0, new AbstractConfigurationBuilder()
        {
            @Override
            protected void doConfigure(MuleContext muleContext) throws Exception
            {
                ((DefaultMuleContext) muleContext).setExtensionManager(extensionManager);
            }
        });
    }

    private List<GenerableResourceContributor> getGenerableResourceContributors()
    {
        return ImmutableList.copyOf(serviceRegistry.lookupProviders(GenerableResourceContributor.class));
    }

    private void createExtensionsManager() throws Exception
    {
        extensionManager = new DefaultExtensionManager();

        Describer[] describers = getDescribers();
        if (ArrayUtils.isEmpty(describers))
        {
            Class<?>[] annotatedClasses = getAnnotatedExtensionClasses();
            if (!ArrayUtils.isEmpty(annotatedClasses))
            {
                describers = new Describer[annotatedClasses.length];
                int i = 0;
                for (Class<?> annotatedClass : annotatedClasses)
                {
                    describers[i++] = new AnnotationsBasedDescriber(annotatedClass);
                }
            }
        }

        if (ArrayUtils.isEmpty(describers))
        {
            extensionManager.discoverExtensions(getClass().getClassLoader());
        }
        else
        {
            loadExtensionsFromDescribers(extensionManager, describers);
        }

        generatedResourcesDirectory = getGenerationTargetDirectory();

        ResourcesGenerator generator = new ExtensionsTestInfrastructureResourcesGenerator(serviceRegistry, generatedResourcesDirectory);

        List<GenerableResourceContributor> resourceContributors = getGenerableResourceContributors();
        for (Extension extension : extensionManager.getExtensions())
        {
            for (GenerableResourceContributor contributor : resourceContributors)
            {
                contributor.contribute(extension, generator);
            }
        }

        generateResourcesAndAddToClasspath(generator);
    }

    private void loadExtensionsFromDescribers(ExtensionManager extensionManager, Describer[] describers)
    {
        for (Describer describer : describers)
        {
            extensionManager.registerExtension(extensionFactory.createFrom(describer.describe()));
        }
    }

    private void generateResourcesAndAddToClasspath(ResourcesGenerator generator) throws Exception
    {
        ClassLoader cl = getClass().getClassLoader();
        Method method = org.springframework.util.ReflectionUtils.findMethod(cl.getClass(), "addURL", URL.class);
        method.setAccessible(true);

        for (GenerableResource resource : generator.dumpAll())
        {
            URL generatedResourceURL = new File(generatedResourcesDirectory, resource.getFilePath()).toURI().toURL();
            method.invoke(cl, generatedResourceURL);
        }
    }

    private File getGenerationTargetDirectory()
    {
        URL url = IOUtils.getResourceAsUrl(getEffectiveConfigFile(), getClass(), true, true);
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


    private class ExtensionsTestInfrastructureResourcesGenerator extends AbstractResourcesGenerator
    {

        private File targetDirectory;

        private ExtensionsTestInfrastructureResourcesGenerator(ServiceRegistry serviceRegistry, File targetDirectory)
        {
            super(serviceRegistry);
            this.targetDirectory = targetDirectory;
        }

        @Override
        protected void write(GenerableResource resource)
        {
            File targetFile = new File(targetDirectory, resource.getFilePath());
            try
            {
                FileUtils.write(targetFile, resource.getContentBuilder().toString());
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
