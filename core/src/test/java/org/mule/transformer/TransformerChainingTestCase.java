/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer;

import org.mule.DefaultMuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.AbstractMuleTestCase;

import java.util.Collections;

public class TransformerChainingTestCase extends AbstractMuleTestCase
{
    public void testSingleChainedTransformer() throws Exception
    {
        AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
        assertNotNull(validTransformer);
        
        DefaultMuleMessage message = new DefaultMuleMessage(new Integer(0), muleContext);
        Transformer messageTransformer = new TransformerChain(validTransformer);
        message.applyTransformers(messageTransformer);

        Object transformedMessage = message.getPayload();
        assertNotNull(transformedMessage);
        assertEquals(new Integer(1), transformedMessage);       
    }

    public void testTwoChainedTransformers() throws Exception
    {
        AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
        assertNotNull(validTransformer);
        
        DefaultMuleMessage message = new DefaultMuleMessage(new Integer(0), muleContext);
        Transformer messageTransformer = new TransformerChain(validTransformer, validTransformer);
        message.applyTransformers(Collections.singletonList(messageTransformer));

        Object transformedMessage = message.getPayload();
        assertNotNull(transformedMessage);
        assertEquals(new Integer(2), transformedMessage);       
    }

    public void testThreeChainedTransformers() throws Exception
    {
        AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
        assertNotNull(validTransformer);
        
        DefaultMuleMessage message = new DefaultMuleMessage(new Integer(0), muleContext);
        Transformer messageTransformer = new TransformerChain(validTransformer, validTransformer, validTransformer);
        message.applyTransformers(messageTransformer);

        Object transformedMessage = message.getPayload();
        assertNotNull(transformedMessage);
        assertEquals(new Integer(3), transformedMessage);       
    }

    public void testIgnoreBadInputDoesNotBreakChainWithTransformationOrderInvalidValid() throws Exception
    {
        AbstractTransformer invalidTransformer = (AbstractTransformer) this.getInvalidTransformer();
        assertNotNull(invalidTransformer);
        invalidTransformer.setIgnoreBadInput(true);
        
        AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
        assertNotNull(validTransformer);
        
        DefaultMuleMessage message = new DefaultMuleMessage(new Integer(0), muleContext);
        Transformer messageTransformer = new TransformerChain(invalidTransformer, validTransformer);
        message.applyTransformers(messageTransformer);

        Object transformedMessage = message.getPayload();
        assertNotNull(transformedMessage);
        assertEquals(new Integer(1), transformedMessage);
    }

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
            message.applyTransformers(messageTransformer);
            fail("Transformer chain is expected to fail because of invalid transformer within chain.");
        }
        catch (TransformerException tfe)
        {
            // ignore
        }
    }

    public void testIgnoreBadInputDoesNotBreakChainWithTransformationOrderValidInvalid() throws Exception
    {
        AbstractTransformer invalidTransformer = (AbstractTransformer) this.getInvalidTransformer();
        assertNotNull(invalidTransformer);
        invalidTransformer.setIgnoreBadInput(true);
        
        AbstractTransformer validTransformer = (AbstractTransformer) this.getIncreaseByOneTransformer();
        assertNotNull(validTransformer);
        
        DefaultMuleMessage message = new DefaultMuleMessage(new Integer(0), muleContext);
        Transformer messageTransformer = new TransformerChain(validTransformer, invalidTransformer);
        message.applyTransformers(messageTransformer);

        Object transformedMessage = message.getPayload();
        assertNotNull(transformedMessage);
        assertEquals(new Integer(1), transformedMessage);
    }

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
            message.applyTransformers(messageTransformer);
            fail("Transformer chain is expected to fail because of invalid transformer within chain.");
        }
        catch (TransformerException tfe)
        {
            assertNotNull(tfe);
        }
    }

    private Transformer getInvalidTransformer() throws Exception
    {
        AbstractTransformer transformer = new AbstractTransformer()
        {
            protected Object doTransform(final Object src, final String encoding) throws TransformerException
            {
                throw new RuntimeException("This transformer must not perform any transformations.");
            }
        };

        // Use this class as a bogus source type to enforce a simple invalid transformer
        transformer.registerSourceType(this.getClass());
        
        return transformer;
    }
    
    private Transformer getIncreaseByOneTransformer() throws Exception
    {
        AbstractTransformer transformer = new AbstractTransformer()
        {
            protected Object doTransform(Object src, String encoding) throws TransformerException
            {
                return new Integer(((Integer) src).intValue() + 1);
            }
        };
        
        transformer.registerSourceType(Integer.class);
        transformer.setReturnClass(Integer.class);
        
        return transformer;
    }
    
}
