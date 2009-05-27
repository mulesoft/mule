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

    public MetaAnnotationTypeFilter(Class<? extends Annotation> annotation)
    {
        this.annotation = annotation;
    }

    public boolean accept(AnnotationInfo info)
    {
        try
        {
            ClassReader r = new ClassReader(info.getClassName());
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
}