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

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.ClassUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class MulePluginClassLoaderTestCase extends AbstractMuleTestCase
{

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
        MulePluginClassLoader ext = new MulePluginClassLoader(new URL[0], parent);
        Class c = ext.loadClass("mypackage.SneakyChatter");
        final Method methodHi = c.getMethod("hi");
        final Object result = methodHi.invoke(c.newInstance());
        assertEquals("Wrong implementation loaded", "Hello", result);
    }
}
