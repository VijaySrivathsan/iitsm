package com.iitsm.web;

import com.iitsm.exception.ItsmException;
import com.iitsm.exception.NotFoundException;
import com.iitsm.exception.UnauthorizedOperationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URI;

/**
 * Turns exceptions into friendly pages, in one place:
 * <ul>
 *   <li>not found -> 404 page; not allowed -> 403 page;</li>
 *   <li>any other business error (invalid transition, invalid input, technician unavailable, ...) ->
 *       back to the page the user came from, with the message shown as an error banner;</li>
 *   <li>database problems -> "service unavailable" page (details only in the log).</li>
 * </ul>
 * Polymorphism at work: one handler per exception <i>family</i>, thanks to the ItsmException hierarchy.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView notFound(NotFoundException e) {
        return new ModelAndView("error/404").addObject("message", e.getMessage());
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView forbidden(UnauthorizedOperationException e) {
        return new ModelAndView("error/403").addObject("message", e.getMessage());
    }

    @ExceptionHandler(ItsmException.class)
    public String businessError(ItsmException e, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("error", e.getMessage());
        return "redirect:" + safeReferer(request);
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ModelAndView databaseError(DataAccessException e) {
        log.error("Database error", e);
        return new ModelAndView("error/general")
                .addObject("message", "The database is not available right now. Please try again shortly.");
    }

    /** Only redirect back within this application (never to another site). */
    private static String safeReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null) {
            return "/dashboard";
        }
        try {
            URI uri = URI.create(referer);
            if (request.getServerName().equalsIgnoreCase(uri.getHost()) && uri.getRawPath() != null
                    && uri.getRawPath().startsWith("/")) {
                return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
            }
        } catch (IllegalArgumentException ignored) {
            // malformed header: fall through
        }
        return "/dashboard";
    }
}
