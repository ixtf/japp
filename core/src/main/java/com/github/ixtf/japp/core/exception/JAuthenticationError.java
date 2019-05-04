package com.github.ixtf.japp.core.exception;

import static com.github.ixtf.japp.core.Constant.ErrorCode.AUTHENTICATION;

public class JAuthenticationError extends JError {
    public JAuthenticationError() {
        super(AUTHENTICATION);
    }
}
