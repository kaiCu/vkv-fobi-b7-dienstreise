package de.lgln.dienstreise.trip.web;

import de.lgln.dienstreise.trip.TripService;
import de.lgln.dienstreise.web.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TripListController {

  private final TripService tripService;

  public TripListController(TripService tripService) {
    this.tripService = tripService;
  }

  @GetMapping("/")
  String root() {
    return "redirect:/trips";
  }

  @GetMapping("/trips")
  String list(CurrentUser user, Model model) {
    model.addAttribute("trips", tripService.listFor(user));
    return "trips/list";
  }
}
