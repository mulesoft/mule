/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.UPLOAD_ATTACHMENT;
import static org.mule.services.soap.internal.metadata.SoapOutputTypeBuilder.ATTACHMENTS_FIELD;
import static org.mule.services.soap.internal.metadata.SoapOutputTypeBuilder.BODY_FIELD;
import static org.mule.test.allure.AllureConstants.WscFeature.WSC_EXTENSION;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(WSC_EXTENSION)
@Stories("Metadata")
public class AttachmentMetadataTestCase extends AbstractMetadataTestCase {

  @Test
  @Description("Checks the Input Metadata of an operation that requires input attachments")
  public void getUploadAttachmentMetadata() {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getMetadata(UPLOAD_ATTACHMENT, UPLOAD_ATTACHMENT);
    List<ParameterModel> parameters = result.get().getModel().getAllParameterModels();

    MetadataType body = getParameterType(parameters, BODY_FIELD);
    assertThat(body, is(instanceOf(NullType.class)));
    ObjectType attachments = toObjectType(getParameterType(parameters, ATTACHMENTS_FIELD));
    Collection<ObjectFieldType> attachmentFields = attachments.getFields();
    assertThat(attachmentFields, hasSize(1));
    assertThat(attachmentFields.iterator().next().getKey().getName().getLocalPart(), is("attachment"));
  }

  @Test
  @Description("Checks the Input Metadata of an operation without attachments")
  public void getEchoMetadata() {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getMetadata(ECHO_FLOW, ECHO);
    MetadataType attachments = getParameterType(result.get().getModel().getAllParameterModels(), ATTACHMENTS_FIELD);
    assertThat(attachments, is(instanceOf(NullType.class)));
  }
}
