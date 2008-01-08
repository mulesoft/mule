/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;

/**
 * This encapsulates two definition parsers - orphan and named - and returns the
 * named definition parser if the "inherit" attribute is set.  This allows a named
 * orphan to be defined (inherit="false") and then extended (inherit="true").
 * The two sub-parsers must be consistent, as described in
 * {@link org.mule.config.spring.parsers.delegate.AbstractParallelDelegatingDefinitionParser}
 */
public class InheritDefinitionParser extends BooleanAttributeSelectionDefinitionParser
{

    public static final String INHERIT = "inherit";

    public InheritDefinitionParser(OrphanDefinitionParser orphan, NamedDefinitionParser named)
    {
        this(false, orphan, named);
    }

    public InheritDefinitionParser(boolean deflt, OrphanDefinitionParser orphan, NamedDefinitionParser named)
    {
        super(INHERIT, deflt, named, orphan);
    }

}
