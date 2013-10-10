/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SmallTest
public class FineGrainedControlClassLoaderTest extends AbstractMuleTestCase
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
