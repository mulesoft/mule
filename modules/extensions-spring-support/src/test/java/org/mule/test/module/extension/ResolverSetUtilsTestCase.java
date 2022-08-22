/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static java.time.Instant.ofEpochMilli;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.api.util.IOUtils.toByteArray;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.runtime.module.extension.internal.resources.BaseExtensionResourcesGeneratorAnnotationProcessor.COMPILATION_MODE;
import static org.mule.test.oauth.ConnectionType.DUO;
import static org.mule.test.oauth.ConnectionType.HYPER;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.event.NullEventFactory;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.extension.api.component.ComponentParameterization;
import org.mule.runtime.extension.api.component.value.ValueDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.oauth.ConnectionProfile;
import org.mule.test.oauth.ConnectionProperties;
import org.mule.test.oauth.ConnectionType;
import org.mule.test.oauth.TestOAuthExtension;
import org.mule.test.values.extension.MyPojo;

import java.lang.reflect.Field;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import io.qameta.allure.Description;
import org.junit.Before;
import org.junit.Test;

public class ResolverSetUtilsTestCase extends AbstractMuleContextTestCase {

  private static final String DEFAULT_PARAMETER_GROUP_NAME = "General";
  private static final String POJO_PARAMETER_NAME = "connectionProperties";

  private static final String POJO_CONNECTION_DESCRIPTION_FIELD_NAME = "connectionDescription";
  private static final String POJO_CONNECTION_TYPE_FIELD_NAME = "connectionType";
  private static final String POJO_CONNECTION_PROPERTY_GRADE_FIELD_NAME = "connectionPropertyGrade";
  private static final String POJO_CONNECTION_TIME_FIELD_NAME = "connectionTime";
  private static final String POJO_CONNECTION_IMPORTED_POJO_FIELD_NAME = "importedPojo";

  private static final String IMPORTED_POJO_ID_FIELD_NAME = "pojoId";
  private static final String IMPORTED_POJO_NAME_FIELD_NAME = "pojoName";
  private static final String IMPORTED_POJO_NUMBER_FIELD_NAME = "pojoNumber";
  private static final String IMPORTED_POJO_BOOLEAN_FIELD_NAME = "pojoBoolean";

  private static final String IMPORTED_POJO_ID_FIELD_VALUE = "pojoIdValue";
  private static final String IMPORTED_POJO_NAME_FIELD_VALUE = "pojoNameValue";
  private static final Integer IMPORTED_POJO_NUMBER_FIELD_VALUE = 1234;
  private static final boolean IMPORTED_POJO_BOOLEAN_FIELD_VALUE = false;

  private static final String POJO_CONNECTION_DESCRIPTION_FIELD_VALUE = "connectionDescriptionValue";
  private static final ConnectionType POJO_CONNECTION_TYPE_FIELD_VALUE = DUO;
  private static final String POJO_CONNECTION_PROPERTY_GRADE_FIELD_VALUE = "connectionPropertyGradeLiteralString";
  private static final ZonedDateTime POJO_CONNECTION_TIME_FIELD_VALUE =
      ZonedDateTime.ofInstant(ofEpochMilli(1619535600000l), ZoneId.of("-03:00"));
  private static final String POJO_CONNECTION_TIME_FIELD_VALUE_AS_STRING = "2021-04-27T12:00:00-03:00";
  private static final MyPojo POJO_CONNECTION_IMPORTED_POJO_FIELD_VALUE =
      new MyPojo(IMPORTED_POJO_ID_FIELD_VALUE, IMPORTED_POJO_NAME_FIELD_VALUE, IMPORTED_POJO_NUMBER_FIELD_VALUE,
                 IMPORTED_POJO_BOOLEAN_FIELD_VALUE);

