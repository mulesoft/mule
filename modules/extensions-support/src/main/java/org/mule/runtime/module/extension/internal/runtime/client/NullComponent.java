/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;

/**
 * Null object pattern implementation for the {@link AbstractComponent} class
 *
 * @since 4.5.0
 */
public class NullComponent extends AbstractComponent implements Initialisable {

  public static final NullComponent NULL_COMPONENT = new NullComponent();

  @Override
  public void initialise() throws InitialisationException {

  }
}
