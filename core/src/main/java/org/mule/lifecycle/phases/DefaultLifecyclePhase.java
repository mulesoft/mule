/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.lifecycle.phases;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.lifecycle.LifecycleStateEnabled;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.lifecycle.LifecycleObject;

/**
 * Represents a configurable lifecycle phase. This is a default implementation of a
 * 'generic phase' in that is can be configured to represnt any phase. Instances of
 * this phase can then be registered with a
 * {@link org.mule.api.lifecycle.LifecycleManager} and by used to enforce a lifecycle
 * phase on an object. Usually, Lifecycle phases have a fixed configuration in which
 * case a specialisation of this class should be created that initialises its
 * configuration internally.
 * <p>
 * Note that this class and {@link org.mule.api.lifecycle.LifecycleTransitionResult}
 * both make assumptions about the interfaces used - the return values and
 * exceptions. These are, currently, that the return value is either void or
 * {@link org.mule.api.lifecycle.LifecycleTransitionResult} and either 0 or 1
 * exceptions can be thrown which are either {@link InstantiationException} or
 * {@link org.mule.api.lifecycle.LifecycleException}.
 * 
 * @see org.mule.api.lifecycle.LifecyclePhase
 */
public class DefaultLifecyclePhase implements LifecyclePhase, MuleContextAware
{
    protected transient final Log logger = LogFactory.getLog(DefaultLifecyclePhase.class);
    private Class<?> lifecycleClass;
    private final Method lifecycleMethod;
    private Set<LifecycleObject> orderedLifecycleObjects = new LinkedHashSet<LifecycleObject>(6);
    private Class<?>[] ignorredObjectTypes;
    private final String name;
    private final String oppositeLifecyclePhase;
    private Set<String> supportedPhases;
    private MuleContext muleContext;

    public DefaultLifecyclePhase(String name, Class<?> lifecycleClass, String oppositeLifecyclePhase)
    {
        this.name = name;
        this.lifecycleClass = lifecycleClass;
        // DefaultLifecyclePhase interface only has one method
        lifecycleMethod = lifecycleClass.getMethods()[0];
        this.oppositeLifecyclePhase = oppositeLifecyclePhase;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    /**
     * Subclasses can override this method to order <code>objects</code> before the
     * lifecycle method is applied to them. This method does not apply any special
     * ordering to <code>objects</code>.
     * 
     * @param objects
     * @param lo
     * @return List with ordered objects
     */
    protected List sortLifecycleInstances(Collection objects, LifecycleObject lo)
    {
        return new ArrayList(objects);
    }

    public void addOrderedLifecycleObject(LifecycleObject lco)
    {
        orderedLifecycleObjects.add(lco);
    }

    public void removeOrderedLifecycleObject(LifecycleObject lco)
    {
        orderedLifecycleObjects.remove(lco);
    }

    protected boolean ignoreType(Class<?> type)
    {
        if (ignorredObjectTypes == null)
        {
            return false;
        }
        else
        {
            for (int i = 0; i < ignorredObjectTypes.length; i++)
            {
                Class<?> ignorredObjectType = ignorredObjectTypes[i];
                if (ignorredObjectType.isAssignableFrom(type))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<LifecycleObject> getOrderedLifecycleObjects()
    {
        return orderedLifecycleObjects;
    }

    public void setOrderedLifecycleObjects(Set<LifecycleObject> orderedLifecycleObjects)
    {
        this.orderedLifecycleObjects = orderedLifecycleObjects;
    }

    public Class<?>[] getIgnoredObjectTypes()
    {
        return ignorredObjectTypes;
    }

    public void setIgnoredObjectTypes(Class<?>[] ignorredObjectTypes)
    {
        this.ignorredObjectTypes = ignorredObjectTypes;
    }

    public Class<?> getLifecycleClass()
    {
        return lifecycleClass;
    }

    public void setLifecycleClass(Class<?> lifecycleClass)
    {
        this.lifecycleClass = lifecycleClass;
    }

    public String getName()
    {
        return name;
    }

    public Set<String> getSupportedPhases()
    {
        return supportedPhases;
    }

    public void setSupportedPhases(Set<String> supportedPhases)
    {
        this.supportedPhases = supportedPhases;
    }

    public void registerSupportedPhase(String phase)
    {
        if (supportedPhases == null)
        {
            supportedPhases = new HashSet<String>();
        }
        supportedPhases.add(phase);
    }

    public boolean isPhaseSupported(String phase)
    {
        if (getSupportedPhases() == null)
        {
            return false;
        }
        else
        {
            if (getSupportedPhases().contains(ALL_PHASES))
            {
                return true;
            }
            else
            {
                return getSupportedPhases().contains(phase);
            }
        }
    }

    public void applyLifecycle(Object o) throws LifecycleException
    {
        if (o == null)
        {
            return;
        }
        if (ignoreType(o.getClass()))
        {
            return;
        }
        if (!getLifecycleClass().isAssignableFrom(o.getClass()))
        {
            return;
        }
        if (o instanceof LifecycleStateEnabled)
        {
            // If an object has its own lifecycle manager "LifecycleStateEnabled" it
            // is possible that
            // its state can be controlled outside the registry i.e. via JMX, double
            // check here that we are
            // not calling the same lifecycle twice
            if (((LifecycleStateEnabled) o).getLifecycleState().isPhaseComplete(this.getName()))
            {
                return;
            }
            else if (!((LifecycleStateEnabled) o).getLifecycleState().isValidTransition(this.getName()))
            {
                return;
            }
        }
        try
        {
            lifecycleMethod.invoke(o);
        }
        catch (final Exception e)
        {
            Throwable t = ExceptionHelper.unwrap(e);

            if (t instanceof LifecycleException)
            {
                throw (LifecycleException) t;
            }

            throw new LifecycleException(CoreMessages.failedToInvokeLifecycle(lifecycleMethod.getName(), o),
                t, this);
        }
    }

    public String getOppositeLifecyclePhase()
    {
        return oppositeLifecyclePhase;
    }
}
