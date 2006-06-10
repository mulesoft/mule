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

import edu.emory.mathcs.backport.java.util.concurrent.Executor;

/**
 * Defines the operations that a pool in charge of the execution of Work
 * instances must expose.
 * 
 * @version $Rev$ $Date$
 */
public interface WorkExecutorPool extends Executor
{

    /**
     * Gets the current number of active threads in the pool.
     * 
     * @return Number of active threads in the pool.
     */
    int getPoolSize();

    /**
     * Gets the maximum number of threads to simultaneously execute.
     * 
     * @return Maximum size.
     */
    int getMaximumPoolSize();

    /**
     * Sets the maximum number of threads to simultaneously execute.
     * 
     * @param aSize Maximum size.
     */
    void setMaximumPoolSize(int aSize);

    WorkExecutorPool start();

    WorkExecutorPool stop();

}
