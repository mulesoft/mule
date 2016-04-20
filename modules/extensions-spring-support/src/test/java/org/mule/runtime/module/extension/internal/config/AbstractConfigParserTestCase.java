/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.getConfigurationFromRegistry;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.util.ArrayUtils;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.HealthStatus;
import org.mule.test.heisenberg.extension.model.Ricin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractConfigParserTestCase extends ExtensionFunctionalTestCase
{

    protected static final String HEISENBERG_BYNAME = "heisenberg";
    protected static final String HEISENBERG_EXPRESSION = "expressionHeisenberg";
    protected static final String HEISENBERG_EXPRESSION_BYREF = "expressionHeisenbergByRef";

    protected static final Long MICROGRAMS_PER_KILO = 22L;
    protected static final String LIDIA = "Lidia";
    protected static final String STEVIA_COFFE_SHOP = "Stevia coffe shop";
    protected static final String POLLOS_HERMANOS = "pollos hermanos";
    protected static final String GUSTAVO_FRING = "Gustavo Fring";
    protected static final String KRAZY_8 = "Krazy-8";
    protected static final String JESSE_S = "Jesse's";
    protected static final int METHYLAMINE_QUANTITY = 75;
    protected static final int PSEUDOEPHEDRINE_QUANTITY = 0;
    protected static final String P2P = "P2P";
    protected static final int P2P_QUANTITY = 25;
    protected static final String HANK = "Hank";
    protected static final String MONEY = "1000000";
    protected static final String SKYLER = "Skyler";
    protected static final String SAUL = "Saul";
    protected static final String WHITE_ADDRESS = "308 Negra Arroyo Lane";
    protected static final String SHOPPING_MALL = "Shopping Mall";
    protected static final HealthStatus INITIAL_HEALTH = HealthStatus.CANCER;
    protected static final HealthStatus FINAL_HEALTH = HealthStatus.DEAD;
    protected static final Ricin WEAPON = new Ricin();

    protected static final String SEASON_1_KEY = "s01";
    protected static final String SEASON_2_KEY = "s02";
    protected static final List<Long> MONTHLY_INCOMES = Arrays.asList(ArrayUtils.toObject(new long[] {12000, 500}));
    protected static final Map<String, List<String>> DEATHS_BY_SEASON = new HashMap<String, List<String>>()
    {{
        put(SEASON_1_KEY, Arrays.asList("emilio", "domingo"));
        put(SEASON_2_KEY, Arrays.asList("tuco", "tortuga"));
    }};


    protected HeisenbergExtension lookupHeisenberg(String key) throws Exception
    {
        return lookupHeisenberg(key, getHeisenbergEvent());
    }

    protected HeisenbergExtension lookupHeisenberg(String key, MuleEvent event) throws Exception
    {
        return getConfigurationFromRegistry(key, event);
    }

    protected MuleEvent getHeisenbergEvent() throws Exception
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
        WEAPON.setMicrogramsPerKilo(10L);
        event.setFlowVariable("weapon", WEAPON);

        return event;
    }

    @Override
    protected String getConfigFile()
    {
        return "heisenberg-config.xml";
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {HeisenbergExtension.class};
    }

}
