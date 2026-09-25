package de.lgln.dienstreise.trip.web;

import de.lgln.dienstreise.trip.AvailabilityService;
import de.lgln.dienstreise.trip.CarAvailability;
import de.lgln.dienstreise.trip.TripCommand;
import de.lgln.dienstreise.trip.TripService;
import de.lgln.dienstreise.trip.domain.CarAlreadyBookedException;
import de.lgln.dienstreise.trip.domain.TripPeriod;
import de.lgln.dienstreise.trip.domain.TripStaleException;
import de.lgln.dienstreise.trip.domain.TripValidationException;
import de.lgln.dienstreise.web.CurrentUser;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Three-step wizard: period, car, destination (contracts/ui-routes.md). Every step carries all
 * values entered so far and re-validates them, so "Zurück" never loses input.
 */
@Controller
public class TripWizardController {

  static final Set<String> PERIOD_FIELDS = Set.of("startDate", "startTime", "endDate", "endTime");

  /** Create or edit; edit URLs contain the trip id. */
  record Wizard(String mode, UUID tripId) {
    static final Wizard CREATE = new Wizard("create", null);

    String stepBase() {
      return tripId == null ? "/trips/new" : "/trips/" + tripId + "/edit";
    }

    String saveAction() {
      return tripId == null ? "/trips" : "/trips/" + tripId;
    }
  }

  private final TripService tripService;
  private final AvailabilityService availability;
  private final TripFormValidator validator;

  public TripWizardController(
      TripService tripService, AvailabilityService availability, TripFormValidator validator) {
    this.tripService = tripService;
    this.availability = availability;
    this.validator = validator;
  }

  @InitBinder("tripForm")
  void allowedFields(WebDataBinder binder) {
    binder.setAllowedFields(
        "startDate",
        "startTime",
        "endDate",
        "endTime",
        "carId",
        "destination",
        "version",
        "action");
  }

  @GetMapping("/trips/new")
  String newTrip(@ModelAttribute("tripForm") TripForm form, BindingResult errors, Model model) {
    return showPeriod(Wizard.CREATE, errors, model);
  }

  @PostMapping("/trips/new/period")
  String period(@ModelAttribute("tripForm") TripForm form, BindingResult errors, Model model) {
    return handlePeriod(Wizard.CREATE, form, errors, model);
  }

  @PostMapping("/trips/new/car")
  String car(@ModelAttribute("tripForm") TripForm form, BindingResult errors, Model model) {
    return handleCar(Wizard.CREATE, form, errors, model);
  }

  @PostMapping("/trips")
  String save(
      CurrentUser user,
      @ModelAttribute("tripForm") TripForm form,
      BindingResult errors,
      Model model,
      RedirectAttributes redirect) {
    if (form.isBack()) {
      return showCar(Wizard.CREATE, form, errors, model);
    }
    String invalid = validateAll(Wizard.CREATE, form, errors, model);
    if (invalid != null) {
      return invalid;
    }
    try {
      tripService.create(user, command(form));
    } catch (TripValidationException | CarAlreadyBookedException | DataAccessException e) {
      return saveFailed(Wizard.CREATE, e, form, errors, model);
    }
    redirect.addFlashAttribute("flashSuccess", "flash.created");
    return "redirect:/trips";
  }

  // --- edit (US3): same steps, the trip itself is excluded from the availability check ---

