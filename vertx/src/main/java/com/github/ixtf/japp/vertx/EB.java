package com.github.ixtf.japp.vertx;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.Message;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jzb 2018-08-30
 */
public interface EB {

    static SingleObserver<String> toObserver(Message<JsonObject> reply) {
        AtomicBoolean completed = new AtomicBoolean();
        return new SingleObserver<String>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onSuccess(@NonNull String item) {
                if (completed.compareAndSet(false, true)) {
                    reply.reply(item);
                }
            }

            @Override
            public void onError(Throwable error) {
                if (completed.compareAndSet(false, true)) {
                    reply.fail(-1, error.getMessage());
                }
            }
        };
    }

    static CompletableObserver toCompletableObserver(Message<JsonObject> reply) {
        AtomicBoolean completed = new AtomicBoolean();
        return new CompletableObserver() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onComplete() {
                if (completed.compareAndSet(false, true)) {
                    reply.reply(null);
                }
            }

            @Override
            public void onError(Throwable error) {
                LoggerFactory.getLogger(this.getClass()).error("", error);
                if (completed.compareAndSet(false, true)) {
                    reply.fail(-1, error.getMessage());
                }
            }
        };
    }

    static void noAction(Message reply) {
        reply.fail(-1, "");
    }

    interface Header {
        String action = "action";
    }

    interface Body {
        String principal = "principal";
    }


}
