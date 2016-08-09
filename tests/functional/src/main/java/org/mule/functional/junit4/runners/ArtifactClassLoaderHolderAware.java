/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines that when the test is going to be run with an {@link ArtifactClassLoaderRunner} it would need to get access to the
 * {@link org.mule.functional.api.classloading.isolation.ArtifactClassLoaderHolder} created by the launcher class loader
 * in order to load classes with the {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader}s created.
 * <p/>
 * A private static method should be defined and annotated with this annotation in order to be called by the runner so the test
 * later could get access to the {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader}s. Only one method
 * should be annotated with this annotation.
 * <p/>
 * Be aware that as the test would be loaded with the isolated class loader it cannot use
 * {@link org.mule.functional.api.classloading.isolation.ArtifactClassLoaderHolder} neither
 * {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader} due to mismatching type between class loader so
 * these objects will need to be used with reflection.
 * <p/>
 * The annotated method mentioned in previous paragraph would have to receive an {@link Object} parameter type.
 * <p/>
 * For more information, see {@link org.mule.functional.junit4.ArtifactFunctionalTestCase} where this is used.
 *
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface ArtifactClassLoaderHolderAware {
}
