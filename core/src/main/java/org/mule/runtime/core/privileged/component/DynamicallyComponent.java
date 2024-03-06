/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.component;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.internal.component.AnnotatedObjectInvocationHandler;

/**
 * Marker interface used by {@link AnnotatedObjectInvocationHandler} to identify any classes created by it.
 *
 * @since 1.0
 */
public interface DynamicallyComponent extends Component {

}
