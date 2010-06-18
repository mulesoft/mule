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

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;

import java.util.Collections;
import java.util.List;

/**
 * Transforms message using supplied tranformer(s)
 */
public class TransformerMessageProcessor implements MessageProcessor
{
    protected List<Transformer> transformers;

    public TransformerMessageProcessor(Transformer transformer)
    {
        this.transformers = Collections.singletonList(transformer);
    }

    public TransformerMessageProcessor(List<Transformer> transformers)
    {
        this.transformers = transformers;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event != null && event.getMessage() != null)
        {
            event.getMessage().applyTransformers(transformers);
        }
        return event;
    }
}
