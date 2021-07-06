/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.processor.chain;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

/**
 * An interface that indicates that the {@link ComponentLocation} can be resolved. It is used as a WA so that the subtypes of
 * {@link ReactiveProcessor} that are associated to a location can be identified and a location can be obtained for those types.
 *
 * // TODO MULE-19594: Handle expression problem for Reactive Processor (ComponentLocation)
 */
public interface HasLocation {

  /**
   * @return the resolved {@link ComponentLocation}.
   */
  public ComponentLocation resolveLocation();

}
