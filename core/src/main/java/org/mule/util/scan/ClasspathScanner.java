/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan;

import org.mule.config.ExceptionHelper;
import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;
import org.mule.util.scan.annotations.AnnotationFilter;
import org.mule.util.scan.annotations.AnnotationTypeFilter;
import org.mule.util.scan.annotations.AnnotationsScanner;
import org.mule.util.scan.annotations.MetaAnnotationTypeFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.ClassReader;

/**
 * This class can be used to scan the classpath for classtypes (or interfaces they implement) or for annotations on the classpath.
 * The type of scanner used depends on the class type passed in. There are currently 3 types of scanner;
 * <ul>
 * <li>{@link InterfaceClassScanner} - will search for all class that are assignable to the interface provided</li>
 * <li>{@link ImplementationClassScanner} - will search for all classes that extend a base type</li>
 * <li>{@link AnnotationsScanner} - will searhc for classes with specific annotations, this can also seach for meta annotations</li>
 * </ul>
 * This scanner uses ASM to search class byte code rather than the classes themselves making orders of magnitude more performant and uses a lot less memory. ASM is the fasted of the
 * byte code manipulation libraries i.e. JavaAssist or BCEL
 */
public class ClasspathScanner
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(ClasspathScanner.class);

    private ClassLoader classLoader;
    private String[] basepaths = new String[]{""};

    public ClasspathScanner(String... basepaths)
    {
        this.classLoader = getClass().getClassLoader();
        this.basepaths = basepaths;
    }

    public ClasspathScanner(ClassLoader classLoader, String... basepaths)
    {
        this.classLoader = classLoader;
        this.basepaths = basepaths;
    }

    public Set<Class> scanFor(Class clazz) throws IOException
    {
        Set<Class> classes = new HashSet<Class>();

        for (int i = 0; i < basepaths.length; i++)
        {
            String basepath = basepaths[i];

            Enumeration<URL> urls = classLoader.getResources(basepath);
            while (urls.hasMoreElements())
            {
                URL url = urls.nextElement();
                if (url.getProtocol().equalsIgnoreCase("file"))
                {
                    classes.addAll(processFileUrl(url, basepath, clazz));
                }
                else if (url.getProtocol().equalsIgnoreCase("jar"))
                {
                    classes.addAll(processJarUrl(url, basepath, clazz));
                }
                else
                {
                    throw new IllegalArgumentException("Do not understand how to handle protocol: " + url.getProtocol());
                }
            }
        }
        return classes;
    }

    protected Set<Class> processJarUrl(URL url, String basepath, Class clazz) throws IOException
    {
        Set<Class> set = new HashSet<Class>();
        String path = url.getFile().substring(5, url.getFile().indexOf("!"));
        path = URLDecoder.decode(path);
        JarFile jar = new JarFile(path);

        for (Enumeration entries = jar.entries(); entries.hasMoreElements();)
        {
            JarEntry entry = (JarEntry) entries.nextElement();
            if (entry.getName().startsWith(basepath) && entry.getName().endsWith(".class"))
            {
                try
                {
                    String name = entry.getName();
                    //Ignore anonymous
                    // TODO RM what about the other anonymous classes like $2, $3 ?
                    if (name.contains("$1"))
                    {
                        continue;
                    }
                    URL classURL = classLoader.getResource(name);
                    ClassReader reader = new ClassReader(classURL.openStream());
                    ClassScanner visitor = getScanner(clazz);
                    reader.accept(visitor, 0);
                    if (visitor.isMatch())
                    {
                        Class c = loadClass(visitor.getClassName());
                        if (c != null)
                        {
                            set.add(c);
                        }
                    }
                }
                catch (Exception e)
                {
                    if (logger.isDebugEnabled())
                    {
                        Throwable t = ExceptionHelper.getRootException(e);
                        logger.debug(String.format("%s: caused by: %s", e.toString(), t.toString()));
                    }
                }
            }
        }
        return set;
    }

    protected Set<Class> processFileUrl(URL url, String basepath, Class clazz) throws IOException
    {
        Set<Class> set = new HashSet<Class>();
        String urlBase = url.getFile();
        urlBase = URLDecoder.decode(urlBase);

        Collection<File> files = FileUtils.listFiles(new File(urlBase), new String[]{"class"}, true);
        for (File file : files)
        {
            try
            {
                ClassReader reader = new ClassReader(new FileInputStream(file));
                ClassScanner visitor = getScanner(clazz);
                reader.accept(visitor, 0);
                if (visitor.isMatch())
                {
                    Class c = loadClass(visitor.getClassName());
                    if (c != null)
                    {
                        set.add(c);
                    }
                }
            }
            catch (IOException e)
            {
                if (logger.isDebugEnabled())
                {
                    Throwable t = ExceptionHelper.getRootException(e);
                    logger.debug(String.format("%s: caused by: %s", e.toString(), t.toString()));
                }
            }
        }
        return set;
    }

    protected Class loadClass(String name)
    {
        String c = name.replace("/", ".");
        try
        {
            return ClassUtils.loadClass(c, classLoader);
        }
        catch (ClassNotFoundException e)
        {
            if (logger.isWarnEnabled())
            {
                logger.warn(String.format("%s : %s", c, e.toString()));
            }
            return null;
        }
    }

    /**
     * Works out the correct scanner based on the class passed in
     * <p/>
     * Note that these could be better architected by breaking out filters into strategy objects, but for now this
     * suits my needs
     *
     * @param clazz the type to scan for
     * @return a scanner suitable for handling the type passed in
     * @see AnnotationsScanner
     * @see InterfaceClassScanner
     * @see ImplementationClassScanner
     */
    protected ClassScanner getScanner(Class clazz)
    {
        if (clazz.isInterface())
        {
            if (clazz.isAnnotation())
            {
                AnnotationFilter filter = null;
                Annotation[] annos = clazz.getDeclaredAnnotations();
                for (int i = 0; i < annos.length; i++)
                {
                    Annotation anno = annos[i];
                    if (anno instanceof Target)
                    {
                        if (((Target) anno).value()[0] == ElementType.ANNOTATION_TYPE)
                        {
                            filter = new MetaAnnotationTypeFilter(clazz, classLoader);
                        }
                    }
                }
                if (filter == null)
                {
                    filter = new AnnotationTypeFilter(clazz);
                }
                return new AnnotationsScanner(filter);
            }
            else
            {
                return new InterfaceClassScanner(clazz);
            }
        }
        else
        {
            return new ImplementationClassScanner(clazz);
        }
    }
}
