/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan.annotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

@SmallTest
public class AnnotationsScannerTestCase extends AbstractMuleTestCase
{
    protected AnnotationsScanner scanner;

    @Before
    public void setUp() throws IOException
    {
        ClassReader r = new ClosableClassReader(SampleClassWithAnnotations.class.getName());
        scanner = new AnnotationsScanner();

        r.accept(scanner, 0);
    }

    @Test
    public void testParamAnnotations() throws Exception
    {
        final List<AnnotationInfo> paramAnnotations = scanner.getParamAnnotations();

        System.out.println("Parameter annotations: " + paramAnnotations);

        assertNotNull(paramAnnotations);
        assertEquals(2, paramAnnotations.size());

        // @Marker("ParamLevel")
        AnnotationInfo ann = paramAnnotations.get(0);
        assertEquals(Marker.class.getName(), ann.getClassName());
        List<AnnotationInfo.NameValue> annValues = ann.getParams();
        assertNotNull(annValues);
        assertEquals(1, annValues.size());
        assertEquals(new AnnotationInfo.NameValue("value", "ParamLevel"), annValues.get(0));

        // @MultiMarker(value = "ParamLevel", param1 = "12", param2 = "abc")
        ann = paramAnnotations.get(1);
        assertEquals(MultiMarker.class.getName(), ann.getClassName());
        annValues = ann.getParams();
        assertNotNull(annValues);
        assertEquals(3, annValues.size());
        assertEquals(new AnnotationInfo.NameValue("value", "ParamLevel"), annValues.get(0));
        assertEquals(new AnnotationInfo.NameValue("param1", "12"), annValues.get(1));
        assertEquals(new AnnotationInfo.NameValue("param2", "abc"), annValues.get(2));
    }

    @Test
    public void testFieldAnnotations() throws Exception
    {
        final List<AnnotationInfo> fieldAnnotations = scanner.getFieldAnnotations();

        System.out.println("Field annotations: " + fieldAnnotations);

        assertNotNull(fieldAnnotations);
        assertEquals(1, fieldAnnotations.size());

        // @Marker("FieldLevel")
        AnnotationInfo ann = fieldAnnotations.get(0);
        assertEquals(Marker.class.getName(), ann.getClassName());
        final List<AnnotationInfo.NameValue> annValues = ann.getParams();
        assertNotNull(annValues);
        assertEquals(1, annValues.size());
        assertEquals(new AnnotationInfo.NameValue("value", "FieldLevel"), annValues.get(0));
    }

    @Test
    public void testClassAnnotations() throws Exception
    {
        final List<AnnotationInfo> classAnnotations = scanner.getClassAnnotations();

        System.out.println("Class annotations: " + classAnnotations);

        assertNotNull(classAnnotations);
        assertEquals(1, classAnnotations.size());

        // @Marker("ClassLevel")
        AnnotationInfo ann = classAnnotations.get(0);
        assertEquals(Marker.class.getName(), ann.getClassName());
        final List<AnnotationInfo.NameValue> annValues = ann.getParams();
        assertNotNull(annValues);
        assertEquals(1, annValues.size());
        assertEquals(new AnnotationInfo.NameValue("value", "ClassLevel"), annValues.get(0));
    }

    @Test
    public void testMethodAnnotations() throws Exception
    {
        final List<AnnotationInfo> methodAnnotations = scanner.getMethodAnnotations();

        System.out.println("Method annotations: " + methodAnnotations);

        assertNotNull(methodAnnotations);
        assertEquals(2, methodAnnotations.size());

        // @Marker("MethodLevel / Main")
        AnnotationInfo ann = methodAnnotations.get(0);
        assertEquals(Marker.class.getName(), ann.getClassName());
        List<AnnotationInfo.NameValue> annValues = ann.getParams();
        assertNotNull(annValues);
        assertEquals(1, annValues.size());
        assertEquals(new AnnotationInfo.NameValue("value", "MethodLevel / Main"), annValues.get(0));

        // @Marker("MethodLevel / toString")
        ann = methodAnnotations.get(1);
        assertEquals(Marker.class.getName(), ann.getClassName());
        annValues = ann.getParams();
        assertNotNull(annValues);
        assertEquals(1, annValues.size());
        assertEquals(new AnnotationInfo.NameValue("value", "MethodLevel / toString"), annValues.get(0));
    }

}
