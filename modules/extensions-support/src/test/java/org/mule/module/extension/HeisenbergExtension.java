/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.extension.ExtensionManager;
import org.mule.extension.annotations.Extensible;
import org.mule.extension.annotations.Extension;
import org.mule.extension.annotations.Operations;
import org.mule.extension.annotations.Parameter;
import org.mule.extension.annotations.ParameterGroup;
import org.mule.extension.annotations.capability.Xml;
import org.mule.extension.annotations.param.Optional;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

@Extension(name = HeisenbergExtension.EXTENSION_NAME, description = HeisenbergExtension.EXTENSION_DESCRIPTION, version = HeisenbergExtension.EXTENSION_VERSION)
@Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
@Xml(schemaLocation = HeisenbergExtension.SCHEMA_LOCATION, namespace = HeisenbergExtension.NAMESPACE, schemaVersion = HeisenbergExtension.SCHEMA_VERSION)
@Extensible(alias = "heisenberg-empire")
public class HeisenbergExtension implements Lifecycle, MuleContextAware
{

    public static final String SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/heisenberg";
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

    @Inject
    private ExtensionManager extensionManager;

    @ParameterGroup
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
    private KnockeableDoor nextDoor;

    /**
     * Doors I might knock on but still haven't made up mind about
     */
    @Parameter
    @Optional
    private Map<String, KnockeableDoor> candidateDoors;

    @Parameter
    private HealthStatus initialHealth;

    @Parameter(alias = "finalHealth")
    private HealthStatus endingHealth;

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
