/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.message.MuleMessage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import java.io.Serializable;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Represents a payload of a {@link MuleMessage} composed of many different parts. Each parts is in itself a
 * {@link MuleMessage}, and has {@code attributes} specific to that parts (such as http part headers).
 * <p>
 * This is useful for representing attachments as part of the payload of a message.
 * 
 * @since 4.0
 */
public class MultiPartPayload implements Serializable
{
    private static final long serialVersionUID = -1435622001805748221L;

    /**
     * The name of a part that does <b>not</b> represent an attachment.
     */
    public static final String BODY_PART_NAME = "_body";

    private List<MuleMessage> parts;

    /**
     * Builds a new {@link MultiPartPayload} with the given {@link MuleMessage}s as parts.
     * 
     * @param parts
     */
    public MultiPartPayload(MuleMessage... parts)
    {
        this(asList(parts));
    }

    /**
     * Builds a new {@link MultiPartPayload} with the given {@link MuleMessage}s as parts.
     * 
     * @param parts
     */
    public MultiPartPayload(List<MuleMessage> parts)
    {
        final Builder<MuleMessage> builder = ImmutableList.builder();

        for (MuleMessage part : parts)
        {
            if (part.getPayload() instanceof MultiPartPayload)
            {
                builder.addAll(((MultiPartPayload) part.getPayload()).getParts());
            }
            else
            {
                builder.add(part);
            }
        }

        this.parts = builder.build();
    }

    /**
     * @return the contained parts.
     */
    public List<MuleMessage> getParts()
    {
        return parts;
    }

    /**
     * @return the names of the contained parts.
     */
    public List<String> getPartsNames()
    {
        return parts.stream().map(m -> m.getAttributes() instanceof AttachmentAttributes
                ? ((AttachmentAttributes) m.getAttributes()).getName()
                : BODY_PART_NAME).collect(toList());
    }

    /**
     * Looks up the part with the passed {@code partName}.
     * 
     * @param partName the name of the part to look for.
     * @return the part with the given name.
     * @throws NoSuchElementException if no part with the given name exists.
     */
    public MuleMessage getPart(String partName)
    {
        return parts.stream()
                    .filter(m -> 
                    {
                        if (BODY_PART_NAME.equals(partName))
                        {
                            return !(m.getAttributes() instanceof AttachmentAttributes);
                        }
                        else
                        {
                            return m.getAttributes() instanceof AttachmentAttributes
                                   && ((AttachmentAttributes) m.getAttributes()).getName().equals(partName);
                        }
                    })
                    .findFirst()
                    .get();
    }

    @Override
    public String toString()
    {
        return MultiPartPayload.class.getSimpleName() + "{" + getPartsNames() + "}";
    }
}
