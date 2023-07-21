/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer.simple;


public class ObjectByteArrayTransformersWithStringsTestCase extends ObjectByteArrayTransformersWithObjectsTestCase {

  private String testObject = "test";

  public Object getTestData() {
    return testObject;
  }

  public Object getResultData() {
    return testObject.getBytes();
  }
}
