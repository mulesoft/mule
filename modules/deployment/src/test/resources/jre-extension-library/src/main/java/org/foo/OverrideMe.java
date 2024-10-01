/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import org.ietf.jgss.IetfExtender;
import org.omg.test.OmgExtender;
import org.w3c.dom.DomExtender;
import org.xml.sax.SaxExtender;
import javax.annotation.JavaxExtender;

public class OverrideMe implements Processor {

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return CoreEvent.builder(event).message(Message.builder(event.getMessage()).value(getPayload()).build()).build();
  }

  private String getPayload() {
    return JavaxExtender.name() + IetfExtender.name() + OmgExtender.name() + DomExtender.name() + SaxExtender.name();
  }

}
