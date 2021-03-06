/*
 * Copyright (C) 2012 Markus Junginger, greenrobot (http://greenrobot.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.greenrobot.event.test;

import java.util.ArrayList;
import java.util.List;

import android.os.Looper;

/**
 * @author Markus Junginger, greenrobot
 */
public class EventBusMainThreadTest extends AbstractEventBusTest {

    private BackgroundPoster backgroundPoster;

    protected void setUp() throws Exception {
        super.setUp();
        backgroundPoster = new BackgroundPoster();
        backgroundPoster.start();
    }

    @Override
    protected void tearDown() throws Exception {
        backgroundPoster.shutdown();
        backgroundPoster.join();
        super.tearDown();
    }

    public void testPost() throws InterruptedException {
        eventBus.register(this);
        eventBus.post("Hello");
        waitForEventCount(1, 1000);

        assertEquals("Hello", lastEvent);
        assertEquals(Looper.getMainLooper().getThread(), lastThread);
    }

    public void testPostInBackgroundThread() throws InterruptedException {
        eventBus.register(this);
        backgroundPoster.post("Hello");
        waitForEventCount(1, 1000);
        assertEquals("Hello", lastEvent);
        assertEquals(Looper.getMainLooper().getThread(), lastThread);
    }

    public void onEventMainThread(String event) {
        trackEvent(event);
    }

    class BackgroundPoster extends Thread {
        private boolean running = true;
        private List<Object> eventQ = new ArrayList<Object>();
        private List<Object> eventsDone = new ArrayList<Object>();

        public BackgroundPoster() {
            super("BackgroundPoster");
        }

        @Override
        public void run() {
            while (running) {
                Object event = pollEvent();
                if (event != null) {
                    eventBus.post(event);
                    synchronized (eventsDone) {
                        eventsDone.add(event);
                        eventsDone.notifyAll();
                    }
                }
            }
        }

        private synchronized Object pollEvent() {
            Object event = null;
            synchronized (eventQ) {
                if (eventQ.isEmpty()) {
                    try {
                        eventQ.wait();
                    } catch (InterruptedException e) {
                    }
                } else {
                    event = eventQ.remove(0);
                }
            }
            return event;
        }

        void shutdown() {
            running = false;
            synchronized (eventQ) {
                eventQ.notifyAll();
            }
        }

        void post(Object event) {
            synchronized (eventQ) {
                eventQ.add(event);
                eventQ.notifyAll();
            }
            synchronized (eventsDone) {
                while (eventsDone.remove(event)) {
                    try {
                        eventsDone.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

    }

}
