package com.github.ixtf.exception;

import com.github.ixtf.Constant;

public class JAuthorizationError extends JError {
    public JAuthorizationError() {
        super(Constant.ErrorCode.AUTHORIZATION);
    }
}
