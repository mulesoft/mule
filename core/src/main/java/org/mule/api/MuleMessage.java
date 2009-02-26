/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api;

import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.MessageAdapter;

import java.util.List;

/**
 * <code>MuleMessage</code> represents a message payload. The Message comprises of
 * the payload itself and properties associated with the payload.
 */

public interface MuleMessage extends MessageAdapter
{

    /**
     * Returns the currently edited Message adapter for this message. If no edits have been made
     * this methd will return the same as {@link #getOriginalAdapter()}
     * @return
     */
    MessageAdapter getAdapter();

    /**
     * Returns the original payload used to create this message. The payload of the message can change if {@link #applyTransformers(java.util.List)} or
     * {@link #applyTransformers(java.util.List, Class)} is called.
     * @return the original payload used to create this message
     */
    MessageAdapter getOriginalAdapter();

    /**
     * Will apply a list of transformers to the payload of the message. This *Will* change the payload of the
     * message. This method provides the only way to alter the paylaod of this message without recreating a
     * copy of the message
     * @param transformers the transformers to apply to the message payload
     * @throws TransformerException if a transformation error occurs or one or more of the transformers passed in a
     * are incompatible with the message payload
     */
    void applyTransformers(List transformers) throws TransformerException;

    /**
     * Will apply a list of transformers to the payload of the message. This *Will* change the payload of the
     * message. This method provides the only way to alter the paylaod of this message without recreating a
     * copy of the message
     * @param transformers the transformers to apply to the message payload
     * @param outputType the required output type for this transformation. by adding this parameter some additional
     * transformations will occur on the message payload to ensure that the final payload is of the specified type.
     * If no transformers can be found in the registry that can transform from the return type of the transformation
     * list to the outputType and exception will be thrown
     * @throws TransformerException if a transformation error occurs or one or more of the transformers passed in a
     * are incompatible with the message payload
     */
    void applyTransformers(List transformers, Class outputType) throws TransformerException;

    /**
     * Update the message payload. This is typically only called if the
     * payload was originally an InputStream. In which case, if the InputStream
     * is consumed, it needs to be replaced for future access.
     *
     * @param payload the object to assign as the message payload
     */
    void setPayload(Object payload);

    /**
     * Will attempt to obtain the payload of this message with the desired Class type. This will
     * try and resolve a transformer that can do this transformation. If a transformer cannot be found
     * an exception is thrown.  Any transfromers added to the reqgistry will be checked for compatability
     * @param outputType the desired return type
     * @return The converted payload of this message. Note that this method will not alter the payload of this
     * message *unless* the payload is an inputstream in which case the stream will be read and the payload will become
     * the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during transformation of the
     * payload
     */
    Object getPayload(Class outputType) throws TransformerException;

    /**
     * Converts the message implementation into a String representation
     *
     * @param encoding The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    String getPayloadAsString(String encoding) throws Exception;

    /**
     * Converts the message implementation into a String representation. If encoding
     * is required it will use the encoding set on the message
     *
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    String getPayloadAsString() throws Exception;

    /**
     * Converts the message implementation into a byte array representation
     *
     * @return byte array of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    byte[] getPayloadAsBytes() throws Exception;

    /**
     * Returns the original payload used to create this message. The payload of the message can change if {@link #applyTransformers(java.util.List)} or
     * {@link #applyTransformers(java.util.List, Class)} is called.
     * @return the original payload used to create this message
     */
    Object getOrginalPayload();
    
}
