/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

import org.mule.extension.http.api.HttpMetadataKey;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.model.ParameterMap;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

public class HttpMetadataResolverTestCase extends AbstractHttpTestCase {

  private MetadataService service;
  private static final String MULTIPART = HttpMetadataKey.MULTIPART.name().toLowerCase();
  private static final String FORM = HttpMetadataKey.FORM.name().toLowerCase();
  private static final String STREAM = HttpMetadataKey.STREAM.name().toLowerCase();

  @Override
  protected String getConfigFile() {
    return "http-metadata-config.xml";
  }

  @Before
  public void setupManager() throws RegistrationException {
    service = muleContext.getRegistry().lookupObject(MetadataService.class);
  }

  @Test
  public void resolvesAny() {
    TypeMetadataDescriptor any = getMetadata("anyExplicit");
    verifyAny(any);
  }

  @Test
  public void resolveDefault() {
    TypeMetadataDescriptor any = getMetadata("anyImplicit");
    verifyAny(any);
  }

  @Test
  public void resolveMultipart() {
    verifyType(MULTIPART, ObjectType.class, MultiPartPayload.class);
  }

  @Test
  public void resolveForm() {
    verifyType(FORM, DictionaryType.class, ParameterMap.class);
  }

  @Test
  public void resolveStream() {
    verifyType(STREAM, BinaryType.class, InputStream.class);
  }

  private void verifyType(String flowName, Class expectedMetadataType, Class keyClass) {
    TypeMetadataDescriptor stream = getMetadata(flowName);
    assertThat(stream.getType(), is(instanceOf(expectedMetadataType)));
    assertThat(stream.getType().getAnnotation(TypeIdAnnotation.class).get().getValue(), is(keyClass.getName()));
  }

  private TypeMetadataDescriptor getMetadata(String flowName) {
    MetadataResult<ComponentMetadataDescriptor> result = service.getMetadata(new ProcessorId(flowName, "0"));
    assertThat(result.isSuccess(), is(true));
    return result.get().getOutputMetadata().get().getPayloadMetadata().get();
  }

  private void verifyAny(TypeMetadataDescriptor any) {
    assertThat(any.getType(), is(instanceOf(UnionType.class)));
    UnionType unionType = (UnionType) any.getType();
    assertThat(unionType.getTypes(), hasSize(3));
  }
}
