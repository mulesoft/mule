/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.api;

/**
 * Decouples the creation of the {@link MuleContainer} from the module requiring it.
 *
 * @since 4.6
 */
public interface MuleContainerProvider {

  /**
   * Creates an instance of a {@link MuleContainer}.
   *
   * @return a newly created {@link MuleContainer}.
   * @throws Exception
   */
  MuleContainer provide() throws Exception;

}
