/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

/**
 * Note: this test will fail if off-line.
 */
public class SchemaValidationMule2225TestCase extends AbstractMuleTestCase
{

    // not available in 1.4 constants?
    public static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    /**
     * If this fails for you, check if you are using JDK 1.5. if so make sure you build from maven with the
     * -Pjava14 profile flag
     * @throws SAXException
     * @throws IOException
     */
    public void testValidation() throws SAXException, IOException
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XML_SCHEMA);
        schemaFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
        Source muleXsd = new StreamSource(load("META-INF/mule.xsd"));
        Schema schema = schemaFactory.newSchema(muleXsd);
        Source muleRootTestXml = new StreamSource(load("org/mule/test/spring/mule-root-test.xml"));
        schema.newValidator().validate(muleRootTestXml);
    }

    protected InputStream load(String name) throws IOException
    {
        InputStream stream = IOUtils.getResourceAsStream(name, getClass());
        assertNotNull("Cannot load " + name, stream);
        return stream;
    }

}
