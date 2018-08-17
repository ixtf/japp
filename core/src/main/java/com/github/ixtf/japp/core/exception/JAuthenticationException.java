package com.github.ixtf.japp.core.exception;

import static com.github.ixtf.japp.core.Constant.ErrorCode.AUTHENTICATION;

public class JAuthenticationException extends JException {
    public JAuthenticationException() {
        super(AUTHENTICATION);
    }
}
