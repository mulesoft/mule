/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.Part;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;

public class WSConsumerTestCase extends AbstractMuleTestCase
{

    @Test
    public void test() throws WSDLException, TransformerException, MalformedURLException, IOException
    {
        javax.wsdl.xml.WSDLReader wsdlReader11 = javax.wsdl.factory.WSDLFactory.newInstance().newWSDLReader();
        Definition def = wsdlReader11.readWSDL("src/test/resources/weather-forecaster.wsdl");
        System.out.println(getSchemas(def));
        System.out.println("--------------------------------");
        System.out.println(((Part) def.getPortType(new QName("http://tests.mule.org/", "WeatherForecaster"))
            .getOperation("GetWeatherByZipCode", null, null)
            .getInput()
            .getMessage()
            .getParts()
            .values()
            .iterator()
            .next()));
    }

    private List<String> getSchemas(Definition def)
        throws WSDLException, TransformerConfigurationException, TransformerFactoryConfigurationError,
        TransformerException
    {
        List<String> schemas = new ArrayList<String>();
        for (Object o : def.getTypes().getExtensibilityElements())
        {
            if (o instanceof javax.wsdl.extensions.schema.Schema)
            {
                Schema schema = (Schema) o;

                schemas.add(schemaToString(schema));

                for (Object location : schema.getIncludes())
                {
                    schemas.add(schemaToString(((SchemaReference) location).getReferencedSchema()));
                }
            }
        }
        return schemas;
    }

    private String schemaToString(Schema schema)
        throws TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException
    {
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(schema.getElement()), new StreamResult(writer));
        return writer.toString();
    }
}
