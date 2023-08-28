/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.ram;

import static org.mule.test.ram.RickAndMortyExtension.RICKS_PHRASE;

public class MiniverseDispatcherProvider extends AbstractScienceTransportProvider {

  @Override
  protected String getResponseWord() {
    return RICKS_PHRASE;
  }
}
