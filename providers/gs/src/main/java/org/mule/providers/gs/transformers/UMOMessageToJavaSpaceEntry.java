/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.gs.transformers;

import org.mule.providers.gs.JiniMessage;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

/**
 * Convers an outbound event ot a JavaSpace entry that can be written to the space.
 *
 * @see net.jini.core.entry.Entry
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class UMOMessageToJavaSpaceEntry extends AbstractEventAwareTransformer {

    public UMOMessageToJavaSpaceEntry() {
        setReturnClass(JiniMessage.class);
    }

    public Object transform(Object src, UMOEventContext context) throws TransformerException {
        String destination = context.getEndpointURI().toString();
        return new JiniMessage(destination, src);
    }
}
