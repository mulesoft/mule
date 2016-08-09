/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.service;

import org.mule.runtime.core.api.MuleException;

/**
 * Adds a resume lifecycle method to an object. This should only be used with {@link org.mule.runtime.core.service.Pausable}
 */
public interface Resumable {

  public static final String PHASE_NAME = "resume";

  void resume() throws MuleException;
}
