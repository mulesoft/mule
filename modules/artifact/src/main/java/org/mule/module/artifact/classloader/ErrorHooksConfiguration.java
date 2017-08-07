/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static org.mule.runtime.core.internal.util.rx.Operators.setupErrorHooks;

import reactor.core.publisher.Hooks;

/**
 * Registers reactor-core error handling hooks when dynamically loaded into each plugin class loader.
 *
 * IMPORTANT: this class is on a different package than the rest of the classes in this module. The reason of that is that this
 * class must be loaded by each artifact class loader that is being disposed. So, it cannot contain any of the prefixes that force
 * a class to be loaded from the container.
 *
 * @since 4.0
 */
public class ErrorHooksConfiguration {

  static {
    setupErrorHooks(Hooks.class);
  }
}
