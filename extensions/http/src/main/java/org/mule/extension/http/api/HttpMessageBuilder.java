/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.IOUtils.toDataHandler;

import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * Base component to create HTTP messages.
 *
 * @since 4.0
 */
public class HttpMessageBuilder {

  /**
   * HTTP headers the message should include.
   */
  @Parameter
  @Optional
  protected Map<String, String> headers = new HashMap<>();

  /**
   * HTTP parts the message should include.
   */
  @Parameter
  @Optional
  protected List<HttpPart> parts = new LinkedList<>();

  public Map<String, String> getHeaders() {
    return headers;
  }

  public Map<String, DataHandler> getParts() {
    return getResolvedParts(parts);
  }

  protected Map<String, DataHandler> getResolvedParts(List<HttpPart> parts) {
    Map<String, DataHandler> resolvedAttachments = new HashMap<>();

    parts.forEach(attachment -> {
      String filename = attachment.getFilename();
      String name = filename != null ? filename : attachment.getId();
      DataHandler dataHandler;
      try {
        dataHandler = toDataHandler(name, attachment.getData(), attachment.getContentType());
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage("Could not create part %s", attachment.getId()), e);
      }
      resolvedAttachments.put(attachment.getId(), dataHandler);
    });

    return resolvedAttachments;
  }
}
