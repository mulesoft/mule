/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.probe;

/**
 * Checks wheter a given {@link Probe} is satisfied or not.
 */
public interface Prober {

  void check(Probe probe);
}
