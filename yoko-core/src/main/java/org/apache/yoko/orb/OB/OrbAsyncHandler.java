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

import static java.lang.System.currentTimeMillis;
import static org.apache.yoko.orb.OB.OrbAsyncHandler.State.OAH_STATE_ACTIVE;
import static org.apache.yoko.orb.OB.OrbAsyncHandler.State.OAH_STATE_DORMANT;
import static org.apache.yoko.orb.OB.OrbAsyncHandler.State.OAH_STATE_SHUTDOWN;

import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.yoko.orb.OBMessaging.Poller_impl;
import org.apache.yoko.orb.OBMessaging.ReplyHandler_impl;
import org.apache.yoko.util.Assert;
import org.omg.CORBA.REBIND;
import org.omg.Messaging.NO_RECONNECT;
import org.omg.Messaging.ReplyHandler;
import org.omg.TimeBase.UtcT;

public class OrbAsyncHandler {
    //
    // A message registered to be sent/received asynchronously
    // 
    class AsyncMessage {
        //
        // the target object
        // 
        public org.omg.CORBA.Object object;

        //
        // The poller used to retrieve the event response
        // 
        public Poller_impl poller;

        //
        // The downcall sent/received
        // 
        public Downcall downcall;

        //
        // Servant onto which to invoke a response when received
        // (poller || reply) == 0
        // 
        public ReplyHandler reply;
    }

    //
    // Worker thread class
    //
    class OAH_Worker extends Thread {
        //
        // parent handler
        // 
        protected OrbAsyncHandler handler_ = null;

        //
        // has this worker been shutdown
        // 
        protected boolean shutdown_ = false;

        //
        // Constructor
        //
        OAH_Worker() {
            super("Yoko:Client:OrbAsyncHandler:OAH_Worker");
        }

        //
        // set the parent handler
        //
        public void handler(OrbAsyncHandler parent) {
            Assert.ensure(parent != null);
            handler_ = parent;
        }

        //
        // get the parent handler
        //
        public OrbAsyncHandler handler() {
            return handler_;
        }

        //
        // thread run process
        //
        public void run() {
            while (true) {
                AsyncMessage msg = null;

                synchronized (handler_.sendMonitor_) {
                    //
                    // check for a shutdown first
                    //
                    synchronized (this) {
                        if (shutdown_ == true)
                            break;
                    }

                    //
                    // if there are no messages to send then we want to
                    // wait until there is
                    //
                    try {
                        if (handler_.uncompletedMsgList_.size() == 0) {
                            handler_.sendMonitor_.wait();
                        }
                    } catch (InterruptedException ex) {
                        //
                        // this is not really an issue
                        //
                    }

                    //
                    // check again if there is a message because the
                    // wait could have been stopped by an interruption
                    // or a shutdown call
                    //
                    if (handler_.uncompletedMsgList_.size() > 0) {
                        msg = (AsyncMessage) handler_.uncompletedMsgList_
                                .removeFirst();
                    }
                }

                //
                // send/receive the request if there is one
                //
                if (msg != null) {

                    //
                    // Check to see if this is a new, unsent message or if
                    // this is a message that was delayed by a replyStart/
                    // replyEnd time policy
                    //
                    if (msg.downcall.unsent()) {
                        //
                        // if we have a RequestStartTime policy set, put the
                        // message back into the uncompletedMsgList
                        //
                        UtcT requestStartTime = msg.downcall
                                .policies().requestStartTime;
                        if (TimeHelper.notEqual(requestStartTime, TimeHelper
                                .utcMin())
                                && TimeHelper.greaterThan(requestStartTime,
                                        TimeHelper.utcNow(0))) {
                            synchronized (handler_.sendMonitor_) {
                                handler_.uncompletedMsgList_.addLast(msg);
                                handler_.sendMonitor_.notifyAll();
                            }
                            continue;
                        }

                        //
                        // if we have a RequestEndTime policy set, then we
                        // should discard the message since it is no longer
                        // valid
                        //
                        org.omg.TimeBase.UtcT requestEndTime = msg.downcall
                                .policies().requestEndTime;
                        if (TimeHelper.notEqual(requestEndTime, TimeHelper
                                .utcMin())
                                && TimeHelper.lessThan(requestEndTime,
                                        TimeHelper.utcNow(0))) {
                            continue;
                        }

                        try {
                            msg.downcall.request();
                        } catch (LocationForward ex) {
                            //
                            // TODO: A REBIND can also be thrown if the policy
                            // has a value of NO_REBIND and returned IORs
                            // policy requirements are incompatible with
                            // effective policies currently in use.
                            //
                            if (msg.downcall.policies().rebindMode == NO_RECONNECT.value) {
                                msg.downcall
                                        .setSystemException(new REBIND());
                            }
                        } catch (FailureException ex) {
                            //
                            // handle failure exception
                            //
                            continue;
                        }
                    }

                    //
                    // check for a ReplyStartTime policy. If it has not
                    // come into effect yet, add this message to the
                    // delayedMsgList
                    //
                    UtcT replyStartTime = msg.downcall
                            .policies().replyStartTime;
                    if (TimeHelper
                            .notEqual(replyStartTime, TimeHelper.utcMin())
                            && TimeHelper.greaterThan(replyStartTime,
                                    TimeHelper.utcNow(0))) {
                        synchronized (handler_.sendMonitor_) {
                            handler_.uncompletedMsgList_.addLast(msg);
                            handler_.sendMonitor_.notifyAll();
                        }
                        continue;
                    }

                    //
                    // check to see if the ReplyEndTime policy prevents us
                    // from delivering the reply
                    //
                    UtcT replyEndTime = msg.downcall
                            .policies().replyEndTime;
                    if (TimeHelper.notEqual(replyEndTime, TimeHelper.utcMin())
                            && TimeHelper.lessThan(replyEndTime, TimeHelper
                                    .utcNow(0))) {
                        continue;
                    }

                    // 
                    // if there is a reply handler to invoke, do it now
                    //
                    if (msg.reply != null) {
                        ReplyHandler_impl reply = (ReplyHandler_impl) msg.reply;
                        reply._OB_invoke(msg.downcall);
                        continue;
                    }

                    //
                    // so there was no reply handler which means there
                    // MUST be a poller
                    //
                    Assert.ensure(msg.poller != null);

                    //
                    // check the poller for its reply handler
                    //
                    ReplyHandler msgReply = msg.poller
                            .associated_handler();
                    if (msgReply != null) {
                        ReplyHandler_impl reply = (ReplyHandler_impl) msgReply;
                        reply._OB_invoke(msg.downcall);
                        continue;
                    }

                    //
                    // there was no reply handler to handle the message
                    // so we can put it into the completed list now and
                    // notify any clients waiting on this list
                    //
                    synchronized (handler_.recvMonitor_) {
                        handler_.completedMsgList_.addLast(msg);
                        handler_.recvMonitor_.notifyAll();
                    }
                }
            }
        }

