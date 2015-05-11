/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.extension.internal.resources.ExtensionResourcesGeneratorAnnotationProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.IOUtils;

import com.google.common.io.ByteSource;
import com.google.testing.compile.JavaFileObjects;

import java.io.File;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.xpath.XPathFactoryImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

@SmallTest
public class ExtensionResourcesGeneratorAnnotationProcessorTestCase extends AbstractMuleTestCase
{

    private static final String GROUP_PARAMETER_1 = "Group parameter 1";
    private static final String GROUP_PARAMETER_2 = "Group parameter 2";

    private XPath xpath;
    private DocumentBuilderFactory builderFactory;

    @Before
    public void before() throws Exception
    {
        XPathFactory xpathFactory = new XPathFactoryImpl();
        xpath = xpathFactory.newXPath();

        builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
    }

    @Test
    public void generateDocumentedSchema() throws Exception
    {
        ArgumentCaptor<ByteSource> byteSourceCaptor = ArgumentCaptor.forClass(ByteSource.class);
        ByteSource byteSource = mock(ByteSource.class);
        when(byteSource.contentEquals(byteSourceCaptor.capture())).thenReturn(true);

        assert_().about(javaSources())
                .that(testSourceFiles())
                .processedWith(new ExtensionResourcesGeneratorAnnotationProcessor())
                .compilesWithoutError()
                .and().generatesFileNamed(StandardLocation.SOURCE_OUTPUT, "", "mule-documentation.xsd")
                .withContents(byteSource);

        ByteSource generatedByteSource = byteSourceCaptor.getValue();
        assertThat(generatedByteSource, is(notNullValue()));
        String generatedSchema = IOUtils.toString(generatedByteSource.openStream());

        assertXpath(generatedSchema, "//xs:attribute[@name='configParameter']/xs:annotation/xs:documentation", "Config parameter");
        assertXpath(generatedSchema, "//xs:attribute[@name='value1']/xs:annotation/xs:documentation", GROUP_PARAMETER_1);
        assertXpath(generatedSchema, "//xs:attribute[@name='value2']/xs:annotation/xs:documentation", GROUP_PARAMETER_2);


        assertXpath(generatedSchema, "//xs:element[@name='operation']/xs:annotation/xs:documentation", "Test Operation");
        assertXpath(generatedSchema, "//xs:complexType[@name='OperationType']/xs:complexContent/xs:extension/xs:attribute[@name='value']/xs:annotation/xs:documentation", "test value");
        assertXpath(generatedSchema, "//xs:complexType[@name='OperationType']/xs:complexContent/xs:extension/xs:attribute[@name='value1']/xs:annotation/xs:documentation", GROUP_PARAMETER_1);
        assertXpath(generatedSchema, "//xs:complexType[@name='OperationType']/xs:complexContent/xs:extension/xs:attribute[@name='value2']/xs:annotation/xs:documentation", GROUP_PARAMETER_2);
    }

    private void assertXpath(String input, String expression, String expected) throws Exception
    {
        assertThat(xpath(input, expression), is(expected));
    }

    private String xpath(String input, String expression) throws Exception
    {
        Node node = builderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(input)));
        return (String) xpath.evaluate(expression, node, XPathConstants.STRING);
    }


    private Iterable<JavaFileObject> testSourceFiles() throws Exception
    {
        // this will be xxx/target/test-classes
        File folder = new File(getClass().getClassLoader().getResource("").getPath().toString());

        // up to levels
        folder = folder.getParentFile().getParentFile();

        folder = new File(folder, "src/test/java/" + getClass().getPackage().getName().replaceAll("\\.", "/"));

        File[] files = folder.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".java");
            }
        });

        List<JavaFileObject> javaFileObjects = new ArrayList<>(files.length);
        for (File file : files)
        {
            javaFileObjects.add(JavaFileObjects.forResource(file.toURI().toURL()));
        }

        return javaFileObjects;
    }
}
