/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.registry.Registry;

/**
 * Declares {@link MuleRegistry} handling methods for {@link MuleContext}.
 *
 * @since 4.0
 *
 * @deprecated Use {@link org.mule.runtime.api.artifact.Registry} instead.
 */
// TODO MULE-13627 Remove downcasts to this interface
@Deprecated
public interface MuleContextWithRegistries extends MuleContext {

  MuleRegistry getRegistry();

  void addRegistry(Registry registry);

  void removeRegistry(Registry registry);

}
