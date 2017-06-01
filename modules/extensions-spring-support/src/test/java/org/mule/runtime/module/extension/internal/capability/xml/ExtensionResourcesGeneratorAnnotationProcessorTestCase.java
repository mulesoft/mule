/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.module.extension.internal.AbstractAnnotationProcessorTestCase;
import org.mule.runtime.module.extension.internal.resources.ExtensionResourcesGeneratorAnnotationProcessor;
import org.mule.tck.size.SmallTest;

import com.google.common.io.ByteSource;

import java.io.StringReader;

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
public class ExtensionResourcesGeneratorAnnotationProcessorTestCase extends AbstractAnnotationProcessorTestCase {

  private static final String GROUP_PARAMETER_1 = "Group parameter 1";
  private static final String GROUP_PARAMETER_2 = "Group parameter 2";

  private XPath xpath;
  private DocumentBuilderFactory builderFactory;

  @Before
  public void before() throws Exception {
    XPathFactory xpathFactory = new XPathFactoryImpl();
    xpath = xpathFactory.newXPath();

    builderFactory = DocumentBuilderFactory.newInstance();
    builderFactory.setNamespaceAware(true);
  }

  @Test
  public void generateDocumentedSchema() throws Exception {
    ArgumentCaptor<ByteSource> byteSourceCaptor = ArgumentCaptor.forClass(ByteSource.class);
    ByteSource byteSource = mock(ByteSource.class);
    when(byteSource.contentEquals(byteSourceCaptor.capture())).thenReturn(true);

    assert_().about(javaSources()).that(testSourceFiles()).withCompilerOptions("-Aextension.version=1.0.0-dev")
        .processedWith(new ExtensionResourcesGeneratorAnnotationProcessor()).compilesWithoutError().and()
        .generatesFileNamed(StandardLocation.SOURCE_OUTPUT, "", "mule-documentation.xsd").withContents(byteSource);

    ByteSource generatedByteSource = byteSourceCaptor.getValue();
    assertThat(generatedByteSource, is(notNullValue()));
    String generatedSchema = IOUtils.toString(generatedByteSource.openStream());

    assertXpath(generatedSchema, "//xs:attribute[@name='configParameter']/xs:annotation/xs:documentation", "Config parameter");
    assertXpath(generatedSchema, "//xs:attribute[@name='configParameterWithComplexJavadoc']/xs:annotation/xs:documentation",
                "Config Parameter with an Optional value");
    assertXpath(generatedSchema, "//xs:attribute[@name='value1']/xs:annotation/xs:documentation", GROUP_PARAMETER_1);
    assertXpath(generatedSchema, "//xs:attribute[@name='value2']/xs:annotation/xs:documentation", GROUP_PARAMETER_2);


    assertXpath(generatedSchema, "//xs:element[@name='operation']/xs:annotation/xs:documentation", "Test Operation");
    assertXpath(generatedSchema,
                "//xs:complexType[@name='OperationType']/xs:complexContent/xs:extension/xs:attribute[@name='value']/xs:annotation/xs:documentation",
                "test value");
    assertXpath(generatedSchema,
                "//xs:complexType[@name='OperationType']/xs:complexContent/xs:extension/xs:attribute[@name='value1']/xs:annotation/xs:documentation",
                GROUP_PARAMETER_1);
    assertXpath(generatedSchema,
                "//xs:complexType[@name='OperationType']/xs:complexContent/xs:extension/xs:attribute[@name='value2']/xs:annotation/xs:documentation",
                GROUP_PARAMETER_2);

    assertXpath(generatedSchema, "//xs:element[@name='ignore-operation-should-be-ignored']/xs:annotation/xs:documentation", "");
    assertXpath(generatedSchema, "//xs:element[@name='private-operation-should-be-ignored']/xs:annotation/xs:documentation", "");

    assertXpath(generatedSchema,
                "//xs:element[@name='operation-with-blank-parameter-description']/xs:annotation/xs:documentation",
                "Test Operation with blank parameter description");
    assertXpath(generatedSchema,
                "//xs:complexType[@name='OperationWithBlankParameterDescriptionType']/xs:complexContent/xs:extension/xs:attribute[@name='value']/xs:annotation/xs:documentation",
                "");

    assertXpath(generatedSchema, "//xs:element[@name='operation-with-javadoc-link-references']/xs:annotation/xs:documentation",
                "Operation that returns a String value");
    assertXpath(generatedSchema,
                "//xs:complexType[@name='OperationWithJavadocLinkReferencesType']/xs:complexContent/xs:extension/xs:attribute[@name='value']/xs:annotation/xs:documentation",
                "this is the String to be returned");
  }

  private void assertXpath(String input, String expression, String expected) throws Exception {
    assertThat(xpath(input, expression), is(expected));
  }

  private String xpath(String input, String expression) throws Exception {
    Node node = builderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(input)));
    return (String) xpath.evaluate(expression, node, XPathConstants.STRING);
  }
}
