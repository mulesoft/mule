/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.transformers;

import org.mule.tck.testmodels.fruit.FruitCleaner;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * TODO
 */

public abstract class AppleMixin
{
    AppleMixin(@JsonProperty("bitten") boolean bitten)
    {
        super();
    }

    @JsonIgnore
    abstract FruitCleaner getAppleCleaner();

    @JsonIgnore
    abstract void setAppleCleaner(FruitCleaner fc);
}
