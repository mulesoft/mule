/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.utils;

import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.server.PathAndMethodRequestMatcher;

/**
 * Generic registry of {@link PathAndMethodRequestMatcher PathAndMethodRequestMatchers} that can handle collision validation and
 * searching through wildcard paths for a matching entry. Additionally, entries can be managed to be temporarily disabled or
 * removed entirely.
 *
 * @param <T> the type of object associated to the registry
 * @since 4.1.5
 */
public interface RequestMatcherRegistry<T> {

  /**
   * Includes a new matcher to return the desired {@code item}. The matcher's path and method will be analysed to determine whether
   * any collisions exists with the already registered objects.
   *
   * @param matcher the {@link PathAndMethodRequestMatcher} to associate with the  {@code item}
   * @param item the object to register under the {@code matcher}
   * @throws org.mule.runtime.api.exception.MuleRuntimeException if a collision is found
   */
  RequestMatcherRegistryEntry add(PathAndMethodRequestMatcher matcher, T item);

  /**
   * Searches this registry for the most specific match for the given {@link HttpRequest} considering all registered
   * {@link PathAndMethodRequestMatcher PathAndMethodRequestMatchers}.
   *
   * @param request the {@link HttpRequest} to match against
   * @return the matching registered object
   */
  T find(HttpRequest request);

  /**
   * Entry of a {@link RequestMatcherRegistry} which allows managing it's visibility.
   *
   * @since 4.1.5
   */
  interface RequestMatcherRegistryEntry {

    /**
     * Temporarily disables the entry from being accessed.
     */
    void disable();

    /**
     * Allows access to the entry.
     */
    void enable();

    /**
     * Removes the entry.
     */
    void remove();
  }

  /**
   * Builder of {@link RequestMatcherRegistry}. Instances can only be obtained via {@link HttpService#getRequestMatcherRegistryBuilder()}.
   *
   * @param <T> the type of object associated to the registry
   * @since 4.1.5
   */
  interface RequestMatcherRegistryBuilder<T> {

    /**
     * Determines which default item should be returned if a method mismatch occurs, meaning a path match was found but the method
     * was not valid.
     *
     * @param item the default value to return
     * @return this builder
     */
    RequestMatcherRegistryBuilder<T> onMethodMismatch(T item);

    /**
     * Determines which default item should be returned if no match is found.
     *
     * @param item the default value to return
     * @return this builder
     */
    RequestMatcherRegistryBuilder<T> onNotFound(T item);

    /**
     * Determines which default item should be returned if a match is found but the entry is disabled.
     *
     * @param item the default value to return
     * @return this builder
     */
    RequestMatcherRegistryBuilder<T> onDisabled(T item);

    /**
     * @return a new {@link RequestMatcherRegistry} configured as specified.
     */
    RequestMatcherRegistry<T> build();

  }
}
