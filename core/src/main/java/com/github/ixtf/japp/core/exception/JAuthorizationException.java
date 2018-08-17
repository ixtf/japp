package com.github.ixtf.japp.core.exception;

import static com.github.ixtf.japp.core.Constant.ErrorCode.AUTHORIZATION;

public class JAuthorizationException extends JException {
    public JAuthorizationException() {
        super(AUTHORIZATION);
    }
}
