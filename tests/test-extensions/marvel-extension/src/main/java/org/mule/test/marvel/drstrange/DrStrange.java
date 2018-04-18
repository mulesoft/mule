/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.drstrange;

import static org.mule.test.marvel.MarvelExtension.MARVEL_EXTENSION;
import static org.mule.test.marvel.drstrange.DrStrange.CONFIG_NAME;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.reference.ConfigReference;
import org.mule.runtime.extension.api.annotation.param.reference.ObjectStoreReference;
import org.mule.test.marvel.ironman.IronMan;

import javax.inject.Inject;

/**
 * Default extension to test cursor streams and lists. Given Dr. Strange's ability to use the eye of Agamotto
 * to reverse and advance time, he's uniquely positioned to manipulate such streams
 */
@Configuration(name = CONFIG_NAME)
@Operations(DrStrangeOperations.class)
@Sources(DrStrangeBytesSource.class)
@ConnectionProviders({MysticConnectionProvider.class, PoolingMysticConnectionProvider.class})
public class DrStrange implements Initialisable {

  public static final String CONFIG_NAME = "dr-strange";

  @Inject
  private ConfigurationComponentLocator locator;

  @ConfigReference(namespace = MARVEL_EXTENSION, name = IronMan.CONFIG_NAME)
  @Parameter
  @Optional
  private String ironManConfig;

  @Parameter
  @Optional
  @ObjectStoreReference
  private String spellStore;

  @Override
  public void initialise() throws InitialisationException {
    if (ironManConfig != null) {
      locator.find(Location.builder().globalName(ironManConfig).build())
          .orElseThrow(() -> new IllegalArgumentException("No bean with name " + ironManConfig));
    }
  }

}