  private static final ConnectionProperties POJO_PARAMETER_VALUE =
      new ConnectionProperties(POJO_CONNECTION_DESCRIPTION_FIELD_VALUE, POJO_CONNECTION_TYPE_FIELD_VALUE,
                               new Literal<String>() {

                                 @Override
                                 public Optional<String> getLiteralValue() {
                                   return of(POJO_CONNECTION_PROPERTY_GRADE_FIELD_VALUE);
                                 }

                                 @Override
                                 public Class<String> getType() {
                                   return null;
                                 }
                               },
                               POJO_CONNECTION_TIME_FIELD_VALUE, POJO_CONNECTION_IMPORTED_POJO_FIELD_VALUE);

  private static final Consumer<ValueDeclarer> IMPORTED_POJO_VALUE_DECLARER =
      importedPojoValueDeclarer -> importedPojoValueDeclarer.asObjectValue()
          .withField(IMPORTED_POJO_ID_FIELD_NAME, IMPORTED_POJO_ID_FIELD_VALUE)
          .withField(IMPORTED_POJO_NAME_FIELD_NAME, IMPORTED_POJO_NAME_FIELD_VALUE)
          .withField(IMPORTED_POJO_NUMBER_FIELD_NAME, IMPORTED_POJO_NUMBER_FIELD_VALUE.toString())
          .withField(IMPORTED_POJO_BOOLEAN_FIELD_NAME, String.valueOf(IMPORTED_POJO_BOOLEAN_FIELD_VALUE));

  private static final Consumer<ValueDeclarer> POJO_VALUE_DECLARER = valueDeclarer -> valueDeclarer.asObjectValue()
      .withField(POJO_CONNECTION_DESCRIPTION_FIELD_NAME, POJO_CONNECTION_DESCRIPTION_FIELD_VALUE)
      .withField(POJO_CONNECTION_TYPE_FIELD_NAME, POJO_CONNECTION_TYPE_FIELD_VALUE.name())
      .withField(POJO_CONNECTION_PROPERTY_GRADE_FIELD_NAME, POJO_CONNECTION_PROPERTY_GRADE_FIELD_VALUE)
      .withField(POJO_CONNECTION_TIME_FIELD_NAME, POJO_CONNECTION_TIME_FIELD_VALUE_AS_STRING)
      .withField(POJO_CONNECTION_IMPORTED_POJO_FIELD_NAME, IMPORTED_POJO_VALUE_DECLARER);

  private static String CONNECTION_PROFILE_PARAMETER_GROUP_NAME = "Connection profile";
  private static final String COMPLEX_PARAMETER_NAME_IN_SHOWINDSL_PARAMETER_GROUP = "profileConnectionProperties";
  private static final String PROFILE_LEVEL_PARAMETER_NAME = "profileLevel";
  private static final String PROFILE_LEVEL_PARAMETER_VALUE = "25";

  private static final Consumer<ValueDeclarer> PROFILE_LEVEL_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.withValue(PROFILE_LEVEL_PARAMETER_VALUE);

  private static final String NON_DEFAULT_PARAMETER_GROUP_NAME = "Connection details";
  private static final String COMPLEX_PARAMETER_NAME_IN_NON_DEFAULT_PARAMETER_GROUP = "anotherConnectionProperties";


  private static final String DETAILS_PRIORITY_PARAMETER_NAME = "detailsPriority";
  private static final String DETAILS_PRIORITY_PARAMETER_VALUE = "55";
  private static final Consumer<ValueDeclarer> DETAILS_PRIORITY_PARAMETER_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.withValue(DETAILS_PRIORITY_PARAMETER_VALUE);

  private static final String OAUTH_CONNECTION_TYPE_PARAMETER_NAME = "oauthConnectionType";
  private static final String OAUTH_CONNECTION_TYPE_PARAMETER_VALUE = "HYPER";
  private static final ConnectionType OAUTH_CONNECTION_TYPE_PARAMETER_ENUM_VALUE = HYPER;
  private static final Consumer<ValueDeclarer> OAUTH_CONNECTION_TYPE_PARAMETER_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.withValue(OAUTH_CONNECTION_TYPE_PARAMETER_VALUE);


