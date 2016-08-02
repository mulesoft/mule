/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

/**
 * Representation of metadata associated to an attachment.
 *
 * @since 4.0
 */
public class AttachmentAttributes extends BaseAttributes
{
    private static final long serialVersionUID = -4718443205714605260L;

    private Map<String, LinkedList<String>> headers;

    private String name;

    private String fileName;

    private long size;

    /**
     * Builds a new instance of this attributes with the given parameters.
     * 
     * @param name The name of the part.
     * @param fileName The name of the file of the attachment, or {@code null} if it wasn't provided.
     * @param size The size in bytes of the attachment, or -1 if unknown or undetermined.
     * @param headers The headers relative to the attachment part.
     */
    public AttachmentAttributes(String name,
            String fileName,
            long size,
            Map<String, LinkedList<String>> headers)
    {
        requireNonNull(name);
        this.name = name;
        this.fileName = fileName;
        this.size = size;
        this.headers = ImmutableMap.copyOf(headers);
    }

    /**
     * Builds a new instance of this attributes with the given name.
     * 
     * @param name The name of the part.
     */
    public AttachmentAttributes(String name)
    {
        this(name, null, -1, Collections.emptyMap());
    }

    /**
     * @return The name of the part.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return The name of the file of the attachment, or {@code null} if it wasn't provided.
     */
    public String getFileName()
    {
        return fileName;
    }

    /**
     * @return The size in bytes of the attachment, as provided by the source. In case it is unknown or cannot be
     *         determined, -1 will be returned.
     */
    public long getSize()
    {
        return size;
    }

    /**
     * TODO Replace with ParameterMap
     * 
     * @return The headers relative to the attachment part.
     */
    public Map<String, LinkedList<String>> getHeaders()
    {
        return headers;
    }
}