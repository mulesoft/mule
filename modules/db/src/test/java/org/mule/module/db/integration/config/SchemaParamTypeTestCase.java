/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Checks that every type defined in JdbcTypes schema type can be used on a template parameter
 */
public class SchemaParamTypeTestCase  extends AbstractParamTypeTestCase
{

    public SchemaParamTypeTestCase(String dbTypeName)
    {
        super(dbTypeName);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        List<Object[]> parameterSets = new ArrayList<Object[]>();

        for (String dbType : getSchemaJdbcTypes())
        {
            parameterSets.add(new Object[] {dbType});
        }

        return parameterSets;
    }

    private static List<String> getSchemaJdbcTypes()
    {
        try
        {
            List<String> schemaTypes = new ArrayList<String>();

            Document doc = parseSchema("/META-INF/mule-db.xsd");

            Element jdbcTypes = findSimpleType(doc, "JdbcDataTypes");

            NodeList childNodes = jdbcTypes.getElementsByTagName("xsd:restriction");
            Element type = (Element) childNodes.item(0);

            NodeList enums = type.getElementsByTagName("xsd:enumeration");
            for (int i = 0; i < enums.getLength(); i++)
            {
                Element currentElement = (Element) enums.item(i);

                schemaTypes.add(currentElement.getAttribute("value"));
            }

            return schemaTypes;
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to configure parameterized test", e);
        }
    }

    private static Element findSimpleType(Document doc, String typeName)
    {
        NodeList simpleTypes = doc.getElementsByTagName("xsd:simpleType");

        for (int i = 0; i < simpleTypes.getLength(); i++)
        {
            Element simpleType = (Element) simpleTypes.item(i);

            if (typeName.equals(simpleType.getAttribute("name")))
            {
                return simpleType;
            }
        }

        throw new IllegalStateException(String.format("Unable to locate element for simple type '%s", typeName));
    }

    private static Document parseSchema(String schema) throws Exception
    {
        URL resource = SchemaParamTypeTestCase.class.getResource(schema);
        File file = new File(resource.toURI());
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        docBuilder = docBuilderFactory.newDocumentBuilder();

        return docBuilder.parse(file);
    }
}