  private static final String SECURITY_LEVEL_PARAMETER_NAME = "securityLevel";
  private static final String SECURITY_LEVEL_PARAMETER_VALUE = "100";
  private static final Consumer<ValueDeclarer> SECURITY_LEVEL_PARAMETER_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.withValue(SECURITY_LEVEL_PARAMETER_VALUE);

  private static final String SOME_NUMBERS_PARAMETER_NAME = "someConnectionNumbers";
  private static final String FIRST_SOME_NUMBER = "1";
  private static final String SECOND_SOME_NUMBER = "2";
  private static final String THIRD_SOME_NUMBER = "3";
  private static final List<Integer> SOME_NUMBERS_PARAMETER_VALUE =
      asList(new Integer[] {Integer.valueOf(FIRST_SOME_NUMBER), Integer.valueOf(SECOND_SOME_NUMBER),
          Integer.valueOf(THIRD_SOME_NUMBER)});
  private static final Consumer<ValueDeclarer> SOME_NUMBERS_PARAMETER_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.asArrayValue().withItem(FIRST_SOME_NUMBER).withItem(SECOND_SOME_NUMBER)
          .withItem(THIRD_SOME_NUMBER);

  private static final String SOME_CONNECTION_PROPERTIES_PARAMETER_NAME = "someOauthConnectionProperties";
  private static final Consumer<ValueDeclarer> SOME_CONNECTION_PROPERTIES_PARAMETER_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.asArrayValue().withItem(POJO_VALUE_DECLARER).withItem(POJO_VALUE_DECLARER);
  private static final List<ConnectionProperties> SOME_CONNECTION_PROPERTIES_PARAMETER_VALUE =
      asList(new ConnectionProperties[] {POJO_PARAMETER_VALUE, POJO_PARAMETER_VALUE});

  private static final String CONNECTION_PROPERTIES_MAP_PARAMETER_NAME = "someMapOfConnectionProperties";
  private static final String MAP_FIRST_KEY = "first";
  private static final String MAP_SECOND_KEY = "second";
  private static final Consumer<ValueDeclarer> CONNECTION_PROPERTIES_MAP_PARAMETER_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.asMapValue().withEntry(MAP_FIRST_KEY, POJO_VALUE_DECLARER).withEntry(MAP_SECOND_KEY,
                                                                                                          POJO_VALUE_DECLARER);
  private static final Map<String, ConnectionProperties> CONNECTION_PROPERTIES_MAP_PARAMETER_VALUE =
      new HashMap<String, ConnectionProperties>() {

        {
          put(MAP_FIRST_KEY, POJO_PARAMETER_VALUE);
          put(MAP_SECOND_KEY, POJO_PARAMETER_VALUE);
        }
      };


  private static final String LITERAL_STRING_PARAMETER_NAME = "literalSecurityDescription";
  private static final String LITERAL_STRING_PARAMETER_VALUE = "#[expression.in.literal]";
  private static final Consumer<ValueDeclarer> LITERAL_STRING_PARAMETER_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.withValue(LITERAL_STRING_PARAMETER_VALUE);



  private static final String TYPED_VALUE_INTEGER_PARAMETER_NAME = "typedSecurityLevel";
  private static final String TYPED_VALUE_INTEGER_PARAMETER_VALUE = "33";
  private static final Consumer<ValueDeclarer> TYPED_VALUE_INTEGER_PARAMETER_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.withValue(TYPED_VALUE_INTEGER_PARAMETER_VALUE);


  private static final String PARAMETER_RESOLVER_STRING_PARAMETER_NAME = "resolverConnectionDisplayName";
  private static final String PARAMETER_RESOLVER_STRING_PARAMETER_VALUE = "paramResolver1";
  private static final Consumer<ValueDeclarer> PARAMETER_RESOLVER_STRING_PARAMETER_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.withValue(PARAMETER_RESOLVER_STRING_PARAMETER_VALUE);


  private static final String LITERAL_STRING_PARAMETER_IN_PG_NAME = "profileDescription";
  private static final String LITERAL_STRING_PARAMETER_IN_PG_VALUE = "literal1";
  private static final Consumer<ValueDeclarer> LITERAL_STRING_PARAMETER_IN_PG_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.withValue(LITERAL_STRING_PARAMETER_IN_PG_VALUE);


