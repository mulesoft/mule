/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.lifecycle.Lifecycle;
import org.mule.lifecycle.LifecycleObject;
import org.mule.lifecycle.RegistryLifecycleCallback;
import org.mule.lifecycle.RegistryLifecycleManager;

import com.google.common.collect.TreeTraverser;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A {@link RegistryLifecycleCallback} to be used with instances
 * of {@link SpringRegistry}. For each object in which a {@link Lifecycle} phase
 * is going to be applied, it detects all the dependencies for that object
 * and applies the same phase on those dependencies first (recursively).
 * <p/>
 * This guarantees that if object A depends on object B and C, necessary lifecycle phases will have
 * been applied on B and C before it is applied to A
 *
 * @since 3.7.0
 */
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

        final DependencyNode root = new DependencyNode(null);

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
            if (node == root)
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
        final DependencyNode node = new DependencyNode(object);
        parent.addChild(node);

        for (Map.Entry<String, Object> dependency : getSpringRegistry().getDependencies(key).entrySet())
        {
            addDependency(node, dependency.getKey(), dependency.getValue());
        }
    }

    private class DependencyNode
    {

        private final Object value;
        private final List<DependencyNode> childs = new LinkedList<>();

        private DependencyNode(Object value)
        {
            this.value = value;
        }

        public DependencyNode addChild(DependencyNode child)
        {
            childs.add(child);
            return this;
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
