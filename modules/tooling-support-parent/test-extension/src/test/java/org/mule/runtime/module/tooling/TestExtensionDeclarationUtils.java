package org.mule.runtime.module.tooling;

import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.NUMBER;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ConnectionElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.SourceElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ConfigurationElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.OperationElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterGroupElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterizedElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.SourceElementDeclarer;

import java.util.List;
import java.util.Map;

public class TestExtensionDeclarationUtils {

  public static final ElementDeclarer TEST_EXTENSION_DECLARER = ElementDeclarer.forExtension("ToolingSupportTest");

  public static final String MISSING_CONFIG_ELEMENT_NAME = "missingConfigRefName";
  public static final String CONFIG_ELEMENT_NAME = "config";
  public static final String CONNECTION_ELEMENT_NAME = "tstConnection";

  public static final String PROVIDED_PARAMETER_NAME = "providedParameter";
  public static final String ACTING_PARAMETER_NAME = "actingParameter";
  public static final String METADATA_KEY_PARAMETER_NAME = "metadataKey";
  public static final String ACTING_PARAMETER_GROUP_NAME = "Acting";

  public static final String SOURCE_ELEMENT_NAME = "simple";
  public static final String INDEPENDENT_SOURCE_PARAMETER_NAME = "independentParam";
  public static final String CONNECTION_DEPENDANT_SOURCE_PARAMETER_NAME = "connectionDependantParam";
  public static final String ACTING_PARAMETER_DEPENDANT_SOURCE_PARAMETER_NAME = "actingParameterDependantParam";

  public static final String CONFIG_LESS_CONNECTION_LESS_OP_ELEMENT_NAME = "configLessConnectionLessOP";
  public static final String CONFIG_LESS_OP_ELEMENT_NAME = "configLessOP";
  public static final String ACTING_PARAMETER_OP_ELEMENT_NAME = "actingParameterOP";
  public static final String PARAMETER_VALUE_PROVIDER_OP_ELEMENT_NAME = "parameterValueProviderWithConfig";
  public static final String COMPLEX_ACTING_PARAMETER_OP_ELEMENT_NAME = "complexActingParameterOP";
  public static final String ACTING_PARAMETER_GROUP_OP_ELEMENT_NAME = "actingParameterGroupOP";
  public static final String NESTED_PARAMETERS_OP_ELEMENT_NAME = "nestedVPsOperation";
  public static final String MULTIPLE_NESTED_PARAMETERS_OP_ELEMENT_NAME = "multipleNestedVPsOperation";

  public static final String CONNECTION_CLIENT_NAME_PARAMETER = "clientName";

  public static final String SOURCE_WITH_MULTI_LEVEL_VALUE ="SourceWithMultiLevelValue";

  public static final String INT_PARAM_NAME = "intParam";
  public static final String STRING_PRAM_NAME = "stringParam";
  public static final String INNER_POJO_PARAM_NAME = "innerPojoParam";
  public static final String SIMPLE_MAP_PARAM_NAME = "simpleMapParam";
  public static final String SIMPLE_LIST_PARAM_NAME = "simpleListParam";
  public static final String COMPLEX_MAP_PARAM_NAME = "complexMapParam";
  public static final String COMPLEX_LIST_PARAM_NAME = "complexListParam";

  public static final String MULTI_LEVEL_PARTIAL_TYPE_KEYS_METADATA_KEY_OP_ELEMENT_NAME = "multiLevelPartialTypeKeysMetadataKey";
  public static final String MULTI_LEVEL_METADATA_KEY_OP_ELEMENT_NAME = "multiLevelTypeKeyMetadataKey";
  public static final String MULTI_LEVEL_SHOW_IN_DSL_GROUP_PARTIAL_TYPE_KEYS_METADATA_KEY_OP_ELEMENT_NAME =
          "multiLevelShowInDslGroupPartialTypeKeysMetadataKey";

  public static final String REQUIRES_CONFIGURATION_OUTPUT_TYPE_RESOLVER_OP_ELEMENT_NAME = "requiresConfigurationOutputTypeKeyResolver";

  public static ConfigurationElementDeclaration configurationDeclaration(String name, ConnectionElementDeclaration connection) {
    ConfigurationElementDeclarer configurationElementDeclarer = TEST_EXTENSION_DECLARER.newConfiguration(CONFIG_ELEMENT_NAME)
            .withRefName(name)
            .withParameterGroup(newParameterGroup()
                                        .withParameter(ACTING_PARAMETER_NAME, name)
                                        .getDeclaration());
    if (connection != null) {
      configurationElementDeclarer.withConnection(connection);
    }
    return configurationElementDeclarer.getDeclaration();
  }

