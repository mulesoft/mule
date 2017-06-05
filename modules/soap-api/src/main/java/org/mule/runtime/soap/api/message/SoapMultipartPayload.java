/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message;

import static org.mule.runtime.core.util.collection.Collectors.toImmutableMap;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;

import java.util.List;
import java.util.Map;

/**
 * {@link DefaultMultiPartPayload} implementation that enables the separated retrieval of the body and the attachments.
 *
 * @since 4.0
 */
public class SoapMultipartPayload extends DefaultMultiPartPayload {

  public SoapMultipartPayload(List<Message> parts) {
    super(parts);
  }

  /**
   * Returns the body part content of the {@link SoapMultipartPayload}.
   *
   * @return an String with the XML response.
   */
  public String getBody() {
    return (String) getPart(BODY_ATTRIBUTES.getName()).getPayload().getValue();
  }

  /**
   * Returns the attachments parts content of the {@link SoapMultipartPayload}.
   *
   * @return a {@link List} with the attachments.
   */
  public Map<String, Object> getAttachments() {
    return getParts().stream()
        .filter(part -> !getPartName(part).equals(BODY_ATTRIBUTES.getName()))
        .collect(toImmutableMap(this::getPartName, part -> part.getPayload().getValue()));
  }

  private String getPartName(Message part) {
    return ((PartAttributes) part.getAttributes().getValue()).getName();
  }
}
