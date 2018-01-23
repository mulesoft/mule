/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.xmlsecurity;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mule.tck.junit4.AbstractMuleTestCase;

import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class XMLSecureFactoriesPropertiesTestCase extends AbstractMuleTestCase
{

    private final DefaultXMLSecureFactories defaultXMLSecureFactories = new DefaultXMLSecureFactories(false, false);
    private final SchemaFactory schemaFactory = XMLSecureFactories.createDefault().getSchemaFactory("http://www.w3.org/2001/XMLSchema");
    private final TransformerFactory transformerFactory = XMLSecureFactories.createDefault().getTransformerFactory();
    private final Validator validatorWrapper = mock(Validator.class);
    private final SchemaFactory schemaFactoryWrapper = mock(SchemaFactory.class);
    private final TransformerFactory transformerFactoryWrapper = mock(TransformerFactory.class);

    @Test
    public void validatorProperties() throws Exception
    {
        SetPropertyAnswer setPropertyAnswer = new SetPropertyAnswer(schemaFactory.newSchema().newValidator());
        doAnswer(setPropertyAnswer).when(validatorWrapper).setProperty(anyString(), anyObject());
        defaultXMLSecureFactories.configureValidator(validatorWrapper);
        assertThat(setPropertyAnswer.exception, is(nullValue()));
        verify(validatorWrapper, times(2)).setProperty(anyString(), anyObject());
    }

    @Test
    public void schemaFactoryProperties() throws Exception
    {
        SetPropertyAnswer setPropertyAnswer = new SetPropertyAnswer(schemaFactory);
        doAnswer(setPropertyAnswer).when(schemaFactoryWrapper).setProperty(anyString(), anyObject());
        defaultXMLSecureFactories.configureSchemaFactory(schemaFactoryWrapper);
        assertThat(setPropertyAnswer.exception, is(nullValue()));
        verify(schemaFactoryWrapper, times(2)).setProperty(anyString(), anyObject());
    }

    @Test
    public void transformerFactoryProperties()
    {
        SetPropertyAnswer setPropertyAnswer = new SetPropertyAnswer(transformerFactory);
        doAnswer(setPropertyAnswer).when(transformerFactoryWrapper).setAttribute(anyString(), anyObject());
        defaultXMLSecureFactories.configureTransformerFactory(transformerFactoryWrapper);
        assertThat(setPropertyAnswer.exception, is(nullValue()));
        verify(transformerFactoryWrapper, times(2)).setAttribute(anyString(), anyObject());
    }

    private class SetPropertyAnswer implements Answer<Void>
    {

        private final Object propertySetter;
        private Exception exception = null;

        SetPropertyAnswer(Object propertySetter)
        {
            this.propertySetter = propertySetter;
        }

        @Override
        public Void answer(InvocationOnMock invocation)
        {
            String name = (String) invocation.getArguments()[0];
            Object value = invocation.getArguments()[1];
            try
            {
                if (propertySetter instanceof SchemaFactory)
                {
                    ((SchemaFactory) propertySetter).setProperty(name, value);
                }
                else if (propertySetter instanceof Validator)
                {
                    ((Validator) propertySetter).setProperty(name, value);
                }
                else if (propertySetter instanceof TransformerFactory)
                {
                    ((TransformerFactory) propertySetter).setAttribute(name, value);
                }
                else
                {
                    throw new IllegalArgumentException("Invalid property setter.");
                }
            }
            catch (Exception e)
            {
                exception = e;
            }

            return null;
        }
    }

}