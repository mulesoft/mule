/*
 *  PlexusContainerContext.java
 *  Copyright 2004 Brian Topping
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.plexus;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.mule.umo.model.ComponentNotFoundException;
import org.mule.umo.model.ComponentResolverException;
import org.mule.umo.model.UMOContainerContext;

import java.io.Reader;
import java.util.Map;

public class PlexusContainerContext implements UMOContainerContext{
    protected Embedder container;

    public PlexusContainerContext() {
        this(new Embedder());
    }

    public PlexusContainerContext(Embedder container) {
        this.container = container;
    }

    public Object getComponent(Object key) throws ComponentNotFoundException {
        try {
            return container.lookup(key.toString());
        } catch (ComponentLookupException e) {
            throw new ComponentNotFoundException("could not load component", e);
        }
    }

    public void configure(Reader configuration, Map configurationProperties) throws ComponentResolverException {
        try {
            container.setConfiguration(configuration);
            container.start();
        } catch (Exception e) {
            throw new ComponentResolverException("problem configuring and starting container", e);
        }
    }
}
