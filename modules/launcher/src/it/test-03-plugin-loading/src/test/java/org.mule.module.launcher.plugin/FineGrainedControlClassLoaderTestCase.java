/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.plugin;

import org.mule.module.launcher.FineGrainedControlClassLoader;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.ClassUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

public class FineGrainedControlClassLoaderTestCase extends AbstractMuleTestCase
{

    /**
     * Parent implementation 1 says 'Hello', child impl2 ignored.
     */
    public void testParentFirst() throws Exception
    {

        // load compiled classes from the previously built test modules
        final URL classPathRoot = ClassUtils.getClassPathRoot(getClass());
        System.out.println("classPathRoot = " + classPathRoot);
        File f = new File(classPathRoot.getPath(), "../../../test-01-plugin-impl-1/target/classes/");
        System.out.println("f = " + f);
        assertTrue("Dependent test classes not found, has required IT test modules been run before?", f.exists());
        URL[] parentUrls = new URL[] {f.toURI().toURL()};
        URLClassLoader parent = new URLClassLoader(parentUrls, Thread.currentThread().getContextClassLoader());

        // now load alternative impl overriding the original behavior
        f = new File(classPathRoot.getPath(), "../../../test-02-plugin-impl-2/target/classes/");
        System.out.println("f = " + f);
        assertTrue("Dependent test classes not found, has required IT test modules been run before?", f.exists());
        URL[] childUrls = new URL[] {f.toURI().toURL()};

        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(childUrls, parent);
        Class c = ext.loadClass("mypackage.SneakyChatter");
        final Method methodHi = c.getMethod("hi");
        final Object result = methodHi.invoke(c.newInstance());
        assertEquals("Wrong implementation loaded", "Hello", result);
    }

    /**
     * Child impl2 overrides parent and says 'Bye'.
     */
    public void testPackageOverrideChild() throws Exception
    {

        // load compiled classes from the previously built test modules
        final URL classPathRoot = ClassUtils.getClassPathRoot(getClass());
        System.out.println("classPathRoot = " + classPathRoot);
        File f = new File(classPathRoot.getPath(), "../../../test-01-plugin-impl-1/target/classes/");
        System.out.println("f = " + f);
        assertTrue("Dependent test classes not found, has required IT test modules been run before?", f.exists());
        URL[] parentUrls = new URL[] {f.toURI().toURL()};
        URLClassLoader parent = new URLClassLoader(parentUrls, Thread.currentThread().getContextClassLoader());

        // now load alternative impl overriding the original behavior
        f = new File(classPathRoot.getPath(), "../../../test-02-plugin-impl-2/target/classes/");
        System.out.println("f = " + f);
        assertTrue("Dependent test classes not found, has required IT test modules been run before?", f.exists());
        URL[] childUrls = new URL[] {f.toURI().toURL()};

        Set<String> overrides = new HashSet<String>();
        overrides.add("mypackage");
        // child will override all classes in 'mypackage'
        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(childUrls, parent, overrides);
        Class c = ext.loadClass("mypackage.SneakyChatter");
        final Method methodHi = c.getMethod("hi");
        final Object result = methodHi.invoke(c.newInstance());
        assertEquals("Wrong implementation loaded", "Bye", result);
    }

