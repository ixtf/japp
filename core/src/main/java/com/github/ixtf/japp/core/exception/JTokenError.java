package com.github.ixtf.japp.core.exception;

import static com.github.ixtf.japp.core.Constant.ErrorCode.TOKEN;

public class JTokenError extends JError {
    public JTokenError() {
        super(TOKEN);
    }
}
