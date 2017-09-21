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
import static org.mule.module.ws.consumer.WSDLUtils.getSchemas;
import static org.mule.util.ClassUtils.getClassPathRoot;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import org.junit.Test;

public class WSDLUtilsTest
{

    public static final Pattern SCHEMA_LOCATION = compile("schemaLocation=\"([^\"]*)\"");
    
    @Test
    public void testSchemasAbsolutePath() throws Exception
    {
        WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
        String testRoot = getClassPathRoot(WSDLUtilsTest.class).getPath();
        Definition wsdlDefinition = wsdlReader.readWSDL(testRoot + "TestIncludedTypes.wsdl");
        List<String> schemas = getSchemas(wsdlDefinition);
        for (String schema : schemas)
        {
            Matcher matcher = SCHEMA_LOCATION.matcher(schema);
            assertThat(matcher.find(), equalTo(true));
            String file = matcher.group(1);
            assertThat(file, startsWith("file:"));
        }
    }
}
