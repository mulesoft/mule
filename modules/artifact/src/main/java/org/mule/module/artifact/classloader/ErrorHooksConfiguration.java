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
 * @since 4.0
 */
public class ErrorHooksConfiguration {

  static {
    setupErrorHooks(Hooks.class);
  }
}