  public static ConfigurationElementDeclaration configurationDeclaration(String name) {
    return configurationDeclaration(name, null);
  }

  public static ConnectionElementDeclaration connectionDeclaration(String clientName) {
    return TEST_EXTENSION_DECLARER.newConnection(CONNECTION_ELEMENT_NAME)
            .withParameterGroup(newParameterGroup()
                                        .withParameter(CONNECTION_CLIENT_NAME_PARAMETER, clientName)
                                        .withParameter(ACTING_PARAMETER_NAME, clientName)
                                        .getDeclaration())
            .getDeclaration();
  }

  public static OperationElementDeclaration configLessConnectionLessOPDeclaration(String configName) {
    return TEST_EXTENSION_DECLARER
            .newOperation(CONFIG_LESS_CONNECTION_LESS_OP_ELEMENT_NAME)
            .withConfig(configName)
            .getDeclaration();

  }

  public static OperationElementDeclaration configLessOPDeclaration(String configName) {
    return configLessOPDeclaration(configName, null);
  }

  public static OperationElementDeclaration configLessOPDeclaration(String configName, String metadataKey) {
    OperationElementDeclarer operationElementDeclarer = TEST_EXTENSION_DECLARER
            .newOperation(CONFIG_LESS_OP_ELEMENT_NAME)
            .withConfig(configName);

    if (metadataKey != null) {
      operationElementDeclarer.withParameterGroup(newParameterGroup()
                                                          .withParameter(METADATA_KEY_PARAMETER_NAME, metadataKey).getDeclaration());
    }
    return operationElementDeclarer
            .getDeclaration();
  }

  public static OperationElementDeclaration actingParameterOPDeclaration(String configName, String actingParameter) {
    return TEST_EXTENSION_DECLARER
            .newOperation(ACTING_PARAMETER_OP_ELEMENT_NAME)
            .withConfig(configName)
            .withParameterGroup(newParameterGroup()
                                        .withParameter(ACTING_PARAMETER_NAME, actingParameter)
                                        .getDeclaration())
            .getDeclaration();
  }

  public static OperationElementDeclaration parameterValueProviderWithConfig(String configName) {
    return TEST_EXTENSION_DECLARER
            .newOperation(PARAMETER_VALUE_PROVIDER_OP_ELEMENT_NAME)
            .withConfig(configName)
            .getDeclaration();
  }

  public static ParameterValue innerPojo(int intParam,
                                         String stringParam,
                                         List<String> listParam,
                                         Map<String, String> mapParam) {
    ParameterListValue.Builder listBuilder = ParameterListValue.builder();
    listParam.forEach(listBuilder::withValue);
    ParameterObjectValue.Builder mapBuilder = ParameterObjectValue.builder();
    mapParam.forEach(mapBuilder::withParameter);
    return ParameterObjectValue.builder()
            .withParameter(INT_PARAM_NAME, Integer.toString(intParam))
            .withParameter(STRING_PRAM_NAME, stringParam)
            .withParameter(SIMPLE_LIST_PARAM_NAME, listBuilder.build())
            .withParameter(SIMPLE_MAP_PARAM_NAME, mapBuilder.build())
            .build();
  }

  public static ParameterValue complexParameterValue(int intParam,
                                                     String stringParam,
                                                     List<String> listParam,
                                                     Map<String, String> mapParam,
                                                     ParameterValue innerPojoParam,
                                                     List<ParameterValue> complexListParam,
                                                     Map<String, ParameterValue> complexMapParam) {
    ParameterListValue.Builder listBuilder = ParameterListValue.builder();
    listParam.forEach(listBuilder::withValue);

    ParameterObjectValue.Builder mapBuilder = ParameterObjectValue.builder();
    mapParam.forEach(mapBuilder::withParameter);

    ParameterListValue.Builder complexListBuilder = ParameterListValue.builder();
    complexListParam.forEach(complexListBuilder::withValue);

    ParameterObjectValue.Builder complexMapBuilder = ParameterObjectValue.builder();
    complexMapParam.forEach(complexMapBuilder::withParameter);

    return ParameterObjectValue.builder()
            .withParameter(COMPLEX_LIST_PARAM_NAME, complexListBuilder.build())
            .withParameter(COMPLEX_MAP_PARAM_NAME, complexMapBuilder.build())
            .withParameter(INNER_POJO_PARAM_NAME, innerPojoParam)
            .withParameter(INT_PARAM_NAME, Integer.toString(intParam))
            .withParameter(STRING_PRAM_NAME, stringParam)
            .withParameter(SIMPLE_LIST_PARAM_NAME, listBuilder.build())
            .withParameter(SIMPLE_MAP_PARAM_NAME, mapBuilder.build())
            .build();
  }

