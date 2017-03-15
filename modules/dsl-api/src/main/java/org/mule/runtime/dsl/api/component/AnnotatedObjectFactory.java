/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

import org.mule.runtime.api.meta.AnnotatedObject;

/**
 * Interface that must be implemented by those classes that are meant to be used as a factory to create complex domain objects
 * which in turn are {@link AnnotatedObject}s.
 * Implementations should extend {@link AbstractAnnotatedObjectFactory}.
 *
 * @param <T> the type of the object to be created, which should be an {@link AnnotatedObject}.
 */
public interface AnnotatedObjectFactory<T> extends ObjectFactory<T>, AnnotatedObject {

}
