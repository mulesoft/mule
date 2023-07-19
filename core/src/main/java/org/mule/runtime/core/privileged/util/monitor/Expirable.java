/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.util.monitor;

/**
 * <code>Expirable</code> implementing classes can be notified when they expire
 */

public interface Expirable {

  void expired();
}
