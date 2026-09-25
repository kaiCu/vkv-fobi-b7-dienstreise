package de.lgln.dienstreise.web;

import de.lgln.dienstreise.trip.domain.TripNotFoundException;
import de.lgln.dienstreise.trip.domain.TripReadOnlyException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

/** Error pages without technical details; unexpected errors are logged with a correlation id. */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(TripNotFoundException.class)
  public ModelAndView tripNotFound() {
    ModelAndView mav = new ModelAndView("error/404", HttpStatus.NOT_FOUND);
    mav.addObject("notFoundKey", "trip.notFound");
    return mav;
  }

  /** Started trips cannot be changed: back to the list with an explanation (FR-023). */
  @ExceptionHandler(TripReadOnlyException.class)
  public String tripReadOnly(HttpServletRequest request) {
    RequestContextUtils.getOutputFlashMap(request).put("flashError", "trip.readOnly");
    return "redirect:/trips";
  }

  /** A malformed trip id in the URL is just another unknown trip. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ModelAndView badPathVariable() {
    return tripNotFound();
  }

  @ExceptionHandler(Exception.class)
  public ModelAndView unexpected(Exception e) throws Exception {
    if (e instanceof ErrorResponse errorResponse) {
      HttpStatusCode status = errorResponse.getStatusCode();
      if (status.value() == 404) {
        return new ModelAndView("error/404", HttpStatus.NOT_FOUND);
      }
      ModelAndView mav = new ModelAndView("error", status);
      mav.addObject("status", status.value());
      return mav;
    }
    String correlationId = UUID.randomUUID().toString();
    log.atError().addKeyValue("correlationId", correlationId).setCause(e).log("unexpected error");
    ModelAndView mav = new ModelAndView("error/500", HttpStatus.INTERNAL_SERVER_ERROR);
    mav.addObject("correlationId", correlationId);
    return mav;
  }
}
