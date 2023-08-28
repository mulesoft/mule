/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.testmodels.fruit;

public interface FruitCleaner {

  void wash(Fruit fruit);

  void polish(Fruit fruit);
}
