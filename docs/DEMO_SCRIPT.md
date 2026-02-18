# Demo Script: Universal DevSecOps Framework

**Duration:** 10 minutes  
**Audience:** Development leads, DevOps engineers, security team

---

## Setup (before demo)

1. Ensure Jenkins is running with all plugins installed
2. Have 3 jobs configured pointing to: `java-maven`, `nodejs-express`, `python-flask` fixtures
3. Have the DevSecOps report from a previous Java build available

---

## Part 1: The Problem (1 minute)

**Script:**
> "Today every project configures its own CI/CD pipeline from scratch.
> Java teams use Maven plugins, Node teams use npm scripts, Python teams use tox.
> Security scanning? Inconsistent at best.
>
> What if **one pipeline** auto-detected your language and applied the right
> security tools automatically? That's what we built."

**Show:** Slide or diagram of the 9-stage pipeline architecture.

---

## Part 2: Java Project — Zero Config (3 minutes)

**Script:**
> "Let's start with our Spring Boot API. I've added exactly one file:
> `Jenkinsfile-universal`. No other configuration."

**Actions:**
1. Show the project: `ls tests/fixtures/java-maven/`
2. Trigger the Jenkins build
3. Watch the Initialize stage: "See — it detected **Java / Maven** automatically"
4. Watch parallel security scans: "SAST, SCA, and Secret scan all running at once"
5. Show the Security Gate: "Threshold is HIGH — anything below that is a warning"
6. Show the HTML report: "One dashboard, dark-themed, shows everything at a glance"

**Key talking points:**
- `detect-language.sh` found `pom.xml` → Java
- Semgrep ran Java rules automatically
- OWASP Dependency-Check scanned the Maven dependencies
- All with zero project-specific configuration

---

## Part 3: Node.js Project — Different Tools, Same Pipeline (3 minutes)

**Script:**
> "Now watch the same pipeline with a Node.js project.
> Same `Jenkinsfile-universal`, completely different tools."

**Actions:**
1. Trigger the Node.js build
2. Highlight: "Language = nodejs, Build Tool = npm"
3. Point out: "ESLint and npm audit instead of SonarQube and OWASP"
4. Show the `.devsecops.yml`:
   ```yaml
   security:
     skip_scans: ["sonarqube"]
     fail_on: "CRITICAL"
   ```
5. Compare reports side-by-side with the Java build

**Key talking point:**
> "Same pipeline file. Different tools selected automatically.
> And notice — this project configured a CRITICAL-only threshold."

---

## Part 4: Configuration Power (2 minutes)

**Script:**
> "Every team can customize without touching pipeline code."

**Show `.devsecops.yml` features:**
1. Custom build command: `build.custom_command`
2. Skip tests: `build.skip_tests: true`
3. Slack notifications: `notifications.slack.channel`
4. Approval gate with custom approvers

**Script:**
> "Convention over configuration — it works with zero config,
> but every knob is available when you need it."

---

## Part 5: Trend Analysis (1 minute)

**Script:**
> "After the second build, something new appears in the report..."

**Show:**
1. Trend section: "↓ 2 resolved SAST findings vs Build #3"
2. Explain: "Metrics stored as artifacts, compared automatically"

**Script:**
> "No external database needed. Works out of the box."

---

## Wrap-Up (30 seconds)

**Script:**
> "To recap:
> - **5 languages** supported today, new ones in under 4 hours
> - **Zero config** for standard projects
> - **Parallel scans** cut security time by 3×
> - **Enterprise notifications** via Slack and Email
> - **1,776 lines** of production-ready Groovy and Bash
>
> Questions?"

---

## Anticipated Q&A

| Question | Answer |
|----------|--------|
| "Can we add PHP?" | Yes — 4 steps, ~30 lines of code, under 4 hours |
| "What about monorepos?" | Use `language.override` in `.devsecops.yml` per subdirectory |
| "Where do reports go?" | Archived as Jenkins artifacts + HTML Publisher plugin |
| "Can we fail on MEDIUM?" | Yes — set `security.fail_on: "MEDIUM"` in config |
| "Does it work on-prem?" | Yes — Docker-based agents, no cloud dependencies |
