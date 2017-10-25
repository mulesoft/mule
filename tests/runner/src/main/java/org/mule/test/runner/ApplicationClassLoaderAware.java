/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines that a test running with {@link ArtifactClassLoaderRunner} would need to get access to the Mule application
 * class loader in order to load classes from it.
 * <p/>
 * A private static method should be defined and annotated with this annotation in order to be called by the runner so the test
 * later could get access to the container {@link ClassLoader}. Only one method should be annotated with this annotation.
 * <p/>
 *
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface ApplicationClassLoaderAware {
}
