# Barrierefreiheit: Prüfprotokoll (BITV 2.0 / WCAG 2.1 AA)

Ergänzt die automatischen axe-Prüfungen der E2E-Tests (`./mvnw verify -Pe2e`) um die
manuelle Prüfung für SC-006. Automatische Tests finden nur einen Teil der Barrieren.

**Umgebung**: dev-Umgebung (`compose.dev.yaml`), Testdaten aus `dev/seed-past-trip.sql`.

## Geprüfte Seiten

| # | Seite | URL |
|---|-------|-----|
| S1 | Liste mit Einträgen | `/trips` (anna mit Seed-Daten) |
| S2 | Leere Liste | `/trips` (ben) |
| S3 | Assistent Schritt 1 (auch Fehlerzustand) | `/trips/new` |
| S4 | Assistent Schritt 2 (mit „nicht verfügbar“) | nach Schritt 1 |
| S5 | Assistent Schritt 3 | nach Schritt 2 |
| S6 | Löschbestätigung | `/trips/{id}/delete` |

## Prüfschritte

| # | Prüfung | Kriterium | Vorgehen |
|---|---------|-----------|----------|
| P1 | Nur Tastatur | 2.1.1, 2.4.3, 2.4.7 | Alle Aufgaben ohne Maus; Reihenfolge logisch, Fokus immer sichtbar, keine Tastaturfalle, Skip-Link funktioniert |
| P2 | Screenreader | 1.3.1, 4.1.2, 3.3.1 | NVDA + Firefox: Überschriften, Landmarken, Beschriftungen, Pflichtfelder, Fehlerzusammenfassung und Feldfehler werden angesagt |
| P3 | „nicht verfügbar“ | 1.3.1, 1.4.1 | Nicht verfügbare Fahrzeuge werden als deaktiviert **und** mit Text „nicht verfügbar“ angesagt |
| P4 | Zoom 200 % | 1.4.4 | Keine abgeschnittenen Inhalte oder Überlagerungen |
| P5 | Zoom 400 % / Reflow 320 px | 1.4.10 | Kein horizontales Scrollen (außer Tabelle im scrollbaren Container) |
| P6 | Kontraste | 1.4.3, 1.4.11 | Text ≥ 4,5:1, Bedienelemente und Fokusrahmen ≥ 3:1 (z. B. Colour Contrast Analyser) |
| P7 | Statusmeldungen | 4.1.3 | Erfolgsmeldung wird ohne Fokuswechsel angesagt (`role=status`) |
| P8 | Seitentitel und Sprache | 2.4.2, 3.1.1 | Titel beschreibt Seite und Schritt, bei Fehlern mit „Fehler:“; `lang="de"` |

## Ergebnisse

| Seite | P1 | P2 | P3 | P4 | P5 | P6 | P7 | P8 | Befunde |
|-------|----|----|----|----|----|----|----|----|---------|
| S1 | | | – | | | | | | |
| S2 | | | – | | | | – | | |
| S3 | | | – | | | | – | | |
| S4 | | | | | | | – | | |
| S5 | | | – | | | | – | | |
| S6 | | | – | | | | – | | |

Legende: ✓ erfüllt, ✗ nicht erfüllt (Befund eintragen), – nicht anwendbar.

**Prüfer/in, Datum**: 

## Bereits behobene Befunde aus der Implementierung

- Abmelde-Button in der Kopfzeile hatte 4,44:1 Kontrast auf `bg-body-tertiary` (axe
  `color-contrast`); auf `btn-outline-dark` umgestellt.
