<!--
Sync Impact Report
==================
Version change: (unversioned template) → 1.0.0
Bump rationale: Initial ratification; all placeholders replaced with concrete governance.

Modified principles:
- [PRINCIPLE_1_NAME] → I. Human-Centered Design
- [PRINCIPLE_2_NAME] → II. Test-Driven Development (NON-NEGOTIABLE)
- [PRINCIPLE_3_NAME] → III. Cloud Native
- [PRINCIPLE_4_NAME] → IV. Shift-Left Security
- [PRINCIPLE_5_NAME] → removed (user defined four principles)

Added sections:
- Quality & Compliance Standards (was [SECTION_2_NAME])
- Development Workflow & Quality Gates (was [SECTION_3_NAME])

Removed sections:
- Fifth principle slot (not required)

Templates (read constitution at runtime, not modified by this command):
- .specify/templates/plan-template.md — Constitution Check gates should map to Principles I–IV
- .specify/templates/spec-template.md — user scenarios align with Principle I
- .specify/templates/tasks-template.md — test-first task ordering aligns with Principle II

Follow-up TODOs: none
-->

# Dienstreise Constitution

## Core Principles

### I. Human-Centered Design

The system exists to serve the people who plan, request, approve, and settle business trips.
Every feature MUST start from a concrete human need, not from a technical capability.

- Every specification MUST describe its users, their goals, and at least one independently
  testable user scenario with acceptance criteria.
- User interfaces MUST be accessible and MUST meet WCAG 2.1 level AA (BITV 2.0 for public
  sector use in Germany).
- Language MUST be plain and understandable to non-technical users; error messages MUST
  state what went wrong and what the user can do next.
- Workflows MUST minimize manual effort: data already known to the system MUST NOT be
  requested from the user again.
- User-facing changes SHOULD be validated with real users or representative feedback before
  they are considered done.

**Rationale**: Software that is technically correct but hard to use fails its purpose.
Accessibility is a legal obligation for public sector software and benefits all users.

### II. Test-Driven Development (NON-NEGOTIABLE)

Production code MUST be written only to make a failing test pass.

- The Red-Green-Refactor cycle is mandatory: write a test, confirm it fails, implement the
  minimum code to pass it, then refactor with all tests green.
- Acceptance criteria from the specification MUST be expressed as automated tests before
  implementation of the corresponding feature begins.
- Bug fixes MUST begin with a test that reproduces the defect.
- The test suite MUST cover unit, integration (contracts between components and services),
  and end-to-end behavior for critical user journeys.
- Tests MUST be deterministic, fast enough to run on every commit, and MUST run in CI.
- Code MUST NOT be merged while any test is failing or skipped without a documented reason.

**Rationale**: Tests written first define behavior precisely, prevent regressions, and make
refactoring safe. They are the executable form of the specification.

### III. Cloud Native

The system MUST be designed to run on container-based cloud platforms and to exploit their
elasticity and resilience.

- Services MUST be packaged as OCI containers and deployed declaratively (e.g., Kubernetes
  manifests or Helm charts); infrastructure MUST be defined as code.
- Services MUST follow Twelve-Factor principles: configuration via environment, stateless
  processes, backing services as attached resources, disposability.
- Services MUST expose health (liveness/readiness) endpoints and MUST shut down gracefully.
- Observability is required: structured logs, metrics, and distributed tracing MUST be
  emitted using open standards (e.g., OpenTelemetry).
- Builds and deployments MUST be fully automated through CI/CD pipelines; manual changes to
  running environments are prohibited.
- Vendor-neutral, open standards SHOULD be preferred over proprietary platform lock-in;
  any lock-in MUST be justified in the implementation plan.

**Rationale**: Cloud native design enables reproducible deployments, horizontal scaling,
fast recovery, and operational transparency.

### IV. Shift-Left Security

Security MUST be addressed from the first specification onward, not bolted on before release.

- Every specification and plan MUST identify the data processed, its protection needs, and
  relevant threats; features handling personal data MUST address GDPR (DSGVO) requirements
  such as data minimization, purpose limitation, and deletion.
- Security requirements MUST be expressed as automated tests where feasible.
- CI pipelines MUST include static analysis (SAST), dependency and container image scanning
  (SCA), and secret detection; findings of severity high or critical MUST block the merge.
- Secrets MUST NOT be stored in source code, images, or version control; they MUST be
  injected at runtime from a secret store.
- Authentication and authorization MUST be enforced on every non-public endpoint following
  the principle of least privilege; all input MUST be validated at trust boundaries.
- Dependencies MUST be pinned and kept current; a software bill of materials (SBOM) MUST be
  produced for every release artifact.

**Rationale**: Defects found early are cheaper and safer to fix. Business travel data
includes personal and financial information that requires protection by design.

## Quality & Compliance Standards

- Accessibility: WCAG 2.1 AA / BITV 2.0 compliance is a release criterion for user-facing
  features.
- Data protection: GDPR (DSGVO) compliance is a release criterion for features processing
  personal data.
- Security: OWASP Top 10 and OWASP ASVS serve as the baseline for web application security.
- Operability: every deployable service MUST provide health endpoints, structured logging,
  and metrics before its first production deployment.

## Development Workflow & Quality Gates

- Work follows the Spec Kit flow: specify → clarify → plan → tasks → implement. The
  Constitution Check in each implementation plan MUST verify compliance with Principles I–IV.
- Every change MUST be submitted through a pull request and reviewed by at least one other
  person before merging to `main`.
- The CI pipeline MUST pass before merge: all tests green, security scans without blocking
  findings, container image built successfully.
- Reviewers MUST verify that tests were written for the change, that user impact is
  described, and that security implications are addressed.
- Deviations from any principle MUST be documented with justification in the plan's
  Complexity Tracking section and approved in review.

## Governance

This constitution supersedes all other development practices for this project. Where
guidance conflicts, the constitution takes precedence.

- **Amendments**: Changes MUST be proposed via pull request that modifies this file, states
  the rationale, and describes the impact on existing specifications, plans, and code.
  Amendments require review and approval by the project maintainers.
- **Versioning**: The constitution follows semantic versioning. MAJOR for removal or
  incompatible redefinition of principles; MINOR for new principles, sections, or materially
  expanded guidance; PATCH for clarifications and wording fixes.
- **Compliance review**: Every pull request review and every implementation plan MUST check
  compliance with this constitution. Compliance SHOULD additionally be reviewed at least once
  per release cycle.
- **Runtime guidance**: Agent- and tool-specific development guidance (e.g., `CLAUDE.md`)
  MUST be consistent with this constitution.

**Version**: 1.0.0 | **Ratified**: 2026-09-27 | **Last Amended**: 2026-09-27
