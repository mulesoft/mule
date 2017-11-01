/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.drools;

import static java.lang.Math.random;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.module.bpm.MessageService;
import org.mule.module.bpm.Rules;
import org.mule.tck.junit4.AbstractMuleTestCase;

public class DroolsConcurrencyTestCase extends AbstractMuleTestCase
{

    private static final int CATEGORY_A = 0;
    private static final int CATEGORY_B = 1;
    private static final int CATEGORY_DELIMITER = 50;
    private static final int POOL_SIZE = 30;
    private final static Drools drools = new Drools();
    private final static Rules rules = mock(Rules.class);
    private final static MessageService messageService = mock(MessageService.class, RETURNS_DEEP_STUBS);

    @BeforeClass
    public static void setUp() throws Exception
    {
        when(rules.getResource()).thenReturn("categoriesFile.drl");
        drools.setMessageService(messageService);
    }

    @Test
    public void testConcurrentRulesFired() throws Exception
    {
        createSession();
        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
        List<WorkerThread> workers = new ArrayList<>();
        List<Future> futures = new ArrayList<>();


        for (int i = 0; i < POOL_SIZE; i++)
        {
            Integer randomValue = (int) (random() * 100);
            WorkerThread worker = new WorkerThread(new Data(randomValue), drools, rules);
            workers.add(worker);
            Future future = executor.submit(worker);
            futures.add(future);
        }

        for (int i = 0; i < POOL_SIZE; i++)
        {
            futures.get(i).get(10, SECONDS);
            assertThat(workers.get(i).getData().getCategory(), equalTo(getExpectedCategory(workers.get(i).getData().getValue())));
        }

        executor.shutdown();
    }

    private Object getExpectedCategory(Integer value)
    {
        if (value < CATEGORY_DELIMITER)
        {
            return CATEGORY_A;
        }

        return CATEGORY_B;
    }

    private static void createSession() throws Exception
    {
        DroolsSessionData sessionData = (DroolsSessionData) drools.createSession(rules);
        when(rules.getSessionData()).thenReturn(sessionData);
    }
}
