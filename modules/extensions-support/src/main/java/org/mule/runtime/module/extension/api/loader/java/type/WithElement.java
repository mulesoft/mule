/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

import java.util.Optional;

import javax.lang.model.element.Element;

/**
 * A generic contract for any kind of component from which, a {@link Element} can be delivered
 *
 * @since 4.1
 */
@NoImplement
public interface WithElement {

  /**
   * @return an {@link Optional} {@link Element} representing the component that implements this interface.
   */
  Optional<? extends Element> getElement();
}
