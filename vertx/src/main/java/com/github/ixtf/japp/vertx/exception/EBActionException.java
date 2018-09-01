package com.github.ixtf.japp.vertx.exception;

import com.github.ixtf.japp.core.Constant;
import com.github.ixtf.japp.core.exception.JException;

public class EBActionException extends JException {
    public EBActionException() {
        super(Constant.ErrorCode.EB_ACTION);
    }
}
