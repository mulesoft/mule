/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan.annotations;

import org.mule.util.scan.ClassScanner;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Scans a single class and registers all annotations on the class in four collections; class annotations, field annotations
 * method annotations and parameter annotations.
 *
 * This scanner can process interfaces, implementation classes and annotation classes.  The scanner uses ASM to read the class
 * bytecode, removing the need to actally load the class which would be expensive.
 *
 * @deprecated: As ASM 3.3.1 is not fully compliant with Java 8, this class has been deprecated, however you can still use it under Java 7.
 */
@Deprecated
public class AnnotationsScanner extends EmptyVisitor implements ClassScanner
{
    protected final Log log = LogFactory.getLog(getClass());

    private List<AnnotationInfo> classAnnotations = new ArrayList<AnnotationInfo>();
    private List<AnnotationInfo> fieldAnnotations = new ArrayList<AnnotationInfo>();
    private List<AnnotationInfo> methodAnnotations = new ArrayList<AnnotationInfo>();
    private List<AnnotationInfo> paramAnnotations = new ArrayList<AnnotationInfo>();

    protected AnnotationInfo currentAnnotation;

    protected static final int PROCESSING_FIELD = 1;
    protected static final int PROCESSING_METHOD = 2;
    protected static final int PROCESSING_CLASS = 3;
    protected static final int PROCESSING_PARAM = 4;

    protected BitSet currentlyProcessing = new BitSet(4);

    private AnnotationFilter filter;

    private BitSet lastProcessing = null;

    private String className;

    private boolean match;

    public AnnotationsScanner()
    {
        super();
    }

    public AnnotationsScanner(AnnotationFilter filter)
    {
        this.filter = filter;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible)
    {
        currentAnnotation = new AnnotationInfo();
        currentAnnotation.setClassName(getAnnotationClassName(desc));
        currentlyProcessing.set(PROCESSING_PARAM);
        if (log.isDebugEnabled())
        {
            log.debug("Parameter Annotation: " + getAnnotationClassName(desc));
        }
        return this;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible)
    {
        // are we processing anything currently? If not w'ere looking at another annotation on the same class element
        if (currentlyProcessing.nextSetBit(0) < 0)
        {
            if(lastProcessing!=null)
            {
                currentlyProcessing = lastProcessing;
            }
            else
            {
                return this;
            }
        }

        currentAnnotation = new AnnotationInfo();
        currentAnnotation.setClassName(getAnnotationClassName(desc));
        if (log.isDebugEnabled())
        {
            log.debug("Annotation: " + getAnnotationClassName(desc));
        }

        return this;
    }


    /**
     * This is the class entry.
     */
    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces)
    {
        currentlyProcessing.set(PROCESSING_CLASS);
        className = name;
    }

    /**
     * We get annotation values in this method, but have to track the current context.
     */
    @Override
    public void visit(String name, Object value)
    {
        if (currentAnnotation != null)
        {
            currentAnnotation.getParams().add(new AnnotationInfo.NameValue(name, value));
        }
        if (log.isDebugEnabled())
        {
            // won't really output nicely with multithreaded parsing
            log.debug("          : " + name + "=" + value);
        }
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value)
    {
        currentlyProcessing.set(PROCESSING_FIELD);
        return this;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions)
    {
        currentlyProcessing.set(PROCESSING_METHOD);
        return this;
    }


    @Override
    public void visitEnd()
    {
        if (currentAnnotation != null)
        {

            if(filter!=null && !filter.accept(currentAnnotation))
            {
                currentlyProcessing.clear();
                currentAnnotation = null;
                return;
            }
            if (currentlyProcessing.get(PROCESSING_CLASS))
            {
                classAnnotations.add(currentAnnotation);
                match = true;
            }
            if (currentlyProcessing.get(PROCESSING_FIELD))
            {
                fieldAnnotations.add(currentAnnotation);
                match = true;
            }
            else if (currentlyProcessing.get(PROCESSING_PARAM))
            {
                paramAnnotations.add(currentAnnotation);
                match = true;
            }
            else if (currentlyProcessing.get(PROCESSING_METHOD))
            {
                methodAnnotations.add(currentAnnotation);
                match = true;                
            }
            currentAnnotation = null;
        }
        lastProcessing = (BitSet)currentlyProcessing.clone();
        currentlyProcessing.clear();

    }

    public String getAnnotationClassName(String rawName)
    {
        return rawName.substring(1, rawName.length() - 1).replace('/', '.');
    }

    public List<AnnotationInfo> getClassAnnotations()
    {
        return classAnnotations;
    }

    public List<AnnotationInfo> getFieldAnnotations()
    {
        return fieldAnnotations;
    }

    public List<AnnotationInfo> getMethodAnnotations()
    {
        return methodAnnotations;
    }

    public List<AnnotationInfo> getParamAnnotations()
    {
        return paramAnnotations;
    }

    public List<AnnotationInfo> getAllAnnotations()
    {
        List<AnnotationInfo> list = new ArrayList<AnnotationInfo>();
        list.addAll(classAnnotations);
        list.addAll(fieldAnnotations);
        list.addAll(methodAnnotations);
        list.addAll(paramAnnotations);
        return list;
    }

    public boolean isMatch()
    {
        return match;
    }

    public String getClassName()
    {
        return className;
    }
}
