/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.classloader;

import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Executes a series of {@link ResourceReleaser}s making sure any errors are handled without preventing the execution of the other
 * releasers.
 *
 * @since 4.5.0
 */
public class ResourceReleaserExecutor {

  private final Collection<Supplier<ResourceReleaser>> resourceReleaserSuppliers = new ArrayList<>();
  private final Consumer<Throwable> onError;

  public ResourceReleaserExecutor(Consumer<Throwable> onError) {
    this.onError = onError;
  }

  /**
   * Registers the given {@code resourceReleaserSupplier}, so it can be executed with {@link #executeResourceReleasers()}.
   *
   * @param resourceReleaserSupplier the {@link ResourceReleaser} supplier to register.
   */
  public void addResourceReleaser(Supplier<ResourceReleaser> resourceReleaserSupplier) {
    resourceReleaserSuppliers.add(resourceReleaserSupplier);
  }

  /**
   * Executes the registered {@link ResourceReleaser} instances.
   */
  public void executeResourceReleasers() {
    for (Supplier<ResourceReleaser> resourceReleaserSupplier : resourceReleaserSuppliers) {
      try {
        resourceReleaserSupplier.get().release();
      } catch (Throwable t) {
        onError.accept(t);
      }
    }
  }
}
