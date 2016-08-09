/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model;

import org.mule.runtime.extension.api.annotation.Alias;

/**
 * A generic contract for any kind of component that can contain an alias name or description
 *
 * @since 4.0
 */
interface WithAlias extends WithAnnotations, WithName {

  String EMPTY = "";

  /**
   * @return The alias of the implementer component
   */
  default String getAlias() {
    final java.util.Optional<Alias> alias = getAnnotation(Alias.class);
    return alias.isPresent() ? alias.get().value() : getName();
  }

  /**
   * @return The description of the implementer component
   */
  default String getDescription() {
    final java.util.Optional<Alias> alias = getAnnotation(Alias.class);
    return alias.isPresent() ? alias.get().description() : EMPTY;
  }
}
