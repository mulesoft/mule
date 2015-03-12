/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.util.scan.annotations.ClosableClassReader;
import org.mule.util.scan.annotations.MetaAnnotationTypeFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.net.URL;
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
 * This class can be used to scan the classpath for classtypes (or interfaces they
 * implement) or for annotations on the classpath. The type of scanner used depends
 * on the class type passed in. There are currently 3 types of scanner;
 * <ul>
 * <li>{@link InterfaceClassScanner} - will search for all class that are assignable
 * to the interface provided</li>
 * <li>{@link ImplementationClassScanner} - will search for all classes that extend a
 * base type</li>
 * <li>{@link AnnotationsScanner} - will search for classes with specific
 * annotations, this can also seach for meta annotations</li>
 * </ul>
 * This scanner uses ASM to search class byte code rather than the classes themselves
 * making orders of magnitude better performance and uses a lot less memory. ASM
 * seems to be the fasted of the byte code manipulation libraries i.e. JavaAssist or
 * BCEL Note that the scanner will not scan inner or anonymous classes.
 *
 * @deprecated: As ASM 3.3.1 is not fully compliant with Java 8, this class has been deprecated, however you can still use it under Java 7.
 */
@Deprecated
public class ClasspathScanner
{
    public static final int INCLUDE_ABSTRACT = 0x01;
    public static final int INCLUDE_INTERFACE = 0x02;
    public static final int INCLUDE_INNER = 0x04;
    public static final int INCLUDE_ANONYMOUS = 0x08;

    public static final int DEFAULT_FLAGS = 0x0;
    
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(ClasspathScanner.class);

    private ClassLoader classLoader;
    
    private String[] basepaths;

    public ClasspathScanner(String... basepaths)
    {
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.basepaths = basepaths;
    }

    public ClasspathScanner(ClassLoader classLoader, String... basepaths)
    {
        this.classLoader = classLoader;
        this.basepaths = basepaths;
    }

    public <T> Set<Class<T>> scanFor(Class<T> clazz) throws IOException
    {
        return scanFor(clazz, DEFAULT_FLAGS);
    }

    public <T> Set<Class<T>> scanFor(Class<T> clazz, int flags) throws IOException
    {
        Set<Class<T>> classes = new HashSet<Class<T>>();

        for (int i = 0; i < basepaths.length; i++)
        {
            String basepath = basepaths[i];

            Enumeration<URL> urls = classLoader.getResources(basepath.trim());
            while (urls.hasMoreElements())
            {
                URL url = urls.nextElement();
                if (url.getProtocol().equalsIgnoreCase("file"))
                {
                    classes.addAll(processFileUrl(url, basepath, clazz, flags));
                }
                else if (url.getProtocol().equalsIgnoreCase("jar"))
                {
                    classes.addAll(processJarUrl(url, basepath, clazz, flags));
                }
                else if (url.getProtocol().equalsIgnoreCase("bundleresource"))
                {
                    logger.debug("Classpath contains an OSGi bundle resource which Mule does not know how to access, therefore this resource will be ignored: + " + url.toString());
                }
                else
                {
                    throw new IllegalArgumentException("Do not understand how to handle protocol: " + url.getProtocol());
                }
            }
        }
        return classes;
    }

    protected <T> Set<Class<T>> processJarUrl(URL url, String basepath, Class<T> clazz, int flags) throws IOException
    {
        Set<Class<T>> set = new HashSet<Class<T>>();
        String path = url.getFile().substring(5, url.getFile().indexOf("!"));
        // We can't URLDecoder.decode(path) since some encoded chars are allowed on file uris
        path = path.replaceAll("%20", " ");
        JarFile jar = new JarFile(path);

        for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();)
        {
            JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith(basepath) && entry.getName().endsWith(".class"))
            {
                try
                {
                    String name = entry.getName();
                    // Ignore anonymous and inner classes
                    if (name.contains("$") && !hasFlag(flags, INCLUDE_INNER))
                    {
                        continue;
                    }
                    
                    URL classURL = classLoader.getResource(name);
                    InputStream classStream = classURL.openStream();
                    ClassReader reader = new ClosableClassReader(classStream);

                    ClassScanner visitor = getScanner(clazz);
                    reader.accept(visitor, 0);
                    if (visitor.isMatch())
                    {
                        @SuppressWarnings("unchecked")
                        Class<T> loadedClass = (Class<T>) loadClass(visitor.getClassName());
                        addClassToSet(loadedClass, set, flags);
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
        jar.close();
        
        return set;
    }

    protected boolean hasFlag(int flags, int flag)
    {
        return (flags & flag) != 0;
    }

    protected <T> Set<Class<T>> processFileUrl(URL url, String basepath, Class<T> clazz, int flags) throws IOException
    {
        Set<Class<T>> set = new HashSet<Class<T>>();
        String urlBase = url.getFile();
        //We can't URLDecoder.decode(path) since some encoded chars are allowed on file uris
        urlBase = urlBase.replaceAll("%20", " ");
        File dir = new File(urlBase);
        if(!dir.isDirectory())
        {
            logger.warn("Cannot process File URL: " + url + ". Path is not a directory");
            return set;
        }

        @SuppressWarnings("unchecked")
        Collection<File> files = FileUtils.listFiles(new File(urlBase), new String[]{"class"}, true);
        for (File file : files)
        {
            try
            {
                //Ignore anonymous and inner classes
                if (file.getName().contains("$") && !hasFlag(flags, INCLUDE_INNER))
                {
                    continue;
                }
                InputStream classStream = new FileInputStream(file);
                ClassReader reader = new ClosableClassReader(classStream);

                ClassScanner visitor = getScanner(clazz);
                reader.accept(visitor, 0);
                if (visitor.isMatch())
                {
                    @SuppressWarnings("unchecked")
                    Class<T> loadedClass = (Class<T>) loadClass(visitor.getClassName());
                    addClassToSet(loadedClass, set, flags);
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

    protected <T> void addClassToSet(Class<T> c, Set<Class<T>> set, int flags)
    {
        if (c != null)
        {
            synchronized (set)
            {
                if (c.isInterface())
                {
                    if (hasFlag(flags, INCLUDE_INTERFACE))
                    {
                        set.add(c);
                    }
                }
                else if (Modifier.isAbstract(c.getModifiers()))
                {
                    if (hasFlag(flags, INCLUDE_ABSTRACT))
                    {
                        set.add(c);
                    }
                }
                else
                {
                    set.add(c);
                }
            }
        }
    }

    protected Class<?> loadClass(String name)
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
    protected ClassScanner getScanner(Class<?> clazz)
    {
        if (clazz.isInterface())
        {
            if (clazz.isAnnotation())
            {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) clazz;
                
                AnnotationFilter filter = null;
                Annotation[] annos = clazz.getDeclaredAnnotations();
                for (int i = 0; i < annos.length; i++)
                {
                    Annotation anno = annos[i];
                    if (anno instanceof Target)
                    {
                        if (((Target) anno).value()[0] == ElementType.ANNOTATION_TYPE)
                        {
                            filter = new MetaAnnotationTypeFilter(annotationClass, classLoader);
                        }
                    }
                }
                if (filter == null)
                {
                    filter = new AnnotationTypeFilter(annotationClass);
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
