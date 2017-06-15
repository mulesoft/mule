/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.entity;

import org.mule.runtime.http.api.domain.entity.multipart.HttpPart;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * An entity that can be sent or received via an {@link org.mule.runtime.http.api.domain.message.request.HttpRequest} or
 * {@link org.mule.runtime.http.api.domain.message.response.HttpResponse}.
 ** <p>
 * There are four distinct types of entities, depending on their content:
 * <ul>
 * <li><b>streamed</b>: The content is received from a stream.
 *     </li>
 * <li><b>composed</b>: The content is made of several parts.
 *     </li>
 * <li><b>empty</b>: There's no content.
 *     </li>
 * <li><b>neither of the above</b>: The content is in memory and a single part.
 *     </li>
 * </ul>
 * These will determine how the content can be accessed.
 *
 * @since 4.0
 */
public interface HttpEntity {

  /**
   * Tells whether this entity's content is stream based, in which case it can successfully accessed through {@link #getContent()}.
   *
   * @return {@code true} if content is streamed, {@code false} otherwise
   */
  boolean isStreaming();

  /**
   * Tells whether or not this entity is composed of several parts, in which case they can be successfully accessed through
   * {@link #getParts()}.
   *
   * @return {@code true} if there are several content parts, {@code false} otherwise
   */
  boolean isComposed();

  /**
   * Provides the entity's content as a stream.
   *
   * @return an {@code InputStream} representing this entity's content
   * @throws UnsupportedOperationException if this is a composed or empty entity
   * @see #isComposed()
   */
  InputStream getContent() throws UnsupportedOperationException;

  /**
   * Provides the entity's content as bytes. If the entity is stream based, then the stream will be consumed as a consequence.
   *
   * @return a byte array representing this entity's content
   * @throws UnsupportedOperationException if this is a composed, streaming or empty entity
   * @see #isStreaming()
   * @see #isComposed()
   */
  byte[] getBytes() throws UnsupportedOperationException;

  /**
   * Provides the entity's content parts. If the entity is stream based, then the stream will be consumed as a consequence.
   *
   * @return a collection of {@link HttpPart HttpParts} representing this entity's content parts
   * @throws IOException if an error occurs handling the parts
   * @throws UnsupportedOperationException if this is not a composed entity
   * @see #isComposed()
   */
  Collection<HttpPart> getParts() throws IOException, UnsupportedOperationException;

}
