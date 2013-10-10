/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.schemadocs;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PathTestCase extends AbstractMuleTestCase
{
    @Test
    public void testTagFromFileName()
    {
        assertEquals("quartz",
                SchemaDocsMain.tagFromFileName("/opt/j2ee/data/bamboo/xml-data/build-dir/MULE-MULEV20X/transports/quartz/target/mule-transport-quartz-2.0.0-RC3-SNAPSHOT.jar!/META-INF/mule-quartz.xsd"));
        assertEquals("foo-bar",
                SchemaDocsMain.tagFromFileName("/opt/j2ee/data/bamboo/xml-data/build-dir/MULE-MULEV20X/transports/quartz/target/mule-transport-quartz-2.0.0-RC3-SNAPSHOT.jar!/META-INF/mule-foo-bar.xsd"));
    }
}
