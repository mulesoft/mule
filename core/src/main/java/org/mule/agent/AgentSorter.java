/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.agent;

import org.mule.api.agent.Agent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

/**
 * Sort {@link Agent} instances so that dependencies of each agent are started before the 
 * actual {@link Agent} is started itself.
 */
public class AgentSorter
{
    @SuppressWarnings("unchecked")
    public static List sortAgents(Collection<Agent> agents)
    {
        List<Agent> sortedAgents = new ArrayList<Agent>();
        
        // step 1: add all agents with no dependencies
        Collection<Agent> agentsWithoutDependencies = CollectionUtils.select(agents, new Predicate()
        {
            public boolean evaluate(Object object)
            {
                return ((Agent) object).getDependentAgents().size() == 0;
            }
        });
        sortedAgents.addAll(agentsWithoutDependencies);
        
        // step 2: process the remaining agents
        List<Agent> remainingAgents = new ArrayList<Agent>(agents);
        remainingAgents.removeAll(agentsWithoutDependencies);
        while (!remainingAgents.isEmpty())
        {
            int processedAgents = 0;
            ListIterator iter = remainingAgents.listIterator();
            while (iter.hasNext())
            {
                Agent agent = (Agent) iter.next();
                if (dependentAgentsPresent(agent.getDependentAgents(), agents, sortedAgents))
                {
                    sortedAgents.add(agent);
                    iter.remove();
                    processedAgents++;
                }
            }
            
            // if we did not process any agents this iteration, the remaining agents 
            // likely form a dependency cycle
            if (processedAgents == 0)
            {
                throw new IllegalArgumentException("Dependency cycle: " + remainingAgents);
            }
        }

        return sortedAgents;
    }
    
    private static boolean dependentAgentsPresent(List dependentClasses, Collection allRegisteredAgents, 
        List sortedAgents)
    {
        Iterator dependencyIterator = dependentClasses.iterator();
        while (dependencyIterator.hasNext())
        {
            Class dependentClass = (Class) dependencyIterator.next();
            
            if (!classExistsInCollection(dependentClass, allRegisteredAgents))
            {
                // this agent is currently not registed, ignore this dependency
                continue;
            }

            if (!classExistsInCollection(dependentClass, sortedAgents))
            {
                return false;
            }
        }
        return true;
    }
    
    private static boolean classExistsInCollection(Class clazz, Collection collection)
    {
        return CollectionUtils.exists(collection, new ClassEqualityPredicate(clazz));
    }
    
    private static class ClassEqualityPredicate implements Predicate
    {
        private Class requiredClass;

        public ClassEqualityPredicate(Class requiredClass)
        {
            super();
            this.requiredClass = requiredClass;
        }
        
        public boolean evaluate(Object object)
        {
            return object.getClass().equals(requiredClass);
        }
    }
    
}


