/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.message.muletosdk;

import static java.util.stream.Collectors.toUnmodifiableList;

import org.mule.sdk.api.http.domain.entity.HttpEntity;
import org.mule.sdk.api.http.domain.entity.multipart.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;

public class HttpEntityWrapper implements HttpEntity {

  private final org.mule.runtime.http.api.domain.entity.HttpEntity muleEntity;

  public HttpEntityWrapper(org.mule.runtime.http.api.domain.entity.HttpEntity muleEntity) {
    this.muleEntity = muleEntity;
  }

  @Override
  public boolean isStreaming() {
    return muleEntity.isStreaming();
  }

  @Override
  public boolean isComposed() {
    return muleEntity.isComposed();
  }

  @Override
  public InputStream getContent() {
    return muleEntity.getContent();
  }

  @Override
  public byte[] getBytes() throws IOException {
    return muleEntity.getBytes();
  }

  @Override
  public Collection<Part> getParts() throws IOException {
    return muleEntity.getParts().stream().map(HttpPartWrapper::new).collect(toUnmodifiableList());
  }

  @Override
  public Optional<Long> getLength() {
    return muleEntity.getLength();
  }

  @Override
  public OptionalLong getBytesLength() {
    return muleEntity.getBytesLength();
  }
}
