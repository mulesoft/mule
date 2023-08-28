/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.processor;

public interface FlowAssertion {

  void verify() throws InterruptedException;

}
