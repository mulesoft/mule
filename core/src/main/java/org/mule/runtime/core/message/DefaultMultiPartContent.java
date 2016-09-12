/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartContent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a payload of a {@link Message} composed of many different parts. Each parts is in itself a {@link Message}, and has
 * {@code attributes} specific to that parts (such as the headers of a single http part).
 * <p>
 * This is useful for representing attachments as part of the payload of a message.
 * 
 * @since 4.0
 */
public class DefaultMultiPartContent implements Serializable, MultiPartContent {

  private static final long serialVersionUID = -1435622001805748221L;

  /**
   * The name of a part that does <b>not</b> represent an attachment.
   */
  private static final String BODY_PART_NAME = "_body";

  /**
   * A singleton attributes instance for parts that do not represent attachments.
   */
  public static final PartAttributes BODY_ATTRIBUTES = new PartAttributes(BODY_PART_NAME);

  private List<Message> parts;

  /**
   * Builds a new {@link DefaultMultiPartContent} with the given {@link Message}s as parts.
   * 
   * @param parts
   */
  public DefaultMultiPartContent(Message... parts) {
    this(asList(parts));
  }

  /**
   * Builds a new {@link DefaultMultiPartContent} with the given {@link Message}s as parts.
   * 
   * @param parts
   */
  public DefaultMultiPartContent(List<Message> parts) {
    final Builder<Message> builder = ImmutableList.builder();

    for (Message part : parts) {
      if (!(part.getAttributes() instanceof PartAttributes)) {
        throw new IllegalArgumentException("Body parts may only have 'PartAttributes' as attributes.");
      }

      if (part.getPayload().getValue() instanceof MultiPartContent) {
        builder.addAll(((MultiPartContent) part.getPayload().getValue()).getParts());
      } else {
        builder.add(part);
      }
    }

    this.parts = builder.build();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.mule.runtime.core.message.MultiPartContent#getParts()
   */
  @Override
  public List<Message> getParts() {
    return parts;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.mule.runtime.core.message.MultiPartContent#getPartsNames()
   */
  @Override
  public List<String> getPartNames() {
    return parts.stream().map(m -> getPartAttributes(m).getName()).collect(toList());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.mule.runtime.core.message.MultiPartContent#getPart(java.lang.String)
   */
  @Override
  public Message getPart(String partName) {
    return parts.stream().filter(m -> partName.equals(getPartAttributes(m).getName())).findFirst().get();
  }

  /**
   * Looks up the part that represents the body (not an attachment) of the message.
   * 
   * @return the part for the body.
   */
  public Message getBodyPart() {
    return parts.stream().filter(m -> BODY_PART_NAME.equals(getPartAttributes(m).getName())).findFirst().orElse(null);
  }

  /**
   * Looks up the part that represents the body (not an attachment) of the message.
   * 
   * @return the part for the body.
   */
  public List<Message> getNonBodyParts() {
    return parts.stream().filter(m -> !BODY_PART_NAME.equals(getPartAttributes(m).getName())).collect(toList());
  }

  /**
   * Checks for the presence of the part that represents the body (not an attachment) of the message.
   * 
   * @return {@code true} if the part for the body is present.
   */
  public boolean hasBodyPart() {
    return parts.stream().anyMatch(m -> BODY_PART_NAME.equals(getPartAttributes(m).getName()));
  }

  private PartAttributes getPartAttributes(Message message) {
    return (PartAttributes) message.getAttributes();
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{" + getPartNames() + "}";
  }
}
