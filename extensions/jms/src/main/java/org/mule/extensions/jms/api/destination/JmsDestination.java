/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.destination;

import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import javax.jms.Destination;

/**
 * Representation of a {@link Destination} that contains it's identifier name
 * and kind of Destination
 *
 * @since 4.0
 */
public final class JmsDestination {

  /**
   * The name that identifies the destination where a reply to a message should be sent
   */
  @Parameter
  @XmlHints(allowReferences = false)
  @DisplayName("Destination Name")
  @Summary("It is the destination where a reply to the message should be sent")
  private String destination;

  /**
   * the type of this destination
   */
  @Parameter
  @Optional(defaultValue = "QUEUE")
  private DestinationType destinationType;


  public JmsDestination() {}

  public JmsDestination(String name, DestinationType type) {
    this.destination = name;
    this.destinationType = type;
  }

  public String getDestination() {
    return destination;
  }

  public DestinationType getDestinationType() {
    return destinationType;
  }
}
