/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.NUMBER;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
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
import org.mule.runtime.app.declaration.api.fluent.SourceElementDeclarer;

import java.util.List;

public interface TestExtensionAware {

  ElementDeclarer TEST_EXTENSION_DECLARER = ElementDeclarer.forExtension("ToolingSupportTest");
  String CONFIG_ELEMENT_NAME = "config";
  String CONNECTION_ELEMENT_NAME = "tstConnection";

  String PROVIDED_PARAMETER_NAME = "providedParameter";
  String ACTING_PARAMETER_NAME = "actingParameter";
  String METADATA_KEY_PARAMETER_NAME = "metadataKey";

  String SOURCE_ELEMENT_NAME = "simple";
  String INDEPENDENT_SOURCE_PARAMETER_NAME = "independentParam";
  String CONNECTION_DEPENDANT_SOURCE_PARAMETER_NAME = "connectionDependantParam";
  String ACTING_PARAMETER_DEPENDANT_SOURCE_PARAMETER_NAME = "actingParameterDependantParam";

  String CONFIG_LESS_CONNECTION_LESS_OP_ELEMENT_NAME = "configLessConnectionLessOP";
  String CONFIG_LESS_OP_ELEMENT_NAME = "configLessOP";
  String ACTING_PARAMETER_OP_ELEMENT_NAME = "actingParameterOP";
  String COMPLEX_ACTING_PARAMETER_OP_ELEMENT_NAME = "complexActingParameterOP";
  String ACTING_PARAMETER_GROUP_OP_ELEMENT_NAME = "actingParameterGroupOP";
  String NESTED_PARAMETERS_OP_ELEMENT_NAME = "nestedVPsOperation";
  String MULTIPLE_NESTED_PARAMETERS_OP_ELEMENT_NAME = "multipleNestedVPsOperation";

  String MULTI_LEVEL_PARTIAL_TYPE_KEYS_METADATA_KEY_OP_ELEMENT_NAME = "multiLevelPartialTypeKeysMetadataKey";
  String MULTI_LEVEL_SHOW_IN_DSL_GROUP_PARTIAL_TYPE_KEYS_METADATA_KEY_OP_ELEMENT_NAME =
      "multiLevelShowInDslGroupPartialTypeKeysMetadataKey";

  String CONNECTION_CLIENT_NAME_PARAMETER = "clientName";

  default ArtifactDeclaration artifactDeclaration(ConfigurationElementDeclaration config) {
    return newArtifact().withGlobalElement(config).getDeclaration();
  }

