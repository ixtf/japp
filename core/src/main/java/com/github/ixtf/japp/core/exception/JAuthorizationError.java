package com.github.ixtf.japp.core.exception;

import static com.github.ixtf.japp.core.Constant.ErrorCode.AUTHORIZATION;

public class JAuthorizationError extends JError {
    public JAuthorizationError() {
        super(AUTHORIZATION);
    }
}
