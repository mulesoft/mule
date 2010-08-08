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
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;

/**
 * A transformer intended to transform a message rather than an arbitrary object
 */
public interface MessageAwareTransformer extends Transformer
{
    /**
     * Transform a message
     */
    Object doTransform(Object src, String encoding, MuleEvent event) throws TransformerException;
}
