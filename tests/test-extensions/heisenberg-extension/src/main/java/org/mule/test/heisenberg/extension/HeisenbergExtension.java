/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.LITERAL;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.REQUIRED;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher;
import org.mule.test.heisenberg.extension.model.ExtendedPersonalInfo;
import org.mule.test.heisenberg.extension.model.HealthStatus;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

@Extension(name = HeisenbergExtension.HEISENBERG, description = HeisenbergExtension.EXTENSION_DESCRIPTION)
@Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
@Extensible(alias = "heisenberg-empire")
@OnException(HeisenbergConnectionExceptionEnricher.class)
@Providers(HeisenbergConnectionProvider.class)
@Sources(HeisenbergSource.class)
public class HeisenbergExtension implements Lifecycle, MuleContextAware
{

    public static final String SCHEMA_VERSION = "1.0-blue";
    public static final String HEISENBERG = "Heisenberg";
    public static final String AGE = "50";
    public static final String EXTENSION_DESCRIPTION = "My Test Extension just to unit test";
    public static final String RICIN_GROUP_NAME = "Dangerous-Ricin";
    public static final String PERSONAL_INFORMATION_GROUP_NAME = "Personal Information";
    public static final String PARAMETER_OVERRIDED_DISPLAY_NAME = "Parameter Custom Display Name";
    public static final String PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME = "literalExpressionWithoutDefault";

    private int initialise = 0;
    private int start = 0;
    private int stop = 0;
    private int dispose = 0;
    public static int sourceTimesStarted = 0;


    private MuleContext muleContext;

    @Inject
    private ExtensionManager extensionManager;

    @ConfigName
    private String configName;

    @Placement(group = PERSONAL_INFORMATION_GROUP_NAME)
    @ParameterGroup
    private ExtendedPersonalInfo personalInfo = new ExtendedPersonalInfo();

    @Parameter
    private List<String> enemies = new LinkedList<>();

    @Parameter
    private List<Long> monthlyIncomes = new LinkedList<>();

    @Parameter
    private boolean cancer;

    @Parameter
    @Optional
    private Map<String, Long> recipe;

    @Parameter
    @Optional
    private Map<String, List<String>> deathsBySeasons;

    @Parameter
    @Optional
    @Placement(order = 1, group = RICIN_GROUP_NAME)
    private Map<String, Ricin> labeledRicin;

    @Parameter
    @Optional
    private KnockeableDoor nextDoor;

    @Parameter
    @Optional
    @Placement(order = 2, group = RICIN_GROUP_NAME)
    private Set<Ricin> ricinPacks;

    @Parameter
    private BigDecimal money;

    @Parameter
    @Optional
    private Weapon weapon = new Ricin();

    @Parameter
    @Optional
    private Function<MuleEvent, Integer> moneyFunction;

    @Parameter
    @Optional
    private List<? extends Weapon> wildCardWeapons;

    @Parameter
    @Optional
    private List<?> wildCardList;

    @Parameter
    @Optional
    private Map<? extends Weapon, ?> wildCardWeaponMap;
    /**
     * Doors I might knock on but still haven't made up mind about
     */
    @Parameter
    @Optional
    private Map<String, KnockeableDoor> candidateDoors;

    @Parameter
    @Optional(defaultValue = "CANCER")
    private HealthStatus initialHealth;

    @Parameter
    @Alias("finalHealth")
    private HealthStatus endingHealth;

    @Parameter
    @Expression(REQUIRED)
    @Optional
    private String labAddress;

    @Parameter
    @Expression(NOT_SUPPORTED)
    @Optional
    private String firstEndevour;

    @Parameter
    @Optional(defaultValue = "#[payload]")
    @Expression(LITERAL)
    private String literalExpressionWithDefault;

    @Parameter
    @Optional
    @Expression(LITERAL)
    @DisplayName(PARAMETER_OVERRIDED_DISPLAY_NAME)
    private String literalExpressionWithoutDefault;

    @Override
    public void initialise() throws InitialisationException
    {
        initialise++;
    }

    @Override
    public void start() throws MuleException
    {
        start++;
    }

    @Override
    public void stop() throws MuleException
    {
        stop++;
    }

    @Override
    public void dispose()
    {
        dispose++;
    }

    public ExtensionManager getExtensionManager()
    {
        return extensionManager;
    }

    public List<String> getEnemies()
    {
        return enemies;
    }

    public void setEnemies(List<String> enemies)
    {
        this.enemies = enemies;
    }

    public boolean isCancer()
    {
        return cancer;
    }

    public BigDecimal getMoney()
    {
        return money;
    }

    public Map<String, Long> getRecipe()
    {
        return recipe;
    }

    public Set<Ricin> getRicinPacks()
    {
        return ricinPacks;
    }

    public KnockeableDoor getNextDoor()
    {
        return nextDoor;
    }

    public Map<String, KnockeableDoor> getCandidateDoors()
    {
        return candidateDoors;
    }

    public int getInitialise()
    {
        return initialise;
    }

    public int getStart()
    {
        return start;
    }

    public int getStop()
    {
        return stop;
    }

    public int getDispose()
    {
        return dispose;
    }

    public HealthStatus getInitialHealth()
    {
        return initialHealth;
    }

    public HealthStatus getEndingHealth()
    {
        return endingHealth;
    }

    public ExtendedPersonalInfo getPersonalInfo()
    {
        return personalInfo;
    }

    void setEndingHealth(HealthStatus endingHealth)
    {
        this.endingHealth = endingHealth;
    }

    void setMoney(BigDecimal money)
    {
        this.money = money;
    }

    public String getLabAddress()
    {
        return labAddress;
    }

    public String getFirstEndevour()
    {
        return firstEndevour;
    }

    public String getLiteralExpressionWithDefault()
    {
        return literalExpressionWithDefault;
    }

    public String getLiteralExpressionWitouthDefault()
    {
        return literalExpressionWithoutDefault;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public Function<MuleEvent, Integer> getMoneyFunction()
    {
        return moneyFunction;
    }

    public Weapon getWeapon()
    {
        return weapon;
    }

    public List<Long> getMonthlyIncomes()
    {
        return monthlyIncomes;
    }

    public Map<String, List<String>> getDeathsBySeasons()
    {
        return deathsBySeasons;
    }

    public Map<String, Ricin> getLabeledRicin()
    {
        return labeledRicin;
    }

    public String getConfigName()
    {
        return configName;
    }
}
