/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.test.marvel.model.Missile;

@Alias(("odd-missile"))
public class OddMissileProvider extends MissileProvider implements Initialisable {

  private int count;

  @Override
  public void initialise() throws InitialisationException {
    count = -1;
  }

  @Override
  public Missile connect() throws ConnectionException {
    Missile missile = super.connect();
    missile.setArmed(++count % 2 == 0);

    return missile;
  }
}
