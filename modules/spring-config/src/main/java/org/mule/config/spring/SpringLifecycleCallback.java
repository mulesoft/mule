/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.lifecycle.LifecycleObject;
import org.mule.lifecycle.RegistryLifecycleCallback;
import org.mule.lifecycle.RegistryLifecycleManager;

import com.google.common.collect.TreeTraverser;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class SpringLifecycleCallback extends RegistryLifecycleCallback<SpringRegistry>
{

    public SpringLifecycleCallback(RegistryLifecycleManager registryLifecycleManager)
    {
        super(registryLifecycleManager);
    }

    @Override
    protected Collection<?> lookupObjectsForLifecycle(LifecycleObject lo)
    {
        Map<String, Object> objects = getSpringRegistry().lookupEntriesForLifecycle(lo.getType());

        DependencyNode root = new DependencyNode(null, null);

        for (Map.Entry<String, Object> entry : objects.entrySet())
        {
            addDependency(root, entry.getKey(), entry.getValue());
        }

        Iterable<DependencyNode> orderedNodes = new TreeTraverser<DependencyNode>()
        {
            @Override
            public Iterable children(DependencyNode node)
            {
                return node.getChilds();
            }
        }.postOrderTraversal(root);

        List<Object> orderedObjects = new LinkedList<>();
        for (DependencyNode node : orderedNodes)
        {
            if (node.isRoot())
            {
                break;
            }

            orderedObjects.add(node.getValue());
        }

        return orderedObjects;
    }

    private SpringRegistry getSpringRegistry()
    {
        return (SpringRegistry) registryLifecycleManager.getLifecycleObject();
    }


    private void addDependency(DependencyNode parent, String key, Object object)
    {
        final DependencyNode node = new DependencyNode(key, object);
        parent.addChild(node);

        for (Map.Entry<String, Object> entry : getSpringRegistry().getDepencies(key).entrySet())
        {
            addDependency(node, entry.getKey(), entry.getValue());
        }
    }

    private class DependencyNode
    {

        private final String key;
        private final Object value;
        private final List<DependencyNode> childs = new LinkedList<>();

        private DependencyNode(String key, Object value)
        {
            this.key = key;
            this.value = value;
        }

        public DependencyNode addChild(DependencyNode child)
        {
            childs.add(child);
            return this;
        }

        public boolean isRoot()
        {
            return key == null;
        }

        public List<DependencyNode> getChilds()
        {
            return childs;
        }

        public Object getValue()
        {
            return value;
        }
    }
}
