/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.mule.impl.work;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

/**
 * PooledExecutor enforcing a timed out "blocked execution policy". The works
 * submitted to this pooled executor MUST be a WorkWrapper.
 *
 * @version $Rev$ $Date$
 */
public class TimedOutPooledExecutor extends PooledExecutor {

    /**
     * Creates a pooled executor. The Channel used to enqueue the submitted
     * Work instance is a queueless synchronous one.
     */
    public TimedOutPooledExecutor() {
        setBlockedExecutionHandler(new TimedOutSpinHandler());
    }

    /**
     * Creates a pooled executor, which uses the provided Channel as its
     * queueing mechanism.
     *
     * @param aChannel Channel to be used to enqueue the submitted Work
     * intances.
     */
    public TimedOutPooledExecutor(Channel aChannel) {
        super(aChannel);
        setBlockedExecutionHandler(new TimedOutSpinHandler());
    }

    /**
     * Executes the provided task, which MUST be an instance of WorkWrapper.
     *
     * @throws IllegalArgumentException Indicates that the provided task is not
     * a WorkWrapper instance.
     */
    public void execute(Runnable aTask) throws InterruptedException {
        if (!(aTask instanceof WorkerContext)) {
            throw new IllegalArgumentException("Please submit a WorkWrapper.");
        }
        super.execute(aTask);
    }

    /**
     * This class implements a time out policy when a work is blocked: it offers
     * the task to the pool until the work has timed out.
     *
     * @version $Rev$ $Date$
     */
    private class TimedOutSpinHandler
            implements PooledExecutor.BlockedExecutionHandler {

        /* (non-Javadoc)
         * @see EDU.oswego.cs.dl.util.concurrent.PooledExecutor.BlockedExecutionHandler#blockedAction(java.lang.Runnable)
         */
        public boolean blockedAction(Runnable arg0) throws InterruptedException {
            WorkerContext work = (WorkerContext) arg0;
            if (!handOff_.offer(arg0, work.getStartTimeout())) {
                // double check.
                if (work.isTimedOut()) {
                    return false;
                }
                return true;
            }
            return true;
        }
    }
}