/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.spring;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertNotNull;

/**
 * Note: this test will fail if off-line.
 */
public class SchemaValidationMule2225TestCase extends AbstractMuleTestCase
{

    /**
     * This test will fail when run with plain JDK 1.4 or even 1.5 - schema validation
     * requires a proper JAXP installation in the JDK's endorsed directory. It works fine
     * with Xerces 2.9.1 (as with mule 1.4.x) or a manually installed JAXP Sun RI on JDK
     * 1.5; JDK 1.6 works out of the box.
     */
    @Test
    public void testValidation() throws SAXException, IOException
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
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