  private static final String ZONED_DATE_TIME_FIELD_NAME = "connectionTime";
  private static final String ZONED_DATE_TIME_FIELD_VALUE = "2021-04-27T12:00:00-03:00";
  private static final ZonedDateTime ZONED_DATE_TIME_VALUE =
      ZonedDateTime.ofInstant(ofEpochMilli(1619535600000l), ZoneId.of("-03:00"));
  private static final Consumer<ValueDeclarer> ZONED_DATE_TIME_FIELD_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.withValue(ZONED_DATE_TIME_FIELD_VALUE);


  private static final String EXTERNAL_POJO_PARAMETER_NAME = "externalPojo";
  private static final String STACKED_POJO_PARAMETER_NAME = "stackedTypePojoParameter";
  private static final String STACKED_ARRAY_PARAMETER_NAME = "stackedTypeArrayParameters";
  private static final String STACKED_MAP_PARAMETER_NAME = "stackedTypeMapParameter";

  private static final Consumer<ValueDeclarer> STACKED_MAP_PARAMETER_VALUE_DECLARER =
      valueDeclarer -> valueDeclarer.asMapValue().withEntry(MAP_FIRST_KEY, FIRST_SOME_NUMBER).withEntry(MAP_SECOND_KEY,
                                                                                                        SECOND_SOME_NUMBER);
  private static final Map<String, Integer> STACKED_MAP_PARAMETER_VALUE =
      new HashMap<String, Integer>() {

        {
          put(MAP_FIRST_KEY, Integer.valueOf(FIRST_SOME_NUMBER));
          put(MAP_SECOND_KEY, Integer.valueOf(SECOND_SOME_NUMBER));
        }
      };

  private ReflectionCache reflectionCache = new ReflectionCache();

  private ExpressionManager expressionManager;

  private ExtensionModel testOAuthExtensionModel;
  private ParameterizedModel testParameterizedModel;

