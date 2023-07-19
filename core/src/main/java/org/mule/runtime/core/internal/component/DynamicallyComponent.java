/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.component;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.privileged.component.AnnotatedObjectInvocationHandler;

/**
 * Marker interface used by {@link AnnotatedObjectInvocationHandler} to identify any classes created by it.
 *
 * @since 1.0
 */
public interface DynamicallyComponent extends Component {

}