  @GetMapping("/trips/{id}/edit")
  String edit(CurrentUser user, @PathVariable UUID id, Model model) {
    TripForm form = tripService.loadForEdit(user, id);
    model.addAttribute("tripForm", form);
    BindingResult errors = new BeanPropertyBindingResult(form, "tripForm");
    model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "tripForm", errors);
    return showPeriod(editing(id), errors, model);
  }

  @PostMapping("/trips/{id}/edit/period")
  String editPeriod(
      CurrentUser user,
      @PathVariable UUID id,
      @ModelAttribute("tripForm") TripForm form,
      BindingResult errors,
      Model model) {
    tripService.checkEditable(user, id);
    return handlePeriod(editing(id), form, errors, model);
  }

  @PostMapping("/trips/{id}/edit/car")
  String editCar(
      CurrentUser user,
      @PathVariable UUID id,
      @ModelAttribute("tripForm") TripForm form,
      BindingResult errors,
      Model model) {
    tripService.checkEditable(user, id);
    return handleCar(editing(id), form, errors, model);
  }

  @PostMapping("/trips/{id}")
  String update(
      CurrentUser user,
      @PathVariable UUID id,
      @ModelAttribute("tripForm") TripForm form,
      BindingResult errors,
      Model model,
      RedirectAttributes redirect) {
    tripService.checkEditable(user, id);
    Wizard wizard = editing(id);
    if (form.isBack()) {
      return showCar(wizard, form, errors, model);
    }
    String invalid = validateAll(wizard, form, errors, model);
    if (invalid != null) {
      return invalid;
    }
    try {
      if (form.getVersion() == null) {
        throw new TripStaleException();
      }
      tripService.update(user, id, form.getVersion(), command(form));
    } catch (TripStaleException e) {
      model.addAttribute("globalError", "trip.staleEdit");
      return showDestination(wizard, form, errors, model);
    } catch (TripValidationException | CarAlreadyBookedException | DataAccessException e) {
      return saveFailed(wizard, e, form, errors, model);
    }
    redirect.addFlashAttribute("flashSuccess", "flash.updated");
    return "redirect:/trips";
  }

  private static Wizard editing(UUID id) {
    return new Wizard("edit", id);
  }

  // --- step handling shared by create and edit ---

  String handlePeriod(Wizard wizard, TripForm form, BindingResult errors, Model model) {
    validator.validatePeriod(form, errors);
    if (hasPeriodErrors(errors)) {
      return showPeriod(wizard, errors, model);
    }
    Short chosen = form.getCarId();
    if (chosen != null && !availability.isAvailable(chosen, form.toPeriod(), wizard.tripId())) {
      form.setCarId(null);
      errors.rejectValue("carId", "trip.car.noLongerAvailable");
    }
    return showCar(wizard, form, errors, model);
  }

  String handleCar(Wizard wizard, TripForm form, BindingResult errors, Model model) {
    if (form.isBack()) {
      return showPeriod(wizard, errors, model);
    }
    validator.validatePeriod(form, errors);
    if (hasPeriodErrors(errors)) {
      return showPeriod(wizard, errors, model);
    }
    validator.validateCar(form, errors);
    if (!errors.hasFieldErrors("carId")
        && !availability.isAvailable(form.getCarId(), form.toPeriod(), wizard.tripId())) {
      form.setCarId(null);
      errors.rejectValue("carId", "trip.car.booked");
    }
    if (errors.hasFieldErrors("carId")) {
      return showCar(wizard, form, errors, model);
    }
    return showDestination(wizard, form, errors, model);
  }

  /** Validates all steps; returns the view of the first failing step, or null. */
  String validateAll(Wizard wizard, TripForm form, BindingResult errors, Model model) {
    validator.validatePeriod(form, errors);
    if (hasPeriodErrors(errors)) {
      return showPeriod(wizard, errors, model);
    }
    validator.validateCar(form, errors);
    if (errors.hasFieldErrors("carId")) {
      return showCar(wizard, form, errors, model);
    }
    validator.validateDestination(form, errors);
    if (errors.hasErrors()) {
      return showDestination(wizard, form, errors, model);
    }
    return null;
  }

  /** Maps a failed save back to the step the user has to correct. */
  String saveFailed(
      Wizard wizard, RuntimeException e, TripForm form, BindingResult errors, Model model) {
    return switch (e) {
      case TripValidationException v -> {
        v.violations()
            .forEach(violation -> errors.rejectValue(violation.field(), violation.code()));
        if (hasPeriodErrors(errors)) {
          yield showPeriod(wizard, errors, model);
        }
        yield errors.hasFieldErrors("carId")
            ? showCar(wizard, form, errors, model)
            : showDestination(wizard, form, errors, model);
      }
      case CarAlreadyBookedException booked -> {
        form.setCarId(null);
        errors.rejectValue("carId", "trip.car.booked");
        yield showCar(wizard, form, errors, model);
      }
      default -> {
        model.addAttribute("globalError", "trip.saveFailed");
        yield showDestination(wizard, form, errors, model);
      }
    };
  }

  static TripCommand command(TripForm form) {
    return new TripCommand(form.getCarId(), form.toPeriod(), form.getDestination());
  }

  // --- views ---

  String showPeriod(Wizard wizard, BindingResult errors, Model model) {
    model.addAttribute("formAction", wizard.stepBase() + "/period");
    return view("trips/period", wizard, errors, model);
  }

  String showCar(Wizard wizard, TripForm form, BindingResult errors, Model model) {
    TripPeriod period = form.toPeriod();
    List<CarAvailability> cars = availability.availabilityFor(period, wizard.tripId());
    model.addAttribute("availability", cars);
    model.addAttribute("noneAvailable", AvailabilityService.noneAvailable(cars));
    model.addAttribute("period", period);
    model.addAttribute("formAction", wizard.stepBase() + "/car");
    return view("trips/car", wizard, errors, model);
  }

  String showDestination(Wizard wizard, TripForm form, BindingResult errors, Model model) {
    model.addAttribute("period", form.toPeriod());
    model.addAttribute("carLabel", availability.carLabel(form.getCarId()).orElse(null));
    model.addAttribute("formAction", wizard.saveAction());
    return view("trips/destination", wizard, errors, model);
  }

  private static String view(String name, Wizard wizard, BindingResult errors, Model model) {
    model.addAttribute("mode", wizard.mode());
    model.addAttribute("hasErrors", errors.hasErrors() || model.containsAttribute("globalError"));
    return name;
  }

  private static boolean hasPeriodErrors(BindingResult errors) {
    return PERIOD_FIELDS.stream().anyMatch(errors::hasFieldErrors);
  }
}
