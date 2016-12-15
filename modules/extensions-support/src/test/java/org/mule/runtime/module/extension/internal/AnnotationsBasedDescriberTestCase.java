/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TLS_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber.DEFAULT_CONNECTION_PROVIDER_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.SAUL_OFFICE_NUMBER;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.AGE;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.EXTENSION_DESCRIPTION;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.arrayOf;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.objectTypeBuilder;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.vegan.extension.VeganExtension.APPLE;
import static org.mule.test.vegan.extension.VeganExtension.BANANA;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.VoidType;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.config.MuleManifest;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.model.property.PagedOperationModelProperty;
import org.mule.runtime.extension.api.runtime.exception.ExceptionEnricherFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.model.property.ExceptionEnricherModelProperty;
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
import org.mule.test.heisenberg.extension.model.HealthStatus;
import org.mule.test.heisenberg.extension.model.Investment;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.SaleInfo;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.heisenberg.extension.model.types.WeaponType;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.vegan.extension.PaulMcCartneySource;
import org.mule.test.vegan.extension.VeganAttributes;
import org.mule.test.vegan.extension.VeganExtension;

import com.google.common.reflect.TypeToken;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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
  private static final String SOURCE_CALLBACK_PARAMETER = "payment";
  private static final String SOURCE_REPEATED_CALLBACK_PARAMETER = "sameNameParameter";
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
  private static final String GET_PAGED_PERSONAL_INFO_OPERATION = "getPagedPersonalInfo";
  private static final String EMPTY_PAGED_OPERATION = "emptyPagedOperation";
  private static final String FAILING_PAGED_OPERATION = "failingPagedOperation";
  private static final String CONNECTION_PAGED_OPERATION = "pagedOperationUsingConnection";
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
  private static final String APPROVE_INVESTMENT = "approve";
  private static final String IGNORED_OPERATION = "ignoredOperation";
  private static final String EXTENSION_VERSION = MuleManifest.getProductVersion();
  private static final String OTHER_HEISENBERG = "OtherHeisenberg";
  private static final String PROCESS_WEAPON = "processWeapon";
  private static final String PROCESS_WEAPON_WITH_DEFAULT_VALUE = "processWeaponWithDefaultValue";
  private static final String PROCESS_INFO = "processSale";
  private static final String FAIL_TO_EXECUTE = "failToExecute";

  @Before
  public void setUp() {
    setDescriber(describerFor(HeisenbergExtension.class));
  }

  @Test
  public void describeTestModule() throws Exception {
    ExtensionDeclarer declarer = describeExtension();

    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    assertExtensionProperties(extensionDeclaration, HEISENBERG);

    assertTestModuleConfiguration(extensionDeclaration);
    assertTestModuleOperations(extensionDeclaration);
    assertTestModuleConnectionProviders(extensionDeclaration);
    assertTestModuleMessageSource(extensionDeclaration);
    assertModelProperties(extensionDeclaration);
  }

  @Test
  public void heisenbergPointer() throws Exception {
    setDescriber(describerFor(HeisenbergPointer.class));
    ExtensionDeclarer declarer = describeExtension();

    ExtensionDeclaration extensionDeclaration = declarer.getDeclaration();
    assertExtensionProperties(extensionDeclaration, OTHER_HEISENBERG);

    assertTestModuleConfiguration(extensionDeclaration);
    assertTestModuleOperations(extensionDeclaration);
    assertTestModuleConnectionProviders(extensionDeclaration);
    assertTestModuleMessageSource(extensionDeclaration);
    assertModelProperties(extensionDeclaration);
  }

  @Test
  public void heisenbergPointerPlusExternalConfig() throws Exception {
    setDescriber(describerFor(HeisenbergPointerPlusExternalConfig.class));
    ExtensionDeclaration extensionDeclaration = describeExtension().getDeclaration();

    assertExtensionProperties(extensionDeclaration, OTHER_HEISENBERG);
    assertThat(extensionDeclaration.getConfigurations().size(), equalTo(2));

    ConfigurationDeclaration configuration = extensionDeclaration.getConfigurations().get(1);
    assertThat(configuration, is(notNullValue()));
    assertThat(configuration.getName(), equalTo(EXTENDED_CONFIG_NAME));
    assertThat(configuration.getAllParameters(), hasSize(30));
    assertParameter(configuration.getAllParameters(), "extendedProperty", "", toMetadataType(String.class), true, SUPPORTED,
                    null);
  }

  @Test(expected = IllegalConfigurationModelDefinitionException.class)
  public void heisenbergWithOperationsConfig() throws Exception {
    describerFor(HeisenbergWithSameOperationsAndConfigs.class)
        .describe(new DefaultDescribingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader()));
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void heisenbergWithParameterGroupAsOptional() throws Exception {
    describerFor(HeisenbergWithParameterGroupAsOptional.class)
        .describe(new DefaultDescribingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader()));
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void heisenbergWithRecursiveParameterGroup() throws Exception {
    describerFor(HeisenbergWithRecursiveParameterGroup.class)
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
    assertThat(operation.getOutputAttributes().getType(), is(instanceOf(VoidType.class)));
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
    assertOutputType(operation.getOutputAttributes(), TYPE_BUILDER.voidType().build(), false);
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

    List<ParameterDeclaration> parameters = conf.getAllParameters();
    assertThat(parameters, hasSize(29));

    assertParameter(parameters, "myName", "", toMetadataType(String.class), false, SUPPORTED, HEISENBERG);
    assertParameter(parameters, "age", "", toMetadataType(Integer.class), false, SUPPORTED, AGE);
    assertParameter(parameters, "enemies", "", listOfString(), true, SUPPORTED, null);
    assertParameter(parameters, "money", "", toMetadataType(BigDecimal.class), true, SUPPORTED, null);
    assertParameter(parameters, "cancer", "", toMetadataType(boolean.class), true, SUPPORTED, null);
    assertParameter(parameters, "cancer", "", toMetadataType(boolean.class), true, SUPPORTED, null);
    assertParameter(parameters, "dateOfBirth", "", toMetadataType(Date.class), false, SUPPORTED, null);
    assertParameter(parameters, "dateOfDeath", "", toMetadataType(Calendar.class), false, SUPPORTED, null);
    assertParameter(parameters, "dateOfConception", "", toMetadataType(LocalDateTime.class), false, SUPPORTED, null);
    assertParameter(parameters, "dateOfGraduation", "", toMetadataType(Calendar.class), false, NOT_SUPPORTED, null);

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
    assertParameter(parameters, "weaponTypeFunction", "", toMetadataType(WeaponType.class), false, SUPPORTED, null);
    assertParameter(parameters, "wildCardWeapons", "", arrayOf(List.class, objectTypeBuilder(Weapon.class)), false, SUPPORTED,
                    null);
    assertParameter(parameters, "wildCards", "", arrayOf(List.class, objectTypeBuilder(Object.class)), false, SUPPORTED, null);
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
                            .id(List.class.getName())
                            .of(TYPE_BUILDER.stringType()
                                .id(String.class
                                    .getName())))
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

  private void assertExtensionProperties(ExtensionDeclaration extensionDeclaration, String expectedName) {
    assertThat(extensionDeclaration, is(notNullValue()));

    assertThat(extensionDeclaration.getName(), is(expectedName));
    assertThat(extensionDeclaration.getDescription(), is(EXTENSION_DESCRIPTION));
    assertThat(extensionDeclaration.getVersion(), is(EXTENSION_VERSION));
  }

  private void assertTestModuleOperations(ExtensionDeclaration extensionDeclaration) throws Exception {
    assertThat(extensionDeclaration.getOperations(), hasSize(26));

    WithOperationsDeclaration withOperationsDeclaration = extensionDeclaration.getConfigurations().get(0);
    assertThat(withOperationsDeclaration.getOperations().size(), is(8));
    assertOperation(withOperationsDeclaration, SAY_MY_NAME_OPERATION, "");
    assertOperation(withOperationsDeclaration, GET_ENEMY_OPERATION, "");
    assertOperation(extensionDeclaration, KILL_OPERATION, "");
    assertOperation(extensionDeclaration, KILL_CUSTOM_OPERATION, "");
    assertOperation(extensionDeclaration, KILL_WITH_WEAPON, "");
    assertOperation(extensionDeclaration, KILL_WITH_RICINS, "");
    assertOperation(extensionDeclaration, KILL_WITH_MULTIPLES_WEAPONS, "");
    assertOperation(extensionDeclaration, KILL_WITH_MULTIPLE_WILDCARD_WEAPONS, "");
    assertOperation(withOperationsDeclaration, GET_PAYMENT_FROM_EVENT_OPERATION, "");
    assertOperation(withOperationsDeclaration, GET_PAYMENT_FROM_MESSAGE_OPERATION, "");
    assertOperation(withOperationsDeclaration, DIE, "");
    assertOperation(extensionDeclaration, KILL_MANY, "");
    assertOperation(extensionDeclaration, KILL_ONE, "");
    assertOperation(withOperationsDeclaration, LAUNDER_MONEY, "");
    assertOperation(extensionDeclaration, INJECTED_EXTENSION_MANAGER, "");
    assertOperation(extensionDeclaration, ALIAS, "");
    assertOperation(withOperationsDeclaration, CALL_SAUL, "");
    assertOperation(extensionDeclaration, CALL_GUS_FRING, "");
    assertOperation(withOperationsDeclaration, GET_SAUL_PHONE, "");
    assertOperation(extensionDeclaration, GET_MEDICAL_HISTORY, "");
    assertOperation(extensionDeclaration, GET_GRAMS_IN_STORAGE, "");
    assertOperation(extensionDeclaration, APPROVE_INVESTMENT, "");
    assertOperation(extensionDeclaration, GET_PAGED_PERSONAL_INFO_OPERATION, "");
    assertOperation(extensionDeclaration, EMPTY_PAGED_OPERATION, "");
    assertOperation(extensionDeclaration, FAILING_PAGED_OPERATION, "");
    assertOperation(extensionDeclaration, CONNECTION_PAGED_OPERATION, "");
    assertOperation(extensionDeclaration, PROCESS_INFO, "");
    assertOperation(extensionDeclaration, PROCESS_WEAPON, "");
    assertOperation(extensionDeclaration, PROCESS_WEAPON_WITH_DEFAULT_VALUE, "");
    assertOperation(extensionDeclaration, FAIL_TO_EXECUTE, "");

    OperationDeclaration operation = getOperation(withOperationsDeclaration, SAY_MY_NAME_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters().isEmpty(), is(true));

    operation = getOperation(withOperationsDeclaration, GET_ENEMY_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(1));
    assertThat(operation.getOutput().getType(), equalTo(toMetadataType(String.class)));
    assertThat(operation.getOutputAttributes().getType(), equalTo(toMetadataType(IntegerAttributes.class)));
    assertParameter(operation.getAllParameters(), "index", "", toMetadataType(int.class), false, SUPPORTED, "0");

    operation = getOperation(extensionDeclaration, KILL_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(2));
    assertThat(operation.getOutput().getType(), equalTo(toMetadataType(String.class)));
    assertThat(operation.getOutputAttributes().getType(), is(instanceOf(VoidType.class)));
    assertParameter(operation.getAllParameters(), "victim", "", toMetadataType(String.class), false, SUPPORTED, "#[payload]");
    assertParameter(operation.getAllParameters(), "goodbyeMessage", "", toMetadataType(String.class), true, SUPPORTED, null);

    operation = getOperation(extensionDeclaration, KILL_WITH_WEAPON);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(3));
    assertParameter(operation.getAllParameters(), "weapon", "", toMetadataType(Weapon.class), true, SUPPORTED, null);
    assertParameter(operation.getAllParameters(), "type", "", toMetadataType(WeaponType.class), true, SUPPORTED, null);
    assertParameter(operation.getAllParameters(), "attributesOfWeapon", "", toMetadataType(Weapon.WeaponAttributes.class), true,
                    SUPPORTED, null);

    operation = getOperation(extensionDeclaration, KILL_WITH_RICINS);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(1));
    assertParameter(operation.getAllParameters(), "ricins", "", arrayOf(List.class, objectTypeBuilder(Ricin.class)), false,
                    SUPPORTED, "#[payload]");

    operation = getOperation(extensionDeclaration, KILL_WITH_MULTIPLES_WEAPONS);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(1));
    assertParameter(operation.getAllParameters(), "weapons", "", arrayOf(List.class, objectTypeBuilder(Weapon.class)), false,
                    SUPPORTED, "#[payload]");

    operation = getOperation(extensionDeclaration, KILL_WITH_MULTIPLE_WILDCARD_WEAPONS);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(1));
    assertParameter(operation.getAllParameters(), "wildCardWeapons", "", arrayOf(List.class, objectTypeBuilder(Weapon.class)),
                    true,
                    SUPPORTED, null);

    operation = getOperation(extensionDeclaration, KILL_CUSTOM_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(2));
    assertParameter(operation.getAllParameters(), "victim", "", toMetadataType(String.class), false, SUPPORTED, "#[payload]");
    assertParameter(operation.getAllParameters(), "goodbyeMessage", "", toMetadataType(String.class), true, SUPPORTED, null);

    operation = getOperation(withOperationsDeclaration, GET_PAYMENT_FROM_EVENT_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters().isEmpty(), is(true));

    operation = getOperation(withOperationsDeclaration, GET_PAYMENT_FROM_MESSAGE_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getOutput().getType(), is(instanceOf(VoidType.class)));
    assertThat(operation.getOutputAttributes().getType(), is(instanceOf(VoidType.class)));
    assertThat(operation.getAllParameters().isEmpty(), is(true));

    operation = getOperation(withOperationsDeclaration, LAUNDER_MONEY);
    assertParameter(operation.getAllParameters(), "amount", "", toMetadataType(long.class), true, SUPPORTED, null);

    operation = getOperation(extensionDeclaration, INJECTED_EXTENSION_MANAGER);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters().isEmpty(), is(true));

    operation = getOperation(extensionDeclaration, ALIAS);
    assertParameter(operation.getAllParameters(), "greeting", "", toMetadataType(String.class), true, SUPPORTED, null);
    assertParameter(operation.getAllParameters(), "myName", "", toMetadataType(String.class), false, SUPPORTED, HEISENBERG);
    assertParameter(operation.getAllParameters(), "age", "", toMetadataType(Integer.class), false, SUPPORTED, AGE);

    operation = getOperation(extensionDeclaration, KNOCK);
    assertParameter(operation.getAllParameters(), "knockedDoor", "", toMetadataType(KnockeableDoor.class), true, SUPPORTED, null);

    operation = getOperation(extensionDeclaration, KNOCK_MANY);
    assertParameter(operation.getAllParameters(), "doors", "", arrayOf(List.class, objectTypeBuilder(KnockeableDoor.class)), true,
                    SUPPORTED, null);

    operation = getOperation(withOperationsDeclaration, CALL_SAUL);
    assertThat(operation.getAllParameters(), is(empty()));

    operation = getOperation(extensionDeclaration, CURE_CANCER);
    assertThat(operation.getAllParameters(), is(empty()));
    java.util.Optional<ExceptionEnricherFactory> exceptionEnricherFactory = operation
        .getModelProperty(ExceptionEnricherModelProperty.class)
        .map(ExceptionEnricherModelProperty::getExceptionEnricherFactory);

    assertThat(exceptionEnricherFactory.isPresent(), is(true));
    assertThat(exceptionEnricherFactory.get().createEnricher(), instanceOf(CureCancerExceptionEnricher.class));

    operation = getOperation(extensionDeclaration, GET_MEDICAL_HISTORY);
    assertParameter(operation.getAllParameters(), "healthByYear", "",
                    TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                        .ofKey(TYPE_BUILDER.numberType().id(Integer.class.getName()))
                        .ofValue(TYPE_LOADER.load(HealthStatus.class)).build(),
                    true, SUPPORTED, null);

    operation = getOperation(extensionDeclaration, GET_GRAMS_IN_STORAGE);
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "grams", "", TYPE_LOADER.load(int[][].class), false, SUPPORTED, "#[payload]");
    assertThat(operation.getOutput().getType(), is(TYPE_LOADER.load(int[][].class)));

    operation = getOperation(extensionDeclaration, APPROVE_INVESTMENT);
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "investment", "", TYPE_LOADER.load(Investment.class), true, SUPPORTED, null);
    assertThat(getType(operation.getOutput().getType()), equalTo(getType(TYPE_LOADER.load(Investment.class))));

    operation = getOperation(extensionDeclaration, IGNORED_OPERATION);
    assertThat(operation, is(nullValue()));

    operation = getOperation(extensionDeclaration, GET_PAGED_PERSONAL_INFO_OPERATION);
    assertThat(operation.getModelProperty(PagedOperationModelProperty.class).isPresent(), is(true));
    assertThat(operation.getOutput().getType(), is(TYPE_LOADER.load(PersonalInfo.class)));

    operation = getOperation(extensionDeclaration, PROCESS_INFO);
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "sales", "", TYPE_LOADER.load(new TypeToken<Map<String, SaleInfo>>() {

    }.getType()),
                    true, SUPPORTED, null);

    operation = getOperation(extensionDeclaration, PROCESS_WEAPON);
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "weapon", "",
                    TYPE_LOADER.load(Weapon.class), false, SUPPORTED, null);

    operation = getOperation(extensionDeclaration, PROCESS_WEAPON_WITH_DEFAULT_VALUE);
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "weapon", "",
                    TYPE_LOADER.load(Weapon.class), false, SUPPORTED, PAYLOAD);

    operation = getOperation(extensionDeclaration, FAIL_TO_EXECUTE);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), is(empty()));
    assertThat(operation.getOutput().getType(), is(instanceOf(VoidType.class)));
  }

  private void assertTestModuleConnectionProviders(ExtensionDeclaration extensionDeclaration) throws Exception {
    assertThat(extensionDeclaration.getConnectionProviders(), hasSize(1));
    ConnectionProviderDeclaration connectionProvider = extensionDeclaration.getConnectionProviders().get(0);
    assertThat(connectionProvider.getName(), is(DEFAULT_CONNECTION_PROVIDER_NAME));

    List<ParameterDeclaration> parameters = connectionProvider.getAllParameters();
    assertThat(parameters, hasSize(2));

    assertParameter(parameters, "saulPhoneNumber", "", toMetadataType(String.class), false, SUPPORTED, SAUL_OFFICE_NUMBER);
    assertParameter(parameters, TLS_ATTRIBUTE_NAME, "", toMetadataType(TlsContextFactory.class), false, NOT_SUPPORTED, null);
    ImplementingTypeModelProperty typeModelProperty =
        connectionProvider.getModelProperty(ImplementingTypeModelProperty.class).get();
    assertThat(typeModelProperty.getType(), equalTo(HeisenbergConnectionProvider.class));
  }

  private void assertTestModuleMessageSource(ExtensionDeclaration extensionDeclaration) throws Exception {
    assertThat(extensionDeclaration.getMessageSources(), hasSize(0));

    SourceDeclaration source = extensionDeclaration.getConfigurations().get(0).getMessageSources().get(0);
    assertThat(source.getName(), is(SOURCE_NAME));

    List<ParameterDeclaration> parameters = source.getAllParameters();
    assertThat(parameters, hasSize(8));

    assertParameter(parameters, SOURCE_PARAMETER, "", toMetadataType(int.class), true, NOT_SUPPORTED, null);
    assertParameter(parameters, SOURCE_CALLBACK_PARAMETER, "", toMetadataType(Long.class), false, SUPPORTED, "#[payload]");
    assertParameter(parameters, SOURCE_REPEATED_CALLBACK_PARAMETER, "", toMetadataType(String.class), false, SUPPORTED, null);
    ImplementingTypeModelProperty typeModelProperty = source.getModelProperty(ImplementingTypeModelProperty.class).get();
    assertThat(typeModelProperty.getType(), equalTo(HeisenbergSource.class));
  }

  private void assertOperation(WithOperationsDeclaration declaration, String operationName, String operationDescription)
      throws Exception {
    OperationDeclaration operation = getOperation(declaration, operationName);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getDescription(), equalTo(operationDescription));
  }

  private void assertParameter(List<ParameterDeclaration> parameters, String name, String description, MetadataType metadataType,
                               boolean required, ExpressionSupport expressionSupport, Object defaultValue) {
    ParameterDeclaration param = findParameter(parameters, name);
    assertThat(param, is(notNullValue()));

    assertThat(param.getName(), equalTo(name));
    assertThat(param.getDescription(), equalTo(description));
    assertThat(getType(param.getType()), equalTo(getType(metadataType)));
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

  @Extension(name = OTHER_HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Configurations(HeisenbergExtension.class)
  @ConnectionProviders(HeisenbergConnectionProvider.class)
  public static class HeisenbergPointer extends HeisenbergExtension {

  }

  @Extension(name = OTHER_HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Configurations({HeisenbergExtension.class, NamedHeisenbergAlternateConfig.class})
  public static class HeisenbergPointerPlusExternalConfig {

  }

  @Configuration(name = EXTENDED_CONFIG_NAME, description = EXTENDED_CONFIG_DESCRIPTION)
  @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
  public static class NamedHeisenbergAlternateConfig extends HeisenbergAlternateConfig {

  }

  @Extension(name = OTHER_HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Operations({DuplicateConfigOperation.class})
  public static class HeisenbergWithInvalidOperation extends HeisenbergExtension {

  }

  @Extension(name = OTHER_HEISENBERG, description = EXTENSION_DESCRIPTION)
  public static class HeisenbergWithParameterGroupAsOptional extends HeisenbergExtension {

    @ParameterGroup("personalInfo")
    @Optional
    private PersonalInfo personalInfo;

  }

  @Extension(name = OTHER_HEISENBERG, description = EXTENSION_DESCRIPTION)
  public static class HeisenbergWithRecursiveParameterGroup extends HeisenbergExtension {

    @ParameterGroup("recursive")
    private RecursiveParameterGroup group;
  }

  @Extension(name = OTHER_HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Operations(HeisenbergAlternateConfig.class)
  @Configurations(HeisenbergAlternateConfig.class)
  public static class HeisenbergWithSameOperationsAndConfigs extends HeisenbergExtension {

  }

  @Extension(name = OTHER_HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Configurations(HeisenbergIsolatedConfig.class)
  public static class HeisenbergWithOperationsPointingToExtension extends HeisenbergExtension {

  }

  @Extension(name = OTHER_HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Operations(HeisenbergExtension.class)
  public static class HeisenbergWithOperationsPointingToExtensionAndDefaultConfig extends HeisenbergExtension {

  }

  @Extension(name = OTHER_HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Operations({HeisenbergExtension.class, GenericlessMessageOperation.class})
  public static class HeisenbergWithGenericlessMessageOperation {

  }

  public static class DuplicateConfigOperation {

    public Long launder(@UseConfig HeisenbergExtension config, @UseConfig HeisenbergExtension config2) {
      return 10L;
    }

  }

  public static class GenericlessMessageOperation {

    public Result noGenerics() {
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

  @Operations(HeisenbergExtension.class)
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

  private static class RecursiveParameterGroup {

    @ParameterGroup("recursive")
    private RecursiveParameterGroup recursiveParameterGroup;
  }
}
