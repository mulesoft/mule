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

import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;
import org.mule.util.scan.annotations.MetaAnnotationTypeFilter;
import org.mule.util.scan.annotations.AnnotationTypeFilter;
import org.mule.util.scan.annotations.AnnotationsScanner;
import org.mule.util.scan.annotations.AnnotationFilter;
import org.mule.config.ExceptionHelper;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.HashSet;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Annotation;

import org.objectweb.asm.ClassReader;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * This class can be used to scan the classpath for classtypes (or interfaces they implement) or for annotations on the classpath.
 * The type of scanner used depends on the class type passed in. There are currently 3 types of scanner;
 * <ul>
 * <li>{@link org.mule.util.scan.InterfaceClassScanner} - will search for all class that are assignable to the interface provided</li>
 * <li>{@link org.mule.util.scan.ImplementationClassScanner} - will search for all classes that extend a base type</li>
 * <li>{@link org.mule.util.scan.annotations.AnnotationsScanner} - will searhc for classes with specific annotations, this can also seach for meta annotations</li>
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

    public ClasspathScanner(String[] basepaths)
    {
        this.classLoader = getClass().getClassLoader();
        this.basepaths = basepaths;
    }

    public ClasspathScanner(String[] basepaths, ClassLoader classLoader)
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
                    if (name.contains("$1"))
                    {
                        continue;
                    }
                    ClassReader reader = new ClassReader(classLoader.getResourceAsStream(name));
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
                    Throwable t = ExceptionHelper.getRootException(e);
                    logger.warn(e.toString() + ": caused by: " + t.toString());
                }
            }
        }
        return set;
    }

    protected Set<Class> processFileUrl(URL url, String basepath, Class clazz) throws IOException
    {
        Set<Class> set = new HashSet<Class>();
        String urlBase = url.getFile();
        Collection<File> files = FileUtils.listFiles(new File(url.getFile()), new String[]{"class"}, true);
        String name = null;
        for (File file : files)
        {
            try
            {
                //Get the actual class name (urlBase - bathpath)
                name = file.getAbsolutePath().substring(urlBase.length() - basepath.length());
                name = name.replaceAll("/", ".");
                if (name.endsWith(".class"))
                {
                    name = name.substring(0, name.length() - 6);
                }

                ClassReader reader = new ClassReader(name);
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
                Throwable t = ExceptionHelper.getRootException(e);
                logger.warn(e.toString() + ": caused by: " + t.toString());
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
            logger.warn(c + " : " + e.toString());
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
     * @see org.mule.util.annotations.AnnotationsScanner
     * @see org.mule.util.scan.InterfaceClassScanner
     * @see org.mule.util.scan.ImplementationClassScanner
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
                            filter = new MetaAnnotationTypeFilter(clazz);
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
