/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
