/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.lifecycle;

import org.mule.runtime.api.exception.MuleException;

/**
 * This callback is used to execute lifecycle behaviour for an object being managed by a {@link LifecycleManager} The callback is
 * used so that transitions can be managed consistently outside of an object
 *
 * @since 3.0
 */
public interface LifecycleCallback<O> {

  void onTransition(String phaseName, O object) throws MuleException;
}
