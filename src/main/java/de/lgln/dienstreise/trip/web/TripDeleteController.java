package de.lgln.dienstreise.trip.web;

import de.lgln.dienstreise.trip.TripService;
import de.lgln.dienstreise.web.CurrentUser;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Deleting needs an explicit confirmation on its own page (FR-021, works without JS). */
@Controller
public class TripDeleteController {

  private final TripService tripService;

  public TripDeleteController(TripService tripService) {
    this.tripService = tripService;
  }

  @GetMapping("/trips/{id}/delete")
  String confirm(CurrentUser user, @PathVariable UUID id, Model model) {
    model.addAttribute("trip", tripService.loadForDelete(user, id));
    return "trips/delete-confirm";
  }

  @PostMapping("/trips/{id}/delete")
  String delete(CurrentUser user, @PathVariable UUID id, RedirectAttributes redirect) {
    tripService.delete(user, id);
    redirect.addFlashAttribute("flashSuccess", "flash.deleted");
    return "redirect:/trips";
  }
}
