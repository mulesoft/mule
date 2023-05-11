/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.metrics.exporter.api;

/**
 * Dummy configuration to use for tests
 */
// TODO W-13065409: Delete and replace tests with real configuration or mock of real configuration
public interface DummyConfiguration {

  public String getExporterType();

  public Integer getExportingInterval();
}
