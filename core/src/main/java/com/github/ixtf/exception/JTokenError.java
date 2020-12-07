package com.github.ixtf.exception;

import static com.github.ixtf.Constant.ErrorCode.TOKEN;

public class JTokenError extends JError {
    public JTokenError() {
        super(TOKEN);
    }
}
