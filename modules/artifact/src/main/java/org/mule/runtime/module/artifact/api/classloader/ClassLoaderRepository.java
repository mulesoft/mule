/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

import java.util.Optional;

/**
 * Provides access to the {@link ClassLoader} registered on the container.
 */
@NoImplement
public interface ClassLoaderRepository {

  /**
   * Returns a class loader with a given ID.
   *
   * @param classLoaderId identifies the class loader to find. Non empty.
   * @return an {@link Optional} {@link ClassLoader} for the provided ID.
   */
  Optional<ClassLoader> find(String classLoaderId);

  /**
   * Returns the ID for a given class loader
   *
   * @return an {@link Optional} {@link String} corresponding to the ID which is being searched for.
   */
  Optional<String> getId(ClassLoader classLoader);
}
