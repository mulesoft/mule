/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.testmodels.fruit;

import java.io.Serializable;

public interface Fruit extends Serializable {

  void bite();

  boolean isBitten();
}
