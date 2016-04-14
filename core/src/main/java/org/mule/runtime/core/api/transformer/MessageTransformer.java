/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transformer;

import org.mule.api.MuleEvent;

/**
 * A transformer intended to transform Mule messages rather than arbitrary objects
 */
public interface MessageTransformer  extends Transformer
{
    /**
     * Thransforms the supplied data and returns the result
     *
     * @param src the data to transform
     * @param event the event currently being processed
     * @return the transformed data
     * @throws TransformerMessagingException if a error occurs transforming the data or if the
     *                              expected returnClass isn't the same as the transformed data
     */
    Object transform(Object src, MuleEvent event) throws TransformerMessagingException;

    /**
     * Thransforms the supplied data and returns the result
     *
     * @param src      the data to transform
     * @param encoding the encoding to use by this transformer.  many transformations will not need encoding unless
     *                 dealing with text so you only need to use this method if yo wish to customize the encoding
     * @param event the event currently being processed
     * @return the transformed data
     * @throws TransformerMessagingException if a error occurs transforming the data or if the
     *                              expected returnClass isn't the same as the transformed data
     */
    Object transform(Object src, String encoding, MuleEvent event) throws TransformerMessagingException;
}
