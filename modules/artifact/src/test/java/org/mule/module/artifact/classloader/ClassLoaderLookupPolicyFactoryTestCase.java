/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.module.artifact.classloader.ClassLoaderLookupPolicyFactory.BLOCKED_PACKAGE_PREFIX;
import static org.mule.module.artifact.classloader.ClassLoaderLookupPolicyFactory.PACKAGE_SEPARATOR;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class ClassLoaderLookupPolicyFactoryTestCase extends AbstractMuleTestCase
{

    public static final String DEFAULT_PACKAGE = "org.bar";
    public static final String CUSTOM_PACKAGE = "org.foo.bar";
    public static final String SYSTEM_PACKAGE = "java.lang";

    private ClassLoaderLookupPolicyFactory parser = new ClassLoaderLookupPolicyFactory();

    @Test(expected = IllegalArgumentException.class)
    public void forbidsOverrideOfSystemPackage()
    {
        parser.create(SYSTEM_PACKAGE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void forbidsOverrideOfDottedSystemPackage()
    {
        parser.create(SYSTEM_PACKAGE);
    }

    @Test
    public void overridesPackage()
    {
        final ClassLoaderLookupPolicy classLoaderLookupPolicy = parser.create(CUSTOM_PACKAGE);

        assertThat(classLoaderLookupPolicy.isOverridden(getClassName(CUSTOM_PACKAGE)), is(true));
        assertThat(classLoaderLookupPolicy.isOverridden(getClassName(DEFAULT_PACKAGE)), is(false));
    }

    @Test
    public void overridesDottedPackage()
    {
        final ClassLoaderLookupPolicy classLoaderLookupPolicy = parser.create(CUSTOM_PACKAGE + PACKAGE_SEPARATOR);

        assertThat(classLoaderLookupPolicy.isOverridden(getClassName(CUSTOM_PACKAGE)), is(true));
        assertThat(classLoaderLookupPolicy.isOverridden(getClassName(DEFAULT_PACKAGE)), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void forbidsBlockingOfSystemPackage()
    {
        parser.create(BLOCKED_PACKAGE_PREFIX + SYSTEM_PACKAGE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void forbidsBlockingOfDottedSystemPackage()
    {
        parser.create(BLOCKED_PACKAGE_PREFIX + SYSTEM_PACKAGE + PACKAGE_SEPARATOR);
    }

    @Test
    public void blocksDottedPackage()
    {
        final ClassLoaderLookupPolicy classLoaderLookupPolicy = parser.create(BLOCKED_PACKAGE_PREFIX + CUSTOM_PACKAGE + PACKAGE_SEPARATOR);

        assertThat(classLoaderLookupPolicy.isBlocked(getClassName(CUSTOM_PACKAGE)), is(true));
        assertThat(classLoaderLookupPolicy.isOverridden(getClassName(DEFAULT_PACKAGE)), is(false));
    }

    @Test
    public void blocksPackage()
    {
        final ClassLoaderLookupPolicy classLoaderLookupPolicy = parser.create(BLOCKED_PACKAGE_PREFIX + CUSTOM_PACKAGE);

        assertThat(classLoaderLookupPolicy.isBlocked(getClassName(CUSTOM_PACKAGE)), is(true));
        assertThat(classLoaderLookupPolicy.isOverridden(getClassName(DEFAULT_PACKAGE)), is(false));
    }

    private String getClassName(String packageName)
    {
        return packageName + ".MyClass";
    }
}