    /**
     * Child impl2 overrides parent, but no child impl available (fallback to parent)
     */
    public void testPackageOverrideChildImplMissing() throws Exception
    {

        // load compiled classes from the previously built test modules
        final URL classPathRoot = ClassUtils.getClassPathRoot(getClass());
        System.out.println("classPathRoot = " + classPathRoot);
        File f = new File(classPathRoot.getPath(), "../../../test-01-plugin-impl-1/target/classes/");
        System.out.println("f = " + f);
        assertTrue("Dependent test classes not found, has required IT test modules been run before?", f.exists());
        URL[] parentUrls = new URL[] {f.toURI().toURL()};
        URLClassLoader parent = new URLClassLoader(parentUrls, Thread.currentThread().getContextClassLoader());

        Set<String> overrides = new HashSet<String>();
        overrides.add("mypackage");
        // child will override all classes in 'mypackage'
        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[0], parent, overrides);
        Class c = ext.loadClass("mypackage.SneakyChatter");
        final Method methodHi = c.getMethod("hi");
        final Object result = methodHi.invoke(c.newInstance());
        assertEquals("Wrong implementation loaded", "Hello", result);
    }

    /**
     * Child blocks the package without providing an implementation for it.
     */
    public void testBlockedPackageChildImplMissing() throws Exception
    {

        // load compiled classes from the previously built test modules
        final URL classPathRoot = ClassUtils.getClassPathRoot(getClass());
        System.out.println("classPathRoot = " + classPathRoot);
        File f = new File(classPathRoot.getPath(), "../../../test-01-plugin-impl-1/target/classes/");
        System.out.println("f = " + f);
        assertTrue("Dependent test classes not found, has required IT test modules been run before?", f.exists());
        URL[] parentUrls = new URL[] {f.toURI().toURL()};
        URLClassLoader parent = new URLClassLoader(parentUrls, Thread.currentThread().getContextClassLoader());

        // confirm the parent is able to load the class
        assertNotNull(parent.loadClass("mypackage.SneakyChatter"));

        Set<String> overrides = new HashSet<String>();
        overrides.add("-mypackage");
        // block the package, this is stronger than override
        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[0], parent, overrides);
        try
        {
            ext.loadClass("mypackage.SneakyChatter");
            fail("Should have failed to load the class");
        }
        catch (ClassNotFoundException e)
        {
            // expected
        }
    }

    /**
     * Child blocks the package, impl2 says 'Bye', impl1 is 'invisible' to the child.
     */
    public void testBlockedPackage() throws Exception
    {

        // load compiled classes from the previously built test modules
        final URL classPathRoot = ClassUtils.getClassPathRoot(getClass());
        System.out.println("classPathRoot = " + classPathRoot);
        File f = new File(classPathRoot.getPath(), "../../../test-01-plugin-impl-1/target/classes/");
        System.out.println("f = " + f);
        assertTrue("Dependent test classes not found, has required IT test modules been run before?", f.exists());
        URL[] parentUrls = new URL[] {f.toURI().toURL()};
        URLClassLoader parent = new URLClassLoader(parentUrls, Thread.currentThread().getContextClassLoader());

        // confirm the parent is able to load the class
        assertNotNull(parent.loadClass("mypackage.SneakyChatter"));

        // now load alternative impl overriding the original behavior
        f = new File(classPathRoot.getPath(), "../../../test-02-plugin-impl-2/target/classes/");
        System.out.println("f = " + f);
        assertTrue("Dependent test classes not found, has required IT test modules been run before?", f.exists());
        URL[] childUrls = new URL[] {f.toURI().toURL()};

        Set<String> overrides = new HashSet<String>();
        overrides.add("-mypackage");
        // block the package, this is stronger than override
        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(childUrls, parent, overrides);
        Class c = ext.loadClass("mypackage.SneakyChatter");
        final Method methodHi = c.getMethod("hi");
        final Object result = methodHi.invoke(c.newInstance());
        assertEquals("Wrong implementation loaded", "Bye", result);
    }


    /**
     * Child impl2 overrides parent, but class is not available in either classloader
     */
    public void testPackageOverrideClassNotFound() throws Exception
    {

        Set<String> overrides = new HashSet<String>();
        overrides.add("mypackage");
        // child will override all classes in 'mypackage'
        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[0], Thread.currentThread().getContextClassLoader(),
                                                              overrides);
        try
        {
            ext.loadClass("mypackage.SneakyChatter");
            fail("Should have thrown a ClassNotFoundException");
        }
        catch (ClassNotFoundException e)
        {
            // expected
        }
    }

    public void testIllegalOverride()
    {
        try
        {
            Set<String> overrides = new HashSet<String>();
            overrides.add("org.mule.module.reboot.MuleContainerBootstrap");
            new FineGrainedControlClassLoader(new URL[0], null, overrides);
            fail("Should have not allowed this illegal override value");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    public void testIllegalBlocked()
    {
        try
        {
            Set<String> overrides = new HashSet<String>();
            overrides.add("-java.util.Collections");
            new FineGrainedControlClassLoader(new URL[0], null, overrides);
            fail("Should have not allowed this illegal 'blocked' value");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }
}
