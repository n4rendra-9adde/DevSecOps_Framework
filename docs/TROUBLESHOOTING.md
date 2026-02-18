# Troubleshooting Guide

## P1: Language Not Detected

**Symptom:** `LANGUAGE=unknown` in Initialize stage

**Diagnosis:**
```bash
cd $WORKSPACE && ls -la pom.xml build.gradle package.json requirements.txt go.mod Cargo.toml
```

**Solutions:**

| Cause | Fix |
|-------|-----|
| No build file present | Add the appropriate build file (`pom.xml`, `package.json`, etc.) |
| Build file in subdirectory | Move to repo root, or use `language.override` in `.devsecops.yml` |
| Permission denied on `detect-language.sh` | Run `chmod +x detect-language.sh` |
| Windows line endings | Run `dos2unix detect-language.sh` |

---

## P2: Build Stage Fails

**Symptom:** `Build failed: <tool> not found`

**Diagnosis:** Check if the build tool is available on the Jenkins agent.
```bash
which mvn && mvn --version
which npm && npm --version
which python && python --version
which go && go version
which cargo && cargo --version
```

**Solutions:**
- **Docker agent:** Ensure the Docker image has required tools installed
- **Bare metal:** Install the tool: `apt-get install maven` / `npm install -g npm`
- **Custom command:** Set `build.custom_command` in `.devsecops.yml`

---

## P3: SonarQube "No files nor directories matching 'target/classes'"

**Symptom:** SonarQube scanner fails with classpath error

**Cause:** Build stage didn't compile, or compiled to a non-standard directory

**Solutions:**
1. Ensure Build stage completes before SAST
2. Verify `sonar.java.binaries` is set correctly:
   ```yaml
   security:
     sonarqube:
       exclusions: "**/generated/**"
   ```
3. Skip SonarQube if not applicable:
   ```yaml
   security:
     skip_scans: ["sonarqube"]
   ```

---

## P4: Trivy "permission denied" on Docker Socket

**Symptom:** `Cannot connect to the Docker daemon`

**Solutions:**
```bash
# Add Jenkins user to docker group
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins

# OR mount socket with correct permissions in Jenkinsfile agent
# args '-v /var/run/docker.sock:/var/run/docker.sock -u root'
```

---

## P5: Approval Gate Never Appears

**Symptom:** Pipeline hangs at Approval stage, no button visible

**Diagnosis:**
- The `input` step only triggers on `branch 'main'` — are you on main?
- Check Jenkins job page (not Console Output) for "Paused for input"

**Solutions:**
1. Navigate to **Build #N → Paused for Input** in Jenkins UI
2. Check the Pipeline Stage View plugin for a gray/yellow stage
3. Verify your Jenkins user is in the `submitter` list (default: `admin, release-manager`)

---

## P6: `.devsecops.yml` Changes Not Applied

**Symptom:** Old behavior persists after config change

**Diagnosis:**
- Check console log for `✅ Loaded .devsecops.yml`
- If it says `ℹ️ No .devsecops.yml found`, the file wasn't committed

**Solutions:**
1. Verify file is committed: `git show HEAD:.devsecops.yml`
2. Check YAML syntax: `python -c "import yaml; yaml.safe_load(open('.devsecops.yml'))"`
3. Use Jenkins **Replay** feature to test changes without committing

---

## P7: Slack Notifications Not Sending

**Symptom:** No Slack messages received during pipeline events

**Diagnosis:**
- Check console for `⚠️ Slack webhook failed` or `ℹ️ Slack not configured`

**Solutions:**

| Cause | Fix |
|-------|-----|
| No webhook configured | Set `SLACK_WEBHOOK` env var in Jenkins or add to `.devsecops.yml` |
| Plugin not installed | Install "Slack Notification" Jenkins plugin |
| Channel doesn't exist | Verify channel name in `.devsecops.yml` |
| Webhook expired | Regenerate in Slack workspace settings |

---

## P8: Trend Analysis Shows "No previous build data"

**Symptom:** Report trend section says "No previous build data available"

**Cause:** This is normal on the first build. Metrics are stored as artifacts and compared across builds.

**Solutions:**
- Wait for a second build — trend will appear on build #2+
- Ensure `archiveArtifacts` and `copyArtifacts` plugins are installed
- Check that `reports/metrics.json` is being archived

---

## P9: Security Gate Fails Unexpectedly

**Symptom:** Build marked UNSTABLE when you expect it to pass

**Diagnosis:**
```bash
# Check what threshold is active
grep "Threshold" console-log.txt

# Check findings
cat reports/semgrep-*.json | python -m json.tool | grep severity
cat reports/gitleaks.json | python -m json.tool | head
```

**Solutions:**
1. Raise the threshold: `security.fail_on: "CRITICAL"` in `.devsecops.yml`
2. Use `NEVER` to disable the security gate entirely (not recommended)
3. Check for false positives in Semgrep output — adjust rulesets via `semgrep.config`

---

## P10: Pipeline Times Out

**Symptom:** Build aborted after 60 minutes

**Solutions:**
1. Increase pipeline timeout in Jenkinsfile: `timeout(time: 120, unit: 'MINUTES')`
2. Increase build timeout in `.devsecops.yml`:
   ```yaml
   build:
     timeout_minutes: 90
   ```
3. Check if a scan tool is hanging — use `performance.container_build_timeout` for container scans
