/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension;

import java.util.Arrays;
import java.util.List;

public class MetadataConnection {

  public static final String PERSON = "PERSON";
  public static final String CAR = "CAR";
  public static final String HOUSE = "HOUSE";
  public static final String NULL = "NULL";
  public static final String VOID = "VOID";

  public List<String> getEntities() {
    return Arrays.asList(PERSON, CAR, HOUSE);
  }

}
