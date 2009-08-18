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

import junit.framework.TestCase;
import org.objectweb.asm.ClassReader;

public class ClasspathAnnotationsScannerTestCase extends TestCase
{
    public void testScanAnnotationsWithFilter() throws Exception
    {
        ClassReader r = new ClosableClassReader(SampleClassWithAnnotations.class.getName());
        AnnotationsScanner scanner = new AnnotationsScanner(new AnnotationTypeFilter(MultiMarker.class));

        r.accept(scanner, 0);

        assertEquals(1, scanner.getAllAnnotations().size());
    }

    public void testScanMetaAnnotations() throws Exception
    {
        ClassReader r = new ClosableClassReader(SampleBeanWithAnnotations.class.getName());
        AnnotationsScanner scanner = new AnnotationsScanner(new MetaAnnotationTypeFilter(Meta.class));

        r.accept(scanner, 0);

        assertEquals(2, scanner.getMethodAnnotations().size());
    }
}
