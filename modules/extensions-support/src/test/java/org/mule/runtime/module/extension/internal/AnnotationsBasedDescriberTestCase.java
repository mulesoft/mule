/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.extension.api.Category.COMMUNITY;
import static org.mule.runtime.extension.api.Category.SELECT;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.REQUIRED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TLS_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber.DEFAULT_CONNECTION_PROVIDER_NAME;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.arrayOf;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.objectTypeBuilder;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.SAUL_OFFICE_NUMBER;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.AGE;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.EXTENSION_DESCRIPTION;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.test.vegan.extension.VeganExtension.APPLE;
import static org.mule.test.vegan.extension.VeganExtension.BANANA;

import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.MuleVersion;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.config.MuleManifest;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.NamedDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OutputDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.type.annotation.TypeAliasAnnotation;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricherFactory;
import org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;
import org.mule.runtime.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.tck.message.IntegerAttributes;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.test.heisenberg.extension.HeisenbergConnectionProvider;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.HeisenbergOperations;
import org.mule.test.heisenberg.extension.HeisenbergSource;
import org.mule.test.heisenberg.extension.MoneyLaunderingOperation;
import org.mule.test.heisenberg.extension.exception.CureCancerExceptionEnricher;
import org.mule.test.heisenberg.extension.model.ExtendedPersonalInfo;
import org.mule.test.heisenberg.extension.model.HealthStatus;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.heisenberg.extension.model.types.WeaponType;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.vegan.extension.PaulMcCartneySource;
import org.mule.test.vegan.extension.VeganAttributes;
import org.mule.test.vegan.extension.VeganExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class AnnotationsBasedDescriberTestCase extends AbstractAnnotationsBasedDescriberTestCase {

  private static final String GET_GRAMS_IN_STORAGE = "getGramsInStorage";
  private static final String EXTENDED_CONFIG_NAME = "extendedConfig";
  private static final String EXTENDED_CONFIG_DESCRIPTION = "extendedDescription";
  private static final String SOURCE_NAME = "ListenPayments";
  private static final String SOURCE_PARAMETER = "initialBatchNumber";
  private static final String SAY_MY_NAME_OPERATION = "sayMyName";
  private static final String GET_ENEMY_OPERATION = "getEnemy";
  private static final String KILL_OPERATION = "kill";
  private static final String KILL_CUSTOM_OPERATION = "killWithCustomMessage";
  private static final String KILL_WITH_WEAPON = "killWithWeapon";
  private static final String KILL_WITH_RICINS = "killWithRicins";
  private static final String KILL_WITH_MULTIPLES_WEAPONS = "killWithMultiplesWeapons";
  private static final String KILL_WITH_MULTIPLE_WILDCARD_WEAPONS = "killWithMultipleWildCardWeapons";
  private static final String GET_PAYMENT_FROM_EVENT_OPERATION = "getPaymentFromEvent";
  private static final String GET_PAYMENT_FROM_MESSAGE_OPERATION = "getPaymentFromMessage";
  private static final String DIE = "die";
  private static final String KILL_MANY = "killMany";
  private static final String KILL_ONE = "killOne";
  private static final String LAUNDER_MONEY = "launder";
  private static final String INJECTED_EXTENSION_MANAGER = "getInjectedExtensionManager";
  private static final String ALIAS = "alias";
  private static final String KNOCK = "knock";
  private static final String KNOCK_MANY = "knockMany";
  private static final String CALL_SAUL = "callSaul";
  private static final String CALL_GUS_FRING = "callGusFring";
  private static final String CURE_CANCER = "cureCancer";
  private static final String GET_SAUL_PHONE = "getSaulPhone";
  private static final String GET_MEDICAL_HISTORY = "getMedicalHistory";
  private static final String IGNORED_OPERATION = "ignoredOperation";
  private static final String EXTENSION_VERSION = MuleManifest.getProductVersion();

  @Before
  public void setUp() {
    setDescriber(describerFor(HeisenbergExtension.class));
  }

  @Test
  public void describeTestModule() throws Exception {
    ExtensionDeclarer declarer = describeExtension();

    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    assertExtensionProperties(extensionDeclaration);

    assertTestModuleConfiguration(extensionDeclaration);
    assertTestModuleOperations(extensionDeclaration);
    assertTestModuleConnectionProviders(extensionDeclaration);
    assertTestModuleMessageSource(extensionDeclaration);
    assertModelProperties(extensionDeclaration);
  }

  @Test
  public void heisenbergPointer() throws Exception {
    setDescriber(describerFor(HeisenbergPointer.class));
    describeTestModule();
  }

  @Test
  public void heisenbergPointerPlusExternalConfig() throws Exception {
    setDescriber(describerFor(HeisenbergPointerPlusExternalConfig.class));
    ExtensionDeclaration extensionDeclaration = describeExtension().getDeclaration();

    assertExtensionProperties(extensionDeclaration);
    assertThat(extensionDeclaration.getConfigurations().size(), equalTo(2));

    ConfigurationDeclaration configuration = extensionDeclaration.getConfigurations().get(1);
    assertThat(configuration, is(notNullValue()));
    assertThat(configuration.getName(), equalTo(EXTENDED_CONFIG_NAME));
    assertThat(configuration.getParameters(), hasSize(29));
    assertParameter(configuration.getParameters(), "extendedProperty", "", toMetadataType(String.class), true, SUPPORTED, null);
  }

  @Test(expected = IllegalConfigurationModelDefinitionException.class)
  public void heisenbergWithOperationsConfig() throws Exception {
    describerFor(HeisenbergWithSameOperationsAndConfigs.class)
        .describe(new DefaultDescribingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader()));
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void heisenbergWithParameterGroupAsOptional() throws Exception {
    describerFor(HeisenbergWithParameterGroupAsOptional.class)
        .describe(new DefaultDescribingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader()));
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void heisenbergWithMoreThanOneConfigInOperation() throws Exception {
    describerFor(HeisenbergWithInvalidOperation.class)
        .describe(new DefaultDescribingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader()));
  }

  @Test(expected = IllegalOperationModelDefinitionException.class)
  public void heisenbergWithOperationPointingToExtension() throws Exception {
    describerFor(HeisenbergWithOperationsPointingToExtension.class)
        .describe(new DefaultDescribingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader()));
  }

  @Test(expected = IllegalConfigurationModelDefinitionException.class)
  public void heisenbergWithOperationPointingToExtensionAndDefaultConfig() throws Exception {
    describerFor(HeisenbergWithOperationsPointingToExtensionAndDefaultConfig.class)
        .describe(new DefaultDescribingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader()));
  }

  @Test
  public void messageOperationWithoutGenerics() throws Exception {
    ExtensionDeclarer declarer = describerFor(HeisenbergWithGenericlessMessageOperation.class)
        .describe(new DefaultDescribingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader()));
    OperationDeclaration operation = getOperation(declarer.getDeclaration(), "noGenerics");

    assertThat(operation.getOutput().getType(), is(instanceOf(AnyType.class)));
    assertThat(operation.getOutputAttributes().getType(), is(instanceOf(NullType.class)));
  }

  @Test
  public void flyweight() {
    setDescriber(describerFor(VeganExtension.class));
    ExtensionDeclarer declarer = describeExtension();

    final ExtensionDeclaration declaration = declarer.getDeclaration();
    final ConfigurationDeclaration appleConfiguration = findDeclarationByName(declaration.getConfigurations(), APPLE);
    final ConfigurationDeclaration bananaConfiguration = findDeclarationByName(declaration.getConfigurations(), BANANA);

    final String sourceName = PaulMcCartneySource.class.getSimpleName();
    SourceDeclaration appleSource = findDeclarationByName(appleConfiguration.getMessageSources(), sourceName);
    SourceDeclaration bananaSource = findDeclarationByName(bananaConfiguration.getMessageSources(), sourceName);
    assertThat(appleSource, is(sameInstance(bananaSource)));

    final String operationName = "spreadTheWord";
    OperationDeclaration appleOperation = findDeclarationByName(appleConfiguration.getOperations(), operationName);
    OperationDeclaration bananaOperation = findDeclarationByName(bananaConfiguration.getOperations(), operationName);

    assertThat(appleOperation, is(sameInstance(bananaOperation)));
  }

  @Test
  public void minMuleVersionIsDescribedCorrectly() {
    setDescriber(describerFor(HeisenbergExtension.class));
    ExtensionDeclarer declarer = describeExtension();
    final ExtensionDeclaration declaration = declarer.getDeclaration();
    assertThat(declaration.getMinMuleVersion(), is(new MuleVersion("4.1")));
  }

  @Test
  public void categoryIsDescribedCorrectly() {
    setDescriber(describerFor(HeisenbergExtension.class));
    ExtensionDeclarer declarer = describeExtension();
    final ExtensionDeclaration declaration = declarer.getDeclaration();
    assertThat(declaration.getCategory(), is(SELECT));
  }

  @Test
  public void minMuleVersionDefaultValueIsDescribedCorrectly() {
    setDescriber(describerFor(PetStoreConnector.class));
    ExtensionDeclarer declarer = describeExtension();
    final ExtensionDeclaration declaration = declarer.getDeclaration();
    assertThat(declaration.getMinMuleVersion(), is(new MuleVersion("4.0")));
  }

  @Test
  public void categoryDefaultValueIsDescribedCorrectly() {
    setDescriber(describerFor(PetStoreConnector.class));
    ExtensionDeclarer declarer = describeExtension();
    final ExtensionDeclaration declaration = declarer.getDeclaration();
    assertThat(declaration.getCategory(), is(COMMUNITY));
  }

  @Test
  public void interceptingOperationWithoutAttributes() {
    setDescriber(describerFor(VeganExtension.class));
    ExtensionDeclarer declarer = describeExtension();
    final ExtensionDeclaration declaration = declarer.getDeclaration();

    OperationDeclaration operation = getOperation(getConfiguration(declaration, BANANA), "getLunch");
    assertThat(operation, is(notNullValue()));
    assertOutputType(operation.getOutput(), toMetadataType(Fruit.class), false);
    assertOutputType(operation.getOutputAttributes(), TYPE_BUILDER.nullType().build(), false);
  }

  @Test
  public void interceptingOperationWithAttributes() {
    setDescriber(describerFor(VeganExtension.class));
    ExtensionDeclarer declarer = describeExtension();
    final ExtensionDeclaration declaration = declarer.getDeclaration();

    OperationDeclaration operation = getOperation(getConfiguration(declaration, BANANA), "getQualifiedLunch");
    assertThat(operation, is(notNullValue()));
    assertOutputType(operation.getOutput(), toMetadataType(Fruit.class), false);
    assertOutputType(operation.getOutputAttributes(), toMetadataType(VeganAttributes.class), false);
  }

  private <T extends NamedDeclaration> T findDeclarationByName(Collection<T> declarations, String name) {
    return declarations.stream().filter(decl -> decl.getName().equals(name)).findFirst()
        .orElseThrow(() -> new NoSuchElementException());
  }


  private void assertTestModuleConfiguration(ExtensionDeclaration extensionDeclaration) throws Exception {
    assertThat(extensionDeclaration.getConfigurations(), hasSize(1));
    ConfigurationDeclaration conf = extensionDeclaration.getConfigurations().get(0);
    assertThat(conf.getName(), equalTo(DEFAULT_CONFIG_NAME));

    List<ParameterDeclaration> parameters = conf.getParameters();
    assertThat(parameters, hasSize(28));

    assertParameter(parameters, "myName", "", toMetadataType(String.class), false, SUPPORTED, HEISENBERG);
    assertParameter(parameters, "age", "", toMetadataType(Integer.class), false, SUPPORTED, AGE);
    assertParameter(parameters, "enemies", "", listOfString(), true, SUPPORTED, null);
    assertParameter(parameters, "money", "", toMetadataType(BigDecimal.class), true, SUPPORTED, null);
    assertParameter(parameters, "cancer", "", toMetadataType(boolean.class), true, SUPPORTED, null);
    assertParameter(parameters, "cancer", "", toMetadataType(boolean.class), true, SUPPORTED, null);
    assertParameter(parameters, "dateOfBirth", "", toMetadataType(Date.class), true, SUPPORTED, null);
    assertParameter(parameters, "dateOfDeath", "", toMetadataType(Calendar.class), true, SUPPORTED, null);
    assertParameter(parameters, "dateOfConception", "", toMetadataType(LocalDateTime.class), false, SUPPORTED, null);

    assertParameter(parameters, "recipe", "",
                    TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                        .ofKey(TYPE_BUILDER.stringType().id(String.class.getName()))
                        .ofValue(TYPE_BUILDER.numberType().id("java.lang.Long")).build(),
                    false, SUPPORTED, null);

    assertParameter(parameters, "ricinPacks", "", arrayOf(Set.class, objectTypeBuilder(Ricin.class)), false, SUPPORTED, null);

    assertParameter(parameters, "nextDoor", "", toMetadataType(KnockeableDoor.class), false, SUPPORTED, null);
    assertParameter(parameters, "candidateDoors", "",
                    TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                        .ofKey(TYPE_BUILDER.stringType().id(String.class.getName()))
                        .ofValue(objectTypeBuilder(KnockeableDoor.class)).build(),
                    false, SUPPORTED, null);

    assertParameter(parameters, "initialHealth", "", toMetadataType(HealthStatus.class), false, SUPPORTED, "CANCER");
    assertParameter(parameters, "finalHealth", "", toMetadataType(HealthStatus.class), true, SUPPORTED, null);
    assertParameter(parameters, "labAddress", "", toMetadataType(String.class), false, REQUIRED, null);
    assertParameter(parameters, "firstEndevour", "", toMetadataType(String.class), false, NOT_SUPPORTED, null);
    assertParameter(parameters, "weapon", "", toMetadataType(Weapon.class), false, SUPPORTED, null);
    assertParameter(parameters, "weaponTypeFunction", "",
                    TYPE_BUILDER.objectType().id(Function.class.getName())
                        .with(new ClassInformationAnnotation(Function.class, asList(MuleEvent.class, WeaponType.class)))
                        .with(new TypeAliasAnnotation(Function.class.getSimpleName())).build(),
                    false, SUPPORTED, null);
    assertParameter(parameters, "wildCardWeapons", "", arrayOf(List.class, objectTypeBuilder(Weapon.class)), false, SUPPORTED,
                    null);
    assertParameter(parameters, "wildCardList", "", arrayOf(List.class, objectTypeBuilder(Object.class)), false, SUPPORTED, null);
    assertParameter(parameters,
                    "wildCardWeaponMap", "", TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                        .ofKey(objectTypeBuilder(Weapon.class)).ofValue(objectTypeBuilder(Object.class)).build(),
                    false, SUPPORTED, null);

    assertParameter(parameters, "monthlyIncomes", "", arrayOf(List.class, TYPE_BUILDER.numberType().id(Long.class.getName())),
                    true, SUPPORTED, null);
    assertParameter(parameters, "labeledRicin", "",
                    TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                        .ofKey(TYPE_BUILDER.stringType().id(String.class.getName())).ofValue(objectTypeBuilder(Ricin.class))
                        .build(),
                    false, SUPPORTED, null);
    assertParameter(parameters, "deathsBySeasons", "",
                    TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                        .ofKey(TYPE_BUILDER.stringType().id(String.class.getName())).ofValue(TYPE_BUILDER.arrayType()
                            .id(List.class.getName()).of(TYPE_BUILDER.stringType().id(String.class.getName())))
                        .build(),
                    false, SUPPORTED, null);
    assertParameter(parameters, "weaponValueMap", "",
                    TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                        .ofKey(TYPE_BUILDER.stringType().id(String.class.getName())).ofValue(TYPE_LOADER.load(Weapon.class))
                        .build(),
                    false, SUPPORTED, null);
    assertParameter(parameters, "healthProgressions", "",
                    TYPE_BUILDER.arrayType().id(List.class.getName()).of(TYPE_LOADER.load(HealthStatus.class)).build(), false,
                    SUPPORTED, null);
  }

  private void assertExtensionProperties(ExtensionDeclaration extensionDeclaration) {
    assertThat(extensionDeclaration, is(notNullValue()));

    assertThat(extensionDeclaration.getName(), is(HEISENBERG));
    assertThat(extensionDeclaration.getDescription(), is(EXTENSION_DESCRIPTION));
    assertThat(extensionDeclaration.getVersion(), is(EXTENSION_VERSION));
  }

  private void assertTestModuleOperations(ExtensionDeclaration extensionDeclaration) throws Exception {
    assertThat(extensionDeclaration.getOperations(), hasSize(25));
    assertOperation(extensionDeclaration, SAY_MY_NAME_OPERATION, "");
    assertOperation(extensionDeclaration, GET_ENEMY_OPERATION, "");
    assertOperation(extensionDeclaration, KILL_OPERATION, "");
    assertOperation(extensionDeclaration, KILL_CUSTOM_OPERATION, "");
    assertOperation(extensionDeclaration, KILL_WITH_WEAPON, "");
    assertOperation(extensionDeclaration, KILL_WITH_RICINS, "");
    assertOperation(extensionDeclaration, KILL_WITH_MULTIPLES_WEAPONS, "");
    assertOperation(extensionDeclaration, KILL_WITH_MULTIPLE_WILDCARD_WEAPONS, "");
    assertOperation(extensionDeclaration, GET_PAYMENT_FROM_EVENT_OPERATION, "");
    assertOperation(extensionDeclaration, GET_PAYMENT_FROM_MESSAGE_OPERATION, "");
    assertOperation(extensionDeclaration, DIE, "");
    assertOperation(extensionDeclaration, KILL_MANY, "");
    assertOperation(extensionDeclaration, KILL_ONE, "");
    assertOperation(extensionDeclaration, LAUNDER_MONEY, "");
    assertOperation(extensionDeclaration, INJECTED_EXTENSION_MANAGER, "");
    assertOperation(extensionDeclaration, ALIAS, "");
    assertOperation(extensionDeclaration, CALL_SAUL, "");
    assertOperation(extensionDeclaration, CALL_GUS_FRING, "");
    assertOperation(extensionDeclaration, GET_SAUL_PHONE, "");
    assertOperation(extensionDeclaration, GET_MEDICAL_HISTORY, "");
    assertOperation(extensionDeclaration, GET_GRAMS_IN_STORAGE, "");

    OperationDeclaration operation = getOperation(extensionDeclaration, SAY_MY_NAME_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getParameters().isEmpty(), is(true));

    operation = getOperation(extensionDeclaration, GET_ENEMY_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getParameters(), hasSize(1));
    assertThat(operation.getOutput().getType(), equalTo(toMetadataType(String.class)));
    assertThat(operation.getOutputAttributes().getType(), equalTo(toMetadataType(IntegerAttributes.class)));
    assertParameter(operation.getParameters(), "index", "", toMetadataType(int.class), false, SUPPORTED, "0");

    operation = getOperation(extensionDeclaration, KILL_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getParameters(), hasSize(2));
    assertThat(operation.getOutput().getType(), equalTo(toMetadataType(String.class)));
    assertThat(operation.getOutputAttributes().getType(), is(instanceOf(NullType.class)));
    assertParameter(operation.getParameters(), "victim", "", toMetadataType(String.class), false, SUPPORTED, "#[payload]");
    assertParameter(operation.getParameters(), "goodbyeMessage", "", toMetadataType(String.class), true, SUPPORTED, null);

    operation = getOperation(extensionDeclaration, KILL_WITH_WEAPON);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getParameters(), hasSize(3));
    assertParameter(operation.getParameters(), "weapon", "", toMetadataType(Weapon.class), true, SUPPORTED, null);
    assertParameter(operation.getParameters(), "type", "", toMetadataType(WeaponType.class), true, SUPPORTED, null);
    assertParameter(operation.getParameters(), "attributesOfWeapon", "", toMetadataType(Weapon.WeaponAttributes.class), true,
                    SUPPORTED, null);

    operation = getOperation(extensionDeclaration, KILL_WITH_RICINS);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getParameters(), hasSize(1));
    assertParameter(operation.getParameters(), "ricinList", "", arrayOf(List.class, objectTypeBuilder(Ricin.class)), false,
                    SUPPORTED, "#[payload]");

    operation = getOperation(extensionDeclaration, KILL_WITH_MULTIPLES_WEAPONS);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getParameters(), hasSize(1));
    assertParameter(operation.getParameters(), "weaponList", "", arrayOf(List.class, objectTypeBuilder(Weapon.class)), false,
                    SUPPORTED, "#[payload]");

    operation = getOperation(extensionDeclaration, KILL_WITH_MULTIPLE_WILDCARD_WEAPONS);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getParameters(), hasSize(1));
    assertParameter(operation.getParameters(), "wildCardWeapons", "", arrayOf(List.class, objectTypeBuilder(Weapon.class)), true,
                    SUPPORTED, null);

    operation = getOperation(extensionDeclaration, KILL_CUSTOM_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getParameters(), hasSize(2));
    assertParameter(operation.getParameters(), "victim", "", toMetadataType(String.class), false, SUPPORTED, "#[payload]");
    assertParameter(operation.getParameters(), "goodbyeMessage", "", toMetadataType(String.class), true, SUPPORTED, null);

    operation = getOperation(extensionDeclaration, GET_PAYMENT_FROM_EVENT_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getParameters().isEmpty(), is(true));

    operation = getOperation(extensionDeclaration, GET_PAYMENT_FROM_MESSAGE_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getOutput().getType(), is(instanceOf(NullType.class)));
    assertThat(operation.getOutputAttributes().getType(), is(instanceOf(NullType.class)));
    assertThat(operation.getParameters().isEmpty(), is(true));

    operation = getOperation(extensionDeclaration, LAUNDER_MONEY);
    assertParameter(operation.getParameters(), "amount", "", toMetadataType(long.class), true, SUPPORTED, null);

    operation = getOperation(extensionDeclaration, INJECTED_EXTENSION_MANAGER);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getParameters().isEmpty(), is(true));

    operation = getOperation(extensionDeclaration, ALIAS);
    assertParameter(operation.getParameters(), "greeting", "", toMetadataType(String.class), true, SUPPORTED, null);
    assertParameter(operation.getParameters(), "myName", "", toMetadataType(String.class), false, SUPPORTED, HEISENBERG);
    assertParameter(operation.getParameters(), "age", "", toMetadataType(Integer.class), false, SUPPORTED, AGE);

    operation = getOperation(extensionDeclaration, KNOCK);
    assertParameter(operation.getParameters(), "knockedDoor", "", toMetadataType(KnockeableDoor.class), true, SUPPORTED, null);

    operation = getOperation(extensionDeclaration, KNOCK_MANY);
    assertParameter(operation.getParameters(), "doors", "", arrayOf(List.class, objectTypeBuilder(KnockeableDoor.class)), true,
                    SUPPORTED, null);

    operation = getOperation(extensionDeclaration, CALL_SAUL);
    assertThat(operation.getParameters(), is(empty()));

    operation = getOperation(extensionDeclaration, CURE_CANCER);
    assertThat(operation.getParameters(), is(empty()));
    java.util.Optional<ExceptionEnricherFactory> exceptionEnricherFactory = operation.getExceptionEnricherFactory();
    assertThat(exceptionEnricherFactory.isPresent(), is(true));
    assertThat(exceptionEnricherFactory.get().createEnricher(), instanceOf(CureCancerExceptionEnricher.class));

    operation = getOperation(extensionDeclaration, GET_MEDICAL_HISTORY);
    assertParameter(operation.getParameters(), "healthByYear", "",
                    TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                        .ofKey(TYPE_BUILDER.numberType().id(Integer.class.getName()))
                        .ofValue(TYPE_LOADER.load(HealthStatus.class)).build(),
                    true, SUPPORTED, null);

    operation = getOperation(extensionDeclaration, GET_GRAMS_IN_STORAGE);
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getParameters(), "grams", "", TYPE_LOADER.load(int[][].class), false, SUPPORTED, "#[payload]");
    assertThat(operation.getOutput().getType(), is(TYPE_LOADER.load(int[][].class)));

    operation = getOperation(extensionDeclaration, IGNORED_OPERATION);
    assertThat(operation, is(nullValue()));

  }

  private void assertTestModuleConnectionProviders(ExtensionDeclaration extensionDeclaration) throws Exception {
    assertThat(extensionDeclaration.getConnectionProviders(), hasSize(1));
    ConnectionProviderDeclaration connectionProvider = extensionDeclaration.getConnectionProviders().get(0);
    assertThat(connectionProvider.getName(), is(DEFAULT_CONNECTION_PROVIDER_NAME));

    List<ParameterDeclaration> parameters = connectionProvider.getParameters();
    assertThat(parameters, hasSize(2));

    assertParameter(parameters, "saulPhoneNumber", "", toMetadataType(String.class), false, SUPPORTED, SAUL_OFFICE_NUMBER);
    assertParameter(parameters, TLS_ATTRIBUTE_NAME, "", toMetadataType(TlsContextFactory.class), false, NOT_SUPPORTED, null);
    ImplementingTypeModelProperty typeModelProperty =
        connectionProvider.getModelProperty(ImplementingTypeModelProperty.class).get();
    assertThat(typeModelProperty.getType(), equalTo(HeisenbergConnectionProvider.class));
  }

  private void assertTestModuleMessageSource(ExtensionDeclaration extensionDeclaration) throws Exception {
    assertThat(extensionDeclaration.getConnectionProviders(), hasSize(1));
    SourceDeclaration source = extensionDeclaration.getMessageSources().get(0);
    assertThat(source.getName(), is(SOURCE_NAME));

    List<ParameterDeclaration> parameters = source.getParameters();
    assertThat(parameters, hasSize(2));

    assertParameter(parameters, SOURCE_PARAMETER, "", toMetadataType(int.class), true, SUPPORTED, null);
    ImplementingTypeModelProperty typeModelProperty = source.getModelProperty(ImplementingTypeModelProperty.class).get();
    assertThat(typeModelProperty.getType(), equalTo(HeisenbergSource.class));
  }

  private void assertOperation(ExtensionDeclaration extensionDeclaration, String operationName, String operationDescription)
      throws Exception {
    OperationDeclaration operation = getOperation(extensionDeclaration, operationName);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getDescription(), equalTo(operationDescription));
  }

  private void assertParameter(List<ParameterDeclaration> parameters, String name, String description, MetadataType metadataType,
                               boolean required, ExpressionSupport expressionSupport, Object defaultValue) {
    ParameterDeclaration param = findParameter(parameters, name);
    assertThat(param, is(notNullValue()));

    assertThat(param.getName(), equalTo(name));
    assertThat(param.getDescription(), equalTo(description));
    assertThat(param.getType(), is(metadataType));
    assertThat(param.isRequired(), is(required));
    assertThat(param.getExpressionSupport(), is(expressionSupport));
    assertThat(param.getDefaultValue(), equalTo(defaultValue));
  }

  private void assertOutputType(OutputDeclaration output, MetadataType type, boolean isDynamic) {
    assertThat(output.getType(), equalTo(type));
    assertThat(output.hasDynamicType(), is(isDynamic));
  }

  private MetadataType listOfString() {
    return TYPE_BUILDER.arrayType().id(List.class.getName()).of(TYPE_BUILDER.stringType().id(String.class.getName())).build();
  }

  protected void assertModelProperties(ExtensionDeclaration extensionDeclaration) {
    ImplementingTypeModelProperty implementingTypeModelProperty =
        extensionDeclaration.getModelProperty(ImplementingTypeModelProperty.class).get();
    assertThat(implementingTypeModelProperty, is(notNullValue()));
    assertThat(HeisenbergExtension.class.isAssignableFrom(implementingTypeModelProperty.getType()), is(true));
  }

  @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Configurations(HeisenbergExtension.class)
  @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
  @Providers(HeisenbergConnectionProvider.class)
  @Sources(HeisenbergSource.class)
  public static class HeisenbergPointer extends HeisenbergExtension {

  }

  @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Configurations({HeisenbergExtension.class, NamedHeisenbergAlternateConfig.class})
  @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
  public static class HeisenbergPointerPlusExternalConfig {

  }

  @Configuration(name = EXTENDED_CONFIG_NAME, description = EXTENDED_CONFIG_DESCRIPTION)
  @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
  public static class NamedHeisenbergAlternateConfig extends HeisenbergAlternateConfig {

  }

  @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Operations({DuplicateConfigOperation.class})
  public static class HeisenbergWithInvalidOperation extends HeisenbergExtension {

  }

  @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
  public static class HeisenbergWithParameterGroupAsOptional extends HeisenbergExtension {

    @ParameterGroup
    @Optional
    private ExtendedPersonalInfo personalInfo;

  }

  @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Operations(HeisenbergAlternateConfig.class)
  @Configurations(HeisenbergAlternateConfig.class)
  public static class HeisenbergWithSameOperationsAndConfigs extends HeisenbergExtension {

  }

  @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Operations(HeisenbergExtension.class)
  @Configurations(HeisenbergIsolatedConfig.class)
  public static class HeisenbergWithOperationsPointingToExtension extends HeisenbergExtension {

  }

  @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Operations(HeisenbergExtension.class)
  public static class HeisenbergWithOperationsPointingToExtensionAndDefaultConfig extends HeisenbergExtension {

  }

  @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Operations({HeisenbergExtension.class, GenericlessMessageOperation.class})
  public static class HeisenbergWithGenericlessMessageOperation {

  }

  public static class DuplicateConfigOperation {

    public Long launder(@UseConfig HeisenbergExtension config, @UseConfig HeisenbergExtension config2) {
      return 10L;
    }

  }

  public static class GenericlessMessageOperation {

    public OperationResult noGenerics() {
      return null;
    }
  }

  public static class HeisenbergAlternateConfig extends HeisenbergExtension {

    @Parameter
    private String extendedProperty;

    public String getExtendedProperty() {
      return extendedProperty;
    }

    public void setExtendedProperty(String extendedProperty) {
      this.extendedProperty = extendedProperty;
    }

  }

  public static class HeisenbergIsolatedConfig {

    @Parameter
    private String extendedProperty;

    public String getExtendedProperty() {
      return extendedProperty;
    }

    public void setExtendedProperty(String extendedProperty) {
      this.extendedProperty = extendedProperty;
    }
  }
}
