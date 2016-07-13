/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.runners;

import static org.mule.functional.util.AnnotationUtils.getAnnotationAttributeFrom;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import org.mule.functional.classloading.isolation.classification.ArtifactUrlClassification;
import org.mule.functional.classloading.isolation.classification.ClassLoaderTestRunner;
import org.mule.functional.classloading.isolation.classification.ClassPathClassifier;
import org.mule.functional.classloading.isolation.classification.ClassPathClassifierContext;
import org.mule.functional.classloading.isolation.classification.MuleClassPathClassifier;
import org.mule.functional.classloading.isolation.classloader.MuleClassLoaderRunnerFactory;
import org.mule.functional.classloading.isolation.classpath.ClassPathUrlProvider;
import org.mule.functional.classloading.isolation.maven.DependencyGraphMavenDependenciesResolver;
import org.mule.functional.classloading.isolation.maven.MavenDependenciesResolver;
import org.mule.functional.classloading.isolation.maven.MavenMultiModuleArtifactMapping;
import org.mule.functional.classloading.isolation.maven.MuleMavenMultiModuleArtifactMapping;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;

import org.junit.internal.builders.AnnotatedBuilder;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.TestClass;

/**
 * A {@link org.junit.runner.Runner} that mimics the class loading model used in a standalone container.
 * In order to detect early issues related to isolation when building plugins these runner allow you to
 * run your functional test cases using an isolated class loader.
 * <p/>
 * {@link org.mule.functional.junit4.ArtifactFunctionalTestCase} should be extended in order to use this
 * runner, it has already annotated the runner and also has the logic to configure extension into {@link org.mule.runtime.core.api.MuleContext}.
 * <p/>
 * See {@link RunnerDelegateTo} for those scenarios where another JUnit runner needs to be used but still the test
 * has to be executed within an isolated class loading model.
 * {@link ArtifactClassLoaderRunnerConfig} allows to define the Extensions to be discovered in the classpath, for each
 * Extension a plugin class loader would be created.
 * {@link PluginClassLoadersAware} allows the test to be injected with the list of {@link ClassLoader}s that were created
 * for each plugin, mostly used in {@link org.mule.functional.junit4.ArtifactFunctionalTestCase} in order to register the extensions.
 * <p/>
 * The class loading model is built by doing a classification of the ClassPath URLs loaded by IDEs and surfire-maven-plugin.
 * The classification bases its logic by reading the dependency tree graph generated with depgraph-maven-plugin.
 * It goes over the tree to select the dependencies and getting the URLs from the Launcher class loader to create the
 * {@link ArtifactClassLoader}s and filters for each one of them.
 * <p/>
 * See {@link ClassPathClassifier} for details about the classification logic. Just for understanding the simple way to
 * describe the classification is by saying that all the provided dependencies (including its transitives) will go to the container
 * class loader, for each extension defined it will create a plugin class loader including its compile dependencies (including transitives)
 * and the rest of the test dependencies (including transitives) will go to the application class loader.
 * If the current artifact being tested is not an extension it will handle it as a plugin, therefore a plugin class loader would
 * be created with its target/classes plus compile dependencies (including transitives) and the mule-module.properties take into account for
 * defining the filter to be applied to the class loader.
 * <p/>
 * Only one instance of the {@link ClassLoader} is created and used for running all the tests classes that are marked to run with this {@link Runner}
 * due to creating the {@link ClassLoader} requires time and has impact when running tests.
 * <p/>
 * A best practice is to a base abstract class for your module tests that extends {@link org.mule.functional.junit4.ArtifactFunctionalTestCase}
 * and defines if needed anything related to the configuration with this annotation that will be applied to all the tests that
 * are being executed for the same VM.
 *
 * @since 4.0
 */
public class ArtifactClassLoaderRunner extends Runner implements Filterable
{
    private final Runner delegate;
    private static ClassLoaderTestRunner classLoaderTestRunner;
    private static boolean pluginClassLoadersInjected = false;

    /**
     * Creates a Runner to run {@code klass}
     *
     * @param clazz
     * @param builder
     * @throws Throwable if there was an error while initializing the runner.
     */
    public ArtifactClassLoaderRunner(Class<?> clazz, RunnerBuilder builder) throws Throwable
    {
        if (classLoaderTestRunner == null)
        {
            classLoaderTestRunner = createClassLoaderTestRunner(clazz);
        }

        final Class<?> isolatedTestClass = getTestClass(clazz);

        final Class<? extends Annotation> runnerDelegateToClass = (Class<? extends Annotation>) classLoaderTestRunner.loadClassWithApplicationClassLoader(RunnerDelegateTo.class.getName());

        final AnnotatedBuilder annotatedBuilder = new AnnotatedBuilder(builder);
        delegate = annotatedBuilder.buildRunner(getAnnotationAttributeFrom(isolatedTestClass, runnerDelegateToClass, "value"), isolatedTestClass);

        if (!pluginClassLoadersInjected)
        {
            injectPluginsClassLoaders(classLoaderTestRunner, isolatedTestClass);
            pluginClassLoadersInjected = true;
        }
    }

