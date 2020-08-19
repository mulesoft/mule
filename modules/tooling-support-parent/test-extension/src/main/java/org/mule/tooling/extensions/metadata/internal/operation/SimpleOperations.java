package org.mule.tooling.extensions.metadata.internal.operation;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.tooling.extensions.metadata.api.parameters.ActingParameterGroup;
import org.mule.tooling.extensions.metadata.api.parameters.LocationKey;
import org.mule.tooling.extensions.metadata.internal.config.SimpleConfiguration;
import org.mule.tooling.extensions.metadata.internal.connection.TstExtensionClient;
import org.mule.tooling.extensions.metadata.internal.metadata.ConfigLessConnectionLessMetadataResolver;
import org.mule.tooling.extensions.metadata.internal.metadata.ConfigLessMetadataResolver;
import org.mule.tooling.extensions.metadata.api.parameters.ComplexActingParameter;
import org.mule.tooling.extensions.metadata.internal.metadata.MultiLevelPartialTypeKeysOutputTypeResolver;
import org.mule.tooling.extensions.metadata.internal.value.ActingParameterGroupVP;
import org.mule.tooling.extensions.metadata.internal.value.ActingParameterVP;
import org.mule.tooling.extensions.metadata.internal.value.ComplexActingParameterVP;
import org.mule.tooling.extensions.metadata.internal.value.ConfigLessConnectionLessNoActingParamVP;
import org.mule.tooling.extensions.metadata.internal.value.ConfigLessNoActingParamVP;
import org.mule.tooling.extensions.metadata.internal.value.LevelThreeVP;
import org.mule.tooling.extensions.metadata.internal.value.LevelTwoVP;
import org.mule.tooling.extensions.metadata.internal.value.MultipleValuesSimpleVP;

import java.util.Map;

public class SimpleOperations {

  @OutputResolver(output = ConfigLessConnectionLessMetadataResolver.class)
  public Result<Object, Void> configLessConnectionLessOP(@Config SimpleConfiguration configuration,
                                                         @Connection TstExtensionClient client,
                                                         @Optional @OfValues(ConfigLessConnectionLessNoActingParamVP.class) String providedParameter,
                                                         @MetadataKeyId(ConfigLessConnectionLessMetadataResolver.class) String metadataKey) {
    return null;
  }

  @OutputResolver(output = ConfigLessMetadataResolver.class)
  public Result<Object, Void> configLessOP(@Config SimpleConfiguration configuration,
                                           @Connection TstExtensionClient client,
                                           @Optional @OfValues(ConfigLessNoActingParamVP.class) String providedParameter,
                                           @Optional(defaultValue = "item") @MetadataKeyId(ConfigLessMetadataResolver.class) String metadataKey) {
    return null;
  }

  @MediaType(TEXT_PLAIN)
  @OutputResolver(output = MultiLevelPartialTypeKeysOutputTypeResolver.class, attributes = MultiLevelPartialTypeKeysOutputTypeResolver.class)
  public Result<String, Object> multiLevelPartialTypeKeysMetadataKey(
      @MetadataKeyId(MultiLevelPartialTypeKeysOutputTypeResolver.class) @ParameterGroup(name="LocationKey") LocationKey locationKey,
      @TypeResolver(MultiLevelPartialTypeKeysOutputTypeResolver.class) Map<String, Object> dynamicParam) {
    return null;
  }

  @MediaType(TEXT_PLAIN)
  @OutputResolver(output = MultiLevelPartialTypeKeysOutputTypeResolver.class)
  public Result<String, Object> multiLevelShowInDslGroupPartialTypeKeysMetadataKey(@MetadataKeyId(MultiLevelPartialTypeKeysOutputTypeResolver.class) @ParameterGroup(name="LocationKeyShowInDsl", showInDsl = true) LocationKey locationKeyShowInDslParam) {
    return null;
  }

  public Result<Void, Object> actingParameterOP(@Config SimpleConfiguration configuration,
                                                @Connection TstExtensionClient client,
                                                String otherRequiredParameterNotRequiredForMetadataNeitherValueProvider,
                                                String actingParameter,
                                                @Optional @OfValues(ActingParameterVP.class) String providedParameter) {
    return null;
  }

  public Result<Void, Object> complexActingParameterOP(@Config SimpleConfiguration configuration,
                                                       @Connection TstExtensionClient client,
                                                       ComplexActingParameter actingParameter,
                                                       @Optional @OfValues(ComplexActingParameterVP.class) String providedParameter) {
    return null;
  }

  public Result<Void, Object> actingParameterGroupOP(@Config SimpleConfiguration configuration,
                                                     @Connection TstExtensionClient client,
                                                     @ParameterGroup(name = "Acting") ActingParameterGroup actingParameterGroup,
                                                     @Optional @OfValues(ActingParameterGroupVP.class) String providedParameter) {
    return null;
  }

  public Result<Void, Object> nestedVPsOperation(@Config SimpleConfiguration configuration,
                                                 @Connection TstExtensionClient client,
                                                 @Optional @OfValues(MultipleValuesSimpleVP.class) String actingParameter,
                                                 @Optional @OfValues(ActingParameterVP.class) String providedParameter) {
    return null;
  }

  public Result<Void, Object> multipleNestedVPsOperation(@Config SimpleConfiguration configuration,
                                                         @Connection TstExtensionClient client,
                                                         @Optional @OfValues(MultipleValuesSimpleVP.class) String levelOne,
                                                         @Optional @OfValues(LevelTwoVP.class) String levelTwo,
                                                         @Optional @OfValues(LevelThreeVP.class) String providedParameter) {
    return null;
  }
}
