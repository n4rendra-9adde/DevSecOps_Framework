# ADR-001: Language Detection via Build File Presence

**Status:** Accepted  
**Date:** February 2025

## Context
We need to auto-detect the project language without requiring explicit configuration from the developer.

## Options Considered
1. **File extension analysis** — scan for `.java`, `.js`, `.py` files
2. **Shebang parsing** — read `#!/usr/bin/env python` from scripts
3. **Build file presence** — check for `pom.xml`, `package.json`, `go.mod`, etc.

## Decision
**Option 3: Build file presence.**

## Rationale
- Build files are always present in valid, buildable projects
- They map directly to the build tool (Maven, npm, pip, etc.) — no second lookup needed
- Easy to extend — adding a new language is a single `elif` check
- Deterministic — no heuristics or confidence scoring required

## Consequences
- Projects **must** have a recognizable build file in the repo root
- Monorepos require `.devsecops.yml` language override
- Shell-only or config-only repos will get `LANGUAGE=unknown`

---

# ADR-002: Groovy Scripts vs Jenkins Shared Library

**Status:** Accepted  
**Date:** February 2025

## Context
Jenkins pipelines can load reusable logic via **Shared Libraries** (Git-based, centrally managed) or **inline Groovy scripts** loaded with the `load` step.

## Decision
**Inline Groovy scripts loaded from the repository.**

## Rationale
- **Zero infrastructure** — no separate Shared Library repo to manage
- **Versioned with the project** — changes to adapters travel with the pipeline
- **Easily portable** — copy the repo and it works on any Jenkins instance
- **Lower adoption barrier** — no admin action needed to configure Global Libraries

## Trade-offs
- Shared Libraries provide better namespacing and class imports
- For very large organizations, centralized libraries reduce duplication
- If adoption grows, migrating to a Shared Library is straightforward

---

# ADR-003: Artifact-Based Trend Storage vs External Database

**Status:** Accepted  
**Date:** February 2025

## Context
Trend analysis needs historical metrics from previous builds. Storage options include Jenkins build artifacts, S3, or an external database (PostgreSQL, Elasticsearch).

## Decision
**Store metrics as Jenkins build artifacts** using `archiveArtifacts` and retrieve via `copyArtifacts`.

## Rationale
- **Zero external dependencies** — works on any Jenkins installation
- **Lifecycle aligned** — metrics live and die with the build they describe
- **Free retention** — Jenkins already manages artifact retention policies
- **Simple** — `writeJSON` + `readJSON` pipeline steps, no credentials or drivers

## Trade-offs
- Limited to comparison with last successful build (not full history)
- No queryable dashboard (for that, push to Elasticsearch/Grafana later)
- Artifacts are lost if builds are deleted

---

# ADR-004: Parallel Security Scans

**Status:** Accepted  
**Date:** February 2025

## Context
Running 3+ security scans (Secret, SAST, SCA) sequentially adds significant pipeline time. We need to decide between sequential safety and parallel speed.

## Decision
**Run Secret Scan, SAST, and SCA in parallel.** Container scan runs sequentially after (it depends on a built image).

## Rationale
- Scans are independent — they read the same source but don't write to shared state
- Typical speedup: 3× on the security phase (from ~6 min to ~2 min)
- Jenkins `parallel` block handles resource allocation

## Consequences
- Console output interleaves — use Stage View plugin for clarity
- Agent needs enough CPU/RAM for 3 concurrent scans
- Container scan remains sequential because it requires `docker build` output

---

# ADR-005: Non-Blocking SonarQube Quality Gate

**Status:** Accepted  
**Date:** February 2025

## Context
SonarQube can block the pipeline until quality gate analysis completes (`sonar.qualitygate.wait=true`). Depending on server load, this can take 30 seconds to 10+ minutes.

## Decision
**Submit analysis results but do not wait for quality gate.**

## Rationale
- Avoids pipeline hangs when SonarQube is slow or unreachable
- Developers can still check quality gate in the SonarQube dashboard
- Pipeline speed is prioritized; security gate uses Semgrep results instead
- Can be overridden per-project via `.devsecops.yml`:
  ```yaml
  security:
    sonarqube:
      quality_gate_wait: true
  ```

## Consequences
- Quality gate violations won't automatically fail the pipeline
- Teams relying on SonarQube must check the dashboard separately (or enable waiting)