  default ConfigurationElementDeclaration configurationDeclaration(String name, ConnectionElementDeclaration connection) {
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

  default ConfigurationElementDeclaration configurationDeclaration(String name) {
    return configurationDeclaration(name, null);
  }

  default ConnectionElementDeclaration connectionDeclaration(String clientName) {
    return TEST_EXTENSION_DECLARER.newConnection(CONNECTION_ELEMENT_NAME)
        .withParameterGroup(newParameterGroup()
            .withParameter(CONNECTION_CLIENT_NAME_PARAMETER, clientName)
            .withParameter(ACTING_PARAMETER_NAME, clientName)
            .getDeclaration())
        .getDeclaration();
  }

  default OperationElementDeclaration configLessConnectionLessOPDeclaration(String configName) {
    return TEST_EXTENSION_DECLARER
        .newOperation(CONFIG_LESS_CONNECTION_LESS_OP_ELEMENT_NAME)
        .withConfig(configName)
        .getDeclaration();

  }

  default OperationElementDeclaration configLessOPDeclaration(String configName) {
    return TEST_EXTENSION_DECLARER
        .newOperation(CONFIG_LESS_OP_ELEMENT_NAME)
        .withConfig(configName)
        .getDeclaration();

  }

  default OperationElementDeclaration multiLevelOPDeclaration(String configName, String continent, String country) {
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

    if (continent != null || country != null) {
      elementDeclarer.withParameterGroup(parameterGroupElementDeclarer.getDeclaration());
    }
    return elementDeclarer.getDeclaration();
  }

  default OperationElementDeclaration multiLevelCompleteOPDeclaration(String configName, String continent, String country,
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

  default OperationElementDeclaration multiLevelShowInDslGroupOPDeclaration(String configName, String continent, String country) {
    OperationElementDeclarer elementDeclarer = TEST_EXTENSION_DECLARER
        .newOperation(MULTI_LEVEL_SHOW_IN_DSL_GROUP_PARTIAL_TYPE_KEYS_METADATA_KEY_OP_ELEMENT_NAME)
        .withConfig(configName);
    ParameterGroupElementDeclarer parameterGroupElementDeclarer = newParameterGroup("LocationKeyShowInDsl");
    if (continent != null) {
      parameterGroupElementDeclarer.withParameter("continent", ParameterSimpleValue.of(continent));
    }
    if (country != null) {
      parameterGroupElementDeclarer.withParameter("country", ParameterSimpleValue.of(country));
    }

    if (continent != null || country != null) {
      elementDeclarer.withParameterGroup(parameterGroupElementDeclarer.getDeclaration());
    }
    return elementDeclarer.getDeclaration();
  }

  default OperationElementDeclaration actingParameterOPDeclaration(String configName, String actingParameter) {
    return TEST_EXTENSION_DECLARER
        .newOperation(ACTING_PARAMETER_OP_ELEMENT_NAME)
        .withConfig(configName)
        .withParameterGroup(newParameterGroup()
            .withParameter(ACTING_PARAMETER_NAME, actingParameter)
            .getDeclaration())
        .getDeclaration();

  }

  default OperationElementDeclaration complexActingParameterOPDeclaration(String configName,
                                                                          String innerActingParameter) {
    final ParameterValue innerPojoValue =
        ParameterObjectValue.builder().ofType("InnerActingParameter").withParameter("stringParam", innerActingParameter).build();
    final ParameterValue actingParameter =
        ParameterObjectValue.builder().ofType("ActingParameter").withParameter("innerActingParameter", innerPojoValue).build();
    return TEST_EXTENSION_DECLARER
        .newOperation(COMPLEX_ACTING_PARAMETER_OP_ELEMENT_NAME)
        .withConfig(configName)
        .withParameterGroup(newParameterGroup()
            .withParameter(ACTING_PARAMETER_NAME, actingParameter)
            .getDeclaration())
        .getDeclaration();

  }

  default OperationElementDeclaration actingParameterGroupOPDeclaration(String configName,
                                                                        String stringValue,
                                                                        int intValue,
                                                                        List<String> listValue) {
    final ParameterListValue.Builder listBuilder = ParameterListValue.builder();
    listValue.forEach(listBuilder::withValue);
    return TEST_EXTENSION_DECLARER
        .newOperation(ACTING_PARAMETER_GROUP_OP_ELEMENT_NAME)
        .withConfig(configName)
        .withParameterGroup(newParameterGroup("Acting")
            .withParameter("stringParam", stringValue)
            .withParameter("intParam", ParameterSimpleValue.of(String.valueOf(intValue), NUMBER))
            .withParameter("listParams", listBuilder.build())
            .getDeclaration())
        .getDeclaration();

  }

  default OperationElementDeclaration nestedVPsOPDeclaration(String configName) {
    return TEST_EXTENSION_DECLARER
        .newOperation(NESTED_PARAMETERS_OP_ELEMENT_NAME)
        .withConfig(configName)
        .getDeclaration();

  }

  default OperationElementDeclaration multipleNestedVPsOPDeclaration(String configName) {
    return TEST_EXTENSION_DECLARER
        .newOperation(MULTIPLE_NESTED_PARAMETERS_OP_ELEMENT_NAME)
        .withConfig(configName)
        .getDeclaration();
  }

  default ComponentElementDeclaration<?> invalidComponentDeclaration() {
    return TEST_EXTENSION_DECLARER.newConstruct("invalid").getDeclaration();
  }

  default SourceElementDeclaration sourceDeclaration(String configName, String continentParameter,
                                                     String countryParameter) {
    return sourceDeclaration(configName, null, continentParameter, countryParameter);
  }

  default SourceElementDeclaration sourceDeclaration(String configName, String actingParameter) {
    return sourceDeclaration(configName, actingParameter, null, null);
  }

  default SourceElementDeclaration sourceDeclaration(String configName, String actingParameter, String continentParameter,
                                                     String countryParameter) {
    return sourceDeclaration(configName, actingParameter, continentParameter, countryParameter, null);
  }

  default SourceElementDeclaration sourceDeclaration(String configName, String actingParameter, String continentParameter,
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

}
