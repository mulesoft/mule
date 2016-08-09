/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.model;

import org.mule.runtime.core.api.MuleEventContext;

/**
 * <code>EntryPoint</code> defines the current entry method on a service. If the invoked method does not have a return value, a
 * {@link org.mule.runtime.core.VoidResult} is returned.
 */
public interface EntryPoint {

  Object invoke(Object component, MuleEventContext context) throws Exception;
}
