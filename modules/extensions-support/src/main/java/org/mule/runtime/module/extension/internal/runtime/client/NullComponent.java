/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
