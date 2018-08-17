package com.github.ixtf.japp.ee.filter;

import javax.servlet.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by jzb on 15-12-5.
 */
public class CharacterEncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        chain.doFilter(req, resp);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
