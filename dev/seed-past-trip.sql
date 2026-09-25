-- Dev only seed data (never loaded by Flyway): one past and one ongoing trip for anna.
-- Load after the app has started once (so the car rows exist):
--   docker compose -f compose.dev.yaml exec -T postgres psql -U dienstreise dienstreise < dev/seed-past-trip.sql
INSERT INTO business_trip (id, owner_subject, car_id, start_at, end_at, destination, created_at, updated_at, version)
VALUES
  (gen_random_uuid(),
   'http://localhost:8180/realms/dienstreise|00000000-0000-0000-0000-00000000a001',
   1, now() - interval '3 days', now() - interval '3 days' + interval '4 hours',
   'Oldenburg', now(), now(), 0),
  (gen_random_uuid(),
   'http://localhost:8180/realms/dienstreise|00000000-0000-0000-0000-00000000a001',
   2, now() - interval '1 hour', now() + interval '2 hours',
   'Braunschweig', now(), now(), 0);
