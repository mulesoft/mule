/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.interception;

import org.mule.runtime.core.api.el.ExpressionManager;

/**
 * Marker interface used by <b>Interception API</b> that indicates that the expression parameters of the implementing components
 * must be interpreted as templates.
 *
 * @see ExpressionManager#parseLogTemplate(String, org.mule.runtime.core.api.event.CoreEvent,
 *      org.mule.runtime.api.component.location.ComponentLocation, org.mule.runtime.api.el.BindingContext)
 *
 * @since 4.5
 */
public interface HasParamsAsTemplateProcessor {

}
