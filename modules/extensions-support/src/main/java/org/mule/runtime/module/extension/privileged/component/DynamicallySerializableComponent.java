/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.privileged.component;

import org.mule.runtime.module.extension.internal.component.AnnotatedObjectInvocationHandler;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Marker interface used by {@link AnnotatedObjectInvocationHandler} to identify any classes created by it.
 * <p>
 * Declares {@link #writeReplace()} in order to override default serialization mechanism.
 *
 * @since 1.0
 */
public interface DynamicallySerializableComponent extends DynamicallyComponent, Serializable {

  /**
   * Changes the object to actually be serialized when this is serialized.
   * <p>
   * This method is declared so that the Dynamic Class Builder (ByteBuddy) can intercept it.
   *
   * @see Serializable
   * @see <a href=
   *      "https://github.com/cglib/cglib/wiki/How-To#cglib-and-java-serialization">https://github.com/cglib/cglib/wiki/How-To#cglib-and-java-serialization<a>
   */
  Object writeReplace() throws ObjectStreamException;
}
