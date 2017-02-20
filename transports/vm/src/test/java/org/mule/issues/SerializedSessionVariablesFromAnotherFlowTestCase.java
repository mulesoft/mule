/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.issues;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class SerializedSessionVariablesFromAnotherFlowTestCase extends FunctionalTestCase
{

    private static final String TEST_MESSAGE = "Inner Flow";
    
    private static final String SESSION_VAR_NAME = "sessionVar";
    
    public SerializedSessionVariablesFromAnotherFlowTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "issues/serialized-session-variables-from-another-flow.xml";
    }

    @Test
    public void whenASessionVariableIsModifiedInExceptionCatchingItIsAvailableInCallingFlow() throws Exception
    {
        muleContext.start();

        MuleMessage m = muleContext.getClient().send("vm://main", "Test", null);

        assertThat((String) m.getSessionProperty(SESSION_VAR_NAME), equalTo(TEST_MESSAGE));
    }

}
