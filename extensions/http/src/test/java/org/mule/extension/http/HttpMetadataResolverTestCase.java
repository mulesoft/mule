/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.api.component.location.Location.builder;
import org.mule.extension.http.api.HttpMetadataKey;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.tck.junit4.matcher.MetadataKeyMatcher;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("HTTP Connector")
@Stories("Metadata")
public class HttpMetadataResolverTestCase extends AbstractHttpTestCase {

  private MetadataService service;
  private static final String MULTIPART = HttpMetadataKey.MULTIPART.name().toLowerCase();
  private static final String FORM = HttpMetadataKey.FORM.name().toLowerCase();
  private static final String STREAM = HttpMetadataKey.STREAM.name().toLowerCase();

  @Rule
  public DynamicPort serverPort = new DynamicPort("serverPort");

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
    verifyAny(getMetadata("anyExplicit"));
  }

  @Test
  public void resolveDefault() {
    verifyAny(getMetadata("anyImplicit"));
  }

  @Test
  public void resolveMultipart() {
    verifyType(MULTIPART, ObjectType.class, MultiPartPayload.class);
  }

  @Test
  public void resolveForm() {
    verifyType(FORM, ObjectType.class, ParameterMap.class);
  }

  @Test
  public void resolveStream() {
    verifyType(STREAM, BinaryType.class, InputStream.class);
  }

  @Description("Resolves the metadata for a HTTP Listener")
  @Test
  public void getListenerMetadata() {
    MetadataResult<ComponentMetadataDescriptor<SourceModel>> server =
        service.getSourceMetadata(Location.builder().globalName("server").addSourcePart().build());
    assertThat(server.isSuccess(), is(true));
    assertThat(server.get().getModel().getOutput().getType(), is(instanceOf(AnyType.class)));
  }

  @Description("Resolves the MetadataKeys of a Request Configuration. The resolution of keys is done implicitly from" +
      "an Enum MetadataKeyId")
  @Test
  public void resolveRequestMetadataKeys() {
    MetadataKeyMatcher[] metadataKeyMatchers = Stream.of(HttpMetadataKey.values())
        .map(Object::toString)
        .map(MetadataKeyMatcher::metadataKeyWithId)
        .toArray(MetadataKeyMatcher[]::new);

    MetadataResult<MetadataKeysContainer> keysResult = service.getMetadataKeys(builder().globalName("reqConfig").build());
    String httpCategory = "HttpCategory";
    assertThat(keysResult.get().getCategories(), contains(httpCategory));
    Set<MetadataKey> keys = keysResult.get().getKeys(httpCategory).get();
    assertThat(keys, hasItems(metadataKeyMatchers));
  }

  private void verifyType(String flowName, Class expectedMetadataType, Class keyClass) {
    OutputModel stream = getMetadata(flowName);
    assertThat(stream.getType(), is(instanceOf(expectedMetadataType)));
    assertThat(stream.getType().getAnnotation(TypeIdAnnotation.class).get().getValue(), is(keyClass.getName()));
  }

  private OutputModel getMetadata(String flowName) {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result =
        service.getOperationMetadata(Location.builder().globalName(flowName).addProcessorsPart().addIndexPart(0).build());
    assertThat(result.isSuccess(), is(true));
    return result.get().getModel().getOutput();
  }

  private void verifyAny(OutputModel any) {
    assertThat(any.getType(), is(instanceOf(UnionType.class)));
    UnionType unionType = (UnionType) any.getType();
    assertThat(unionType.getTypes(), hasSize(3));
  }
}
