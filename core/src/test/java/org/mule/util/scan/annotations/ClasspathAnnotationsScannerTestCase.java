/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.scan.annotations;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.objectweb.asm.ClassReader;

import static org.junit.Assert.assertEquals;

@SmallTest
public class ClasspathAnnotationsScannerTestCase extends AbstractMuleTestCase
{

    @Test
    public void testScanAnnotationsWithFilter() throws Exception
    {
        ClassReader r = new ClosableClassReader(SampleClassWithAnnotations.class.getName());
        AnnotationsScanner scanner = new AnnotationsScanner(new AnnotationTypeFilter(MultiMarker.class));

        r.accept(scanner, 0);

        assertEquals(1, scanner.getAllAnnotations().size());
    }

    @Test
    public void testScanMetaAnnotations() throws Exception
    {
        ClassReader r = new ClosableClassReader(SampleBeanWithAnnotations.class.getName());
        AnnotationsScanner scanner = new AnnotationsScanner(new MetaAnnotationTypeFilter(Meta.class));

        r.accept(scanner, 0);

        assertEquals(2, scanner.getMethodAnnotations().size());
    }
}
