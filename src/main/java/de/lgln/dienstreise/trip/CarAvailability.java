package de.lgln.dienstreise.trip;

/** Whether a car is free in the requested period; nothing about other trips (FR-019). */
public record CarAvailability(Short carId, String label, boolean available) {}
