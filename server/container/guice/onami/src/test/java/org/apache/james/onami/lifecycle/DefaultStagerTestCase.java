/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.onami.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

public class DefaultStagerTestCase {

    @Test
    public void stagerShouldStageObjectsRegisteredWhileStaging() {
        final Stager<TestAnnotationA> stager = new DefaultStager<TestAnnotationA>(TestAnnotationA.class);
        final AtomicBoolean staged = new AtomicBoolean();
        stager.register(new Stageable() {
            @Override
            public void stage(StageHandler stageHandler) {
                stager.register(new Stageable() {
                    @Override
                    public void stage(StageHandler stageHandler) {
                        staged.set(true);
                    }
                });
            }
        });

        stager.stage();

        Assert.assertTrue(staged.get());
    }

    /*
     * Deadlock scenario:
     * 1. DefaultStager holds lock while calling Stageable.stage();
     * 2. Stageable.stage() blocks on some thread
     * 3. the thread blocks on the lock in DefaultStager.register()
     */
    @Test
    public void stagerShouldNotDeadlockWhileStagingObjectChains() {
        final AtomicBoolean staged = new AtomicBoolean();
        final Stager<TestAnnotationA> stager = new DefaultStager<TestAnnotationA>(TestAnnotationA.class);
        stager.register(new Stageable() {
            @Override
            public void stage(StageHandler stageHandler) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        stager.register(new Stageable() {
                            @Override
                            public void stage(StageHandler stageHandler) {
                                staged.set(true);
                            }
                        });
                    }
                });
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        stager.stage();

        Assert.assertTrue(staged.get());
    }
}