  @Before
  public void setup() throws Exception {
    testOAuthExtensionModel = loadExtension(TestOAuthExtension.class, new DefaultJavaExtensionModelLoader());

    expressionManager = muleContext.getExpressionManager();
    testParameterizedModel = testOAuthExtensionModel.getConfigurationModels().get(0).getConnectionProviders().get(0);
    MuleRegistry muleRegistry = ((DefaultMuleContext) muleContext).getRegistry();
    muleRegistry.registerObject("Extensions Manager Mock", mock(ExtensionManager.class));
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe parameters of complex types.")
  public void complexParameter() throws Exception {
    testComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME, POJO_PARAMETER_NAME, POJO_VALUE_DECLARER,
                                  POJO_PARAMETER_VALUE);
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe parameters of complex types that belongs to a parameter group that is shownInDsl.")
  public void complexParameterInShowInDslParameterGroup() throws Exception {
    testComponentParameterization(CONNECTION_PROFILE_PARAMETER_GROUP_NAME, COMPLEX_PARAMETER_NAME_IN_SHOWINDSL_PARAMETER_GROUP,
                                  POJO_VALUE_DECLARER, new ConnectionProfile(POJO_PARAMETER_VALUE, null, null));
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe parameters of simple types that belongs to a parameter group that is shownInDsl")
  public void simpleParameterInShowInDslParameterGroup() throws Exception {
    testComponentParameterization(CONNECTION_PROFILE_PARAMETER_GROUP_NAME, PROFILE_LEVEL_PARAMETER_NAME,
                                  PROFILE_LEVEL_VALUE_DECLARER,
                                  new ConnectionProfile(null, Integer.valueOf(PROFILE_LEVEL_PARAMETER_VALUE), null));
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe parameters of complex types that belongs to the default parameter group.")
  public void complexParameterInNonShowDslInNonDefaultParameterGroup() throws Exception {
    testComponentParameterization(NON_DEFAULT_PARAMETER_GROUP_NAME, COMPLEX_PARAMETER_NAME_IN_NON_DEFAULT_PARAMETER_GROUP,
                                  POJO_VALUE_DECLARER,
                                  POJO_PARAMETER_VALUE);
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe simple parameters that belongs to a non-default parameter group that is not shownInDsl.")
  public void simpleParameterInNonShowInDslNonDefaultParameterGroup() throws Exception {
    testComponentParameterization(NON_DEFAULT_PARAMETER_GROUP_NAME, DETAILS_PRIORITY_PARAMETER_NAME,
                                  DETAILS_PRIORITY_PARAMETER_VALUE_DECLARER,
                                  Integer.valueOf(DETAILS_PRIORITY_PARAMETER_VALUE));
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe enum parameters.")
  public void enumParameter() throws Exception {
    testComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME, OAUTH_CONNECTION_TYPE_PARAMETER_NAME,
                                  OAUTH_CONNECTION_TYPE_PARAMETER_VALUE_DECLARER,
                                  OAUTH_CONNECTION_TYPE_PARAMETER_ENUM_VALUE);
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe integer parameters that needs to be transformed from String.")

  public void integerParameterAsString() throws Exception {
    testComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME, SECURITY_LEVEL_PARAMETER_NAME,
                                  SECURITY_LEVEL_PARAMETER_VALUE_DECLARER,
                                  Integer.valueOf(SECURITY_LEVEL_PARAMETER_VALUE));
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe a parameter whose time is List<Integer>.")
  public void arrayOfIntegerParameter() throws Exception {
    testComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME, SOME_NUMBERS_PARAMETER_NAME,
                                  SOME_NUMBERS_PARAMETER_VALUE_DECLARER,
                                  SOME_NUMBERS_PARAMETER_VALUE);
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe a parameter whose type is a list of a complex type.")
  public void arrayOfComplexTypeParameter() throws Exception {
    testComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME, SOME_CONNECTION_PROPERTIES_PARAMETER_NAME,
                                  SOME_CONNECTION_PROPERTIES_PARAMETER_VALUE_DECLARER,
                                  SOME_CONNECTION_PROPERTIES_PARAMETER_VALUE);
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe a parameter whose type is a map with a complex value.")

  public void mapOfComplexValueTypeParameter() throws Exception {
    testComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME, CONNECTION_PROPERTIES_MAP_PARAMETER_NAME,
                                  CONNECTION_PROPERTIES_MAP_PARAMETER_VALUE_DECLARER,
                                  CONNECTION_PROPERTIES_MAP_PARAMETER_VALUE);
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe a parameter whose type is a Literal.")
  public void literalTypeParameter() throws Exception {
    Literal<String> literalSecurityDescription =
        (Literal<String>) getResolvedValueFromComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME,
                                                                        LITERAL_STRING_PARAMETER_NAME,
                                                                        LITERAL_STRING_PARAMETER_VALUE_DECLARER);
    assertThat(literalSecurityDescription.getLiteralValue().get(),
               is(LITERAL_STRING_PARAMETER_VALUE));
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe a parameter whose type is a TypedValue.")
  public void typedValueTypeParameter() throws Exception {
    TypedValue<Integer> securityLevelTypedValue =
        (TypedValue<Integer>) getResolvedValueFromComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME,
                                                                            TYPED_VALUE_INTEGER_PARAMETER_NAME,
                                                                            TYPED_VALUE_INTEGER_PARAMETER_VALUE_DECLARER);
    assertThat(securityLevelTypedValue.getValue(),
               is(Integer.valueOf(TYPED_VALUE_INTEGER_PARAMETER_VALUE)));
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe a parameter whose type is a ParameterResolver.")
  public void parameterResolverTypeParameter() throws Exception {
    ParameterResolver<String> connectionDisplayName =
        (ParameterResolver<String>) getResolvedValueFromComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME,
                                                                                  PARAMETER_RESOLVER_STRING_PARAMETER_NAME,
                                                                                  PARAMETER_RESOLVER_STRING_PARAMETER_VALUE_DECLARER);
    assertThat(connectionDisplayName.resolve(),
               is(PARAMETER_RESOLVER_STRING_PARAMETER_VALUE));
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe a parameter of Literal type inside a showInDsl parameter group.")
  public void literalTypeParameterInParameterGroupShowInDsl() throws Exception {
    Literal<String> literalProfileDescription =
        ((ConnectionProfile) (getResolvedValueFromComponentParameterization(CONNECTION_PROFILE_PARAMETER_GROUP_NAME,
                                                                            LITERAL_STRING_PARAMETER_IN_PG_NAME,
                                                                            LITERAL_STRING_PARAMETER_IN_PG_VALUE_DECLARER)))
                                                                                .getProfileDescription();
    assertThat(literalProfileDescription.getLiteralValue().get(),
               is(LITERAL_STRING_PARAMETER_IN_PG_VALUE));
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe a field of Literal type inside a complex parameter.")
  public void literalTypeParameterInPojo() throws Exception {
    Literal<String> connectionPropertiesGrade =
        ((ConnectionProperties) (getResolvedValueFromComponentParameterization(NON_DEFAULT_PARAMETER_GROUP_NAME,
                                                                               COMPLEX_PARAMETER_NAME_IN_NON_DEFAULT_PARAMETER_GROUP,
                                                                               POJO_VALUE_DECLARER)))
                                                                                   .getConnectionPropertyGrade();
    assertThat(connectionPropertiesGrade.getLiteralValue().get(),
               is(POJO_CONNECTION_PROPERTY_GRADE_FIELD_VALUE));
  }

  @Test
  @Description("Validates that ComponentParameterization API handles a complex parameter that injects a mule dependency.")
  public void complexParameterHasInjectedDependency() throws Exception {
    ConnectionProperties connectionProperties =
        ((ConnectionProperties) (getResolvedValueFromComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME, POJO_PARAMETER_NAME,
                                                                               POJO_VALUE_DECLARER)));
    assertThat(connectionProperties.getExtensionManager(), is(notNullValue()));
  }

  @Test
  @Description("Validates that ComponentParameterization API handles a map parameter with complex values that inject a mule dependency.")
  public void complexParameterInMapValueHasInjectedDependency() throws Exception {
    Map<String, ConnectionProperties> stringConnectionPropertiesMap =
        (Map<String, ConnectionProperties>) getResolvedValueFromComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME,
                                                                                          CONNECTION_PROPERTIES_MAP_PARAMETER_NAME,
                                                                                          CONNECTION_PROPERTIES_MAP_PARAMETER_VALUE_DECLARER);
    stringConnectionPropertiesMap.values()
        .forEach(connectionProperties -> assertThat(connectionProperties.getExtensionManager(), is(notNullValue())));
  }

  @Test
  @Description("Validates that ComponentParameterization API handles a list parameter with complex items that inject a mule dependency.")
  public void complexParameterInListHasInjectedDependency() throws Exception {

    List<ConnectionProperties> stringConnectionPropertiesMap =
        (List<ConnectionProperties>) getResolvedValueFromComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME,
                                                                                   SOME_CONNECTION_PROPERTIES_PARAMETER_NAME,
                                                                                   SOME_CONNECTION_PROPERTIES_PARAMETER_VALUE_DECLARER);
    stringConnectionPropertiesMap
        .forEach(connectionProperties -> assertThat(connectionProperties.getExtensionManager(), is(notNullValue())));
  }

  @Test
  @Description("Validates that ComponentParameterization API handles a parameter of complex type that inject a mule dependency and belongs to a showInDsl parameter group.")
  public void complexParameterInShowInDslParameterGroupHasInjectedDependency() throws Exception {
    ConnectionProfile connectionProfile =
        ((ConnectionProfile) (getResolvedValueFromComponentParameterization(CONNECTION_PROFILE_PARAMETER_GROUP_NAME,
                                                                            COMPLEX_PARAMETER_NAME_IN_SHOWINDSL_PARAMETER_GROUP,
                                                                            POJO_VALUE_DECLARER)));
    assertThat(connectionProfile.getProfileConnectionProperties().getExtensionManager(), is(notNullValue()));
  }

  @Test
  @Description("Validates that ComponentParameterization API handles a complex parameter with a Date type field.")
  public void complexParameterWithDateField() throws Exception {
    ConnectionProperties connectionProperties =
        (ConnectionProperties) getResolvedValueFromComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME, POJO_PARAMETER_NAME,
                                                                             POJO_VALUE_DECLARER);
    assertThat(connectionProperties.getConnectionTime(),
               is(POJO_CONNECTION_TIME_FIELD_VALUE));
  }

  @Test
  @Description("Validates that ComponentParameterization API handles a list parameter whose items are of complex type with a Date type field.")
  public void listParameterOfComplexTypeWithDateField() throws Exception {
    List<ConnectionProperties> stringConnectionPropertiesMap =
        (List<ConnectionProperties>) getResolvedValueFromComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME,
                                                                                   SOME_CONNECTION_PROPERTIES_PARAMETER_NAME,
                                                                                   SOME_CONNECTION_PROPERTIES_PARAMETER_VALUE_DECLARER);
    stringConnectionPropertiesMap
        .forEach(connectionProperties -> assertThat(connectionProperties.getConnectionTime(),
                                                    is(POJO_CONNECTION_TIME_FIELD_VALUE)));

  }

  @Test
  @Description("Validates that ComponentParameterization API handles a Date type parameter")
  public void parameterOfDateType() throws Exception {
    testComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME, ZONED_DATE_TIME_FIELD_NAME,
                                  ZONED_DATE_TIME_FIELD_VALUE_DECLARER,
                                  ZONED_DATE_TIME_VALUE);
  }


