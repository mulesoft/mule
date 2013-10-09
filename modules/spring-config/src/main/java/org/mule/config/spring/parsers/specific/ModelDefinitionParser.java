/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.delegate.InheritDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.processors.ProvideDefaultName;
import org.mule.model.seda.SedaModel;

public class ModelDefinitionParser extends InheritDefinitionParser
{

    public ModelDefinitionParser()
    {
        super(makeOrphan(), new NamedDefinitionParser());
    }

    private static OrphanDefinitionParser makeOrphan()
    {
        OrphanDefinitionParser orphan = new OrphanDefinitionParser(SedaModel.class, true);
        orphan.registerPreProcessor(new ProvideDefaultName("model"));
        return orphan;
    }

}
