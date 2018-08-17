package com.github.ixtf.japp.core.exception;

import static com.github.ixtf.japp.core.Constant.ErrorCode.TOKEN;

public class JTokenException extends JException {
    public JTokenException() {
        super(TOKEN);
    }
}