  @Test
  @Description("Validates that ComponentParameterization API handles a parameter of a type that belongs to another extension.")
  public void externalTypeParameter() throws Exception {
    testComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME, EXTERNAL_POJO_PARAMETER_NAME,
                                  IMPORTED_POJO_VALUE_DECLARER,
                                  POJO_CONNECTION_IMPORTED_POJO_FIELD_VALUE);
  }

  @Test
  @Description("Validates that ComponentParameterization API handles a complex parameter parameter with a field of a type that belongs to another extension.")
  public void externalTypeInsidePojoParameter() throws Exception {
    ConnectionProperties connectionProperties =
        (ConnectionProperties) getResolvedValueFromComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME, POJO_PARAMETER_NAME,
                                                                             POJO_VALUE_DECLARER);
    assertThat(connectionProperties.getImportedPojo(),
               is(POJO_CONNECTION_IMPORTED_POJO_FIELD_VALUE));
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe a parameter whose type is a custom pojo wrapped in a TypedValue wrapped in a ParameterResolver.")
  public void complexTypeInATypedValueInAParameterResolver() throws Exception {
    ParameterResolver<TypedValue<MyPojo>> stackedTypeParameter =
        (ParameterResolver<TypedValue<MyPojo>>) getResolvedValueFromComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME,
                                                                                              STACKED_POJO_PARAMETER_NAME,
                                                                                              IMPORTED_POJO_VALUE_DECLARER);
    assertThat(stackedTypeParameter.resolve().getValue(), is(POJO_CONNECTION_IMPORTED_POJO_FIELD_VALUE));
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe a parameter whose type is an arrylist wrapped in a TypedValue wrapped in a ParameterResolver.")
  public void listInATypedValueInAParameterResolver() throws Exception {
    ParameterResolver<TypedValue<List<Integer>>> stackedTypeParameter =
        (ParameterResolver<TypedValue<List<Integer>>>) getResolvedValueFromComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME,
                                                                                                     STACKED_ARRAY_PARAMETER_NAME,
                                                                                                     SOME_NUMBERS_PARAMETER_VALUE_DECLARER);
    assertThat(stackedTypeParameter.resolve().getValue(), is(SOME_NUMBERS_PARAMETER_VALUE));
  }

  @Test
  @Description("Validates that ComponentParameterization API can describe a parameter whose type is a map wrapped in a TypedValue wrapped in a ParameterResolver.")
  public void mapInATypedValueInAParameterResolver() throws Exception {
    ParameterResolver<TypedValue<List<Integer>>> stackedTypeParameter =
        (ParameterResolver<TypedValue<List<Integer>>>) getResolvedValueFromComponentParameterization(DEFAULT_PARAMETER_GROUP_NAME,
                                                                                                     STACKED_MAP_PARAMETER_NAME,
                                                                                                     STACKED_MAP_PARAMETER_VALUE_DECLARER);
    assertThat(stackedTypeParameter.resolve().getValue(), is(STACKED_MAP_PARAMETER_VALUE));
  }

  private void testComponentParameterization(String parameterGroupName, String parameterName,
                                             Consumer<ValueDeclarer> valueDeclarer, Object valueToCompare)
      throws Exception {
    assertThat(getResolvedValueFromComponentParameterization(parameterGroupName, parameterName, valueDeclarer),
               is(valueToCompare));
  }

  /**
   * This method creates a component parameterization with only the given parameter information and then resolves its value with
   * an empty event.
   * 
   * @param parameterGroupName    the name of the parameter group the parameter belongs to
   * @param parameterName         the name of the parameter
   * @param valueDeclarerConsumer the consumer of value declarer that represents the value of the parameter
   * @return the resolved value of the paremeter, or the whole parameter group if the parameter belongs to a showInDsl parameter
   *         group
   */
  private Object getResolvedValueFromComponentParameterization(String parameterGroupName, String parameterName,
                                                               Consumer<ValueDeclarer> valueDeclarerConsumer)
      throws Exception {
    ResolverSet resolverSet =
        ResolverSetUtils.getResolverSetFromComponentParameterization(
                                                                     ComponentParameterization.builder(testParameterizedModel)
                                                                         .withParameter(parameterGroupName, parameterName,
                                                                                        valueDeclarerConsumer)
                                                                         .build(),
                                                                     muleContext, true, reflectionCache, expressionManager,
                                                                     testParameterizedModel.getName());
    ParameterGroupModel parameterGroupModel = testParameterizedModel.getParameterGroupModels().stream()
        .filter(pgm -> pgm.getName().equals(parameterGroupName)).findAny().get();
    ValueResolvingContext valueResolvingContext = ValueResolvingContext.builder(NullEventFactory.getNullEvent()).build();
    ResolverSetResult resolverSetResult = resolverSet.resolve(valueResolvingContext);
    if (parameterGroupModel.isShowInDsl()) {
      String parameterGroupFieldName =
          ((Field) (parameterGroupModel.getModelProperty(ParameterGroupModelProperty.class).get().getDescriptor().getContainer()))
              .getName();
      return resolverSetResult.get(parameterGroupFieldName);
    } else {
      return resolverSetResult.get(parameterName);
    }
  }

  private static ExtensionModel loadExtension(Class<?> clazz, ExtensionModelLoader loader) {
    Map<String, Object> params = SmallMap.of(TYPE_PROPERTY_NAME, clazz.getName(),
                                             VERSION, getProductVersion(),
                                             COMPILATION_MODE, true);

    final DslResolvingContext dslResolvingContext = getDefault(new LinkedHashSet<>());

    final String basePackage = clazz.getPackage().toString();
    final ClassLoader pluginClassLoader = new ClassLoader(clazz.getClassLoader()) {

      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith(basePackage)) {
          byte[] classBytes;
          try {
            classBytes =
                toByteArray(this.getClass().getResourceAsStream("/" + name.replaceAll("\\.", "/") + ".class"));
            return this.defineClass(null, classBytes, 0, classBytes.length);
          } catch (Exception e) {
            return super.loadClass(name);
          }
        } else {
          return super.loadClass(name, resolve);
        }
      }
    };

    return loader.loadExtensionModel(pluginClassLoader, dslResolvingContext, params);
  }

}
