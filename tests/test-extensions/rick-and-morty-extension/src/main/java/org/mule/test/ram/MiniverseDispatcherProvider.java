/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.ram;

import static org.mule.test.ram.RickAndMortyExtension.RICKS_PHRASE;

public class MiniverseDispatcherProvider extends AbstractScienceTransportProvider {

  @Override
  protected String getResponseWord() {
    return RICKS_PHRASE;
  }
}
