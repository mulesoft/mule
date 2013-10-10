/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;

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
