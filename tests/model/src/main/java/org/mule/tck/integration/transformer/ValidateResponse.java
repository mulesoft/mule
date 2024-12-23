/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.integration.transformer;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.InputStream;

/**
 * Throws an exception if the message does not contain "success".
 */
public class ValidateResponse extends AbstractComponent implements Processor {

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {

    final Object src = event.getMessage().getPayload().getValue();
    String response = null;
    if (src instanceof String) {
      response = (String) src;
    } else if (src instanceof InputStream) {
      response = IOUtils.toString((InputStream) src);
    }

    if (response != null && response.contains("success")) {
      return event;
    } else {
      throw new TransformerException(createStaticMessage("Invalid response from flow: " + response));
    }
  }

}


