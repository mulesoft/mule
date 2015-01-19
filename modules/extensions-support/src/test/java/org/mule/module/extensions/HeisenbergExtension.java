/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.extensions.annotations.Extensible;
import org.mule.extensions.annotations.Extension;
import org.mule.extensions.annotations.Operations;
import org.mule.extensions.annotations.Parameter;
import org.mule.extensions.annotations.Parameters;
import org.mule.extensions.annotations.capability.Xml;
import org.mule.extensions.annotations.param.Optional;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Extension(name = HeisenbergExtension.EXTENSION_NAME, description = HeisenbergExtension.EXTENSION_DESCRIPTION, version = HeisenbergExtension.EXTENSION_VERSION)
@Operations({HeisenbergOperations.class, HeisenbergAliasOperations.class})
@Xml(schemaLocation = HeisenbergExtension.SCHEMA_LOCATION, namespace = HeisenbergExtension.NAMESPACE, schemaVersion = HeisenbergExtension.SCHEMA_VERSION)
@Extensible(alias = "heisenberg-empire")
public class HeisenbergExtension implements Lifecycle, MuleContextAware
{

    public static final String SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/extension/heisenberg";
    public static final String NAMESPACE = "heisenberg";
    public static final String SCHEMA_VERSION = "1.0-blue";

    public static final String HEISENBERG = "Heisenberg";
    public static final String AGE = "50";
    public static final String EXTENSION_NAME = "heisenberg";
    public static final String EXTENSION_DESCRIPTION = "My Test Extension just to unit test";
    public static final String EXTENSION_VERSION = "1.0";

    private int initialise = 0;
    private int start = 0;
    private int stop = 0;
    private int dispose = 0;

    private MuleContext muleContext;

    @Parameters
    private ExtendedPersonalInfo personalInfo = new ExtendedPersonalInfo();

    @Parameter
    private List<String> enemies = new LinkedList<>();

    @Parameter
    private BigDecimal money;

    @Parameter
    private boolean cancer;

    @Parameter
    @Optional
    private Map<String, Long> recipe;

    @Parameter
    @Optional
    private Set<Ricin> ricinPacks;

    @Parameter
    @Optional
    private Door nextDoor;

    /**
     * Doors I might knock on but still haven't made up mind about
     */
    @Parameter
    @Optional
    private Map<String, Door> candidateDoors;

    @Parameter
    private HealthStatus initialHealth;

    @Parameter
    private HealthStatus finalHealth;

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

    public void setCancer(boolean cancer)
    {
        this.cancer = cancer;
    }

    public BigDecimal getMoney()
    {
        return money;
    }

    public void setMoney(BigDecimal money)
    {
        this.money = money;
    }

    public Map<String, Long> getRecipe()
    {
        return recipe;
    }

    public void setRecipe(Map<String, Long> recipe)
    {
        this.recipe = recipe;
    }

    public Set<Ricin> getRicinPacks()
    {
        return ricinPacks;
    }

    public void setRicinPacks(Set<Ricin> ricinPacks)
    {
        this.ricinPacks = ricinPacks;
    }

    public Door getNextDoor()
    {
        return nextDoor;
    }

    public void setNextDoor(Door nextDoor)
    {
        this.nextDoor = nextDoor;
    }

    public Map<String, Door> getCandidateDoors()
    {
        return candidateDoors;
    }

    public void setCandidateDoors(Map<String, Door> candidateDoors)
    {
        this.candidateDoors = candidateDoors;
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

    public void setInitialHealth(HealthStatus initialHealth)
    {
        this.initialHealth = initialHealth;
    }

    public HealthStatus getFinalHealth()
    {
        return finalHealth;
    }

    public ExtendedPersonalInfo getPersonalInfo()
    {
        return personalInfo;
    }

    public void setPersonalInfo(ExtendedPersonalInfo personalInfo)
    {
        this.personalInfo = personalInfo;
    }

    public void setFinalHealth(HealthStatus finalHealth)
    {
        this.finalHealth = finalHealth;
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
}
