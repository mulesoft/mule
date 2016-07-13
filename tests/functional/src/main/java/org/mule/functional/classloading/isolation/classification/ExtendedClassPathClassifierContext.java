/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classification;

import org.mule.functional.classloading.isolation.classpath.MavenArtifactToClassPathUrlsResolver;
import org.mule.functional.classloading.isolation.maven.MavenArtifact;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunner;

import java.io.File;

/**
 * Just extends {@link ClassPathClassifierContext} to append specific data needed to classify the context internally in {@link MuleClassPathClassifier}.
 *
 * @since 4.0
 */
public class ExtendedClassPathClassifierContext
{
    private final ClassPathClassifierContext classificationContext;
    private final MavenArtifactToClassPathUrlsResolver artifactToClassPathURLResolver;
    private final File targetTestClassesFolder;

    /**
     * Creates a {@link ExtendedClassPathClassifierContext} used internally in {@link MuleClassPathClassifier} to do the classification.
     *
     * @param classificationContext the initial {@link ClassPathClassifierContext} context passed by {@link ArtifactClassLoaderRunner}
     * @param artifactToClassPathURLResolver resolves the {@link java.net.URL} from the class path for a given artifactId
     * @param targetTestClassesFolder the target/test-classes folder of the current artifact being tested
     */
    public ExtendedClassPathClassifierContext(final ClassPathClassifierContext classificationContext, final MavenArtifactToClassPathUrlsResolver artifactToClassPathURLResolver, final File targetTestClassesFolder)
    {
        this.classificationContext = classificationContext;
        this.artifactToClassPathURLResolver = artifactToClassPathURLResolver;
        this.targetTestClassesFolder = targetTestClassesFolder;
    }

    public MavenArtifactToClassPathUrlsResolver getArtifactToClassPathURLResolver()
    {
        return artifactToClassPathURLResolver;
    }

    public MavenArtifact getRootArtifact()
    {
        return classificationContext.getDependencyGraph().getRootArtifact();
    }

    public ClassPathClassifierContext getClassificationContext()
    {
        return classificationContext;
    }

    public File getTargetTestClassesFolder()
    {
        return targetTestClassesFolder;
    }
}
