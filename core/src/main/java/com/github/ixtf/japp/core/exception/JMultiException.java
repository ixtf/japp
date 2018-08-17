package com.github.ixtf.japp.core.exception;

import com.github.ixtf.japp.core.Constant;
import lombok.Getter;

import java.util.Collection;

public class JMultiException extends JException {
    @Getter
    private final Collection<JException> exceptions;

    public JMultiException(Collection<JException> exceptions) {
        super(Constant.ErrorCode.MULTI);
        this.exceptions = exceptions;
    }

}