    private Class<?> getTestClass(Class<?> clazz) throws InitializationError
    {
        try
        {
            return classLoaderTestRunner.loadClassWithApplicationClassLoader(clazz.getName());
        }
        catch (Exception e)
        {
            throw new InitializationError(e);
        }
    }

    /**
     * Creates the {@link ClassLoaderTestRunner} with the isolated class loaders.
     *
     * @param klass the test class being executed
     * @throws IOException if an error happened while reading {@link org.mule.functional.classloading.isolation.utils.RunnerModuleUtils#EXCLUDED_PROPERTIES_FILE} file
     * @return creates a {@link ClassLoaderTestRunner} that would be used to run the test. This way the test will be isolated and it will behave
     * similar as an application running in a Mule standalone container.
     */
    private static ClassLoaderTestRunner createClassLoaderTestRunner(Class<?> klass) throws IOException
    {
        // Initializes utility classes
        ClassPathUrlProvider classPathUrlsProvider = new ClassPathUrlProvider();
        MavenDependenciesResolver mavenDependenciesResolver = new DependencyGraphMavenDependenciesResolver();
        MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping = new MuleMavenMultiModuleArtifactMapping();
        MuleClassLoaderRunnerFactory classLoaderRunnerFactory = new MuleClassLoaderRunnerFactory();
        ClassPathClassifier classPathClassifier = new MuleClassPathClassifier();

        // Does the classification and creation of the isolated ClassLoader
        ClassPathClassifierContext context = new ClassPathClassifierContext(klass, classPathUrlsProvider.getURLs(),
                                                                            mavenDependenciesResolver.buildDependencies(), mavenMultiModuleArtifactMapping);
        ArtifactUrlClassification artifactUrlClassification = classPathClassifier.classify(context);
        return classLoaderRunnerFactory.createClassLoaderTestRunner(context.getExtraBootPackages(), artifactUrlClassification);
    }

    /**
     * Invokes the method to inject the plugin class loaders as the test is annotated with {@link PluginClassLoadersAware}.
     *
     * @param classLoaderTestRunner the result {@link ArtifactClassLoader}s defined for container, plugins and application
     * @param isolatedTestClass the test {@link Class} loaded with the isolated {@link ClassLoader}
     * @throws IllegalStateException if the test doesn't have an annotated method to inject plugin class loaders or if it has more than one method annotated.
     * @throws Throwable if an error ocurrs while setting the list of {@link ArtifactClassLoader}s for plugins.
     */
    private static void injectPluginsClassLoaders(ClassLoaderTestRunner classLoaderTestRunner, Class<?> isolatedTestClass) throws Throwable
    {
        TestClass testClass = new TestClass(isolatedTestClass);
        Class<? extends Annotation> artifactContextAwareAnn = (Class<? extends Annotation>) classLoaderTestRunner.loadClassWithApplicationClassLoader(PluginClassLoadersAware.class.getName());
        List<FrameworkMethod> contextAwareMethods = testClass.getAnnotatedMethods(artifactContextAwareAnn);
        if (contextAwareMethods.size() != 1)
        {
            throw new IllegalStateException("Isolation tests need to have one method marked with annotation " + PluginClassLoadersAware.class.getName());
        }
        for (FrameworkMethod method : contextAwareMethods)
        {
            if (!method.isStatic() || method.isPublic())
            {
                throw new IllegalStateException("Method marked with annotation " + PluginClassLoadersAware.class.getName() + " should be private static and it should receive a parameter of type List<" + ArtifactClassLoader.class + ">");
            }
            method.getMethod().setAccessible(true);
            try
            {
                method.invokeExplosively(null, classLoaderTestRunner.getPluginsClassLoaders());
            }
            catch (IllegalArgumentException e)
            {
                throw new IllegalStateException("Method marked with annotation " + PluginClassLoadersAware.class.getName() + " should receive a parameter of type List<" + ArtifactClassLoader.class + ">");
            }
            finally
            {
                method.getMethod().setAccessible(false);
            }
        }
    }

    /**
     * @return delegates to the internal runner to get the description needed by JUnit.
     */
    @Override
    public Description getDescription()
    {
        return delegate.getDescription();
    }

    /**
     * When the test is about to be executed the ThreadContextClassLoader is changed to use the application class loader that
     * was created so the execution of the test will be done using an isolated class loader that mimics the standalone container.
     *
     * @param notifier the {@link RunNotifier} from JUnit that will be notified about the results of the test methods invoked.
     */
    @Override
    public void run(RunNotifier notifier)
    {
        withContextClassLoader(classLoaderTestRunner.getApplicationClassLoader().getClassLoader(), () -> delegate.run(notifier));
    }

    /**
     * Delegates to the inner runner to filter.
     *
     * @param filter the {@link Filter} from JUnit to select a single test.
     * @throws NoTestsRemainException
     */
    @Override
    public void filter(Filter filter) throws NoTestsRemainException
    {
        if (delegate instanceof Filterable)
        {
            ((Filterable) delegate).filter(filter);
        }
    }
}
