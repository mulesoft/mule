/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.ClassUtils;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class SchemaDefaultsTestCase extends AbstractMuleTestCase
{
    private static String MULE_CORE_SCHEMA_FILE = "META-INF/mule.xsd";
    private Document schema;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        SAXReader reader = new SAXReader();
        schema = reader.read(ClassUtils.getResource("META-INF/mule.xsd", this.getClass()).openStream());
    }

    public void testConfigurationDefaults()
    {
        Element configurationType = (Element) schema.selectSingleNode("/xsd:schema/xsd:complexType[@name='configurationType']");

        assertEquals(Boolean.toString(muleContext.getConfiguration().isDefaultSynchronousEndpoints()),
            configurationType.selectSingleNode("xsd:attribute[@name='defaultSynchronousEndpoints']/@default")
                .getStringValue());
        assertEquals(Boolean.toString(muleContext.getConfiguration().isDefaultRemoteSync()),
            configurationType.selectSingleNode("xsd:attribute[@name='defaultRemoteSync']/@default")
                .getStringValue());
        assertEquals(muleContext.getConfiguration().getDefaultSynchronousEventTimeout(),
            configurationType.numberValueOf("xsd:attribute[@name='defaultSynchronousEventTimeout']/@default")
                .intValue());
        assertEquals(muleContext.getConfiguration().getDefaultTransactionTimeout(),
            configurationType.numberValueOf("xsd:attribute[@name='defaultTransactionTimeout']/@default")
                .intValue());

    }

}
