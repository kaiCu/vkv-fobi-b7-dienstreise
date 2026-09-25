package de.lgln.dienstreise.trip;

import de.lgln.dienstreise.trip.domain.TripPeriod;

/** Validated input for creating or changing a trip. */
public record TripCommand(Short carId, TripPeriod period, String destination) {}