  public static OperationElementDeclaration complexActingParameterOPDeclaration(String configName,
                                                                                ParameterValue actingParameter) {
    return TEST_EXTENSION_DECLARER
            .newOperation(COMPLEX_ACTING_PARAMETER_OP_ELEMENT_NAME)
            .withConfig(configName)
            .withParameterGroup(newParameterGroup()
                                        .withParameter(ACTING_PARAMETER_NAME, actingParameter)
                                        .getDeclaration())
            .getDeclaration();

  }

  public static OperationElementDeclaration actingParameterGroupOPDeclaration(String configName,
                                                                              String stringValue,
                                                                              int intValue,
                                                                              List<String> listValue) {
    final ParameterListValue.Builder listBuilder = ParameterListValue.builder();
    listValue.forEach(listBuilder::withValue);
    return TEST_EXTENSION_DECLARER
            .newOperation(ACTING_PARAMETER_GROUP_OP_ELEMENT_NAME)
            .withConfig(configName)
            .withParameterGroup(newParameterGroup(ACTING_PARAMETER_GROUP_NAME)
                                        .withParameter("stringParam", stringValue)
                                        .withParameter("intParam", ParameterSimpleValue.of(String.valueOf(intValue), NUMBER))
                                        .withParameter("listParams", listBuilder.build())
                                        .getDeclaration())
            .getDeclaration();

  }

  public static OperationElementDeclaration nestedVPsOPDeclaration(String configName) {
    return TEST_EXTENSION_DECLARER
            .newOperation(NESTED_PARAMETERS_OP_ELEMENT_NAME)
            .withConfig(configName)
            .getDeclaration();

  }

  public static OperationElementDeclaration multipleNestedVPsOPDeclaration(String configName) {
    return TEST_EXTENSION_DECLARER
            .newOperation(MULTIPLE_NESTED_PARAMETERS_OP_ELEMENT_NAME)
            .withConfig(configName)
            .getDeclaration();
  }

  public static SourceElementDeclaration sourceDeclaration(String configName, String continentParameter,
                                                     String countryParameter) {
    return sourceDeclaration(configName, null, continentParameter, countryParameter);
  }

  public static SourceElementDeclaration sourceDeclaration(String configName, String actingParameter) {
    return sourceDeclaration(configName, actingParameter, null, null);
  }

  public static SourceElementDeclaration sourceDeclaration(String configName, String actingParameter, String continentParameter,
                                                     String countryParameter) {
    return sourceDeclaration(configName, actingParameter, continentParameter, countryParameter, null);
  }

  public static SourceElementDeclaration sourceDeclaration(String configName, String actingParameter, String continentParameter,
                                                     String countryParameter, String cityParameter) {
    SourceElementDeclarer sourceElementDeclarer = TEST_EXTENSION_DECLARER
            .newSource(SOURCE_ELEMENT_NAME)
            .withConfig(configName);
    if (actingParameter != null) {
      sourceElementDeclarer
              .withParameterGroup(newParameterGroup()
                                          .withParameter(INDEPENDENT_SOURCE_PARAMETER_NAME, "")
                                          .withParameter(CONNECTION_DEPENDANT_SOURCE_PARAMETER_NAME, "")
                                          .withParameter(ACTING_PARAMETER_DEPENDANT_SOURCE_PARAMETER_NAME, "")
                                          .withParameter(ACTING_PARAMETER_NAME, actingParameter)
                                          .getDeclaration());
    }

    ParameterGroupElementDeclarer parameterGroupElementDeclarer = newParameterGroup("LocationKey");

    if (continentParameter != null) {
      parameterGroupElementDeclarer.withParameter("continent", ParameterSimpleValue.of(continentParameter));
    }
    if (countryParameter != null) {
      parameterGroupElementDeclarer.withParameter("country", ParameterSimpleValue.of(countryParameter));
    }
    if (cityParameter != null) {
      parameterGroupElementDeclarer.withParameter("city", ParameterSimpleValue.of(cityParameter));
    }
    if (continentParameter != null || countryParameter != null || cityParameter != null) {
      sourceElementDeclarer.withParameterGroup(parameterGroupElementDeclarer.getDeclaration());
    }

    return sourceElementDeclarer.getDeclaration();
  }

  public static OperationElementDeclaration multiLevelOPDeclarationPartialTypeKeys(String configName, String continent, String country) {
    OperationElementDeclarer elementDeclarer = TEST_EXTENSION_DECLARER
            .newOperation(MULTI_LEVEL_PARTIAL_TYPE_KEYS_METADATA_KEY_OP_ELEMENT_NAME)
            .withConfig(configName);
    setLocationParameterGroup(continent, country, elementDeclarer, "LocationKey");
    return elementDeclarer.getDeclaration();
  }

