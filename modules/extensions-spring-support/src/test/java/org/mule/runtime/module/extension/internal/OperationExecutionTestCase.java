/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.SAUL_OFFICE_NUMBER;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.CALL_GUS_MESSAGE;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.CURE_CANCER_MESSAGE;
import static org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher.ENRICHED_MESSAGE;
import static org.mule.test.heisenberg.extension.model.HealthStatus.DEAD;
import static org.mule.test.heisenberg.extension.model.KnockeableDoor.knock;
import static org.mule.test.heisenberg.extension.model.Ricin.RICIN_KILL_MESSAGE;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.functional.junit4.FlowRunner;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class OperationExecutionTestCase extends ExtensionFunctionalTestCase
{

    private static final String GUSTAVO_FRING = "Gustavo Fring";
    private static final BigDecimal MONEY = BigDecimal.valueOf(1000000);
    private static final String GOODBYE_MESSAGE = "Say hello to my little friend";
    private static final String VICTIM = "Skyler";
    public static final String HEISENBERG = "heisenberg";
    public static final String KILL_RESULT = String.format("Killed with: %s , Type %s and attribute %s", RICIN_KILL_MESSAGE, Weapon.WeaponType.MELEE_WEAPON.name(), "Pizza on the rooftop");

    public static final long PAYMENT = 100;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {HeisenbergExtension.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "heisenberg-operation-config.xml";
    }

    @Test
    public void operationWithReturnValueAndWithoutParameters() throws Exception
    {
        assertThat(HEISENBERG, is(runFlow("sayMyName").getMessage().getPayload()));
    }

    @Test
    public void operationWithReturnValueOnTarget() throws Exception
    {
        FlowRunner runner = flowRunner("sayMyNameOnTarget").withPayload("");
        runner.spyObjects();
        MuleEvent event = runner.buildEvent();

        MuleEvent responseEvent = runner.run();

        assertThat(responseEvent.getMessage().getPayload(), is(""));
        verify(event, never()).setMessage(any(MuleMessage.class));
        verify(event.getMessage(), never()).setPayload(any(Object.class));

        MuleMessage responseMessage = event.getFlowVariable("myFace");
        assertThat(responseMessage.getPayload(), is(HEISENBERG));
    }

    @Test
    public void voidOperationWithoutParameters() throws Exception
    {
        MuleEvent responseEvent = flowRunner("die").withPayload(EMPTY).run();

        assertThat(responseEvent.getMessageAsString(), is(EMPTY));
        assertThat(getConfig(HEISENBERG).getEndingHealth(), is(DEAD));
    }

    @Test
    public void operationWithFixedParameter() throws Exception
    {
        assertThat(GUSTAVO_FRING, is(runFlow("getFixedEnemy").getMessage().getPayload()));
    }

    @Test
    public void operationWithDefaulValueParameter() throws Exception
    {
        assertThat(GUSTAVO_FRING, is(runFlow("getDefaultEnemy").getMessage().getPayload()));
    }

    @Test
    public void operationWithDynamicParameter() throws Exception
    {
        doTestExpressionEnemy(0);
    }

    @Test
    public void operationWithTransformedParameter() throws Exception
    {
        doTestExpressionEnemy("0");
    }

    @Test
    public void operationWithInjectedEvent() throws Exception
    {
        flowRunner("collectFromEvent").withPayload(Long.valueOf(PAYMENT)).run();
        assertThat(getConfig(HEISENBERG).getMoney(), is(MONEY.add(BigDecimal.valueOf(PAYMENT))));
    }

    @Test
    public void operationWithInjectedMessage() throws Exception
    {
        flowRunner("collectFromMessage").withPayload(Long.valueOf(PAYMENT)).run();
        assertThat(getConfig(HEISENBERG).getMoney(), is(MONEY.add(BigDecimal.valueOf(PAYMENT))));
    }

    @Test
    public void parameterFixedAtPayload() throws Exception
    {
        assertKillByPayload("killFromPayload");
    }

    @Test
    public void optionalParameterDefaultingToPayload() throws Exception
    {
        assertKillByPayload("customKillWithDefault");
    }

    @Test
    public void optionalParameterWithDefaultOverride() throws Exception
    {
        MuleEvent event = flowRunner("customKillWithoutDefault").withPayload("")
                                                                .withFlowVariable("goodbye", GOODBYE_MESSAGE)
                                                                .withFlowVariable("victim", VICTIM)
                                                                .run();

        assertKillPayload(event);
    }

    @Test
    public void oneNestedOperation() throws Exception
    {
        MuleEvent event = runFlow("killOne");
        String expected = "Killed the following because I'm the one who knocks:\n" +
                          "bye bye, Gustavo Fring\n";

        assertThat(expected, is(event.getMessageAsString()));
    }

    @Test
    public void manyNestedOperations() throws Exception
    {
        MuleEvent event = runFlow("killMany");
        String expected = "Killed the following because I'm the one who knocks:\n" +
                          "bye bye, Gustavo Fring\n" +
                          "bye bye, Frank\n" +
                          "bye bye, Nazi dudes\n";

        assertThat(expected, is(event.getMessageAsString()));
    }

    @Test
    public void manyNestedOperationsSupportedButOnlyOneProvided() throws Exception
    {
        MuleEvent event = runFlow("killManyButOnlyOneProvided");
        String expected = "Killed the following because I'm the one who knocks:\n" +
                          "bye bye, Gustavo Fring\n";

        assertThat(expected, is(event.getMessageAsString()));
    }

    @Test
    public void getInjectedDependency() throws Exception
    {
        ExtensionManager extensionManager = (ExtensionManager) runFlow("injectedExtensionManager").getMessage().getPayload();
        assertThat(extensionManager, is(sameInstance(muleContext.getExtensionManager())));
    }

    @Test
    public void alias() throws Exception
    {
        String alias = runFlow("alias").getMessageAsString();
        assertThat(alias, is("Howdy!, my name is Walter White and I'm 52 years old"));
    }

    @Test
    public void operationWithStaticInlinePojoParameter() throws Exception
    {
        String response = getPayloadAsString(runFlow("knockStaticInlineDoor").getMessage());
        assertKnockedDoor(response, "Inline Skyler");
    }

    @Test
    public void operationWithDynamicInlinePojoParameter() throws Exception
    {
        assertDynamicDoor("knockDynamicInlineDoor");
    }

    @Test
    public void operationWithStaticTopLevelPojoParameter() throws Exception
    {
        String response = getPayloadAsString(runFlow("knockStaticTopLevelDoor").getMessage());
        assertKnockedDoor(response, "Top Level Skyler");
    }

    @Test
    public void operationWithDynamicTopLevelPojoParameter() throws Exception
    {
        assertDynamicDoor("knockDynamicTopLevelDoor");
    }

    @Test
    public void operationWithInlineListParameter() throws Exception
    {
        List<String> response = (List<String>) flowRunner("knockManyWithInlineList").withPayload("")
                                                                                    .withFlowVariable("victim", "Saul")
                                                                                    .run()
                                                                                    .getMessage()
                                                                                    .getPayload();
        assertThat(response, Matchers.contains(knock("Inline Skyler"), knock("Saul")));
    }

    @Test
    public void operationWithExpressionListParameter() throws Exception
    {
        List<KnockeableDoor> doors = Arrays.asList(new KnockeableDoor("Skyler"), new KnockeableDoor("Saul"));

        List<String> response = (List<String>) flowRunner("knockManyByExpression").withPayload("")
                                                                                  .withFlowVariable("doors", doors)
                                                                                  .run()
                                                                                  .getMessage()
                                                                                  .getPayload();
        assertThat(response, Matchers.contains(knock("Skyler"), knock("Saul")));
    }

    @Test
    public void operationWhichRequiresConnection() throws Exception
    {
        assertThat(getPayloadAsString(runFlow("callSaul").getMessage()), is("You called " + SAUL_OFFICE_NUMBER));
    }

    @Test
    public void extensionWithExceptionEnricher() throws Throwable
    {
        expectedException.expect(ConnectionException.class);
        expectedException.expectMessage(is(ENRICHED_MESSAGE + CALL_GUS_MESSAGE));
        runFlowAndThrowCause("callGus");
    }

    @Test
    public void operationWithExceptionEnricher() throws Throwable
    {
        expectedException.expect(HeisenbergException.class);
        expectedException.expectMessage(is(CURE_CANCER_MESSAGE));
        runFlowAndThrowCause("cureCancer");
    }

    private void runFlowAndThrowCause(String callGus) throws Throwable
    {
        throw flowRunner(callGus).runExpectingException().getCause();
    }

    @Test
    public void operationWhichConsumesANonInstantiableArgument() throws Exception
    {
        Ricin ricinWeapon = new Ricin();
        ricinWeapon.setMicrogramsPerKilo(10L);

        MuleEvent event = flowRunner("killWithWeapon").withPayload(EMPTY).withFlowVariable("weapon", ricinWeapon).run();
        assertThat(event.getMessageAsString(), is(KILL_RESULT));
    }


    @Test
    public void connectionProviderDefaultValueSaulPhoneNumber() throws Exception
    {
        MuleEvent getSaulNumber = runFlow("getSaulNumber");
        assertThat(getSaulNumber.getMessage().getPayload(), is(SAUL_OFFICE_NUMBER));
    }

    @Test
    public void operationWhichConsumesAListOfNonInstantiableArgument() throws Exception
    {
        Ricin ricinWeapon1 = new Ricin();
        ricinWeapon1.setMicrogramsPerKilo(10L);
        Ricin ricinWeapon2 = new Ricin();
        ricinWeapon2.setMicrogramsPerKilo(10L);

        List<Weapon> weaponList = Arrays.asList(ricinWeapon1, ricinWeapon2);
        MuleEvent event = flowRunner("killWithMultipleWeapons").withPayload(EMPTY).withFlowVariable("weapons", weaponList).run();

        List<String> result = weaponList.stream().map(Weapon::kill).collect(Collectors.toList());
        assertThat(event.getMessage().getPayload(), is(result));
    }

    @Test
    public void operationWithLiteralArgument() throws Exception
    {
        MuleEvent event = flowRunner("literalEcho").withPayload(EMPTY).run();
        assertThat(event.getMessage().getPayload(), is("#[money]"));
    }

    private void assertDynamicDoor(String flowName) throws Exception
    {
        assertDynamicVictim(flowName, "Skyler");
        assertDynamicVictim(flowName, "Saul");
    }

    private void assertDynamicVictim(String flowName, String victim) throws Exception
    {
        assertKnockedDoor(getPayloadAsString(flowRunner(flowName).withPayload("")
                                                                 .withFlowVariable("victim", victim)
                                                                 .run()
                                                                 .getMessage()),
                victim);
    }

    private void assertKnockedDoor(String actual, String expected)
    {
        assertThat(actual, is(knock(expected)));
    }

    private void assertKillPayload(MuleEvent event) throws MuleException
    {
        assertThat(event.getMessageAsString(), is(String.format("%s, %s", GOODBYE_MESSAGE, VICTIM)));
    }

    private void assertKillByPayload(String flowName) throws Exception
    {
        assertKillPayload(flowRunner(flowName).withPayload(VICTIM).withFlowVariable("goodbye", GOODBYE_MESSAGE).run());
    }

    private void doTestExpressionEnemy(Object enemyIndex) throws Exception
    {
        MuleEvent event = flowRunner("expressionEnemy").withPayload(EMPTY).withFlowVariable("enemy", enemyIndex).run();

        assertThat(event.getMessageAsString(), is(GUSTAVO_FRING));
    }

    private HeisenbergExtension getConfig(String name) throws Exception
    {
        return ExtensionsTestUtils.getConfigurationFromRegistry(name, getTestEvent(""));
    }
}
