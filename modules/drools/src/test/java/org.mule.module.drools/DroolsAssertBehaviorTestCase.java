/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.drools;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.module.bpm.MessageService;
import org.mule.module.bpm.Rules;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.drools.Drools.USE_EQUALITY_ASSERT_BEHAVIOR;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import static org.mule.tck.MuleTestUtils.TestCallback;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized. class)
public class DroolsAssertBehaviorTestCase extends AbstractMuleTestCase
{

    private final static Drools drools = new Drools();
    private final static Rules rules = mock(Rules.class);
    private final static MessageService messageService = mock(MessageService.class, RETURNS_DEEP_STUBS);
    private final String propertyValue ;
    private final TestCallback testCallback ;

    public DroolsAssertBehaviorTestCase(String propertyValue, TestCallback testCallback) {
        this.propertyValue = propertyValue;
        this.testCallback = testCallback;
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {"false", identityBehaviourCallback},
                {"true", equalityBehaviourCallback}
        });
    }

    @BeforeClass
    public static void setUp () throws Exception {
        when(rules.getResource()).thenReturn("rulesFile.drl");
        drools.setMessageService(messageService);
    }

    @Test
    public void testAssertBehaviour() throws Exception
    {
        testWithSystemProperty(USE_EQUALITY_ASSERT_BEHAVIOR, propertyValue, testCallback);
    }

    public static class TestFact {
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

    static TestCallback identityBehaviourCallback =  new TestCallback(){
        @Override
        public void run() throws Exception {
            createSession();
            Object handle1 = drools.assertFact(rules, new TestFact("idTest", "descriptionTest"));
            Object handle2 = drools.assertFact(rules, new TestFact("idTest", "descriptionTest"));
            assertThat(handle1, is(not(handle2)));
        }
    };

    static TestCallback equalityBehaviourCallback =  new TestCallback(){
        @Override
        public void run() throws Exception {
            createSession();
            Object handle1 = drools.assertFact(rules, new TestFact("idTest", "descriptionTest"));
            Object handle2 = drools.assertFact(rules, new TestFact("idTest", "descriptionTest"));
            assertThat(handle1, is(handle2));
        }
    };

    private static void createSession () throws Exception
    {
        DroolsSessionData sessionData = (DroolsSessionData) drools.createSession(rules);
        when(rules.getSessionData()).thenReturn(sessionData);
    }
}
