/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.repository.internal;

import org.eclipse.aether.RepositorySystem;

/**
 * Factory for a {@code RepositorySystem}.
 *
 * @since 4.0
 */
public interface RepositorySystemFactory {

  /**
   * @return the {@code RepositorySystem} created by the factory.
   */
  RepositorySystem createRepositorySystem();

}
