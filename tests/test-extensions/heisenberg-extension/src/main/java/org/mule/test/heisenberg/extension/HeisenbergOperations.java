/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.meta.model.ExecutionType.CPU_INTENSIVE;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.annotation.DataTypeParameters;
import org.mule.runtime.extension.api.annotation.Ignore;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.RestrictedTo;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.tck.message.IntegerAttributes;
import org.mule.test.heisenberg.extension.exception.CureCancerExceptionEnricher;
import org.mule.test.heisenberg.extension.exception.HealthException;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;
import org.mule.test.heisenberg.extension.exception.NullExceptionEnricher;
import org.mule.test.heisenberg.extension.model.HealthStatus;
import org.mule.test.heisenberg.extension.model.Investment;
import org.mule.test.heisenberg.extension.model.KillParameters;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.heisenberg.extension.model.RecursiveChainA;
import org.mule.test.heisenberg.extension.model.RecursiveChainB;
import org.mule.test.heisenberg.extension.model.RecursivePojo;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.SaleInfo;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.heisenberg.extension.model.types.WeaponType;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class HeisenbergOperations {

  public static final String CURE_CANCER_MESSAGE = "Can't help you, you are going to die";
  public static final String CALL_GUS_MESSAGE = "You are not allowed to speak with gus.";
  public static final String KILL_WITH_GROUP = "KillGroup";

  public static final String OPERATION_WITH_DISPLAY_NAME_PARAMETER = "literalEcho";
  public static final String OPERATION_WITH_SUMMARY = "knockMany";
  public static final String OPERATION_WITH_EXAMPLE = "alias";
  public static final String OPERATION_PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME = "literalExpression";
  public static final String OPERATION_PARAMETER_OVERRIDED_DISPLAY_NAME = "Custom overrided display name";
  public static final String KNOCKEABLE_DOORS_SUMMARY = "List of Knockeable Doors";
  public static final String DOOR_PARAMETER = "doors";
  public static final String GREETING_PARAMETER = "greeting";
  public static final String OPERATION_PARAMETER_EXAMPLE = "Hello my friend!";

  @Inject
  private ExtensionManager extensionManager;

  @DataTypeParameters
  public String sayMyName(@UseConfig HeisenbergExtension config) {
    return config.getPersonalInfo().getName();
  }

  public void die(@UseConfig HeisenbergExtension config) {
    config.setEndingHealth(HealthStatus.DEAD);
  }

  @DataTypeParameters
  public Result<String, IntegerAttributes> getEnemy(@UseConfig HeisenbergExtension config,
                                                    @Optional(defaultValue = "0") int index) {
    Charset lastSupportedEncoding = Charset.availableCharsets().values().stream().reduce((first, last) -> last).get();
    org.mule.runtime.api.metadata.DataType dt =
        DataType.builder().type(String.class).mediaType("dead/dead").charset(lastSupportedEncoding.toString()).build();

    return Result.<String, IntegerAttributes>builder().output(config.getEnemies().get(index))
        .mediaType(dt.getMediaType()).attributes(new IntegerAttributes(index)).build();
  }

  public String kill(@Optional(defaultValue = PAYLOAD) String victim, String goodbyeMessage) throws Exception {
    return killWithCustomMessage(new KillParameters(victim, goodbyeMessage));
  }

  public String killWithCustomMessage(@ParameterGroup(KILL_WITH_GROUP) KillParameters killParameters) {
    return format("%s, %s", killParameters.getGoodbyeMessage(), killParameters.getVictim());
  }

  public String knock(KnockeableDoor knockedDoor) {
    return knockedDoor.knock();
  }

  public List<Ricin> killWithRicins(@Optional(defaultValue = PAYLOAD) List<Ricin> ricins) {
    return ricins;
  }

  public String killWithWeapon(Weapon weapon, WeaponType type, Weapon.WeaponAttributes attributesOfWeapon) {
    return format("Killed with: %s , Type %s and attribute %s", weapon.kill(), type.name(), attributesOfWeapon.getBrand());
  }

  public List<String> killWithMultiplesWeapons(@Optional(defaultValue = PAYLOAD) List<Weapon> weapons) {
    return weapons.stream().map(Weapon::kill).collect(Collectors.toList());
  }

  public List<String> killWithMultipleWildCardWeapons(List<? extends Weapon> wildCardWeapons) {
    return wildCardWeapons.stream().map(Weapon::kill).collect(Collectors.toList());
  }

  @Throws(HeisenbergErrorTyperProvider.class)
  public String killMany(@RestrictedTo(HeisenbergExtension.class) List<NestedProcessor> killOperations, String reason)
      throws Exception {
    StringBuilder builder = new StringBuilder("Killed the following because " + reason + ":\n");
    for (NestedProcessor processor : killOperations) {
      builder.append(processor.process()).append("\n");
    }

    return builder.toString();
  }

  public String killOne(@RestrictedTo(HeisenbergExtension.class) NestedProcessor killOperation, String reason) throws Exception {
    StringBuilder builder = new StringBuilder("Killed the following because " + reason + ":\n");
    builder.append(killOperation.process()).append("\n");

    return builder.toString();
  }

  public ExtensionManager getInjectedExtensionManager() {
    return extensionManager;
  }

  public void getPaymentFromEvent(@UseConfig HeisenbergExtension config, Event event) {
    Long payment = (Long) event.getMessage().getPayload().getValue();
    config.setMoney(config.getMoney().add(BigDecimal.valueOf(payment)));
  }

  public String alias(@Example(OPERATION_PARAMETER_EXAMPLE) String greeting, @ParameterGroup("Personal Info") PersonalInfo info) {
    return String.format("%s, my name is %s and I'm %d years old", greeting, info.getName(), info.getAge());
  }

  public void getPaymentFromMessage(@UseConfig HeisenbergExtension config, Message message) {
    Long payment = (Long) message.getPayload().getValue();
    config.setMoney(config.getMoney().add(BigDecimal.valueOf(payment)));
  }

  public List<String> knockMany(@Summary(KNOCKEABLE_DOORS_SUMMARY) List<KnockeableDoor> doors) {
    return doors.stream().map(KnockeableDoor::knock).collect(toList());
  }

  public String callSaul(@Connection HeisenbergConnection connection) {
    return connection.callSaul();
  }

  public String callGusFring() throws HeisenbergException {
    throw new HeisenbergException(CALL_GUS_MESSAGE);
  }

  @OnException(CureCancerExceptionEnricher.class)
  @Throws(HeisenbergErrorTyperProvider.class)
  public String cureCancer() throws HealthException {
    throw new HealthException(CURE_CANCER_MESSAGE);
  }

  @Execution(CPU_INTENSIVE)
  public Investment approve(Investment investment,
                            @Optional RecursivePojo recursivePojo,
                            @Optional RecursiveChainB recursiveChainB,
                            @Optional RecursiveChainA recursiveChainA) {
    investment.approve();
    return investment;
  }

  public Map<Integer, HealthStatus> getMedicalHistory(Map<Integer, HealthStatus> healthByYear) {
    return healthByYear;
  }

  public String getSaulPhone(@Connection HeisenbergConnection connection) {
    return connection.getSaulPhoneNumber();
  }

  public ParameterResolver<String> literalEcho(
                                               @DisplayName(OPERATION_PARAMETER_OVERRIDED_DISPLAY_NAME) ParameterResolver<String> literalExpression) {
    return literalExpression;
  }

  public int[][] getGramsInStorage(@Optional(defaultValue = PAYLOAD) int[][] grams) {
    return grams;
  }

  public Map<String, SaleInfo> processSale(Map<String, SaleInfo> sales) {
    return sales;
  }

  public ParameterResolver<Weapon> processWeapon(@Optional ParameterResolver<Weapon> weapon) {
    return weapon;
  }

  public ParameterResolver<List<Weapon>> processWeaponList(@Optional ParameterResolver<List<Weapon>> weapons) {
    return weapons;
  }

  public ParameterResolver<Weapon> processWeaponWithDefaultValue(@Optional(
      defaultValue = "#[payload]") ParameterResolver<Weapon> weapon) {
    return weapon;
  }

  public ParameterResolver<List<Weapon>> processWeaponListWithDefaultValue(@Optional(
      defaultValue = "#[payload]") ParameterResolver<List<Weapon>> weapons) {
    return weapons;
  }

  public ParameterResolver<List<String>> processAddressBook(ParameterResolver<List<String>> phoneNumbers) {
    return phoneNumbers;
  }

  @OnException(NullExceptionEnricher.class)
  public void failToExecute() throws HeisenbergException {
    callGusFring();
  }

  @Ignore
  public void ignoredOperation() {

  }
}
