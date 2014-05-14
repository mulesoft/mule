/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleEvent;
import org.mule.extensions.introspection.Describer;
import org.mule.module.extensions.Door;
import org.mule.module.extensions.HealthStatus;
import org.mule.module.extensions.HeisenbergExtension;
import org.mule.module.extensions.Ricin;
import org.mule.module.extensions.internal.introspection.AnnotationsBasedDescriber;
import org.mule.module.extensions.internal.runtime.resolver.ValueResolver;
import org.mule.tck.junit4.ExtensionsFunctionalTestCase;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ConfigParserTestCase extends ExtensionsFunctionalTestCase
{

    private static final String HEISENBERG_BYNAME = "heisenberg";
    private static final String HEISENBERG_PLACEHOLDERS = "heisenbergWithPlaceHolders";
    private static final String HEISENBERG_BYREF = "heisenbergByRef";
    private static final String HEISENBERG_EXPRESSION = "expressionHeisenberg";
    private static final String HEISENBERG_EXPRESSION_BYREF = "expressionHeisenbergByRef";

    private static final Long MICROGRAMS_PER_KILO = 22L;
    private static final String LIDIA = "Lidia";
    private static final String STEVIA_COFFE_SHOP = "Stevia coffe shop";
    private static final String POLLOS_HERMANOS = "pollos hermanos";
    private static final String GUSTAVO_FRING = "Gustavo Fring";
    private static final String KRAZY_8 = "Krazy-8";
    private static final String JESSE_S = "Jesse's";
    private static final String METHYLAMINE = "methylamine";
    private static final int METHYLAMINE_QUANTITY = 75;
    private static final String PSEUDOEPHEDRINE = "pseudoephedrine";
    private static final int PSEUDOEPHEDRINE_QUANTITY = 0;
    private static final String P2P = "P2P";
    private static final int P2P_QUANTITY = 25;
    private static final String HANK = "Hank";
    private static final String MONEY = "1000000";
    private static final String SKYLER = "Skyler";
    private static final String SAUL = "Saul";
    private static final String WHITE_ADDRESS = "308 Negra Arroyo Lane";
    private static final String SHOPPING_MALL = "Shopping Mall";
    private static final int DEATH_YEAR = 2011;
    private static final HealthStatus INITIAL_HEALTH = HealthStatus.CANCER;
    private static final HealthStatus FINAL_HEALTH = HealthStatus.DEAD;

    @Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                {HEISENBERG_BYNAME}, {HEISENBERG_PLACEHOLDERS}, {HEISENBERG_BYREF}, {HEISENBERG_EXPRESSION}, {HEISENBERG_EXPRESSION_BYREF}
        });
    }

    @Parameter(0)
    public String testConfig;

    @Override
    protected String getConfigFile()
    {
        return "heisenberg-config.xml";
    }

    @Override
    protected Describer[] getManagedDescribers()
    {
        return new Describer[] {new AnnotationsBasedDescriber(HeisenbergExtension.class)};
    }

    @Test
    public void config() throws Exception
    {
        HeisenbergExtension heisenberg = lookupHeisenberg(testConfig);
        assertHeisenbergConfig(heisenberg);
    }

    @Test
    public void sameInstanceForEquivalentEvent() throws Exception
    {
        ValueResolver heisenbergResolver = muleContext.getRegistry().lookupObject(testConfig);
        MuleEvent event = getHeisenbergEvent();
        HeisenbergExtension heisenberg = (HeisenbergExtension) heisenbergResolver.resolve(event);
        assertThat(heisenberg, is(sameInstance(heisenbergResolver.resolve(event))));
    }

    @Test
    public void lifeCycle() throws Exception
    {
        HeisenbergExtension heisenberg = lookupHeisenberg(testConfig);
        assertThat(heisenberg.getInitialise(), is(1));
        assertThat(heisenberg.getStart(), is(1));

        muleContext.stop();
        assertThat(heisenberg.getStop(), is(1));

        muleContext.dispose();
        assertThat(heisenberg.getDispose(), is(1));
    }

    @Test
    public void muleContextInjected() throws Exception
    {
        HeisenbergExtension heisenberg = lookupHeisenberg(testConfig);
        assertThat(heisenberg.getMuleContext(), is(muleContext));
    }

    private HeisenbergExtension lookupHeisenberg(String key) throws Exception
    {
        ValueResolver heisenbergResolver = muleContext.getRegistry().lookupObject(key);
        return (HeisenbergExtension) heisenbergResolver.resolve(getHeisenbergEvent());
    }

    private MuleEvent getHeisenbergEvent() throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("lidia", LIDIA);

        event.setFlowVariable("myName", HeisenbergExtension.HEISENBERG);
        event.setFlowVariable("age", HeisenbergExtension.AGE);
        event.setFlowVariable("microgramsPerKilo", MICROGRAMS_PER_KILO);
        event.setFlowVariable("steviaCoffeShop", STEVIA_COFFE_SHOP);
        event.setFlowVariable("pollosHermanos", POLLOS_HERMANOS);
        event.setFlowVariable("gustavoFring", GUSTAVO_FRING);
        event.setFlowVariable("krazy8", KRAZY_8);
        event.setFlowVariable("jesses", JESSE_S);
        event.setFlowVariable("methylamine", METHYLAMINE_QUANTITY);
        event.setFlowVariable("pseudoephedrine", PSEUDOEPHEDRINE_QUANTITY);
        event.setFlowVariable("p2p", P2P_QUANTITY);
        event.setFlowVariable("hank", HANK);
        event.setFlowVariable("money", MONEY);
        event.setFlowVariable("skyler", SKYLER);
        event.setFlowVariable("saul", SAUL);
        event.setFlowVariable("whiteAddress", WHITE_ADDRESS);
        event.setFlowVariable("shoppingMall", SHOPPING_MALL);
        event.setFlowVariable("initialHealth", INITIAL_HEALTH);
        event.setFlowVariable("finalHealth", FINAL_HEALTH);

        return event;
    }

    private void assertHeisenbergConfig(HeisenbergExtension heisenberg)
    {
        assertNotNull(heisenberg);

        assertSimpleProperties(heisenberg);
        assertRecipe(heisenberg);
        assertDoors(heisenberg);
        assertRicinPacks(heisenberg);
        assertCandidateDoors(heisenberg);
    }

    private void assertRicinPacks(HeisenbergExtension heisenberg)
    {
        Set<Ricin> ricinPacks = heisenberg.getRicinPacks();

        assertNotNull(ricinPacks);
        assertEquals(1, ricinPacks.size());
        Ricin ricin = ricinPacks.iterator().next();
        assertEquals(MICROGRAMS_PER_KILO, ricin.getMicrogramsPerKilo());
        assertDoor(ricin.getDestination(), LIDIA, STEVIA_COFFE_SHOP);
    }

    private void assertDoors(HeisenbergExtension heisenberg)
    {
        Door door = heisenberg.getNextDoor();
        assertDoor(door, GUSTAVO_FRING, POLLOS_HERMANOS);

        Door previous = door.getPrevious();
        assertDoor(door.getPrevious(), KRAZY_8, JESSE_S);
        assertNull(previous.getPrevious());
    }

    private void assertRecipe(HeisenbergExtension heisenberg)
    {
        Map<String, Long> recipe = heisenberg.getRecipe();
        assertNotNull(recipe);
        assertEquals(3, recipe.size());
        assertEquals(Long.valueOf(METHYLAMINE_QUANTITY), recipe.get(METHYLAMINE));
        assertEquals(Long.valueOf(PSEUDOEPHEDRINE_QUANTITY), recipe.get(PSEUDOEPHEDRINE));
        assertEquals(Long.valueOf(P2P_QUANTITY), recipe.get(P2P));
    }

    private void assertSimpleProperties(HeisenbergExtension heisenberg)
    {
        assertEquals(HeisenbergExtension.HEISENBERG, heisenberg.getPersonalInfo().getMyName());
        assertEquals(Integer.valueOf(HeisenbergExtension.AGE), heisenberg.getPersonalInfo().getAge());

        List<String> enemies = heisenberg.getEnemies();
        assertThat(enemies, notNullValue());
        assertEquals(2, enemies.size());
        assertEquals(GUSTAVO_FRING, enemies.get(0));
        assertEquals(HANK, enemies.get(1));

        assertTrue(heisenberg.isCancer());
        assertThat(heisenberg.getInitialHealth(), is(INITIAL_HEALTH));
        assertThat(heisenberg.getFinalHealth(), is(FINAL_HEALTH));

        Calendar dayOfBirth = Calendar.getInstance();
        dayOfBirth.setTime(heisenberg.getPersonalInfo().getLifetimeInfo().getDateOfBirth());

        //only compare year to avoid timezone related flakyness
        assertEquals(getDateOfBirth().get(Calendar.YEAR), dayOfBirth.get(Calendar.YEAR));
        assertEquals(getDateOfDeath().get(Calendar.YEAR), heisenberg.getPersonalInfo().getLifetimeInfo().getDateOfDeath().get(Calendar.YEAR));

        assertEquals(new BigDecimal(MONEY), heisenberg.getMoney());
    }

    private void assertCandidateDoors(HeisenbergExtension heisenberg)
    {
        Map<String, Door> candidates = heisenberg.getCandidateDoors();
        assertNotNull(candidates);
        assertEquals(2, candidates.size());

        assertDoor(candidates.get(SKYLER.toLowerCase()), SKYLER, WHITE_ADDRESS);
        assertDoor(candidates.get(SAUL.toLowerCase()), SAUL, SHOPPING_MALL);
    }

    private void assertDoor(Door door, String victim, String address)
    {
        assertNotNull(door);
        assertEquals(victim, door.getVictim());
        assertEquals(address, door.getAddress());
    }

    public static Calendar getDateOfBirth()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 1959);
        calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 7);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    public static Calendar getDateOfDeath()
    {
        Calendar calendar = getDateOfBirth();
        calendar.set(Calendar.YEAR, DEATH_YEAR);

        return calendar;
    }
}