        //
        // called to stop this worker thread
        //
        public synchronized void shutdown() {
            shutdown_ = true;
        }
    }

    //
    // possible states for the handler
    //
    final class State {
        public static final int OAH_STATE_DORMANT = 0;

        public static final int OAH_STATE_ACTIVE = 1;

        public static final int OAH_STATE_SHUTDOWN = 2;
    }

    //
    // the group of unsent messages
    // 
    protected LinkedList uncompletedMsgList_ = null;

    //
    // the group of completed messages
    //
    protected LinkedList completedMsgList_ = null;

    //
    // the send monitor
    //
    protected Object sendMonitor_ = null;

    //
    // the receive monitor
    //
    protected Object recvMonitor_ = null;

    //
    // The worker threads
    //
    protected OAH_Worker[] worker_ = null;

    //
    // the number of worker threads
    //
    protected int numWorkers_;

    //
    // the current state of the handler
    //
    protected int state_ = OAH_STATE_DORMANT;

    //
    // constructor
    //
    OrbAsyncHandler(int worker_threads) {
        numWorkers_ = worker_threads;
        if (numWorkers_ <= 0)
            numWorkers_ = 1;
    }

    //
    // activate the handler
    //
    public synchronized void activate() {
        //
        // make sure we're not in the shutdown state
        // 
        Assert.ensure(state_ != OAH_STATE_SHUTDOWN);

        //
        // no need to activate more than once...
        //
        if (state_ == OAH_STATE_ACTIVE)
            return;

        //
        // now put this handler into the activated state
        //
        state_ = OAH_STATE_ACTIVE;

        //
        // create the necessary message lists and monitors
        //
        uncompletedMsgList_ = new LinkedList();
        completedMsgList_ = new LinkedList();
        sendMonitor_ = new Object();
        recvMonitor_ = new Object();

        //
        // create the worker thread now
        // 
        worker_ = new OAH_Worker[numWorkers_];
        for (int i = 0; i < numWorkers_; i++) {
            worker_[i] = new OAH_Worker();
            worker_[i].handler(this);
            worker_[i].start();
        }
    }

    //
    // shutdown the handler
    //
    public synchronized void shutdown() {
        //
        // no need to shutdown more than once
        //
        if (state_ == OAH_STATE_SHUTDOWN)
            return;

        //
        // if we're in the DORMANT state, then we haven't been
        // initialized yet so there is no need to perform a cleanup
        //
        if (state_ == OAH_STATE_DORMANT) {
            state_ = OAH_STATE_SHUTDOWN;
            return;
        }

        //
        // go into the shutdown state
        //
        state_ = OAH_STATE_SHUTDOWN;

        //
        // stop the worker from processing
        //
        for (int i = 0; i < numWorkers_; i++)
            worker_[i].shutdown();

        //
        // wake up the worker if its sleepingon the send monitor
        //
        synchronized (sendMonitor_) {
            sendMonitor_.notifyAll();
        }

        //
        // join the worker thread to be assured of its completion
        //
        for (int i = 0; i < numWorkers_; i++) {
            while (true) {
                try {
                    worker_[i].join();
                    break;
                } catch (InterruptedException ex) {
                    continue;
                }
            }
        }

        //
        // clear the contents of the message lists
        //
        uncompletedMsgList_.clear();
        completedMsgList_.clear();
    }

