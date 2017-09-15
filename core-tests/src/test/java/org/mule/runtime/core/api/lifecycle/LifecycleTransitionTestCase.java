/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.privileged.lifecycle.AbstractLifecycleManager;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class LifecycleTransitionTestCase extends AbstractMuleTestCase {

  @Test
  public void testTransitions() throws MuleException {
    ObjectWithLifecycle o = new ObjectWithLifecycle();
    int transitions = 0;
    assertEquals(transitions, o.numTransitions());

    o.initialise();
    transitions++;
    assertEquals(transitions, o.numTransitions());
    assertEquals(ObjectWithLifecycle.INIT, o.lastTransition());
    o.start();
    transitions++;
    assertEquals(transitions, o.numTransitions());
    assertEquals(ObjectWithLifecycle.START, o.lastTransition());
    try {
      // Can't start again
      o.start();
      fail();
    } catch (Exception ex) {
      // this exception was expected
    }
    for (int i = 0; i < 5; i++) {
      o.stop();
      transitions++;
      assertEquals(transitions, o.numTransitions());
      assertEquals(ObjectWithLifecycle.STOP, o.lastTransition());
      o.start();
      transitions++;
      assertEquals(transitions, o.numTransitions());
      assertEquals(ObjectWithLifecycle.START, o.lastTransition());
    }
    o.stop();
    transitions++;
    assertEquals(transitions, o.numTransitions());
    assertEquals(ObjectWithLifecycle.STOP, o.lastTransition());
    try {
      // Can't stop again
      o.stop();
      fail();
    } catch (Exception ex) {
      // this exception was expected
    }
    o.dispose();
    transitions++;
    assertEquals(transitions, o.numTransitions());
    assertEquals(ObjectWithLifecycle.DISPOSE, o.lastTransition());
  }

  public static class MyLifecycleManager extends AbstractLifecycleManager {

    public MyLifecycleManager(String id, Object object) {
      super(id, object);
    }

    @Override
    protected void registerTransitions() {
      // init dispose
      addDirectTransition(NotInLifecyclePhase.PHASE_NAME, Initialisable.PHASE_NAME);
      addDirectTransition(NotInLifecyclePhase.PHASE_NAME, Disposable.PHASE_NAME);
      addDirectTransition(Initialisable.PHASE_NAME, Startable.PHASE_NAME);
      addDirectTransition(Initialisable.PHASE_NAME, Disposable.PHASE_NAME);

      // start stop
      addDirectTransition(Startable.PHASE_NAME, Stoppable.PHASE_NAME);
      addDirectTransition(Stoppable.PHASE_NAME, Startable.PHASE_NAME);
      addDirectTransition(Stoppable.PHASE_NAME, Disposable.PHASE_NAME);


      registerLifecycleCallback(Initialisable.PHASE_NAME, new LifecycleCallback<ObjectWithLifecycle>() {

        @Override
        public void onTransition(String phaseName, ObjectWithLifecycle object) {
          object.doInit();
        }
      });
      registerLifecycleCallback(Disposable.PHASE_NAME, new LifecycleCallback<ObjectWithLifecycle>() {

        @Override
        public void onTransition(String phaseName, ObjectWithLifecycle object) {
          object.doDispose();
        }
      });
      registerLifecycleCallback(Startable.PHASE_NAME, new LifecycleCallback<ObjectWithLifecycle>() {

        @Override
        public void onTransition(String phaseName, ObjectWithLifecycle object) {
          object.doStart();
        }
      });
      registerLifecycleCallback(Stoppable.PHASE_NAME, new LifecycleCallback<ObjectWithLifecycle>() {

        @Override
        public void onTransition(String phaseName, ObjectWithLifecycle object) {
          object.doStop();
        }
      });
    }
  }

  public static class ObjectWithLifecycle implements Lifecycle {

    public static final char INIT = 'i';
    public static final char DISPOSE = 'd';
    public static final char START = 'a';
    public static final char STOP = 'o';

    private LifecycleManager manager = new MyLifecycleManager("this", this);
    private String transitionHistory = "";

    @Override
    public void dispose() {
      try {
        manager.fireLifecycle(Disposable.PHASE_NAME);
      } catch (LifecycleException e) {
        throw new RuntimeException(e);
      }
    }

    private void doDispose() {
      transitionHistory += DISPOSE;
    }

    @Override
    public void initialise() throws InitialisationException {
      try {
        manager.fireLifecycle(Initialisable.PHASE_NAME);
      } catch (LifecycleException e) {
        throw new RuntimeException(e);
      }
    }

    private void doInit() {
      transitionHistory += INIT;
    }

    @Override
    public void start() throws MuleException {
      manager.fireLifecycle(Startable.PHASE_NAME);
    }

    private void doStart() {
      transitionHistory += START;
    }

    @Override
    public void stop() throws MuleException {
      manager.fireLifecycle(Stoppable.PHASE_NAME);
    }

    private void doStop() {
      transitionHistory += STOP;
    }


    public String getTransitionHistory() {
      return transitionHistory;
    }

    public int numTransitions() {
      return transitionHistory.length();
    }

    public char lastTransition() {
      return transitionHistory.length() == 0 ? 0 : transitionHistory.charAt(transitionHistory.length() - 1);
    }

  }
}
