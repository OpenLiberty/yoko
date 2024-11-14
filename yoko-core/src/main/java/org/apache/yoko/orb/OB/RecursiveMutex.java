/*
 * Copyright 2024 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.orb.OB;

import static java.lang.Thread.currentThread;

public class RecursiveMutex {
    private Thread owner_;

    private boolean locked_;

    private int count_;

    public RecursiveMutex() {
        owner_ = null;
        locked_ = false;
        count_ = 0;
    }

    public synchronized void lock() {
        if (locked_ && currentThread() == owner_) {
            count_++;
            return;
        }

        while (locked_) {
            try {
                wait();
            } catch (InterruptedException ex) {
            }
        }

        //
        // Assert: owner_ == null, count_ = 0
        //
        locked_ = true;
        owner_ = currentThread();
        count_ = 1;
    }

    public synchronized void unlock() {
        // Assert: owner_ == Thread.currentThread, count_ > 0, locked = true
        count_--;
        if (count_ <= 0) {
            locked_ = false;
            owner_ = null;
            notifyAll();
        }
    }
}
