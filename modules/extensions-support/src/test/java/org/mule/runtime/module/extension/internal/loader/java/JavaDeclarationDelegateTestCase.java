/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.Boolean.TRUE;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
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
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DEFAULT_CONNECTION_PROVIDER_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.SAUL_OFFICE_NUMBER;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.AGE;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.arrayOf;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.assertMessageType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.objectTypeBuilder;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.vegan.extension.VeganExtension.APPLE;
import static org.mule.test.vegan.extension.VeganExtension.BANANA;

import org.mule.metadata.api.builder.NumberTypeBuilder;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExecutableComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandlerFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.test.heisenberg.extension.AsyncHeisenbergSource;
import org.mule.test.heisenberg.extension.HeisenbergConnectionProvider;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.HeisenbergOperations;
import org.mule.test.heisenberg.extension.HeisenbergSource;
import org.mule.test.heisenberg.extension.MoneyLaunderingOperation;
import org.mule.test.heisenberg.extension.SecureHeisenbergConnectionProvider;
import org.mule.test.heisenberg.extension.exception.CureCancerExceptionEnricher;
import org.mule.test.heisenberg.extension.model.HealthStatus;
import org.mule.test.heisenberg.extension.model.Investment;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.Methylamine;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.SaleInfo;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.heisenberg.extension.model.types.DEAOfficerAttributes;
import org.mule.test.heisenberg.extension.model.types.IntegerAttributes;
import org.mule.test.heisenberg.extension.model.types.WeaponType;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.vegan.extension.PaulMcCartneySource;
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

