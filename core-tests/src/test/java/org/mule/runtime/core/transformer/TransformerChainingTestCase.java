/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.TransformerMessagingException;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

public class TransformerChainingTestCase extends AbstractMuleTestCase
{

    private MuleContext muleContext = mock(MuleContext.class);
    private MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    private TransformationService transformationService;

    @Before
    public void setUp() throws Exception
    {
        when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
        transformationService = new TransformationService(muleContext);
    }

    @Test
    public void testSingleChainedTransformer() throws Exception
    {
        AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
        assertNotNull(validTransformer);
        
        MuleMessage message = new DefaultMuleMessage(new Integer(0), muleContext);
        Transformer messageTransformer = new TransformerChain(validTransformer);
        message = transformationService.applyTransformers(message, null, messageTransformer);

        Object transformedMessage = message.getPayload();
        assertNotNull(transformedMessage);
        assertEquals(new Integer(1), transformedMessage);
    }

    @Test
    public void testTwoChainedTransformers() throws Exception
    {
        AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
        assertNotNull(validTransformer);
        
        DefaultMuleMessage message = new DefaultMuleMessage(new Integer(0), muleContext);
        Transformer messageTransformer = new TransformerChain(validTransformer, validTransformer);
        message = (DefaultMuleMessage) transformationService.applyTransformers(message, null, singletonList(messageTransformer));

        Object transformedMessage = message.getPayload();
        assertNotNull(transformedMessage);
        assertEquals(new Integer(2), transformedMessage);
    }

    @Test
    public void testThreeChainedTransformers() throws Exception
    {
        AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
        assertNotNull(validTransformer);
        
        MuleMessage message = new DefaultMuleMessage(new Integer(0), muleContext);
        Transformer messageTransformer = new TransformerChain(validTransformer, validTransformer, validTransformer);
        message = transformationService.applyTransformers(message, null, messageTransformer);

        Object transformedMessage = message.getPayload();
        assertNotNull(transformedMessage);
        assertEquals(new Integer(3), transformedMessage);
    }

    @Test(expected = TransformerMessagingException.class)
    public void testIgnoreBadInputBreaksWithTransformationOrderInvalidValidWhenEnforcedOn() throws Exception
    {
        AbstractTransformer invalidTransformer = (AbstractTransformer) this.getInvalidTransformer();
        assertNotNull(invalidTransformer);
        invalidTransformer.setIgnoreBadInput(true);

        AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
        assertNotNull(validTransformer);

        DefaultMuleMessage message = new DefaultMuleMessage(new Integer(0), muleContext);
        Transformer messageTransformer = new TransformerChain(invalidTransformer, validTransformer);
        transformationService.applyTransformers(message, null, messageTransformer);
    }

    @Test
    public void testIgnoreBadInputBreaksChainWithTransformationOrderInvalidValid() throws Exception
    {
        AbstractTransformer invalidTransformer = (AbstractTransformer) this.getInvalidTransformer();
        assertNotNull(invalidTransformer);
        invalidTransformer.setIgnoreBadInput(false);
        
        AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
        assertNotNull(validTransformer);
        
        DefaultMuleMessage message = new DefaultMuleMessage(new Integer(0), muleContext);
        Transformer messageTransformer = new TransformerChain(invalidTransformer, validTransformer);
        
        try
        {
            transformationService.applyTransformers(message, null, messageTransformer);
            fail("Transformer chain is expected to fail because of invalid transformer within chain.");
        }
        catch (MuleException tfe)
        {
            // ignore
        }
    }

    @Test
    public void testIgnoreBadInputBreaksChainWithTransformationOrderValidInvalid() throws Exception
    {
        AbstractTransformer invalidTransformer = (AbstractTransformer) this.getInvalidTransformer();
        assertNotNull(invalidTransformer);
        invalidTransformer.setIgnoreBadInput(false);

        AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
        assertNotNull(validTransformer);

        DefaultMuleMessage message = new DefaultMuleMessage(new Integer(0), muleContext);
        Transformer messageTransformer = new TransformerChain(validTransformer, invalidTransformer);

        try
        {
            transformationService.applyTransformers(message, null, messageTransformer);
            fail("Transformer chain is expected to fail because of invalid transformer within chain.");
        }
        catch (MuleException tfe)
        {
            assertNotNull(tfe);
        }
    }

    private Transformer getInvalidTransformer() throws Exception
    {
        AbstractTransformer transformer = new AbstractTransformer()
        {
            @Override
            protected Object doTransform(final Object src, final String encoding) throws TransformerException
            {
                throw new RuntimeException("This transformer must not perform any transformations.");
            }
        };

        // Use this class as a bogus source type to enforce a simple invalid transformer
        transformer.registerSourceType(DataTypeFactory.create(this.getClass()));
        
        return transformer;
    }
    
    private Transformer getIncreaseByOneTransformer() throws Exception
    {
        AbstractTransformer transformer = new AbstractTransformer()
        {
            @Override
            protected Object doTransform(Object src, String encoding) throws TransformerException
            {
                return new Integer(((Integer) src).intValue() + 1);
            }
        };
        
        DataType<Integer> integerDataType = DataTypeFactory.create(Integer.class);
        transformer.registerSourceType(integerDataType);
        transformer.setReturnDataType(integerDataType);

        return transformer;
    }
    
}
