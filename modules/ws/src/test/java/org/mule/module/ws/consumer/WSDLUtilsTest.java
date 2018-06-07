/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import static java.util.regex.Pattern.compile;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.core.StringContains.containsString;
import static org.mule.module.ws.consumer.WSDLUtils.getSchemas;
import static org.mule.util.ClassUtils.getClassPathRoot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import org.junit.Test;

public class WSDLUtilsTest
{

    private static final String TEST_SCHEMAS = "TestSchemasWithBareSchemaLocationFile.wsdl";
    private static final String TEST_SCHEMAS_SELF_REFERENCE = "TestSchemasWithSelfReference.wsdl";
    private static final String FILE_PREFIX = "file:";
    private static final String IMPORTED_SCHEMA = "TestSchema.xsd";
    public static final Pattern SCHEMA_LOCATION = compile("schemaLocation=\"([^\"]*)\"");

    /**
     * This tests verifies that the schema location is a bare XSD file
     * and the schemas resulting from extracting those XSD files is
     * an URI prefixed by file:. That implies that the bare
     * schema location was converted to an absolute URI.
     * 
     * @throws Exception exception during test
     */
    @Test
    public void testSchemasAbsolutePath() throws Exception
    {
        WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
        String testRoot = getClassPathRoot(WSDLUtilsTest.class).getPath();
        Definition wsdlDefinition = wsdlReader.readWSDL(testRoot + TEST_SCHEMAS);
        validateProcessedWsdl(wsdlDefinition);

        List<String> schemas = getSchemas(wsdlDefinition);
        for (String schema : schemas)
        {
            Matcher matcher = SCHEMA_LOCATION.matcher(schema);
            assertThat(matcher.find(), equalTo(true));
            String file = matcher.group(1);
            assertThat(file, startsWith(FILE_PREFIX));
            assertThat(file, containsString(IMPORTED_SCHEMA));
        }
    }

    @Test
    public void testSchemasWithSelfReference() throws Exception
    {
        WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
        String testRoot = getClassPathRoot(WSDLUtilsTest.class).getPath();
        Definition wsdlDefinition = wsdlReader.readWSDL(testRoot + TEST_SCHEMAS_SELF_REFERENCE);

        List<String> schemas = getSchemas(wsdlDefinition);
        for (String schema : schemas)
        {
            Matcher matcher = SCHEMA_LOCATION.matcher(schema);
            assertThat(matcher.find(), equalTo(true));
            String file = matcher.group(1);
            assertThat(file, startsWith(FILE_PREFIX));
            assertThat(file, containsString(IMPORTED_SCHEMA));
        }
    }

    private void validateProcessedWsdl(Definition wsdlDefinition)
    {
        List<Types> typesList = new ArrayList<Types>();
        typesList.add(wsdlDefinition.getTypes());

        for (Types types : typesList)
        {
            for (Object o : types.getExtensibilityElements())
            {
                if (o instanceof javax.wsdl.extensions.schema.Schema)
                {
                    validateCurrentSchema(o);
                }
            }
        }
    }

    private void validateCurrentSchema(Object o)
    {
        Schema schema = (Schema) o;
        Collection<List<SchemaReference>> schemaImportsCollection = schema.getImports().values();
        for (List<SchemaReference> schemaReferences : schemaImportsCollection)
        {
            for (SchemaReference schemaReference : schemaReferences)
            {
                String schemaLocationURI = schemaReference.getSchemaLocationURI();
                assertThat(schemaLocationURI, equalTo(IMPORTED_SCHEMA));
            }
        }
    }
}
