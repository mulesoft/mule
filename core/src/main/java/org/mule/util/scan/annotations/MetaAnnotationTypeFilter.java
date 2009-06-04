/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan.annotations;

import java.lang.annotation.Annotation;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.AnnotationVisitor;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Will filter for a meta annotation type specified as the annotation class.  Meta annotations are annotations on other
 * annotations.  this filter allows discovery of annotations on a class that have the same meta annotation.
 */
public class MetaAnnotationTypeFilter implements AnnotationFilter
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(MetaAnnotationTypeFilter.class);

    private Class<? extends Annotation> annotation;

    private ClassLoader classLoader;

    /**
     * Creates an Meta Annotation Filter that look for Meta annotation on an annotation class
     * @param annotation the annotation class to read
     * @param basepath the base path of the location of the class. Specifying this will be more
     * reliable in applications that use Multiple classloaders since ASM uses the system classloader
     * to read classes and thus ignores child classloaders.  The basepath should also start with a scheme,
     * usually file: or jar:
     */
    public MetaAnnotationTypeFilter(Class<? extends Annotation> annotation, ClassLoader classLoader)
    {
        this.annotation = annotation;
        this.classLoader = classLoader;
    }

    /**
     * Creates an Meta Annotation Filter that look for Meta annotation on an annotation class. This constructor
     * will cause the class reading to read from the System clssloader
     * @param annotation the annotation class to read
     */
    public MetaAnnotationTypeFilter(Class<? extends Annotation> annotation)
    {
        this.annotation = annotation;
        classLoader = Thread.currentThread().getContextClassLoader();
    }

    public boolean accept(AnnotationInfo info)
    {
        try
        {
            URL classUrl = getClassURL(info.getClassName());
            ClassReader r = new ClassReader(classUrl.openStream());
          
            MetaAnnotationScanner scanner = new MetaAnnotationScanner(new AnnotationTypeFilter(annotation));
            r.accept(scanner, 0);

            return scanner.getClassAnnotations().size() == 1;
        }
        catch (IOException e)
        {
            logger.debug(e);
            return false;
        }
    }

    private class MetaAnnotationScanner extends AnnotationsScanner
    {
        public MetaAnnotationScanner(AnnotationFilter filter)
        {
            super(filter);
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String desc, final boolean visible)
        {
            currentAnnotation = new AnnotationInfo();
            currentAnnotation.setClassName(getAnnotationClassName(desc));
            if (log.isDebugEnabled())
            {
                log.debug("Annotation: " + getAnnotationClassName(desc));
            }

            // are we processing anything currently?
            if (currentlyProcessing.nextSetBit(0) < 0)
            {
                // no, this is a meta annotation
                currentlyProcessing.set(PROCESSING_CLASS);
            }

            return this;
        }

    }

    public URL getClassURL(String className)
    {
        String resource = className.replace(".", "/") + ".class";
        return classLoader.getResource(resource);
    }
}