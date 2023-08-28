/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing.outbound;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.CoreEvent.Builder;

/**
 * Provides a way to define callbacks that work on instances of {@link CoreEvent} {@link Builder}s.
 *
 * @since 4.0
 */
public interface EventBuilderConfigurer {

  /**
   * Applies changes on the given {@code builder}.
   *
   * @param builder the {@link CoreEvent} builder to configure.
   */
  void configure(Builder builder);

  /**
   * Notifies that the processing of the target {@link CoreEvent} of the {@link #configure(Builder)} configured {@link Builder} is
   * complete.
   *
   * @since 4.2
   * @since 4.1.2
   */
  void eventCompleted();
}
