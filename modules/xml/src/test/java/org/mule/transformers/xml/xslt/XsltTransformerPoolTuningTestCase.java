/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xslt;

import org.junit.Test;
import org.mule.api.transformer.Transformer;
import org.mule.module.xml.transformer.XsltTransformer;
import org.mule.tck.junit4.FunctionalTestCase;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class XsltTransformerPoolTuningTestCase extends FunctionalTestCase {
    private static final long MILLISECONDS_IN_ONE_HOUR = 60 * 60 * 1000;

    private static final int EXPECTED_DEFAULT_MAX_IDLE_TRANSFORMERS = 32;
    private static final int EXPECTED_DEFAULT_MAX_ACTIVE_TRANSFORMERS = 32;
    private static final long EXPECTED_DEFAULT_TIME_BETWEEN_EVICTIONS = -1;

    @Override
    protected String getConfigFile() {
        return "xslt-transformer-pool-tuning-configuration.xml";
    }

    @Test
    public void defaultTransformer() {
        XsltTransformer transformer = getRegisteredXsltTransformer("defaultTransformer");
        assertThat(transformer.getMaxIdleTransformers(),
                is(EXPECTED_DEFAULT_MAX_IDLE_TRANSFORMERS));
        assertThat(transformer.getMaxActiveTransformers(),
                is(EXPECTED_DEFAULT_MAX_ACTIVE_TRANSFORMERS));
        assertThat(transformer.getTimeBetweenTransformerEvictions(),
                is(EXPECTED_DEFAULT_TIME_BETWEEN_EVICTIONS));
    }

    @Test
    public void specifyingMaxIdleTransformers() {
        int specifiedMaxIdleTransformers = 10;

        assertThat("Specified value is the same as the default value; testing is invalid.",
                specifiedMaxIdleTransformers, is(not(EXPECTED_DEFAULT_MAX_IDLE_TRANSFORMERS)));

        XsltTransformer transformer =
                getRegisteredXsltTransformer("transformerSpecifyingTenMaxIdleTransformers");
        assertThat(transformer.getMaxIdleTransformers(), is(specifiedMaxIdleTransformers));
        assertThat(transformer.getMaxActiveTransformers(),
                is(EXPECTED_DEFAULT_MAX_ACTIVE_TRANSFORMERS));
        assertThat(transformer.getTimeBetweenTransformerEvictions(),
                is(EXPECTED_DEFAULT_TIME_BETWEEN_EVICTIONS));
    }

    @Test
    public void specifyingMaxActiveTransformers() {
        int specifiedMaxActiveTransformers = 10;

        assertThat("Specified value is the same as the default value; testing is invalid.",
                specifiedMaxActiveTransformers, is(not(EXPECTED_DEFAULT_MAX_ACTIVE_TRANSFORMERS)));

        XsltTransformer transformer =
                getRegisteredXsltTransformer("transformerSpecifyingTenMaxActiveTransformers");
        assertThat(transformer.getMaxIdleTransformers(),
                is(EXPECTED_DEFAULT_MAX_IDLE_TRANSFORMERS));
        assertThat(transformer.getMaxActiveTransformers(), is(specifiedMaxActiveTransformers));
        assertThat(transformer.getTimeBetweenTransformerEvictions(),
                is(EXPECTED_DEFAULT_TIME_BETWEEN_EVICTIONS));
    }

    @Test
    public void specifyingTimeBetweenEvictions() {
        assertThat("Specified value is the same as the default value; testing is invalid.",
                MILLISECONDS_IN_ONE_HOUR, is(not(EXPECTED_DEFAULT_TIME_BETWEEN_EVICTIONS)));

        XsltTransformer transformer = getRegisteredXsltTransformer(
                "transformerSpecifyingHourlyTimeBetweenTransformerEvictions");
        assertThat(transformer.getMaxIdleTransformers(),
                is(EXPECTED_DEFAULT_MAX_IDLE_TRANSFORMERS));
        assertThat(transformer.getMaxActiveTransformers(),
                is(EXPECTED_DEFAULT_MAX_ACTIVE_TRANSFORMERS));
        assertThat(transformer.getTimeBetweenTransformerEvictions(), is(MILLISECONDS_IN_ONE_HOUR));
    }

    private XsltTransformer getRegisteredXsltTransformer(String name) {
        Transformer transformer = muleContext.getRegistry().lookupTransformer(name);
        assertThat(transformer, is(instanceOf(XsltTransformer.class)));
        XsltTransformer xsltTransformer = (XsltTransformer) transformer;
        return xsltTransformer;
    }
}
