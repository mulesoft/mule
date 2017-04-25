/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.soap;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.module.extension.soap.internal.loader.InvokeOperationDeclarer.ATTACHMENTS_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.InvokeOperationDeclarer.HEADERS_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.InvokeOperationDeclarer.REQUEST_PARAM;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.extension.api.soap.SoapAttachment;

import java.util.List;
import java.util.Set;

import org.junit.Test;

public class InvokeMetadataTestCase extends SoapExtensionArtifactFunctionalTestCase {

  private static final String INVALID_KEY_ERROR = "The binding operation name [invalidKey] was not found in the current wsdl";
  private MuleMetadataService metadataService;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    metadataService = muleContext.getRegistry().lookupObject(MuleMetadataService.class);
  }

  @Test
  public void metadataKeys() {
    Location location = Location.builder().globalName("getLeagues").addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> result = metadataService.getMetadataKeys(location);
    assertThat(result.isSuccess(), is(true));
    Set<MetadataKey> keys = result.get().getKeysByCategory().values().iterator().next();
    assertThat(keys, hasSize(1));
    MetadataKey leaguesService = keys.iterator().next();
    assertThat(leaguesService.getId(), is("leagues"));
    assertThat(leaguesService.getChilds(), hasSize(3));
    List<String> operationKeysNames = leaguesService.getChilds().stream().map(MetadataKey::getId).collect(toList());
    assertThat(operationKeysNames, containsInAnyOrder("getLeagues", "getLeagueTeams", "getPresidentInfo"));
  }

  @Test
  public void outputMetadata() {
    OperationModel model = getMetadata("getTeams");
    ObjectType output = toObjectType(model.getOutput().getType());
    assertThat(getTypeId(output).get(), containsString("getTeamsResponse"));
    assertThat(output.getFields(), hasSize(1));
    ObjectType responseType = toObjectType(output.getFields().iterator().next().getValue());
    assertThat(responseType.getFields(), hasSize(1));
    assertThat(responseType.getFields().iterator().next().getValue(), is(instanceOf(StringType.class)));
  }

  @Test
  public void inputHeadersMetadata() {
    OperationModel model = getMetadata("getLeagueTeams");
    ObjectType headers = toObjectType(getParameter(model, HEADERS_PARAM).getType());
    assertThat(headers.getFields(), hasSize(1));
    ObjectType auth = toObjectType(headers.getFields().iterator().next().getValue());
    assertThat(auth.getFields(), hasSize(1));
    assertThat(getTypeId(auth).get(), containsString("auth"));
    assertThat(auth.getFields(), hasSize(1));
    assertThat(auth.getFields().iterator().next().getValue(), is(instanceOf(StringType.class)));
  }

  @Test
  public void requestMetadata() {
    OperationModel model = getMetadata("getPresidentInfo");
    ObjectType request = toObjectType(getParameter(model, REQUEST_PARAM).getType());
    assertThat(request.getFields(), hasSize(1));
    ObjectType getPresidentInfoType = toObjectType(request.getFields().iterator().next().getValue());
    assertThat(getPresidentInfoType.getFields(), hasSize(1));
    assertThat(getPresidentInfoType.getFields().iterator().next().getValue(), is(instanceOf(BooleanType.class)));
  }

  @Test
  public void inputAttachmentsMetadata() {
    OperationModel model = getMetadata("uploadResult");
    ObjectType attachments = toObjectType(getParameter(model, ATTACHMENTS_PARAM).getType());
    assertThat(attachments.getFields(), hasSize(1));
    ObjectFieldType attachment = attachments.getFields().iterator().next();
    assertThat(attachment.getKey().getName().getLocalPart(), is("result"));
    assertThat(getTypeId(toObjectType(attachment.getValue())).get(), containsString(SoapAttachment.class.getName()));
  }

  @Test
  public void attributesMetadata() {
    OperationModel model = getMetadata("getPresidentInfo");
    ObjectType attributes = toObjectType(model.getOutputAttributes().getType());
    assertThat(attributes.getFields(), hasSize(2));
    ObjectFieldType soapHeaders = attributes.getFields().iterator().next();
    assertThat(soapHeaders.getKey().getName().getLocalPart(), is("headers"));
    ObjectType headersType = toObjectType(soapHeaders.getValue());
    assertThat(headersType.getFields(), hasSize(1));
    ObjectFieldType identity = headersType.getFields().iterator().next();
    assertThat(identity.getKey().getName().getLocalPart(), is("identity"));
    assertThat(toObjectType(identity.getValue()).getFields(), hasSize(1));
    assertThat(toObjectType(identity.getValue()).getFields().iterator().next().getValue(), is(instanceOf(StringType.class)));
  }

  @Test
  public void invalidKey() {
    Location location = Location.builder().globalName("invalidKey").addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertThat(result.isSuccess(), is(false));
    assertThat(result.getFailures(), hasSize(5));
    result.getFailures().forEach(failure -> assertThat(failure.getReason(), containsString(INVALID_KEY_ERROR)));
  }

  private ParameterModel getParameter(OperationModel model, String name) {
    return model.getAllParameterModels().stream().filter(p -> p.getName().equals(name)).findAny().get();
  }

  private ObjectType toObjectType(MetadataType headers) {
    assertThat(headers, is(instanceOf(ObjectType.class)));
    return ((ObjectType) headers);
  }

  private OperationModel getMetadata(String flow) {
    Location location = Location.builder().globalName(flow).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertThat(result.isSuccess(), is(true));
    return result.get().getModel();
  }
}
