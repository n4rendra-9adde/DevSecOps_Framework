// NotificationManager.groovy - Universal DevSecOps Pipeline Notification Manager
// Sends notifications for pipeline events via Slack and Email.

// ──────────────────────────────────────────────────────────────
// Slack Notifications
// ──────────────────────────────────────────────────────────────

/**
 * Sends a Slack message via webhook or slackSend plugin.
 * Falls back silently if neither is available.
 */
def sendSlack(String message, String color = '#36a64f', Map config = [:]) {
    def channel = config?.notifications?.slack?.channel ?: '#devops-alerts'

    // Try plugin-based slackSend first
    try {
        slackSend(channel: channel, color: color, message: message)
        return true
    } catch (Exception ignored) {}

    // Fallback: raw webhook via httpRequest
    def webhookUrl = env.SLACK_WEBHOOK ?: config?.notifications?.slack?.webhook_url
    if (webhookUrl) {
        try {
            def payload = groovy.json.JsonOutput.toJson([
                channel: channel,
                attachments: [[
                    color: color,
                    text: message,
                    footer: "Universal DevSecOps Framework",
                    ts: (System.currentTimeMillis() / 1000).toLong()
                ]]
            ])
            httpRequest(
                url: webhookUrl,
                httpMode: 'POST',
                contentType: 'APPLICATION_JSON',
                requestBody: payload,
                validResponseCodes: '200'
            )
            return true
        } catch (Exception e) {
            echo "⚠️ Slack webhook failed: ${e.getMessage()}"
        }
    }

    echo "ℹ️ Slack not configured — skipping notification"
    return false
}

// ──────────────────────────────────────────────────────────────
// Email Notifications
// ──────────────────────────────────────────────────────────────

/**
 * Sends an email notification via Jenkins emailext plugin.
 */
def sendEmail(String subject, String body, Map config = [:]) {
    def recipients = config?.notifications?.email?.recipients?.join(',') ?:
                     env.CHANGE_AUTHOR_EMAIL ?:
                     'devops-team@company.com'

    try {
        emailext(
            subject: subject,
            body: body,
            mimeType: 'text/html',
            to: recipients
        )
        return true
    } catch (Exception e) {
        echo "⚠️ Email send failed: ${e.getMessage()}"
        return false
    }
}

// ──────────────────────────────────────────────────────────────
// High-Level Event Methods
// ──────────────────────────────────────────────────────────────

/**
 * Notify that an approval is required.
 */
def notifyApprovalRequested(String stageName, String approvers, Map config = [:]) {
    def buildUrl = env.BUILD_URL ?: '(no URL)'
    def commit   = env.GIT_COMMIT?.take(7) ?: 'N/A'

    def slackMsg = """\
🚨 *Approval Required*
*Project:* ${env.JOB_NAME}  •  *Build:* #${env.BUILD_NUMBER}
*Stage:* ${stageName}  •  *Commit:* `${commit}`
*Approvers:* ${approvers}
<${buildUrl}input|👉 Click to Approve>"""

    sendSlack(slackMsg, '#f59e0b', config)

    def emailBody = """
<h2>🚨 Approval Required</h2>
<table>
  <tr><td><b>Project</b></td><td>${env.JOB_NAME}</td></tr>
  <tr><td><b>Build</b></td><td>#${env.BUILD_NUMBER}</td></tr>
  <tr><td><b>Stage</b></td><td>${stageName}</td></tr>
  <tr><td><b>Commit</b></td><td>${commit}</td></tr>
  <tr><td><b>Approvers</b></td><td>${approvers}</td></tr>
</table>
<p><a href="${buildUrl}input">Click here to approve</a></p>
"""
    if (config?.notifications?.email?.on_approval != false) {
        sendEmail("⏳ Approval Required: ${env.JOB_NAME} #${env.BUILD_NUMBER}", emailBody, config)
    }
}

/**
 * Notify final build status (SUCCESS / FAILURE / UNSTABLE).
 */
def notifyBuildStatus(String status, String summary, Map config = [:]) {
    def colorMap  = [SUCCESS: '#22c55e', FAILURE: '#ef4444', UNSTABLE: '#eab308']
    def emojiMap  = [SUCCESS: '✅', FAILURE: '❌', UNSTABLE: '⚠️']
    def color     = colorMap[status] ?: '#94a3b8'
    def emoji     = emojiMap[status] ?: 'ℹ️'

    def slackMsg = "${emoji} *${status}*: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n${summary}"

    // Respect per-status notification preferences
    def shouldNotify = true
    if (status == 'SUCCESS' && config?.notifications?.slack?.on_success == false) shouldNotify = false
    if (status == 'FAILURE' && config?.notifications?.slack?.on_failure == false) shouldNotify = false

    if (shouldNotify) {
        sendSlack(slackMsg, color, config)
    }

    // Email
    def shouldEmail = true
    if (status == 'SUCCESS' && config?.notifications?.email?.on_success == false) shouldEmail = false
    if (status == 'FAILURE' && config?.notifications?.email?.on_failure == false) shouldEmail = false

    if (shouldEmail) {
        sendEmail("${emoji} ${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER}", "<p>${summary}</p>", config)
    }
}

/**
 * Notify about security findings exceeding threshold.
 */
def notifySecurityAlert(String tool, int findingsCount, String severity, Map config = [:]) {
    if (findingsCount <= 0) return

    def msg = "🚨 *Security Alert*: ${tool} found *${findingsCount}* issues (severity >= ${severity}) in ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    sendSlack(msg, '#ef4444', config)
}

return this
