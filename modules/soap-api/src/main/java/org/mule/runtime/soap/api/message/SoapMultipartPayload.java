/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message;

import static org.mule.runtime.core.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;
import static org.mule.runtime.core.api.util.collection.Collectors.toImmutableMap;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * {@link DefaultMultiPartPayload} implementation that enables the separated retrieval of the body and the attachments.
 *
 * @since 4.0
 */
public class SoapMultipartPayload implements MultiPartPayload {

  private final DefaultMultiPartPayload multipart;

  public SoapMultipartPayload(List<Message> parts) {
    this.multipart = new DefaultMultiPartPayload(parts);
  }

  /**
   * Returns the body part content of the {@link SoapMultipartPayload}.
   *
   * @return an String with the XML response.
   */
  public InputStream getBody() {
    return (InputStream) multipart.getPart(BODY_ATTRIBUTES.getName()).getPayload().getValue();
  }

  /**
   * Returns the attachments parts content of the {@link SoapMultipartPayload}.
   *
   * @return a {@link Map} with the attachments.
   */
  public Map<String, InputStream> getAttachments() {
    return multipart.getParts().stream()
        .filter(part -> !getPartName(part).equals(BODY_ATTRIBUTES.getName()))
        .collect(toImmutableMap(this::getPartName, part -> (InputStream) part.getPayload().getValue()));
  }

  private String getPartName(Message part) {
    return ((PartAttributes) part.getAttributes().getValue()).getName();
  }

  @Override
  public List<Message> getParts() {
    return multipart.getParts();
  }

  @Override
  public List<String> getPartNames() {
    return multipart.getPartNames();
  }

  @Override
  public Message getPart(String partName) {
    return multipart.getPart(partName);
  }

  @Override
  public Map<String, Message> getNamedParts() {
    return multipart.getNamedParts();
  }
}
