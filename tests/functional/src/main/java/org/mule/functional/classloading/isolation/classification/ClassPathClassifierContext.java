/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classification;

import static java.util.Collections.addAll;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.functional.classloading.isolation.utils.RunnerModuleUtils.getExcludedProperties;
import static org.mule.functional.util.AnnotationUtils.getAnnotationAttributeFromHierarchy;
import org.mule.functional.classloading.isolation.maven.DependenciesGraph;
import org.mule.functional.classloading.isolation.maven.MavenArtifact;
import org.mule.functional.classloading.isolation.maven.MavenArtifactMatcherPredicate;
import org.mule.functional.classloading.isolation.maven.MavenMultiModuleArtifactMapping;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfig;

import com.google.common.collect.Sets;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a context that contains what is needed in order to do a classpath classification.
 * It is used in {@link ClassPathClassifier}.
 *
 * @since 4.0
 */
public class ClassPathClassifierContext
{
    public static final int GROUP_ID_ARTIFACT_ID_TYPE_PATTERN_CHUNKS = 3;
    public static final String EXCLUDED_MODULES = "excluded.modules";

    private final Class<?> testClass;
    private final List<URL> classPathURLs;
    private final DependenciesGraph dependenciesGraph;
    private final MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping;
    private final Predicate<MavenArtifact> exclusions;
    private final Set<String> extraBootPackages;

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Creates a context used for doing the classification of the class path.
     *
     * @param testClass the test {@link Class} being tested
     * @param classPathURLs the whole set of {@link URL}s that were loaded by IDE/Maven Surefire plugin when running the test
     * @param dependenciesGraph the maven dependencies graph for the artifact that the test belongs to
     * @param mavenMultiModuleArtifactMapping a mapper to get multi-module folder for artifactIds
     * @throws IOException if an error happened while reading {@link org.mule.functional.classloading.isolation.utils.RunnerModuleUtils#EXCLUDED_PROPERTIES_FILE} file
     */
    public ClassPathClassifierContext(final Class<?> testClass, final List<URL> classPathURLs, final DependenciesGraph dependenciesGraph, final MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping) throws IOException
    {
        this.testClass = testClass;
        this.classPathURLs = classPathURLs;
        this.dependenciesGraph = dependenciesGraph;
        this.mavenMultiModuleArtifactMapping = mavenMultiModuleArtifactMapping;

        Properties excludedProperties = getExcludedProperties();
        this.exclusions = createExclusionsPredicate(testClass, excludedProperties);
        this.extraBootPackages = getExtraBootPackages(testClass, excludedProperties);
    }

    /**
     * @return the {@link Class} for the test that is going to be executed
     */
    public Class<?> getTestClass()
    {
        return testClass;
    }

    /**
     * @return a {@link List} of {@link URL}s for the classpath provided by JUnit (it is the complete list of URLs)
     */
    public List<URL> getClassPathURLs()
    {
        return classPathURLs;
    }

    /**
     * @return a {@link DependenciesGraph} for the given artifact tested.
     */
    public DependenciesGraph getDependencyGraph()
    {
        return dependenciesGraph;
    }

    /**
     * @return {@link MavenMultiModuleArtifactMapping} mapper for artifactIds and multi-module folders.
     */
    public MavenMultiModuleArtifactMapping getMavenMultiModuleArtifactMapping()
    {
        return mavenMultiModuleArtifactMapping;
    }

    /**
     * @return {@link Predicate} to be used to exclude artifacts from being added to application {@link ClassLoader} due to
     * they are going to be in container {@link ClassLoader}.
     */
    public Predicate<MavenArtifact> getExclusions()
    {
        return exclusions;
    }

    /**
     * @return {@link Set} of {@link String}s containing the extra boot packages defined to be appended to the container in addition to the pre-defined ones.
     */
    public Set<String> getExtraBootPackages()
    {
        return extraBootPackages;
    }

    /**
     * The list of exclusion GroutId/ArtifactId/Type to be excluded from application/plugin class loaders due to these are supposed to
     * be exposed by the container.
     * <p/>
     * It defined by the file {@link org.mule.functional.classloading.isolation.utils.RunnerModuleUtils#EXCLUDED_PROPERTIES_FILE} and can be changed by having this file in the module that is tested or
     * appended to the default excluded groutId/artifactId/type by marking the test with the annotation {@link ArtifactClassLoaderRunnerConfig}.
     *
     * @param klass the test {@link Class} being tested
     * @param excludedProperties {@link Properties} that has the list of excluded modules
     * @return a {@link Predicate} to be used in order to excluded maven artifacts from application/plugin class loaders.
     */
    private Predicate<MavenArtifact> createExclusionsPredicate(final Class<?> klass, Properties excludedProperties)
    {
        Predicate<MavenArtifact> exclusionPredicate = null;
        String excludedModules = excludedProperties.getProperty(EXCLUDED_MODULES);
        if (excludedModules != null)
        {
            exclusionPredicate = createPredicate(exclusionPredicate, excludedModules);
        }
        else
        {
            logger.warn(EXCLUDED_MODULES + " found but there is no list of modules defined to be excluded, this could be the reason why the test may fail later due to JUnit classes are not found");
        }
        List<String> exclusionsAnnotated = getAnnotationAttributeFromHierarchy(klass, ArtifactClassLoaderRunnerConfig.class, "exclusions");
        for (String exclusionsToBeAppended : exclusionsAnnotated)
        {
            if (exclusionsToBeAppended != null && exclusionsToBeAppended.length() > 0)
            {
                exclusionPredicate = createPredicate(exclusionPredicate, exclusionsToBeAppended);
            }
        }

        // If no exclusion is defined the predicate should always return false to any artifact due to none is excluded
        return exclusionPredicate == null ? x -> false : exclusionPredicate;
    }

    /**
     * Creates the predicate or adds a new one to the given one by splitting the exclusions patterns.
     *
     * @param exclusionPredicate the current exclusion predicate to compose with an OR operation (if not null).
     * @param exclusions the coma separated list of patterns to parse and generate exclusions for.
     * @return a new {@link Predicate} with the exclusions.
     */
    private Predicate<MavenArtifact> createPredicate(final Predicate<MavenArtifact> exclusionPredicate, final String exclusions)
    {
        Predicate<MavenArtifact> predicate = exclusionPredicate;
        for (String exclusion : exclusions.split(","))
        {
            String[] exclusionSplit = exclusion.split(":");
            if (exclusionSplit.length != GROUP_ID_ARTIFACT_ID_TYPE_PATTERN_CHUNKS)
            {
                throw new IllegalArgumentException("Exclusion pattern should have the format groupId:artifactId:type");
            }
            Predicate<MavenArtifact> artifactExclusion = new MavenArtifactMatcherPredicate(exclusionSplit[0], exclusionSplit[1], exclusionSplit[2]);
            if (predicate == null)
            {
                predicate = artifactExclusion;
            }
            else
            {
                predicate = predicate.or(artifactExclusion);
            }
        }
        return predicate;
    }

    /**
     * Gets the {@link Set} of {@link String}s of packages to be added to the container {@link ClassLoader} in addition to the ones already
     * pre-defined by the mule container.
     *
     * @param klass the test {@link Class} being tested
     * @param excludedProperties {@link Properties }that has the list of extra boot packages definitions
     * @return a {@link Set} of {@link String}s with the extra boot packages to be appended
     */
    private Set<String> getExtraBootPackages(Class<?> klass, Properties excludedProperties)
    {
        Set<String> packages = Sets.newHashSet();

        List<String> extraBootPackagesList = getAnnotationAttributeFromHierarchy(klass, ArtifactClassLoaderRunnerConfig.class, "extraBootPackages");
        extraBootPackagesList.stream().filter(extraBootPackages -> !isEmpty(extraBootPackages)).forEach(extraBootPackages -> addAll(packages, extraBootPackages.split(",")));

        String excludedExtraBootPackages = excludedProperties.getProperty("extraBoot.packages");
        if (excludedExtraBootPackages != null)
        {
            for (String extraBootPackage : excludedExtraBootPackages.split(","))
            {
                packages.add(extraBootPackage);
            }
        }
        else
        {
            logger.warn(EXCLUDED_MODULES + " found but there is no list of extra boot packages defined to be added to container, this could be the reason why the test may fail later due to JUnit classes are not found");
        }
        return packages;
    }


}
