package com.github.ixtf.japp.vertx;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.vertx.reactivex.core.eventbus.Message;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public interface EB {

    static <T> SingleObserver<T> toSingleObserver(Message reply) {
        AtomicBoolean completed = new AtomicBoolean();
        return new SingleObserver<T>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onSuccess(@NonNull T item) {
                if (completed.compareAndSet(false, true)) {
                    reply.reply(item);
                }
            }

            @Override
            public void onError(Throwable error) {
                if (completed.compareAndSet(false, true)) {
                    handleError(reply, error);
                }
            }
        };
    }

    static CompletableObserver toCompletableObserver(Message reply) {
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
                if (completed.compareAndSet(false, true)) {
                    handleError(reply, error);
                }
            }
        };
    }

    static void handleError(Message reply, Throwable error) {
        LoggerFactory.getLogger(reply.address()).error("", error);
        reply.fail(-1, error.getMessage());
    }

    static void noAction(Message reply) {
        reply.fail(-1, "action not exist");
    }

    interface Header {
        String action = "action";
    }

    interface Body {
        String principal = "principal";
    }
}
