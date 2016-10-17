/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.metadata;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.ECHO_ACCOUNT;
import static org.mule.extension.ws.WscTestUtils.NO_PARAMS;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;

public class BodyMetadataTestCase extends AbstractMetadataTestCase {

  @Test
  @Description("Checks the dynamic metadata of the request body parameter for the echo operation")
  public void getEchoInputBody() {
    MetadataResult<ComponentMetadataDescriptor> result = getMetadata(ECHO_FLOW, ECHO);
    MetadataType type = result.get().getInputMetadata().get().getParameterMetadata(BODY_PARAM).get().getType();

    Collection<ObjectFieldType> fields = toObjectType(type).getFields();
    assertThat(fields, hasSize(1));
    ObjectFieldType operationField = fields.iterator().next();
    assertThat(operationField.getKey().getName().getLocalPart(), is(ECHO));

    Collection<ObjectFieldType> operationParams = toObjectType(operationField.getValue()).getFields();
    assertThat(operationParams, hasSize(1));
    ObjectFieldType field = operationParams.iterator().next();
    assertThat(field.getKey().getName().getLocalPart(), is("text"));
    assertThat(field.getValue(), is(instanceOf(StringType.class)));
  }

  @Test
  @Description("Checks the dynamic metadata of the request body parameter for the echo operation")
  public void getNoParamsInputBody() {
    MetadataResult<ComponentMetadataDescriptor> result = getMetadata(NO_PARAMS_FLOW, NO_PARAMS);
    MetadataType type = result.get().getInputMetadata().get().getParameterMetadata(BODY_PARAM).get().getType();

    Collection<ObjectFieldType> fields = toObjectType(type).getFields();
    assertThat(fields, hasSize(1));
    ObjectFieldType operationField = fields.iterator().next();
    assertThat(operationField.getKey().getName().getLocalPart(), is(NO_PARAMS));

    Collection<ObjectFieldType> operationParams = toObjectType(operationField.getValue()).getFields();
    assertThat(operationParams, hasSize(0));
  }

  @Test
  @Description("Checks the dynamic metadata of the request body parameter for the echoAccount operation")
  public void getEchoAccountInputBody() {
    MetadataResult<ComponentMetadataDescriptor> result = getMetadata(ECHO_ACCOUNT_FLOW, ECHO_ACCOUNT);
    MetadataType type = result.get().getInputMetadata().get().getParameterMetadata(BODY_PARAM).get().getType();

    Collection<ObjectFieldType> fields = toObjectType(type).getFields();
    assertThat(fields, hasSize(1));
    ObjectFieldType operationField = fields.iterator().next();
    assertThat(operationField.getKey().getName().getLocalPart(), is(ECHO_ACCOUNT));

    Collection<ObjectFieldType> operationParams = toObjectType(operationField.getValue()).getFields();
    assertThat(operationParams, hasSize(2));
    Iterator<ObjectFieldType> iterator = operationParams.iterator();
    ObjectFieldType accountField = iterator.next();
    assertThat(accountField.getKey().getName().getLocalPart(), is("account"));

    ObjectType accountType = toObjectType(accountField.getValue());
    Collection<ObjectFieldType> accountFields = accountType.getFields();
    assertThat(accountFields, hasSize(4));

    ObjectFieldType name = iterator.next();
    assertThat(name.getKey().getName().getLocalPart(), is("name"));
    assertThat(name.getValue(), is(instanceOf(StringType.class)));
  }
}
