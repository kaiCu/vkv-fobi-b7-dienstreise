package de.lgln.dienstreise.trip;

import de.lgln.dienstreise.trip.domain.BusinessTrip;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Every read of a single trip is scoped by owner (FR-013, FR-022). */
public interface TripRepository extends JpaRepository<BusinessTrip, UUID> {

  Optional<BusinessTrip> findByIdAndOwnerSubject(UUID id, String ownerSubject);

  @EntityGraph(attributePaths = "car")
  List<BusinessTrip> findByOwnerSubjectOrderByStartAtAscCreatedAtAscIdAsc(String ownerSubject);

  @Query(
      "select count(t) > 0 from BusinessTrip t"
          + " where t.car.id = :carId and t.startAt < :end and :start < t.endAt")
  boolean existsOverlap(
      @Param("carId") Short carId, @Param("start") Instant start, @Param("end") Instant end);

  @Query(
      "select count(t) > 0 from BusinessTrip t"
          + " where t.car.id = :carId and t.startAt < :end and :start < t.endAt"
          + " and t.id <> :excludeId")
  boolean existsOverlapExcluding(
      @Param("carId") Short carId,
      @Param("start") Instant start,
      @Param("end") Instant end,
      @Param("excludeId") UUID excludeId);

  @Modifying
  @Query("delete from BusinessTrip t where t.endAt < :cutoff")
  int deleteByEndAtBefore(@Param("cutoff") Instant cutoff);
}
