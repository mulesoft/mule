/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.drools;

import org.junit.Before;
import org.junit.Test;
import org.mule.module.bpm.MessageService;
import org.mule.module.bpm.Rules;
import org.mule.tck.junit4.AbstractMuleTestCase;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DroolsTestCase extends AbstractMuleTestCase
{

    private final Drools drools = new Drools();
    private final Rules rules = mock(Rules.class);
    private final MessageService messageService = mock(MessageService.class, RETURNS_DEEP_STUBS);

    @Before
    public void setUp() throws Exception
    {
        when(rules.getResource()).thenReturn("rulesFile.drl");
        drools.setMessageService(messageService);
        DroolsSessionData sessionData = (DroolsSessionData) drools.createSession(rules);
        when(rules.getSessionData()).thenReturn(sessionData);
    }

    @Test
    public void testMemoryLeakCausedBySaveDuplicatedObjects() throws Exception
    {
        Object handle1 = drools.assertFact(rules, new TestFact("idTest", "descriptionTest"));
        Object handle2 = drools.assertFact(rules, new TestFact("idTest", "descriptionTest"));
        assertThat(handle1, is(handle2));
    }

    public class TestFact {
        private final String id;
        private String description;

        public TestFact(String id, String description) {
            this.id = id;
            this.description = description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestFact testFact = (TestFact) o;

            if (id != null ? !id.equals(testFact.id) : testFact.id != null) return false;
            return description != null ? description.equals(testFact.description) : testFact.description == null;

        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (description != null ? description.hashCode() : 0);
            return result;
        }

        public String getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }
}
