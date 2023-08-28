/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension;

import java.util.Arrays;
import java.util.List;

public class ValuesConnection {

  public List<String> getEntities() {
    return Arrays.asList("connection1", "connection2", "connection3");
  }
}
