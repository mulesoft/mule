/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.xmlsecurity;

import static java.util.Arrays.asList;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.runtime.core.api.util.xmlsecurity.XMLSecureFactories;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.List;

import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class XMLSecureFactoriesPropertiesTestCase extends AbstractMuleTestCase {

  private static final List<String> FACTORY_ATTRIBUTES = asList(ACCESS_EXTERNAL_STYLESHEET, ACCESS_EXTERNAL_DTD);
  private static final List<String> VALIDATOR_PROPERTIES = asList(ACCESS_EXTERNAL_SCHEMA, ACCESS_EXTERNAL_DTD);
  private static final List<String> SCHEMA_FACTORY_PROPERTIES = asList(ACCESS_EXTERNAL_SCHEMA, ACCESS_EXTERNAL_DTD);

  private final DefaultXMLSecureFactories defaultXMLSecureFactories = new DefaultXMLSecureFactories(false, false);
  private final SchemaFactory schemaFactory =
      XMLSecureFactories.createDefault().getSchemaFactory("http://www.w3.org/2001/XMLSchema");
  private final TransformerFactory transformerFactory = XMLSecureFactories.createDefault().getTransformerFactory();
  private final Validator validatorWrapper = mock(Validator.class);
  private final SchemaFactory schemaFactoryWrapper = mock(SchemaFactory.class);
  private final TransformerFactory transformerFactoryWrapper = mock(TransformerFactory.class);

  @Test
  public void validatorProperties() throws Exception {
    SetPropertyAnswer setPropertyAnswer = new SetPropertyAnswer(schemaFactory.newSchema().newValidator());
    doAnswer(setPropertyAnswer).when(validatorWrapper).setProperty(anyString(), anyObject());
    defaultXMLSecureFactories.configureValidator(validatorWrapper);
    assertThat(setPropertyAnswer.exception, is(nullValue()));
    for (String property : VALIDATOR_PROPERTIES) {
      verify(validatorWrapper).setProperty(property, "");
    }
  }

  @Test
  public void schemaFactoryProperties() throws Exception {
    SetPropertyAnswer setPropertyAnswer = new SetPropertyAnswer(schemaFactory);
    doAnswer(setPropertyAnswer).when(schemaFactoryWrapper).setProperty(anyString(), anyObject());
    defaultXMLSecureFactories.configureSchemaFactory(schemaFactoryWrapper);
    assertThat(setPropertyAnswer.exception, is(nullValue()));
    for (String property : SCHEMA_FACTORY_PROPERTIES) {
      verify(schemaFactoryWrapper).setProperty(property, "");
    }
  }

  @Test
  public void transformerFactoryProperties() {
    SetPropertyAnswer setPropertyAnswer = new SetPropertyAnswer(transformerFactory);
    doAnswer(setPropertyAnswer).when(transformerFactoryWrapper).setAttribute(anyString(), anyObject());
    defaultXMLSecureFactories.configureTransformerFactory(transformerFactoryWrapper);
    assertThat(setPropertyAnswer.exception, is(nullValue()));
    for (String property : FACTORY_ATTRIBUTES) {
      verify(transformerFactoryWrapper).setAttribute(property, "");
    }
  }

  private class SetPropertyAnswer implements Answer<Void> {

    private final Object propertySetter;
    private Exception exception = null;

    SetPropertyAnswer(Object propertySetter) {
      this.propertySetter = propertySetter;
    }

    @Override
    public Void answer(InvocationOnMock invocation) {
      String name = (String) invocation.getArguments()[0];
      Object value = invocation.getArguments()[1];
      try {
        if (propertySetter instanceof SchemaFactory) {
          ((SchemaFactory) propertySetter).setProperty(name, value);
        } else if (propertySetter instanceof Validator) {
          ((Validator) propertySetter).setProperty(name, value);
        } else if (propertySetter instanceof TransformerFactory) {
          ((TransformerFactory) propertySetter).setAttribute(name, value);
        } else {
          throw new IllegalArgumentException("Invalid property setter.");
        }
      } catch (Exception e) {
        exception = e;
      }

      return null;
    }
  }

}