    //
    // add a polled request to the queued list
    //
    public void addMessage(Downcall down,
            Poller_impl poller) {
        Assert.ensure(down != null);
        Assert.ensure(poller != null);

        //
        // activate the handler if it isn't already
        //
        activate();

        //
        // create a new unsent message
        //
        AsyncMessage msg = new AsyncMessage();
        msg.object = null;
        msg.poller = poller;
        msg.downcall = down;
        msg.reply = null;

        //
        // add it to the internal unsent list and notify the worker
        // thread that there is a message that needs sending
        //
        synchronized (sendMonitor_) {
            uncompletedMsgList_.addLast(msg);
            sendMonitor_.notify();
        }
    }

    //
    // add a reply handled request to the queued list
    //
    public void addMessage(Downcall down, ReplyHandler reply) {
        Assert.ensure(down != null);
        Assert.ensure(reply != null);

        //
        // activate this handler if it isn't already
        //
        activate();

        //
        // create a new async message
        //
        AsyncMessage msg = new AsyncMessage();
        msg.object = null;
        msg.poller = null;
        msg.downcall = down;
        msg.reply = reply;

        //
        // add it to the internal unsent list and notify the worker
        // thread that there is a message that needs sending
        //
        synchronized (sendMonitor_) {
            uncompletedMsgList_.addLast(msg);
            sendMonitor_.notify();
        }
    }

    //
    // poll if a message has completed
    //
    public boolean is_ready(Poller_impl poller,
            int timeout) {
        Assert.ensure(poller != null);

        boolean waitInfinite = false;
        if (timeout == -1)
            waitInfinite = true;

        while (true) {
            //
            // get the start time
            //
            long start_time = System.currentTimeMillis();

            //
            // check the list to see if any messages match our poller
            //
            synchronized (recvMonitor_) {
                ListIterator i = completedMsgList_.listIterator(0);

                while (i.hasNext()) {
                    AsyncMessage msg = (AsyncMessage) i.next();
                    if (msg.poller == poller) {
                        //
                        // check for the ReplyEndTime policy. If it is in
                        // effect, we can no longer deliver the reply.
                        // Otherwise, indicate to the client that the reply
                        // is ready.
                        //
                        UtcT replyEndTime = msg.downcall
                                .policies().replyEndTime;
                        if (TimeHelper.notEqual(replyEndTime, TimeHelper
                                .utcMin())
                                && TimeHelper.lessThan(replyEndTime, TimeHelper
                                        .utcNow(0))) {
                            return false;
                        }
                        return true;
                    }
                }
            }

            //
            // if the timeout is 0 then we need to return immediately
            //
            if (timeout == 0)
                return false;

            //
            // otherwise, wait the specified time on the receive queue to
            // see if a message arrives that matches our poller
            //
            try {
                synchronized (recvMonitor_) {
                    if (waitInfinite == true)
                        recvMonitor_.wait();
                    else
                        recvMonitor_.wait(timeout);
                }
            } catch (InterruptedException ex) {
                //
                // an interruption isn't so bad... we can simply recheck
                // again for a completed, matching message and if its not
                // there, then resume sleeping
                //
            }

            //
            // recalculate the time to wait next time around
            //
            if (waitInfinite == false) {
                //
                // get the ending time
                //
                long end_time = currentTimeMillis();

                //
                // calculate the difference in milliseconds and subtract
                // from timeout
                //
                long diff_time = end_time - start_time;
                if (diff_time > timeout)
                    timeout = 0;
                else
                    timeout -= diff_time;
            }
        }
    }

    //
    // perform a wait until any response comes in
    // Used by PollableSets to block on responses
    //
    public void waitOnResponse(int timeout) {
        try {
            synchronized (recvMonitor_) {
                if (timeout == -1)
                    recvMonitor_.wait();
                else
                    recvMonitor_.wait(timeout);
            }
        } catch (InterruptedException ex) {
            //
            // Only the PollableSet uses this method and if this method
            // returns it will check for an appropriate message... if it
            // doesn't exist, then it will resume waiting again.
            // Therefore we don't need to perform a continuation here
            // just to fulfill the time
            // 
        }
    }

    //
    // get a response
    //
    public Downcall poll_response(
            Poller_impl poller) {
        Assert.ensure(poller != null);

        synchronized (recvMonitor_) {
            //
            // search the list for a matching message
            //
            ListIterator iter = completedMsgList_.listIterator(0);

            while (iter.hasNext()) {
                AsyncMessage msg = (AsyncMessage) iter.next();

                if (msg.poller == poller) {
                    //
                    // remove this item from the list
                    //
                    iter.remove();

                    //
                    // return the downcall
                    //
                    return msg.downcall;
                }
            }
        }

        //
        // is_ready should have been called first to verify that a
        // response was ready so it is an error to not have one
        //
        throw Assert.fail();
    }
}
