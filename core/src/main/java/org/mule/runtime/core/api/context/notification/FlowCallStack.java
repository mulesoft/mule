/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.api.annotation.NoImplement;

import java.io.Serializable;
import java.util.List;

/**
 * Keeps context information about the executing flows and its callers in order to provide augmented troubleshooting information
 * for an application developer.
 *
 * @since 3.8.0
 */
@NoImplement
public interface FlowCallStack extends Serializable, Cloneable {

  /**
   * @return the object at the top of this stack without removing it from the stack or null if the stack is empty.
   */
  FlowStackElement peek();

  /**
   * @return the current elements of this stack as a list, ordered from top to bottom.
   */
  List<FlowStackElement> getElements();

  /**
   * @return a deep copy of this object.
   */
  FlowCallStack clone();
}
