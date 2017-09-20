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
import java.util.Optional;

/**
 * An entity that can be sent or received via an {@link org.mule.runtime.http.api.domain.message.request.HttpRequest} or
 * {@link org.mule.runtime.http.api.domain.message.response.HttpResponse}.
 ** <p>
 * There are three distinct types of entities, depending on their content:
 * <ul>
 * <li><b>streamed</b>: The content is received from a stream.
 *     </li>
 * <li><b>composed</b>: The content is made of several parts.
 *     </li>
 * <li><b>neither of the above</b>: The content is in memory and whole.
 *     </li>
 * </ul>
 * These will determine how the content can be accessed although there may overlap.
 *
 * @since 4.0
 */
public interface HttpEntity {

  /**
   * Tells whether this entity's content is stream based. Streamed entities can only provide their content once, regardless of the
   * access method.
   *
   * @return {@code true} if content is streamed, {@code false} otherwise
   * @see #getContent()
   */
  boolean isStreaming();

  /**
   * Tells whether or not this entity is composed of several parts, in which case they should be available through {@link #getParts()}.
   *
   * @return {@code true} if there are several content parts, {@code false} otherwise
   */
  boolean isComposed();

  /**
   * Provides the entity's content as a stream. All streaming entities should provide their stream.
   *
   * @return an {@code InputStream} representing this entity's content or {@code null} if such representation is not possible
   */
  InputStream getContent();

  /**
   * Provides the entity's content as bytes. If the entity is stream based, then the stream will be consumed as a consequence.
   *
   * @return a byte array representing this entity's content or {@code null} if such representation is not possible
   * @throws IOException if an error occurs creating the byte array
   */
  byte[] getBytes() throws IOException;

  /**
   * Provides the entity's content parts. If the entity is stream based, then the stream will be consumed as a consequence. Non
   * composed entities should return an empty collection.
   *
   * @return a collection of {@link HttpPart HttpParts} representing this entity's content parts, if present
   * @throws IOException if an error occurs handling the parts
   * @see #isComposed()
   */
  Collection<HttpPart> getParts() throws IOException;

  /**
   * Provides the length (in bytes) of the {@link HttpEntity}, if known. For the most part, only received entities from HTTP
   * messages that carried a 'Content-Length' header will return a length.
   *
   * @return an {@link Optional} with the length (in bytes) or an empty one if unknown
   */
  Optional<Long> getLength();

}
