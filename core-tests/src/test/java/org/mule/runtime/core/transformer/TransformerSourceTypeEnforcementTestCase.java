/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.DataTypeFactory;

import com.google.common.base.Charsets;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class TransformerSourceTypeEnforcementTestCase extends AbstractMuleTestCase
{

    private MuleContext muleContext = mock(MuleContext.class);
    private MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);

    @Before
    public void setUp() throws Exception
    {
        when(muleConfiguration.getDefaultEncoding()).thenReturn(Charsets.UTF_8.name());
        when(muleConfiguration.useExtendedTransformations()).thenReturn(true);
        when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    }

    @Test
    public void ignoresBadInputIfEnforcementOff() throws TransformerException
    {
        AbstractTransformer transformer = createDummyTransformer(true);

        setTransformationEnforcement(false);

        Object result = transformer.transform("TEST");
        assertEquals("TEST", result);

        Mockito.verify(muleConfiguration, times(1)).useExtendedTransformations();
    }

    @Test
    public void rejectsBadInputIfEnforcementOn() throws TransformerException
    {
        AbstractTransformer transformer = createDummyTransformer(true);

        setTransformationEnforcement(true);

        try
        {
            transformer.transform("TEST");
            fail("Transformation should fail because source type is not supported");
        }
        catch (TransformerException expected)
        {
        }

        Mockito.verify(muleConfiguration, times(1)).useExtendedTransformations();
    }

    @Test
    public void rejectsBadInputUsingDefaultEnforcement() throws TransformerException
    {
        AbstractTransformer transformer = createDummyTransformer(true);

        try
        {
            transformer.transform("TEST");
            fail("Transformation should fail because source type is not supported");
        }
        catch (TransformerException expected)
        {
        }
    }

    @Test
    public void transformsValidSourceTypeWithNoCheckForEnforcement() throws TransformerException
    {
        AbstractTransformer transformer = createDummyTransformer(true);
        transformer.sourceTypes.add(DataTypeFactory.STRING);
        transformer.returnType = DataTypeFactory.STRING;

        when(muleContext.getConfiguration()).thenReturn(muleConfiguration);

        Object result = transformer.transform("TEST");
        assertEquals("TRANSFORMED", result);

        Mockito.verify(muleConfiguration, times(0)).useExtendedTransformations();
    }

    private void setTransformationEnforcement(boolean enforce)
    {
        when(muleConfiguration.useExtendedTransformations()).thenReturn(enforce);
        when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    }

    private AbstractTransformer createDummyTransformer(boolean ignoreBadInput)
    {
        AbstractTransformer result = new AbstractTransformer()
        {

            @Override
            protected Object doTransform(Object src, String enc) throws TransformerException
            {
                return "TRANSFORMED";
            }
        };

        result.sourceTypes.add(DataTypeFactory.BYTE_ARRAY);
        result.setMuleContext(muleContext);
        result.setIgnoreBadInput(ignoreBadInput);

        return result;
    }
}
