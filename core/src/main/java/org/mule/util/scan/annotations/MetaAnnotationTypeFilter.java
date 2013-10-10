/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.scan.annotations;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;

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
     * Creates an Meta Annotation Filter that look for Meta annotation on an
     * annotation class
     *
     * @param annotation the annotation class to read
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
            if (classUrl == null)
            {
                logger.debug("Failed to load annotation class: " + info);
                return false;
            }

            InputStream classStream = classUrl.openStream();
            ClassReader r = new ClosableClassReader(classStream);

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
