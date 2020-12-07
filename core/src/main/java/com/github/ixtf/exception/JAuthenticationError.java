package com.github.ixtf.exception;

import com.github.ixtf.Constant;

public class JAuthenticationError extends JError {
    public JAuthenticationError() {
        super(Constant.ErrorCode.AUTHENTICATION);
    }
}
