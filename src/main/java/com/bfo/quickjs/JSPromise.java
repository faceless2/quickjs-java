package com.bfo.quickjs;

import java.util.function.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * A CompleteableFuture that mirrors a JS Promise.
 * When this future is completed in Java, the next call to context.poll() will complete the JS promise.
 * When the JS promise is completed, this Future is completed to match.
 */
public class JSPromise extends CompletableFuture<Object> implements JSType, AutoCloseable {

    private final JSContext ctx;
    private long pointer;
    private final int index;
    private JSPromise finalStage;
    private volatile boolean promiseCompleted; // has the JS promise that is equivalent to this been completed?

    JSPromise(JSContext ctx, long pointer, int index) {
        this.ctx = ctx;
        this.pointer = pointer;
        this.index = index;

        handle(new BiFunction<Object,Throwable,Object>() {
            public Object apply(final Object value, final Throwable ex) {
                if (!promiseCompleted) {
                    ctx.pollQueue(new Runnable() {
                        public void run() {
                            if (ex != null) {
                                ctx.getRuntime().getLogger().log(JSRuntime.Logger.DEBUG, "JS promise {} rejected", pointer, ex);
                                byte[] data = ctx.pack(ex);
                                ctx.getRuntime().fnPromiseResolve(JSPromise.this, data);
                            } else {
                                ctx.getRuntime().getLogger().log(JSRuntime.Logger.DEBUG, "JS promise {} resolved: {}", pointer, value);
                                byte[] data = ctx.pack(value);
                                ctx.getRuntime().fnPromiseResolve(JSPromise.this, data);
                            }
                            ctx.pollAfterPromise(finalStage);
                            finalStage = null;
                        }
                    });
                }
                return null;
            }
        });
    }

    @Override public long getPointer() {
        return pointer;
    }

    int getIndex() {
        return index;
    }

    @Override public JSContext getContext() {
        return ctx;
    }

    @Override public void close() throws Exception {
        if (pointer != 0) {
            ctx.getRuntime().fnPromiseClose(this);
            pointer = 0;  
        }
    }

    void notifyCompletedByJS() {
        promiseCompleted = true;
    }

    /**
     * When this promise resolves, a task may be queued on JS.
     * If that task fails, we need to fail the "final Stage", the promise returned from evalAsync.
     * For every JSPromise completed during a call to evalAsync, that promise is passed into this
     * method before evalAsync returns.
     */
    void setFinalStage(JSPromise finalStage) {
        this.finalStage = finalStage;
    }

    public String toString() {
        return "[JSPromise:" + ctx.getPointer()+"." + getIndex()+" @" + getPointer() + " state="+state()+"]";
    }

/**

A JSPromise is created either from within the JS engine, or by us as a proxy for a CompletableFuture.
A worked example, eg if we have a method, eg "CompleteableFuture<Object> loader() { ... }" called from JS.

evalAsync is called, which calls a script which (at some point) makes a callback to "loader()"
  when loader() is called it returns a CompletableFuture A.
  to return A to the JS engine we wrap it in a JSPRomise B, which depends on A. B is returned to JS.
  ... maybe other JSPromises are created from within the JS
  but eventually evalAsync returns, with a final JSPromise C.
  For all JSPromise objects created since evalAsync was entered, "setFinalPromise" is called on them
  with a value of "C" (so B.setFinalPromise(C) is called).
  evalAsync returns C

... time passes, and eventually A resolves, perhaps on another thread.
  the resolution of A immediately resolves B on that same thread
  the resolution of B calls context.pollQueue to enqueue a task that will notify JS that B is resolved.

context.poll() is called on the main thread.
  previously queued task is run and JS is notified the promise is completed.
  poll() is called.
    if poll() errors, it calls JSRuntime.fnHandleRejectedPromise(), which sets context.pendingRejection
  poll completes. Was context.pendingRejection set? If so, reject the final promise: C

Clear as mud!

*/

}
