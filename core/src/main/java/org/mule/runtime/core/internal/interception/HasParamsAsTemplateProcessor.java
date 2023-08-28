/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