import com.google.common.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class JavaDeclarationDelegateTestCase extends AbstractJavaExtensionDeclarationTestCase {

  private static final String GET_GRAMS_IN_STORAGE = "getGramsInStorage";
  private static final String EXTENDED_CONFIG_NAME = "extended-config";
  private static final String SOURCE_NAME = "ListenPayments";
  private static final String ASYNC_SOURCE_NAME = "AsyncListenPayments";
  private static final String DEA_SOURCE_NAME = "dea-radio";
  private static final String SOURCE_PARAMETER = "initialBatchNumber";
  private static final String SOURCE_CALLBACK_PARAMETER = "payment";
  private static final String SOURCE_REPEATED_CALLBACK_PARAMETER = "sameNameParameter";
  private static final String SAY_MY_NAME_OPERATION = "sayMyName";
  private static final String NAME_AS_STREAM = "nameAsStream";
  private static final String GET_ENEMY_OPERATION = "getEnemy";
  private static final String GET_ALL_ENEMIES_OPERATION = "getAllEnemies";
  private static final String KILL_OPERATION = "kill";
  private static final String KILL_CUSTOM_OPERATION = "killWithCustomMessage";
  private static final String KILL_WITH_WEAPON = "killWithWeapon";
  private static final String KILL_WITH_RICINS = "killWithRicins";
  private static final String KILL_WITH_MULTIPLES_WEAPONS = "killWithMultiplesWeapons";
  private static final String KILL_WITH_MULTIPLE_WILDCARD_WEAPONS = "killWithMultipleWildCardWeapons";
  private static final String GET_PAGED_PERSONAL_INFO_OPERATION = "getPagedPersonalInfo";
  private static final String EMPTY_PAGED_OPERATION = "emptyPagedOperation";
  private static final String FAILING_PAGED_OPERATION = "failingPagedOperation";
  private static final String CONNECTION_PAGED_OPERATION = "pagedOperationUsingConnection";
  private static final String DIE = "die";
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
  private static final String OTHER_HEISENBERG = "OtherHeisenberg";
  private static final String PROCESS_WEAPON = "processWeapon";
  private static final String PROCESS_WEAPON_LIST = "processWeaponList";
  private static final String PROCESS_WEAPON_WITH_DEFAULT_VALUE = "processWeaponWithDefaultValue";
  private static final String PROCESS_INFO = "processSale";
  private static final String FAIL_TO_EXECUTE = "failToExecute";
  private static final String THROW_ERROR = "throwError";
  public static final String ECHO_AN_OPERATION_WITH_ALIAS = "echo";
  public static final String BY_PASS_WEAPON = "byPassWeapon";
  public static final MetadataType WEAPON_TYPE = TYPE_LOADER.load(Weapon.class);
  public static final MetadataType STRING_TYPE = TYPE_LOADER.load(String.class);
  public static final MetadataType INT_TYPE = toMetadataType(int.class);

  @Before
  public void setUp() {
    setLoader(loaderFor(HeisenbergExtension.class));
  }

  @Test
  public void describeTestModule() throws Exception {
    ExtensionDeclarer declarer = declareExtension();

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
    setLoader(loaderFor(HeisenbergPointer.class));
    ExtensionDeclarer declarer = declareExtension();

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
    setLoader(loaderFor(HeisenbergPointerPlusExternalConfig.class));
    ExtensionDeclaration extensionDeclaration = declareExtension().getDeclaration();

    assertExtensionProperties(extensionDeclaration, OTHER_HEISENBERG);
    assertThat(extensionDeclaration.getConfigurations().size(), equalTo(2));

    ConfigurationDeclaration configuration = extensionDeclaration.getConfigurations().get(1);
    assertThat(configuration, is(notNullValue()));
    assertThat(configuration.getName(), equalTo(EXTENDED_CONFIG_NAME));
    assertThat(configuration.getAllParameters(), hasSize(32));
    assertParameter(configuration.getAllParameters(), "extendedProperty", "", STRING_TYPE, true, SUPPORTED,
                    null);
  }

  @Test(expected = IllegalConfigurationModelDefinitionException.class)
  public void heisenbergWithOperationsConfig() throws Exception {
    loaderFor(HeisenbergWithSameOperationsAndConfigs.class)
        .declare(new DefaultExtensionLoadingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader(),
                                                    getDefault(emptySet())));
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void heisenbergWithParameterGroupAsOptional() throws Exception {
    loaderFor(HeisenbergWithParameterGroupAsOptional.class)
        .declare(new DefaultExtensionLoadingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader(),
                                                    getDefault(emptySet())));
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void heisenbergWithRecursiveParameterGroup() throws Exception {
    loaderFor(HeisenbergWithRecursiveParameterGroup.class)
        .declare(new DefaultExtensionLoadingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader(),
                                                    getDefault(emptySet())));
  }


  @Test(expected = IllegalModelDefinitionException.class)
  public void heisenbergWithMoreThanOneConfigInOperation() throws Exception {
    loaderFor(HeisenbergWithInvalidOperation.class)
        .declare(new DefaultExtensionLoadingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader(),
                                                    getDefault(emptySet())));
  }

  @Test(expected = IllegalOperationModelDefinitionException.class)
  public void heisenbergWithOperationPointingToExtension() throws Exception {
    loaderFor(HeisenbergWithOperationsPointingToExtension.class)
        .declare(new DefaultExtensionLoadingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader(),
                                                    getDefault(emptySet())));
  }

  @Test(expected = IllegalConfigurationModelDefinitionException.class)
  public void heisenbergWithOperationPointingToExtensionAndDefaultConfig() throws Exception {
    loaderFor(HeisenbergWithOperationsPointingToExtensionAndDefaultConfig.class)
        .declare(new DefaultExtensionLoadingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader(),
                                                    getDefault(emptySet())));
  }

  @Test
  public void messageOperationWithoutGenerics() throws Exception {
    ExtensionDeclarer declarer = loaderFor(HeisenbergWithGenericlessMessageOperation.class)
        .declare(new DefaultExtensionLoadingContext(HeisenbergWithSameOperationsAndConfigs.class.getClassLoader(),
                                                    getDefault(emptySet())));
    OperationDeclaration operation = getOperation(declarer.getDeclaration(), "noGenerics");

    assertThat(operation.getOutput().getType(), is(instanceOf(AnyType.class)));
    assertThat(operation.getOutputAttributes().getType(), is(instanceOf(VoidType.class)));
  }

  @Test
  public void listOfResultsOperation() throws Exception {
    ExtensionDeclarer declarer = loaderFor(HeisenbergWithListOfResultOperations.class)
        .declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
    OperationDeclaration operation = getOperation(declarer.getDeclaration(), "listOfResults");

    MetadataType outputType = operation.getOutput().getType();
    assertThat(outputType, is(instanceOf(ArrayType.class)));
    assertMessageType(((ArrayType) outputType).getType(), TYPE_LOADER.load(Integer.class),
                      TYPE_LOADER.load(IntegerAttributes.class));
    assertThat(operation.getOutputAttributes().getType(), is(instanceOf(VoidType.class)));
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidParameterGroupName() throws Exception {
    loaderFor(HeisenbergWithParameterGroupDefaultName.class)
        .declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
  }

  @Test
  public void listOfResultsOperationWithoutGenerics() throws Exception {
    ExtensionDeclarer declarer = loaderFor(HeisenbergWithListOfResultOperations.class)
        .declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
    OperationDeclaration operation = getOperation(declarer.getDeclaration(), "listOfResultsWithoutGenerics");

    MetadataType outputType = operation.getOutput().getType();
    assertThat(outputType, is(instanceOf(ArrayType.class)));
    assertMessageType(((ArrayType) outputType).getType(), TYPE_BUILDER.anyType().build(), TYPE_BUILDER.voidType().build());
  }

  @Test
  public void flyweight() {
    setLoader(loaderFor(VeganExtension.class));
    ExtensionDeclarer declarer = declareExtension();

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
    setLoader(loaderFor(HeisenbergExtension.class));
    ExtensionDeclarer declarer = declareExtension();
    final ExtensionDeclaration declaration = declarer.getDeclaration();
  }

  @Test
  public void categoryIsDescribedCorrectly() {
    setLoader(loaderFor(HeisenbergExtension.class));
    ExtensionDeclarer declarer = declareExtension();
    final ExtensionDeclaration declaration = declarer.getDeclaration();
    assertThat(declaration.getCategory(), is(SELECT));
  }

  @Test
  public void minMuleVersionDefaultValueIsDescribedCorrectly() {
    setLoader(loaderFor(PetStoreConnector.class));
    ExtensionDeclarer declarer = declareExtension();
    final ExtensionDeclaration declaration = declarer.getDeclaration();
  }

  @Test
  public void categoryDefaultValueIsDescribedCorrectly() {
    setLoader(loaderFor(PetStoreConnector.class));
    ExtensionDeclarer declarer = declareExtension();
    final ExtensionDeclaration declaration = declarer.getDeclaration();
    assertThat(declaration.getCategory(), is(COMMUNITY));
  }

  @Test
  public void flowListeningOperationWithoutAttributes() {
    setLoader(loaderFor(VeganExtension.class));
    ExtensionDeclarer declarer = declareExtension();
    final ExtensionDeclaration declaration = declarer.getDeclaration();

    OperationDeclaration operation = getOperation(getConfiguration(declaration, BANANA), "getLunch");
    assertThat(operation, is(notNullValue()));
    assertOutputType(operation.getOutput(), toMetadataType(Fruit.class), false);
    assertOutputType(operation.getOutputAttributes(), TYPE_BUILDER.voidType().build(), false);
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
    assertThat(parameters, hasSize(31));

    assertParameter(parameters, "myName", "", STRING_TYPE, false, SUPPORTED, HEISENBERG);
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
                    TYPE_BUILDER.objectType()
                        .openWith(TYPE_BUILDER.numberType().integer().with(new ClassInformationAnnotation(Long.class)))
                        .with(new ClassInformationAnnotation(Map.class, asList(String.class, Long.class)))
                        .build(),
                    false, SUPPORTED, null);

    assertParameter(parameters, "ricinPacks", "", arrayOf(Set.class, objectTypeBuilder(Ricin.class)), false, SUPPORTED, null);

    assertParameter(parameters, "nextDoor", "", toMetadataType(KnockeableDoor.class), false, SUPPORTED, null);
    assertParameter(parameters, "candidateDoors", "",
                    TYPE_BUILDER.objectType()
                        .openWith((objectTypeBuilder(KnockeableDoor.class)).build())
                        .with(new ClassInformationAnnotation(Map.class, asList(String.class, KnockeableDoor.class)))
                        .build(),
                    false, SUPPORTED, null);

    assertParameter(parameters, "initialHealth", "", toMetadataType(HealthStatus.class), false, SUPPORTED, "CANCER");
    assertParameter(parameters, "finalHealth", "", toMetadataType(HealthStatus.class), true, SUPPORTED, null);
    assertParameter(parameters, "labAddress", "", STRING_TYPE, false, REQUIRED, null);
    assertParameter(parameters, "firstEndevour", "", STRING_TYPE, false, NOT_SUPPORTED, null);
    assertParameter(parameters, "weapon", "", toMetadataType(Weapon.class), false, SUPPORTED, null);
    assertParameter(parameters, "wildCardWeapons", "", arrayOf(List.class, objectTypeBuilder(Weapon.class)), false, SUPPORTED,
                    null);
    assertParameter(parameters, "wildCards", "", arrayOf(List.class, objectTypeBuilder(Object.class)), false, SUPPORTED, null);
    assertParameter(parameters, "worksAtDEA", "", toMetadataType(boolean.class), false, SUPPORTED, valueOf(TRUE));
    assertParameter(parameters, "lovesMinerals", "", toMetadataType(boolean.class), true, SUPPORTED, null);

    assertParameter(parameters, "monthlyIncomes", "", arrayOf(List.class, longTypeBuilder()),
                    true, SUPPORTED, null);
    assertParameter(parameters, "labeledRicin", "",
                    TYPE_BUILDER.objectType()
                        .openWith(objectTypeBuilder(Ricin.class))
                        .with(new ClassInformationAnnotation(Map.class, asList(String.class, Ricin.class)))
                        .build(),
                    false, SUPPORTED, null);
    assertParameter(parameters, "deathsBySeasons", "",
                    TYPE_BUILDER.objectType()
                        .with(new ClassInformationAnnotation(Map.class, asList(String.class, List.class)))
                        .openWith(TYPE_BUILDER.arrayType()
                            .of(TYPE_BUILDER.stringType()))
                        .build(),
                    false, SUPPORTED, null);
    assertParameter(parameters, "weaponValueMap", "",
                    TYPE_BUILDER.objectType()
                        .with(new ClassInformationAnnotation(Map.class, asList(String.class, Weapon.class)))
                        .openWith(WEAPON_TYPE)
                        .build(),
                    false, SUPPORTED, null);
    assertParameter(parameters, "healthProgressions", "",
                    TYPE_BUILDER.arrayType().of(TYPE_LOADER.load(HealthStatus.class)).build(), false,
                    SUPPORTED, null);
  }

  private NumberTypeBuilder longTypeBuilder() {
    return TYPE_BUILDER.numberType().integer().with(new ClassInformationAnnotation(Long.class));
  }

  private void assertExtensionProperties(ExtensionDeclaration extensionDeclaration, String expectedName) {
    assertThat(extensionDeclaration, is(notNullValue()));

    assertThat(extensionDeclaration.getName(), is(expectedName));
    assertThat(extensionDeclaration.getVersion(), is(MULE_VERSION));
  }

  private void assertTestModuleOperations(ExtensionDeclaration extensionDeclaration) throws Exception {
    assertThat(extensionDeclaration.getOperations(), hasSize(41));

    WithOperationsDeclaration withOperationsDeclaration = extensionDeclaration.getConfigurations().get(0);
    assertThat(withOperationsDeclaration.getOperations().size(), is(15));
    assertOperation(withOperationsDeclaration, SAY_MY_NAME_OPERATION, "");
    assertOperation(withOperationsDeclaration, NAME_AS_STREAM, "");
    assertOperation(withOperationsDeclaration, GET_ENEMY_OPERATION, "");
    assertOperation(withOperationsDeclaration, GET_ALL_ENEMIES_OPERATION, "");
    assertOperation(extensionDeclaration, KILL_OPERATION, "");
    assertOperation(extensionDeclaration, KILL_CUSTOM_OPERATION, "");
    assertOperation(extensionDeclaration, KILL_WITH_WEAPON, "");
    assertOperation(extensionDeclaration, KILL_WITH_RICINS, "");
    assertOperation(extensionDeclaration, KILL_WITH_MULTIPLES_WEAPONS, "");
    assertOperation(extensionDeclaration, KILL_WITH_MULTIPLE_WILDCARD_WEAPONS, "");
    assertOperation(withOperationsDeclaration, DIE, "");
    assertOperation(withOperationsDeclaration, LAUNDER_MONEY, "");
    assertOperation(extensionDeclaration, INJECTED_EXTENSION_MANAGER, "");
    assertOperation(extensionDeclaration, ALIAS, "");
    assertOperation(withOperationsDeclaration, CALL_SAUL, "");
    assertOperation(extensionDeclaration, CALL_GUS_FRING, "");
    assertOperation(withOperationsDeclaration, GET_SAUL_PHONE, "");
    assertOperation(extensionDeclaration, GET_MEDICAL_HISTORY, "");
    assertOperation(extensionDeclaration, GET_GRAMS_IN_STORAGE, "");
    assertOperation(extensionDeclaration, APPROVE_INVESTMENT, "");
    assertOperation(withOperationsDeclaration, GET_PAGED_PERSONAL_INFO_OPERATION, "");
    assertOperation(withOperationsDeclaration, EMPTY_PAGED_OPERATION, "");
    assertOperation(withOperationsDeclaration, FAILING_PAGED_OPERATION, "");
    assertOperation(withOperationsDeclaration, CONNECTION_PAGED_OPERATION, "");
    assertOperation(extensionDeclaration, PROCESS_INFO, "");
    assertOperation(extensionDeclaration, PROCESS_WEAPON, "");
    assertOperation(extensionDeclaration, PROCESS_WEAPON_LIST, "");
    assertOperation(extensionDeclaration, PROCESS_WEAPON_WITH_DEFAULT_VALUE, "");
    assertOperation(extensionDeclaration, FAIL_TO_EXECUTE, "");
    assertOperation(extensionDeclaration, THROW_ERROR, "");
    assertOperation(extensionDeclaration, BY_PASS_WEAPON, "");
    assertOperation(extensionDeclaration, ECHO_AN_OPERATION_WITH_ALIAS, "");

    OperationDeclaration operation = getOperation(withOperationsDeclaration, SAY_MY_NAME_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters().isEmpty(), is(true));

    operation = getOperation(withOperationsDeclaration, GET_ENEMY_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(1));
    assertThat(operation.getOutput().getType(), equalTo(STRING_TYPE));
    assertThat(operation.getOutputAttributes().getType(), equalTo(toMetadataType(IntegerAttributes.class)));
    assertParameter(operation.getAllParameters(), "index", "", INT_TYPE, false, SUPPORTED, "0");
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(withOperationsDeclaration, GET_ALL_ENEMIES_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(0));
    assertThat(operation.getOutput().getType(), is(instanceOf(ArrayType.class)));
    assertMessageType(((ArrayType) operation.getOutput().getType()).getType(), STRING_TYPE,
                      TYPE_LOADER.load(IntegerAttributes.class));
    assertThat(operation.getOutputAttributes().getType(), is(instanceOf(VoidType.class)));
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, KILL_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(2));
    assertThat(operation.getOutput().getType(), equalTo(STRING_TYPE));
    assertThat(operation.getOutputAttributes().getType(), is(instanceOf(VoidType.class)));
    assertParameter(operation.getAllParameters(), "victim", "", STRING_TYPE, false, SUPPORTED, "#[payload]");
    assertParameter(operation.getAllParameters(), "goodbyeMessage", "", STRING_TYPE, true, SUPPORTED, null);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, KILL_WITH_WEAPON);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(3));
    assertParameter(operation.getAllParameters(), "weapon", "", WEAPON_TYPE, true, SUPPORTED, null);
    assertParameter(operation.getAllParameters(), "type", "", toMetadataType(WeaponType.class), true, SUPPORTED, null);
    assertParameter(operation.getAllParameters(), "attributesOfWeapon", "", toMetadataType(Weapon.WeaponAttributes.class), true,
                    SUPPORTED, null);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, KILL_WITH_RICINS);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(1));
    assertParameter(operation.getAllParameters(), "ricins", "", arrayOf(List.class, objectTypeBuilder(Ricin.class)), false,
                    SUPPORTED, "#[payload]");
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, KILL_WITH_MULTIPLES_WEAPONS);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(1));
    assertParameter(operation.getAllParameters(), "weapons", "", arrayOf(List.class, objectTypeBuilder(Weapon.class)), false,
                    SUPPORTED, "#[payload]");
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, KILL_WITH_MULTIPLE_WILDCARD_WEAPONS);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(1));
    assertParameter(operation.getAllParameters(), "wildCardWeapons", "", arrayOf(List.class, objectTypeBuilder(Weapon.class)),
                    true,
                    SUPPORTED, null);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, KILL_CUSTOM_OPERATION);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), hasSize(2));
    assertParameter(operation.getAllParameters(), "victim", "", STRING_TYPE, false, SUPPORTED, "#[payload]");
    assertParameter(operation.getAllParameters(), "goodbyeMessage", "", STRING_TYPE, true, SUPPORTED, null);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(withOperationsDeclaration, LAUNDER_MONEY);
    assertParameter(operation.getAllParameters(), "amount", "", toMetadataType(long.class), true, SUPPORTED, null);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, INJECTED_EXTENSION_MANAGER);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters().isEmpty(), is(true));
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, ALIAS);
    assertParameter(operation.getAllParameters(), "greeting", "", STRING_TYPE, true, SUPPORTED, null);
    assertParameter(operation.getAllParameters(), "myName", "", STRING_TYPE, false, SUPPORTED, HEISENBERG);
    assertParameter(operation.getAllParameters(), "age", "", toMetadataType(Integer.class), false, SUPPORTED, AGE);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, KNOCK);
    assertParameter(operation.getAllParameters(), "knockedDoor", "", toMetadataType(KnockeableDoor.class), true, SUPPORTED, null);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, KNOCK_MANY);
    assertParameter(operation.getAllParameters(), "doors", "", arrayOf(List.class, objectTypeBuilder(KnockeableDoor.class)), true,
                    SUPPORTED, null);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(withOperationsDeclaration, CALL_SAUL);
    assertThat(operation.getAllParameters(), is(empty()));
    assertConnected(operation, true);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, CURE_CANCER);
    assertThat(operation.getAllParameters(), is(empty()));
    assertConnected(operation, false);
    assertTransactional(operation, false);
    java.util.Optional<ExceptionHandlerFactory> exceptionEnricherFactory = operation
        .getModelProperty(ExceptionHandlerModelProperty.class)
        .map(ExceptionHandlerModelProperty::getExceptionHandlerFactory);

    assertThat(exceptionEnricherFactory.isPresent(), is(true));
    assertThat(exceptionEnricherFactory.get().createHandler(), instanceOf(CureCancerExceptionEnricher.class));

    operation = getOperation(extensionDeclaration, GET_MEDICAL_HISTORY);
    assertParameter(operation.getAllParameters(), "healthByYear", "",
                    TYPE_BUILDER.objectType()
                        .with(new ClassInformationAnnotation(Map.class, asList(String.class, HealthStatus.class)))
                        .openWith(TYPE_LOADER.load(HealthStatus.class))
                        .build(),
                    true, SUPPORTED, null);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, GET_GRAMS_IN_STORAGE);
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "grams", "", TYPE_LOADER.load(int[][].class), false, SUPPORTED,
                    "#[payload]");
    assertThat(operation.getOutput().getType(), is(TYPE_LOADER.load(int[][].class)));
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, APPROVE_INVESTMENT);
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "investment", "", TYPE_LOADER.load(Investment.class), true, SUPPORTED, null);
    assertThat(getType(operation.getOutput().getType()), equalTo(getType(TYPE_LOADER.load(Investment.class))));
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, IGNORED_OPERATION);
    assertThat(operation, is(nullValue()));

    operation = getOperation(withOperationsDeclaration, GET_PAGED_PERSONAL_INFO_OPERATION);
    assertThat(operation.getModelProperty(PagedOperationModelProperty.class).isPresent(), is(true));
    assertThat(operation.getOutput().getType(), is(instanceOf(ArrayType.class)));
    ArrayType outputType = (ArrayType) operation.getOutput().getType();
    assertThat(outputType.getType(), is(TYPE_LOADER.load(PersonalInfo.class)));
    assertConnected(operation, true);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, PROCESS_INFO);
    assertThat(operation, is(notNullValue()));
    assertConnected(operation, false);
    assertTransactional(operation, false);
    assertParameter(operation.getAllParameters(), "sales", "", TYPE_LOADER.load(new TypeToken<Map<String, SaleInfo>>() {

    }.getType()),
                    true, SUPPORTED, null);

    operation = getOperation(extensionDeclaration, PROCESS_WEAPON);
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "weapon", "",
                    WEAPON_TYPE, false, SUPPORTED, null);
    assertConnected(operation, false);

    operation = getOperation(extensionDeclaration, PROCESS_WEAPON_LIST);
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "weapons", "",
                    TYPE_LOADER.load(new TypeToken<List<Weapon>>() {}.getType()), false, SUPPORTED, null);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, PROCESS_WEAPON_WITH_DEFAULT_VALUE);
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "weapon", "",
                    WEAPON_TYPE, false, SUPPORTED, PAYLOAD);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, "processWeaponListWithDefaultValue");
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "weapons", "",
                    TYPE_LOADER.load(new TypeToken<List<Weapon>>() {}.getType()), false, SUPPORTED, PAYLOAD);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, "processAddressBook");
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "phoneNumbers", "",
                    TYPE_LOADER.load(new TypeToken<List<String>>() {}.getType()), true, SUPPORTED, null);
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, FAIL_TO_EXECUTE);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), is(empty()));
    assertThat(operation.getOutput().getType(), is(instanceOf(VoidType.class)));
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, THROW_ERROR);
    assertThat(operation, is(notNullValue()));
    assertThat(operation.getAllParameters(), is(empty()));
    assertThat(operation.getOutput().getType(), is(instanceOf(VoidType.class)));
    assertConnected(operation, false);
    assertTransactional(operation, false);

    operation = getOperation(extensionDeclaration, BY_PASS_WEAPON);
    assertThat(operation, is(notNullValue()));
    assertParameter(operation.getAllParameters(), "awesomeWeapon", "", WEAPON_TYPE, true, SUPPORTED, null);
    assertParameter(operation.getAllParameters(), "awesomeName", "", STRING_TYPE, true, SUPPORTED, null);
    assertThat(operation.getOutput().getType(), is(instanceOf(ObjectType.class)));
    assertConnected(operation, false);
    assertTransactional(operation, false);
  }

  private void assertTestModuleConnectionProviders(ExtensionDeclaration extensionDeclaration) throws Exception {
    assertThat(extensionDeclaration.getConnectionProviders(), hasSize(2));
    ConnectionProviderDeclaration connectionProvider = extensionDeclaration.getConnectionProviders().get(0);
    assertThat(connectionProvider.getName(), is(DEFAULT_CONNECTION_PROVIDER_NAME));

    List<ParameterDeclaration> parameters = connectionProvider.getAllParameters();
    assertThat(parameters, hasSize(3));

    assertParameter(parameters, "saulPhoneNumber", "", STRING_TYPE, false, SUPPORTED, SAUL_OFFICE_NUMBER);
    ImplementingTypeModelProperty typeModelProperty =
        connectionProvider.getModelProperty(ImplementingTypeModelProperty.class).get();
    assertThat(typeModelProperty.getType(), equalTo(HeisenbergConnectionProvider.class));

    parameters = extensionDeclaration.getConnectionProviders().get(1).getAllParameters();
    assertParameter(parameters, TLS_PARAMETER_NAME, "", toMetadataType(TlsContextFactory.class), true, NOT_SUPPORTED, null);
  }

  private void assertTestModuleMessageSource(ExtensionDeclaration extensionDeclaration) throws Exception {
    assertThat(extensionDeclaration.getMessageSources(), hasSize(1));
    SourceDeclaration source = extensionDeclaration.getMessageSources().get(0);
    assertThat(source.getName(), is(DEA_SOURCE_NAME));

    final MetadataType outputType = source.getOutput().getType();
    assertThat(outputType, is(instanceOf(ArrayType.class)));

    assertMessageType(((ArrayType) outputType).getType(), STRING_TYPE,
                      TYPE_LOADER.load(DEAOfficerAttributes.class));
    assertThat(source.getOutputAttributes().getType(), equalTo(TYPE_LOADER.load(Object.class)));

    ConfigurationDeclaration config = extensionDeclaration.getConfigurations().get(0);
    assertThat(config.getMessageSources(), hasSize(3));
    assertHeisenbergSource(config.getMessageSources().get(0), ASYNC_SOURCE_NAME, AsyncHeisenbergSource.class);
    assertHeisenbergSource(config.getMessageSources().get(1), SOURCE_NAME, HeisenbergSource.class);
  }

  private void assertHeisenbergSource(SourceDeclaration source, String sourceName, Class<? extends Source> type) {
    assertThat(source.getName(), is(sourceName));

    List<ParameterDeclaration> parameters = source.getAllParameters();
    assertThat(parameters, hasSize(28));

    assertParameter(parameters, SOURCE_PARAMETER, "", INT_TYPE, true, NOT_SUPPORTED, null);
    assertParameter(parameters, SOURCE_CALLBACK_PARAMETER, "", toMetadataType(Long.class), false, SUPPORTED, "#[payload]");
    assertParameter(parameters, SOURCE_REPEATED_CALLBACK_PARAMETER, "", STRING_TYPE, false, SUPPORTED, null);
    assertParameter(parameters, "methylamine", "", toMetadataType(Methylamine.class), false, SUPPORTED, null);
    ImplementingTypeModelProperty typeModelProperty = source.getModelProperty(ImplementingTypeModelProperty.class).get();
    assertThat(typeModelProperty.getType(), equalTo(type));
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
    assertParameterType(metadataType, param);
    assertThat(param.isRequired(), is(required));
    assertThat(param.getExpressionSupport(), is(expressionSupport));
    assertThat(param.getDefaultValue(), equalTo(defaultValue));
  }

  private void assertParameterType(MetadataType metadataType, ParameterDeclaration param) {
    ClassInformationAnnotation typeInfo = metadataType.getAnnotation(ClassInformationAnnotation.class).orElse(null);
    if (typeInfo != null) {
      ClassInformationAnnotation paramTypeInfo = param.getType().getAnnotation(ClassInformationAnnotation.class).orElse(null);
      assertThat(typeInfo.getClassname(), equalTo(paramTypeInfo.getClassname()));
    } else {
      assertThat(getId(metadataType), equalTo(getId(param.getType())));
    }
  }

  private void assertConnected(ExecutableComponentDeclaration declaration, boolean connected) {
    assertThat(declaration.isRequiresConnection(), is(connected));
  }

  private void assertTransactional(ExecutableComponentDeclaration declaration, boolean transactional) {
    assertThat(declaration.isTransactional(), is(transactional));
  }

  private void assertOutputType(OutputDeclaration output, MetadataType type, boolean isDynamic) {
    assertThat(output.getType(), equalTo(type));
    assertThat(output.hasDynamicType(), is(isDynamic));
  }

  private MetadataType listOfString() {
    return TYPE_BUILDER.arrayType().of(TYPE_BUILDER.stringType()).with(new ClassInformationAnnotation(List.class)).build();
  }

  protected void assertModelProperties(ExtensionDeclaration extensionDeclaration) {
    ImplementingTypeModelProperty implementingTypeModelProperty =
        extensionDeclaration.getModelProperty(ImplementingTypeModelProperty.class).get();
    assertThat(implementingTypeModelProperty, is(notNullValue()));
    assertThat(HeisenbergExtension.class.isAssignableFrom(implementingTypeModelProperty.getType()), is(true));
  }

  @Extension(name = OTHER_HEISENBERG)
  @Configurations(HeisenbergExtension.class)
  @ConnectionProviders({HeisenbergConnectionProvider.class, SecureHeisenbergConnectionProvider.class})
  public static class HeisenbergPointer extends HeisenbergExtension {

  }

  @Extension(name = OTHER_HEISENBERG)
  @Configurations({HeisenbergExtension.class, NamedHeisenbergAlternateConfig.class})
  public static class HeisenbergPointerPlusExternalConfig {

  }

  @Configuration(name = EXTENDED_CONFIG_NAME)
  @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
  public static class NamedHeisenbergAlternateConfig extends HeisenbergAlternateConfig {

  }

  @Extension(name = OTHER_HEISENBERG)
  @Operations({DuplicateConfigOperation.class})
  public static class HeisenbergWithInvalidOperation extends HeisenbergExtension {

  }

  @Extension(name = OTHER_HEISENBERG)
  public static class HeisenbergWithParameterGroupAsOptional extends HeisenbergExtension {

    @ParameterGroup(name = "personalInfo")
    @Optional
    private PersonalInfo personalInfo;

  }

  @Extension(name = OTHER_HEISENBERG)
  public static class HeisenbergWithRecursiveParameterGroup extends HeisenbergExtension {

    @ParameterGroup(name = "recursive")
    private RecursiveParameterGroup group;
  }

  @Extension(name = OTHER_HEISENBERG)
  @Operations(HeisenbergAlternateConfig.class)
  @Configurations(HeisenbergAlternateConfig.class)
  public static class HeisenbergWithSameOperationsAndConfigs extends HeisenbergExtension {

  }

  @Extension(name = OTHER_HEISENBERG)
  @Configurations(HeisenbergIsolatedConfig.class)
  public static class HeisenbergWithOperationsPointingToExtension extends HeisenbergExtension {

  }

  @Extension(name = OTHER_HEISENBERG)
  @Operations(HeisenbergExtension.class)
  public static class HeisenbergWithOperationsPointingToExtensionAndDefaultConfig extends HeisenbergExtension {

  }

  @Extension(name = OTHER_HEISENBERG)
  @Operations({HeisenbergExtension.class, GenericlessMessageOperation.class})
  public static class HeisenbergWithGenericlessMessageOperation {

  }

  @Extension(name = OTHER_HEISENBERG)
  @Operations({HeisenbergExtension.class, ListOfResultsOperations.class})
  public static class HeisenbergWithListOfResultOperations {

  }

  @Extension(name = OTHER_HEISENBERG)
  @Operations({HeisenbergExtension.class})
  public static class HeisenbergWithParameterGroupDefaultName {

    @ParameterGroup(name = DEFAULT_GROUP_NAME)
    private PersonalInfo personalInfo;
  }

  public static class DuplicateConfigOperation {

    public Long launder(@Config HeisenbergExtension config, @Config HeisenbergExtension config2) {
      return 10L;
    }

  }

  public static class GenericlessMessageOperation {

    public Result noGenerics() {
      return null;
    }
  }

  public static class ListOfResultsOperations {

    public List<Result<Integer, IntegerAttributes>> listOfResults() {
      return null;
    }

    public List<Result> listOfResultsWithoutGenerics() {
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

    @ParameterGroup(name = "recursive")
    private RecursiveParameterGroup recursiveParameterGroup;
  }
}
