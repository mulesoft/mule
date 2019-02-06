package org.mule.test.module.extension.metadata;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.test.module.extension.metadata.MetadataExtensionFunctionalTestCase.ResolutionType.EXPLICIT_RESOLUTION;

import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.junit.Test;

public class SourceWithCallbackParametersMetadataTest extends MetadataExtensionFunctionalTestCase<SourceModel> {

  public SourceWithCallbackParametersMetadataTest(ResolutionType resolutionType) {
    super(resolutionType);
    this.provider = resolutionType == EXPLICIT_RESOLUTION ? MetadataService::getSourceMetadata
        : (metadataService, componentId, key) -> metadataService.getSourceMetadata(componentId);
    this.location = builder().globalName(SOURCE_METADATA_WITH_CALLBACK_PARAMETERS).addSourcePart().build();
  }

  @Override
  protected String getConfigFile() {
    return METADATA_TEST;
  }

  @Test
  public void getSourceDynamicInputMetadataFromCallbackParameters() throws Exception {
    final MetadataResult<ComponentMetadataDescriptor<SourceModel>> result = getComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertThat(result.isSuccess(), is(true));
    ComponentMetadataDescriptor<SourceModel> componentMetadata = result.get();
    SourceModel sourceModel = componentMetadata.getModel();

    SourceCallbackModel sourceSuccessCallback = sourceModel.getSuccessCallback().get();

    assertExpectedType(getParameter(sourceSuccessCallback, RESPONSE_PARAMETER_NAME).getType(), personType);
    assertExpectedType(getParameter(sourceSuccessCallback, SUCCESS_OBJECT_PARAMETER_NAME).getType(), carType);

    SourceCallbackModel sourceErrorCallback = sourceModel.getErrorCallback().get();

    assertExpectedType(getParameter(sourceErrorCallback, RESPONSE_PARAMETER_NAME).getType(), houseType);
    assertExpectedType(getParameter(sourceErrorCallback, ERROR_OBJECT_PARAMETER_NAME).getType(), personType);
  }

  private ParameterModel getParameter(ParameterizedModel owner, String parameterName) {
    return owner.getAllParameterModels().stream().filter(parameterModel -> parameterModel.getName().equals(parameterName))
        .findFirst().orElse(null);
  }

}
