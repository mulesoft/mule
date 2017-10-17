/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.ThrowableRootCauseMatcher.hasRootCause;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.MULE_MESSAGE_COLLECTION;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.tck.junit4.matcher.DataTypeCompatibilityMatcher.assignableTo;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.SAUL_OFFICE_NUMBER;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.CALL_GUS_MESSAGE;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.CURE_CANCER_MESSAGE;
import static org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher.ENRICHED_MESSAGE;
import static org.mule.test.heisenberg.extension.model.HealthStatus.CANCER;
import static org.mule.test.heisenberg.extension.model.HealthStatus.DEAD;
import static org.mule.test.heisenberg.extension.model.HealthStatus.HEALTHY;
import static org.mule.test.heisenberg.extension.model.KnockeableDoor.knock;
import static org.mule.test.heisenberg.extension.model.Ricin.RICIN_KILL_MESSAGE;
import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.exception.HealthException;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;
import org.mule.test.heisenberg.extension.model.BarberPreferences;
import org.mule.test.heisenberg.extension.model.CarDealer;
import org.mule.test.heisenberg.extension.model.CarWash;
import org.mule.test.heisenberg.extension.model.HealthStatus;
import org.mule.test.heisenberg.extension.model.Investment;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.SaleInfo;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.heisenberg.extension.model.types.IntegerAttributes;
import org.mule.test.heisenberg.extension.model.types.WeaponType;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class OperationExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  public static final String HEISENBERG = "heisenberg";
  public static final String KILL_RESULT = String.format("Killed with: %s , Type %s and attribute %s", RICIN_KILL_MESSAGE,
                                                         WeaponType.MELEE_WEAPON.name(), "Pizza on the rooftop");
  private static final String GUSTAVO_FRING = "Gustavo Fring";
  private static final String GOODBYE_MESSAGE = "Say hello to my little friend";
  private static final String VICTIM = "Skyler";
  private static final String EMPTY_STRING = "";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"heisenberg-operation-config.xml", "vegan-config.xml"};
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void operationWithReturnValueAndWithoutParameters() throws Exception {
    assertThat(HEISENBERG, equalTo(runFlow("sayMyName").getMessage().getPayload().getValue()));
  }

  @Test
  public void operationWithReturnValueOnTarget() throws Exception {
    FlowRunner runner = flowRunner("sayMyNameOnTarget").withPayload(EMPTY_STRING);

    CoreEvent responseEvent = runner.run();

    assertThat(responseEvent.getMessage().getPayload().getValue(), is(EMPTY_STRING));

    Message responseMessage = (Message) responseEvent.getVariables().get("myFace").getValue();
    assertThat(responseMessage.getPayload().getValue(), is(HEISENBERG));
  }

  @Test
  public void getInlineGroupDefinition() throws Exception {
    Message message = flowRunner("getBarberPreferences").withPayload(EMPTY_STRING).run().getMessage();

    assertThat(message.getPayload().getValue(), is(notNullValue()));
    assertThat(message.getPayload().getDataType().getMediaType().matches(APPLICATION_JAVA), is(true));

    BarberPreferences preferences = (BarberPreferences) message.getPayload().getValue();
    assertThat(preferences.getBeardTrimming(), is(BarberPreferences.BEARD_KIND.MUSTACHE));
    assertThat(preferences.isFullyBald(), is(false));
  }

  @Test
  public void getInlineGroupDefinitionAsArgument() throws Exception {
    Message message = flowRunner("getInlineInfo").withPayload(EMPTY_STRING).run().getMessage();

    assertThat(message.getPayload().getValue(), is(notNullValue()));

    BarberPreferences preferences = (BarberPreferences) message.getPayload().getValue();
    assertThat(preferences.getBeardTrimming(), is(BarberPreferences.BEARD_KIND.MUSTACHE));
    assertThat(preferences.isFullyBald(), is(true));
  }

  @Test
  public void getInlineGroupPersonalInfoAsArgument() throws Exception {
    Message message = flowRunner("getInlinePersonalInfo").withPayload(EMPTY_STRING).run().getMessage();

    assertThat(message.getPayload().getValue(), is(notNullValue()));
    PersonalInfo value = (PersonalInfo) message.getPayload().getValue();
    assertThat(value.getAge(), is(26));
    assertThat(value.getKnownAddresses().get(0), is("explicitAddress"));

    assertThat(value.getName(), is("Pepe"));
  }

  @Test
  public void voidOperationWithoutParameters() throws Exception {
    CoreEvent responseEvent = flowRunner("die").withPayload(EMPTY).run();

    assertThat(responseEvent.getMessage().getPayload().getValue(), is(EMPTY));
    assertThat(getConfig(HEISENBERG).getEndingHealth(), is(DEAD));
  }

  @Test
  public void operationWithFixedParameter() throws Exception {
    assertThat(GUSTAVO_FRING, equalTo(runFlow("getFixedEnemy").getMessage().getPayload().getValue()));
  }

  @Test
  public void operationWithDefaulValueParameter() throws Exception {
    assertThat(GUSTAVO_FRING, equalTo(runFlow("getDefaultEnemy").getMessage().getPayload().getValue()));
  }

  @Test
  public void operationWhichReturnsListOfMessages() throws Exception {
    TypedValue<List<Message>> payload = runFlow("getAllEnemies").getMessage().getPayload();
    assertThat(payload.getDataType(), is(assignableTo(MULE_MESSAGE_COLLECTION)));

    List<Message> enemies = payload.getValue();
    HeisenbergExtension heisenberg = getConfig(HEISENBERG);

    assertThat(enemies, hasSize(heisenberg.getEnemies().size()));

    int index = 0;
    for (Message enemyMessage : enemies) {
      assertEnemyMessage(heisenberg, index, enemyMessage);
      index++;
    }
  }

  private void assertEnemyMessage(HeisenbergExtension heisenberg, int index, Message enemyMessage) {
    assertThat(enemyMessage.getPayload().getValue(), is(heisenberg.getEnemies().get(index)));
    assertThat(enemyMessage.getAttributes().getValue(), is(instanceOf(IntegerAttributes.class)));
    assertThat(((IntegerAttributes) enemyMessage.getAttributes().getValue()).getValue(), is(index));
  }

  @Test
  public void randomAccessOnOperationWhichReturnsListOfMessages() throws Exception {
    List<Message> enemies = (List<Message>) runFlow("getAllEnemies").getMessage().getPayload().getValue();
    HeisenbergExtension heisenberg = getConfig(HEISENBERG);

    assertThat(enemies, hasSize(heisenberg.getEnemies().size()));
    int index = enemies.size() - 1;
    assertEnemyMessage(heisenberg, index, enemies.get(index));
    index = 0;
    assertEnemyMessage(heisenberg, index, enemies.get(index));
  }

  @Test
  public void operationWithDynamicParameter() throws Exception {
    doTestExpressionEnemy(0);
  }

  @Test
  public void operationWithTransformedParameter() throws Exception {
    doTestExpressionEnemy("0");
  }

  @Test
  public void parameterFixedAtPayload() throws Exception {
    assertKillByPayload("killFromPayload");
  }

  @Test
  public void optionalParameterDefaultingToPayload() throws Exception {
    assertKillByPayload("customKillWithDefault");
  }

  @Test
  public void optionalParameterWithDefaultOverride() throws Exception {
    CoreEvent event =
        flowRunner("customKillWithoutDefault").withPayload(EMPTY_STRING).withVariable("goodbye", GOODBYE_MESSAGE)
            .withVariable("victim", VICTIM).run();

    assertKillPayload(event);
  }

  @Test
  public void getInjectedDependency() throws Exception {
    ExtensionManager extensionManager =
        (ExtensionManager) runFlow("injectedExtensionManager").getMessage().getPayload().getValue();
    assertThat(extensionManager, is(sameInstance(muleContext.getExtensionManager())));
  }

  @Test
  public void alias() throws Exception {
    String alias = (String) runFlow("alias").getMessage().getPayload().getValue();
    assertThat(alias, is("Howdy!, my name is Walter White and I'm 52 years old"));
  }

  @Test
  public void operationWithStaticInlinePojoParameter() throws Exception {
    String response = getPayloadAsString(runFlow("knockStaticInlineDoor").getMessage());
    assertKnockedDoor(response, "Inline Skyler");
  }

  @Test
  public void operationWithRequiredParameterButNullReturningExpression() throws Exception {
    expectedException.expect(hasRootCause(instanceOf(IllegalArgumentException.class)));
    runFlow("knockWithNullDoor");
  }

  @Test
  public void operationWithDynamicInlinePojoParameter() throws Exception {
    assertDynamicDoor("knockDynamicInlineDoor");
  }

  @Test
  public void operationWithStaticTopLevelPojoParameter() throws Exception {
    String response = getPayloadAsString(runFlow("knockStaticTopLevelDoor").getMessage());
    assertKnockedDoor(response, "Top Level Skyler");
  }

  @Test
  public void operationWithDynamicTopLevelPojoParameter() throws Exception {
    assertDynamicDoor("knockDynamicTopLevelDoor");
  }

  @Test
  public void operationWithInlineListParameter() throws Exception {
    List<String> response = (List<String>) flowRunner("knockManyWithInlineList").withPayload(EMPTY_STRING)
        .withVariable("victim", "Saul").run().getMessage().getPayload().getValue();
    assertThat(response, contains(knock("Inline Skyler"), knock("Saul")));
  }

  @Test
  public void operationWithExpressionListParameter() throws Exception {
    List<KnockeableDoor> doors = Arrays.asList(new KnockeableDoor("Skyler"), new KnockeableDoor("Saul"));

    List<String> response =
        (List<String>) flowRunner("knockManyByExpression").withPayload(EMPTY_STRING).withVariable("doors", doors)
            .run().getMessage().getPayload().getValue();
    assertThat(response, Matchers.contains(knock("Skyler"), knock("Saul")));
  }

  @Test
  public void operationWhichRequiresConnection() throws Exception {
    assertThat(getPayloadAsString(runFlow("callSaul").getMessage()), is("You called " + SAUL_OFFICE_NUMBER));
  }

  @Test
  public void extensionWithExceptionEnricher() throws Throwable {
    expectedException.expect(ConnectionException.class);
    expectedException.expectMessage(is(ENRICHED_MESSAGE + CALL_GUS_MESSAGE));
    runFlowAndThrowCause("callGus");
  }

  @Test
  public void operationWithExceptionEnricher() throws Throwable {
    expectedException.expect(HeisenbergException.class);
    expectedException.expectCause(is(instanceOf(HealthException.class)));
    expectedException.expectMessage(containsString(CURE_CANCER_MESSAGE));
    runFlowAndThrowCause("cureCancer");
  }

  private void runFlowAndThrowCause(String callGus) throws Throwable {
    throw flowRunner(callGus).runExpectingException().getCause();
  }

  @Test
  public void operationWhichConsumesANonInstantiableArgument() throws Exception {
    Ricin ricinWeapon = new Ricin();
    ricinWeapon.setMicrogramsPerKilo(10L);

    CoreEvent event = flowRunner("killWithWeapon").withPayload(EMPTY).withVariable("weapon", ricinWeapon).run();
    assertThat(event.getMessage().getPayload().getValue(), is(KILL_RESULT));
  }


  @Test
  public void connectionProviderDefaultValueSaulPhoneNumber() throws Exception {
    CoreEvent getSaulNumber = runFlow("getSaulNumber");
    assertThat(getSaulNumber.getMessage().getPayload().getValue(), is(SAUL_OFFICE_NUMBER));
  }

  @Test
  public void operationWhichConsumesAListOfNonInstantiableArgument() throws Exception {
    Ricin ricinWeapon1 = new Ricin();
    ricinWeapon1.setMicrogramsPerKilo(10L);
    Ricin ricinWeapon2 = new Ricin();
    ricinWeapon2.setMicrogramsPerKilo(10L);

    List<Weapon> weaponList = Arrays.asList(ricinWeapon1, ricinWeapon2);
    CoreEvent event = flowRunner("killWithMultipleWeapons").withPayload(EMPTY).withVariable("weapons", weaponList).run();

    List<String> result = weaponList.stream().map(Weapon::kill).collect(Collectors.toList());
    assertThat(event.getMessage().getPayload().getValue(), is(result));
  }

  @Test
  public void operationWithListPojoAsDefaultPayload() throws Exception {
    Ricin ricinWeapon1 = new Ricin();
    ricinWeapon1.setMicrogramsPerKilo(20L);
    Ricin ricinWeapon2 = new Ricin();
    ricinWeapon2.setMicrogramsPerKilo(22L);

    List<Ricin> ricins = (List<Ricin>) flowRunner("killWithRicinDefaultPayload")
        .withPayload(Arrays.asList(ricinWeapon1, ricinWeapon2)).run().getMessage().getPayload().getValue();

    assertThat(ricins, hasSize(2));
    assertThat(ricins.get(0), instanceOf(Ricin.class));
    assertThat(ricins.get(1), instanceOf(Ricin.class));

    Ricin ricin1 = ricins.get(0);
    assertThat(ricin1.getMicrogramsPerKilo(), is(20L));

    Ricin ricin2 = ricins.get(1);
    assertThat(ricin2.getMicrogramsPerKilo(), is(22L));
  }

  @Test
  public void operationWithListPojoAsChildElementsOverridesDefault() throws Exception {
    List<Ricin> ricins =
        (List<Ricin>) flowRunner("killWithRicinAsChildElement").withPayload(EMPTY).run().getMessage().getPayload().getValue();

    assertThat(ricins, hasSize(2));
    assertThat(ricins.get(0), instanceOf(Ricin.class));
    assertThat(ricins.get(1), instanceOf(Ricin.class));

    Ricin ricin1 = ricins.get(0);
    assertThat(ricin1.getMicrogramsPerKilo(), is(20L));
    assertThat(ricin1.getDestination().getVictim(), is("Lidia"));
    assertThat(ricin1.getDestination().getAddress(), is("Stevia coffe shop"));

    Ricin ricin2 = ricins.get(1);
    assertThat(ricin2.getMicrogramsPerKilo(), is(22L));
    assertThat(ricin2.getDestination().getVictim(), is("Gustavo Fring"));
    assertThat(ricin2.getDestination().getAddress(), is("pollos hermanos"));
  }

  @Test
  public void operationWithParameterResolver() throws Exception {
    Object value = flowRunner("literalEcho").withPayload(EMPTY).run().getMessage().getPayload().getValue();
    assertThat(value, equalTo("#[money]"));
  }

  @Test
  public void getMedicalHistory() throws Exception {
    Map<String, HealthStatus> getMedicalHistory =
        (Map<String, HealthStatus>) flowRunner("getMedicalHistory").run().getMessage().getPayload().getValue();
    assertThat(getMedicalHistory, is(notNullValue()));
    assertThat(getMedicalHistory.entrySet().size(), is(3));
    assertThat(getMedicalHistory.get("2013"), is(HEALTHY));
    assertThat(getMedicalHistory.get("2014"), is(CANCER));
    assertThat(getMedicalHistory.get("2015"), is(DEAD));
  }

  @Test
  public void getGramsInStorage() throws Exception {
    int[][] gramsInStorage =
        (int[][]) flowRunner("getGramsInStorage").withPayload(new int[][] {{0, 22}, {1, 10}, {2, 30}}).run().getMessage()
            .getPayload().getValue();
    assertThat(gramsInStorage[0][0], is(0));
    assertThat(gramsInStorage[0][1], is(22));
    assertThat(gramsInStorage[1][0], is(1));
    assertThat(gramsInStorage[1][1], is(10));
    assertThat(gramsInStorage[2][0], is(2));
    assertThat(gramsInStorage[2][1], is(30));
  }

  @Test
  public void abstractParameterWithSubtypesAndParameterGroup() throws Exception {
    Investment investment = (Investment) flowRunner("investment").run().getMessage().getPayload().getValue();
    assertThat(investment, is(instanceOf(CarWash.class)));

    CarWash carWash = (CarWash) investment;
    assertThat(carWash.getCommercialName(), is("A1"));
    assertThat(carWash.getInvestmentInfo(), is(notNullValue()));
    assertThat(carWash.getInvestmentInfo().getValuation(), equalTo(100L));
    assertThat(carWash.getCarsPerMinute(), is(5));
    assertThat(carWash.isApproved(), is(true));
    assertThat(carWash.getInvestmentSpinOffs(), is(notNullValue()));
    assertThat(carWash.getInvestmentSpinOffs().get("other-car-wash"), is(instanceOf(CarWash.class)));

    CarWash spinOff = (CarWash) carWash.getInvestmentSpinOffs().get("other-car-wash");
    assertThat(spinOff.getCommercialName(), is("B1"));
    assertThat(spinOff.getInvestmentInfo(), is(notNullValue()));
    assertThat(spinOff.getInvestmentInfo().getValuation(), equalTo(10L));
    assertThat(spinOff.getCarsPerMinute(), is(1));
    assertThat(spinOff.getDiscardedInvestments(), is(notNullValue()));
    assertThat(spinOff.getDiscardedInvestments().size(), is(1));
    assertThat(spinOff.getDiscardedInvestments().get(0), is(instanceOf(CarDealer.class)));

    CarDealer discarded = (CarDealer) spinOff.getDiscardedInvestments().get(0);
    assertThat(discarded.getCommercialName(), is("Premium Cars"));
    assertThat(discarded.getCarStock(), is(50));
    assertThat(discarded.getInvestmentInfo(), is(notNullValue()));
    assertThat(discarded.getInvestmentInfo().getValuation(), equalTo(666L));
    assertThat(discarded.getInvestmentInfo().getInvestmentPlanB(), is(instanceOf(CarDealer.class)));

    CarDealer planB = (CarDealer) discarded.getInvestmentInfo().getInvestmentPlanB();
    assertThat(planB.getCommercialName(), is("Not So Premium Cars"));
    assertThat(planB.getCarStock(), is(5));
    assertThat(planB.getInvestmentInfo(), is(notNullValue()));
    assertThat(planB.getInvestmentInfo().getValuation(), equalTo(333L));
  }

  @Test
  public void operationWithMapOfComplexType() throws Exception {
    final String dean = "Dean";
    final Map<String, SaleInfo> salesInfo =
        (Map<String, SaleInfo>) flowRunner("processSale").run().getMessage().getPayload().getValue();
    assertThat(salesInfo, hasKey(dean));
    final SaleInfo saleInfo = salesInfo.get(dean);
    assertThat(saleInfo.getAmount(), is(500));
    assertThat(saleInfo.getDetails(), is("Some detail"));
  }

  @Test
  public void listOfMapsAsParameter() throws Exception {
    String expectedMessage = "an Apple";
    List<Map<String, String>> listOfMaps = new ArrayList<>();
    Map<String, String> map = new HashMap<>();
    map.put(Apple.class.getSimpleName(), expectedMessage);
    listOfMaps.add(map);
    CoreEvent event = flowRunner("eatComplexListOfMaps").withPayload(listOfMaps).run();
    List<Map<String, String>> result = (List<Map<String, String>>) event.getMessage().getPayload().getValue();
    assertThat(result, hasSize(1));
    assertThat(result.get(0).get(Apple.class.getSimpleName()), is(expectedMessage));
  }

  @Test
  public void operationWithInputStreamContentParameterInParameterGroup() throws Exception {
    String theMessage = "This is an important message";
    Object result = flowRunner("operationWithInputStreamContentParam")
        .withVariable("msg", new ByteArrayInputStream(theMessage.getBytes()))
        .run()
        .getMessage()
        .getPayload()
        .getValue();
    assertThat(result, is(theMessage));
  }

  @Test
  public void operationWithAliasedParametersAsChild() throws Exception {
    Map<String, Weapon> value =
        (Map<String, Weapon>) flowRunner("operationWithAliasedParametersAsChild").run().getMessage().getPayload().getValue();
    assertThat(value, is(IsMapContaining.hasEntry(is("SomeName"), is(instanceOf(Ricin.class)))));
  }

  @Test
  public void operationWithAliasedParametersAsReference() throws Exception {
    Map<String, Weapon> value =
        (Map<String, Weapon>) flowRunner("operationWithAliasedParametersAsChild").run().getMessage().getPayload().getValue();
    assertThat(value, is(IsMapContaining.hasEntry(is("SomeName"), is(instanceOf(Ricin.class)))));
  }

  @Test
  public void aliasedOperation() throws Exception {
    ParameterResolver<String> result =
        (ParameterResolver<String>) flowRunner("aliasedOperation").run().getMessage().getPayload().getValue();
    assertThat(result.resolve(), is("an expression"));
  }

  private void assertDynamicDoor(String flowName) throws Exception {
    assertDynamicVictim(flowName, "Skyler");
    assertDynamicVictim(flowName, "Saul");
  }

  private void assertDynamicVictim(String flowName, String victim) throws Exception {
    assertKnockedDoor(getPayloadAsString(flowRunner(flowName).withPayload(EMPTY_STRING).withVariable("victim", victim).run()
        .getMessage()), victim);
  }

  private void assertKnockedDoor(String actual, String expected) {
    assertThat(actual, is(knock(expected)));
  }

  private void assertKillPayload(CoreEvent event) throws MuleException {
    assertThat(event.getMessage().getPayload().getValue(),
               is(format("%s, %s", GOODBYE_MESSAGE, VICTIM)));
  }

  private void assertKillByPayload(String flowName) throws Exception {
    assertKillPayload(flowRunner(flowName).withPayload(VICTIM).withVariable("goodbye", GOODBYE_MESSAGE).run());
  }

  private void doTestExpressionEnemy(Object enemyIndex) throws Exception {
    CoreEvent event = flowRunner("expressionEnemy").withPayload(EMPTY).withVariable("enemy", enemyIndex).run();

    assertThat(event.getMessage().getPayload().getValue(), is(GUSTAVO_FRING));
  }

  private HeisenbergExtension getConfig(String name) throws Exception {
    return ExtensionsTestUtils.getConfigurationFromRegistry(name, CoreEvent
        .builder(create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION)).message(of(EMPTY_STRING)).build(), muleContext);
  }
}
