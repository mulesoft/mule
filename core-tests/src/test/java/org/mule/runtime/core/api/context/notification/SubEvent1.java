/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

public class SubEvent1 extends Event1 {

  public SubEvent1() {
    // empty
  }

  public SubEvent1(String id) {
    super(id);
  }

}
