/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transformer;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;

/**
 * A transformer intended to transform Mule messages rather than arbitrary objects
 */
public interface MessageTransformer  extends Transformer, MessageProcessor
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
