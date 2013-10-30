/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.FlakinessDetectorTestRunner;
import org.mule.tck.size.SmallTest;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(FlakinessDetectorTestRunner.class)
public class FineGrainedControlClassLoaderTestCase extends AbstractMuleTestCase
{
    @Test
    public void isBlockedFQClassName() throws Exception
    {
        Set<String> overrides = new HashSet<String>(1);
        overrides.add("-org.mycompany.MyClass");
        FineGrainedControlClassLoader classLoader = new FineGrainedControlClassLoader(new URL[0], null,
            overrides);
        assertTrue(classLoader.isBlocked("org.mycompany.MyClass"));
        assertFalse(classLoader.isBlocked("MyClass"));
        assertFalse(classLoader.isBlocked("org.mycompany.MyClassFactory"));
    }

    @Test
    public void isBlockedNotFQClassName() throws Exception
    {
        Set<String> overrides = new HashSet<String>(1);
        overrides.add("-MyClass");
        FineGrainedControlClassLoader classLoader = new FineGrainedControlClassLoader(new URL[0], null,
            overrides);
        assertTrue(classLoader.isBlocked("MyClass"));
        assertFalse(classLoader.isBlocked("MyClassFactory"));
        assertFalse(classLoader.isBlocked("org.mycompany.MyClass"));
    }

    @Test
    public void isBlockedPackageName() throws Exception
    {
        Set<String> overrides = new HashSet<String>(1);
        overrides.add("-org.mycompany");
        FineGrainedControlClassLoader classLoader = new FineGrainedControlClassLoader(new URL[0], null,
            overrides);
        assertTrue(classLoader.isBlocked("org.mycompany.MyClass"));
        assertTrue(classLoader.isBlocked("org.mycompany.somepackage.MyClass"));
    }

    @Test
    public void isOverriddenFQClassName() throws Exception
    {
        Set<String> overrides = new HashSet<String>(1);
        overrides.add("org.mycompany.MyClass");
        FineGrainedControlClassLoader classLoader = new FineGrainedControlClassLoader(new URL[0], null,
            overrides);
        assertTrue(classLoader.isOverridden("org.mycompany.MyClass"));
        assertFalse(classLoader.isOverridden("MyClass"));
        assertFalse(classLoader.isOverridden("org.mycompany.MyClassFactory"));
    }

    @Test
    public void isOverriddenNotFQClassName() throws Exception
    {
        Set<String> overrides = new HashSet<String>(1);
        overrides.add("MyClass");
        FineGrainedControlClassLoader classLoader = new FineGrainedControlClassLoader(new URL[0], null,
            overrides);
        assertTrue(classLoader.isOverridden("MyClass"));
        assertFalse(classLoader.isOverridden("MyClassFactory"));
        assertFalse(classLoader.isOverridden("org.mycompany.MyClass"));
    }

    @Test
    public void isOverriddenPackageName() throws Exception
    {
        Set<String> overrides = new HashSet<String>(1);
        overrides.add("org.mycompany");
        FineGrainedControlClassLoader classLoader = new FineGrainedControlClassLoader(new URL[0], null,
            overrides);
        assertTrue(classLoader.isOverridden("org.mycompany.MyClass"));
        assertTrue(classLoader.isOverridden("org.mycompany.somepackage.MyClass"));
    }
}
