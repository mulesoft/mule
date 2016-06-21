/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import javax.activation.DataHandler;

/**
 *  Use to obtain and mutate message attachments defined in two scopes, inbound and outbound.
 */
public interface MutableMessageAttachments extends MessageAttachments
{

    /**
     * Allows for arbitrary data attachments to be associated with the Message. These attachments work in the
     * same way that email attachments work. Attachments can be binary or text
     *
     * @param name the name to associate with the attachment
     * @param dataHandler The attachment {@link DataHandler} to use. This will be used to interact with the attachment data
     * @throws Exception if the attachment cannot be added for any reason
     * @see DataHandler
     * @since 3.0
     */
    void addOutboundAttachment(String name, DataHandler dataHandler) throws Exception;

    /**
     *  Adds an outgoing attachment to the message
     * @param object the input stream to the contents of the attachment. This object can either be a {@link java.net.URL}, which will construct a URL data source, or
     * a {@link java.io.File}, which will construct a file data source.  Any other object will be used as the raw contents of the attachment
     *
     * @param contentType the content type of the attachment.  Note that the charset attribute can be specifed too i.e. text/plain;charset=UTF-8
     * @param name the name to associate with the attachments
     * @throws Exception if the attachment cannot be read or created
     * @since 3.0
     */
    void addOutboundAttachment(String name, Object object, String contentType) throws Exception;

    /**
     * Remove an attachment form this message with the specified name
     * @param name the name of the attachment to remove. If the attachment does not exist, the request may be ignored
     * @throws Exception different messaging systems handle attachments differently, as such some will throw an exception
     * if an attachment does dot exist.
     * @since 3.0
     */
    void removeOutboundAttachment(String name) throws Exception;

    /**
     * Removes all outbound attachments on this message.  Note: inbound attachments are immutable.
     * {@link org.mule.runtime.core.PropertyScope#OUTBOUND}.
     */
    void clearAttachments();

}