  public static OperationElementDeclaration multiLevelOPDeclaration(String configName, String continent, String country) {
    OperationElementDeclarer elementDeclarer = TEST_EXTENSION_DECLARER
            .newOperation(MULTI_LEVEL_METADATA_KEY_OP_ELEMENT_NAME)
            .withConfig(configName);
    setLocationParameterGroup(continent, country, elementDeclarer, "LocationKey");
    return elementDeclarer.getDeclaration();
  }

  public static SourceElementDeclaration sourceWithMultiLevelValue(String configName, String continent, String country) {
    SourceElementDeclarer elementDeclarer = TEST_EXTENSION_DECLARER
            .newSource(SOURCE_WITH_MULTI_LEVEL_VALUE)
            .withConfig(configName);
    setLocationParameterGroup(continent, country, elementDeclarer, "values");

    return elementDeclarer.getDeclaration();
  }

  private static void setLocationParameterGroup(String continent, String country, ParameterizedElementDeclarer elementDeclarer, String locationKey) {
    ParameterGroupElementDeclarer parameterGroupElementDeclarer = newParameterGroup(locationKey);
    if (continent != null) {
      parameterGroupElementDeclarer.withParameter("continent", ParameterSimpleValue.of(continent));
    }
    if (country != null) {
      parameterGroupElementDeclarer.withParameter("country", ParameterSimpleValue.of(country));
    }

    if (continent != null || country != null) {
      elementDeclarer.withParameterGroup(parameterGroupElementDeclarer.getDeclaration());
    }
  }

  public static OperationElementDeclaration multiLevelCompleteOPDeclaration(String configName, String continent, String country,
                                                                      String city) {
    OperationElementDeclarer elementDeclarer = TEST_EXTENSION_DECLARER
            .newOperation(MULTI_LEVEL_PARTIAL_TYPE_KEYS_METADATA_KEY_OP_ELEMENT_NAME)
            .withConfig(configName);
    ParameterGroupElementDeclarer parameterGroupElementDeclarer = newParameterGroup("LocationKey");
    if (continent != null) {
      parameterGroupElementDeclarer.withParameter("continent", ParameterSimpleValue.of(continent));
    }
    if (country != null) {
      parameterGroupElementDeclarer.withParameter("country", ParameterSimpleValue.of(country));
    }
    if (city != null) {
      parameterGroupElementDeclarer.withParameter("city", ParameterSimpleValue.of(city));
    }

    if (continent != null || country != null) {
      elementDeclarer.withParameterGroup(parameterGroupElementDeclarer.getDeclaration());
    }
    return elementDeclarer.getDeclaration();
  }

  public static OperationElementDeclaration multiLevelShowInDslGroupOPDeclaration(String configName, String continent, String country) {
    OperationElementDeclarer elementDeclarer = TEST_EXTENSION_DECLARER
            .newOperation(MULTI_LEVEL_SHOW_IN_DSL_GROUP_PARTIAL_TYPE_KEYS_METADATA_KEY_OP_ELEMENT_NAME)
            .withConfig(configName);
    setLocationParameterGroup(continent, country, elementDeclarer, "LocationKeyShowInDsl");
    return elementDeclarer.getDeclaration();
  }

  public static ComponentElementDeclaration<?> invalidComponentDeclaration() {
    return TEST_EXTENSION_DECLARER.newConstruct("invalid").getDeclaration();
  }

  public static ComponentElementDeclaration<?> componentDeclarationWrongConfigRef() {
    return TEST_EXTENSION_DECLARER
            .newOperation(MULTI_LEVEL_SHOW_IN_DSL_GROUP_PARTIAL_TYPE_KEYS_METADATA_KEY_OP_ELEMENT_NAME)
            .withConfig(MISSING_CONFIG_ELEMENT_NAME).getDeclaration();
  }

  public static ComponentElementDeclaration<?> requiresConfigurationOutputTypeKeyResolverOP() {
    return TEST_EXTENSION_DECLARER
            .newOperation(REQUIRES_CONFIGURATION_OUTPUT_TYPE_RESOLVER_OP_ELEMENT_NAME)
            .withParameterGroup(newParameterGroup().withParameter("type", ParameterSimpleValue.of("someType")).getDeclaration())
            .getDeclaration();
  }

  public static ComponentElementDeclaration<?> invalidExtensionDeclaration() {
    return ElementDeclarer.forExtension("invalid_extension_model").newConstruct("invalid").getDeclaration();
  }

}
