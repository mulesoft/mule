/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class ClassLoaderFactory {

    private static ClassLoaderFactory factory;
    private Map sharedClassLoaders;

    public static ClassLoaderFactory getInstance() {
        if (factory == null) {
            factory = new ClassLoaderFactory();
        }
        return factory;
    }

    private ClassLoaderFactory() {
        this.sharedClassLoaders = new HashMap();
    }

    public ClassLoader createComponentClassLoader(RegistryComponent component) throws MalformedURLException {
        DelegatingClassLoader dcl = new DelegatingClassLoader();
        Library[] libraries = component.getLibraries();
        for (int i = 0; i < libraries.length; i++) {
            dcl.addClassLoader(getSharedClassLoader(libraries[i]));
        }
        URL[] urls = getUrlsFrom(component.getInstallRoot(), component.getClassPathElements());
        boolean isParentFirst = component.isClassLoaderParentFirst();
        JbiClassLoader ccl = new JbiClassLoader(urls, dcl, isParentFirst);
        return ccl;
    }

    private ClassLoader getSharedClassLoader(Library library) throws MalformedURLException {
        ClassLoader cl = (ClassLoader) this.sharedClassLoaders.get(library.getName());
        if (cl == null) {
            URL[] urls = getUrlsFrom(library.getInstallRoot(), library.getClassPathElements());
            boolean isParentFirst = library.isClassLoaderParentFirst();
            cl = new JbiClassLoader(urls, null, isParentFirst);
            this.sharedClassLoaders.put(library.getName(), cl);
        }
        return cl;
    }

    private URL[] getUrlsFrom(String root, List paths) throws MalformedURLException {
        URL[] urls = new URL[paths.size()];
        for (int i = 0; i < urls.length; i++) {
            String cpElement = (String) paths.get(i);
            urls[i] = new File(root, cpElement).toURL();
        }
        return urls;
    }


    /**
     * ClassLoader for a component.
     * This class loader is able to resolve class either
     * by first looking at the parent ot itself.
     *
     * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
     */
    public static class JbiClassLoader extends URLClassLoader {

        private boolean parentFirst;

        public JbiClassLoader(URL[] urls, ClassLoader parent, boolean parentFirst) {
            super(urls, parent);
            this.parentFirst = parentFirst;
        }

        protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // First, check if the class has already been loaded
            Class clazz = findLoadedClass(name);
            if (clazz == null) {
                if (this.parentFirst) {
                    try {
                        clazz = getParent().loadClass(name);
                    } catch (ClassNotFoundException cnfe) {
                        clazz = findClass(name);
                    }
                } else {
                    try {
                        clazz = findClass(name);
                    } catch (ClassNotFoundException e) {
                        clazz = getParent().loadClass(name);
                    }
                }
            }
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }
    }

    /**
     * ClassLoader for shared libraries
     *
     * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
     */
    public static class DelegatingClassLoader extends SecureClassLoader {
        private List loaders;
        public DelegatingClassLoader() {
            this.loaders = new ArrayList();
        }
        public void addClassLoader(ClassLoader loader) {
            if (loader == null) {
                throw new IllegalArgumentException("loader can not be null");
            }
            loaders.add(loader);
        }
        protected Class findClass(String name) throws ClassNotFoundException {
            for (Iterator iter = this.loaders.iterator(); iter.hasNext();) {
                ClassLoader loader = (ClassLoader) iter.next();
                try {
                    return loader.loadClass(name);
                } catch (ClassNotFoundException e) {
                    // expected
                }
            }
            throw new ClassNotFoundException(name);
        }
    }

}
