/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.probe;

/**
 * Checks wheter a given {@link Probe} is satisfied or not.
 */
public interface Prober {

  void check(Probe probe);
}
