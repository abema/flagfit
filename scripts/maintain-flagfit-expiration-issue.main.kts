@file:Repository("https://repo.maven.apache.org/maven2")
@file:DependsOn("org.json:json:20200518")
@file:DependsOn("org.kohsuke:github-api:1.315")

import org.json.JSONObject
import org.kohsuke.github.GHIssue
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GitHub
import java.io.File
import java.util.regex.Pattern

class FlagExpirationIssueMaintainer {

  fun maintain() {
    val githubToken = System.getenv("GITHUB_TOKEN")
    val repoName = System.getenv("GITHUB_REPOSITORY")
    val headSha = System.getenv("HEAD_SHA")
    val gitHub = GitHub.connectUsingOAuth(githubToken)
    val repo = gitHub.getRepository(repoName)
    val targetRuleIdList = listOf(FLAGFIT_DEADLINE_SOON, FLAGFIT_DEADLINE_EXPIRED)
    val file = File("./lint-results.sarif")
    val content = file.readText()
    val jsonData = JSONObject(content)
    val runs = jsonData
      .getJSONArray("runs")
    val results = runs.getJSONObject(0).getJSONArray("results")
    val label = "futureflag-expiration"
    val limitIssue = 50
    val existingIssues = repo.queryIssues()
      .label(label)
      .state(GHIssueState.OPEN)
      .pageSize(limitIssue)
      .list()
      .take(limitIssue)
      .toMutableList()
    if (existingIssues.size >= limitIssue) {
      throw IllegalStateException(
        "Found more than $limitIssue Issues with $label set, " +
          "please make sure it is less than ${limitIssue}!"
      )
    }

    for (i in 0 until results.length()) {
      val result = results.getJSONObject(i)
      val ruleId = result.getString("ruleId")
      if (ruleId in targetRuleIdList) {
        val message = result.getJSONObject("message").getString("markdown")

        val keyPatternRegex = "`key: (.*?)`"
        val authorPatternRegex = "`author: (.*?)`"
        val key = matchText(text = message, patternRegex = keyPatternRegex)
        val assignee = matchText(text = message, patternRegex = authorPatternRegex)

        val issueTitle = "$ruleId <$key>"
        val issueBody = "## Warning\n\n$message"
        val locations = result.getJSONArray("locations").getJSONObject(0)
        val physicalLocation = locations.getJSONObject("physicalLocation")
        val artifactLocation = physicalLocation.getJSONObject("artifactLocation")
        val baseUrl = repo.htmlUrl
        val uri = artifactLocation.getString("uri")
        val contextRegion = physicalLocation.getJSONObject("contextRegion")
        val artifactUri = "${baseUrl}/blob/${headSha}/${uri}" +
          "#L${contextRegion.getInt("startLine")}-L${
            contextRegion.getInt("endLine")
          }"

        val isExistingIssue = existingIssues.any { issue ->
          val body = issue.body
          key == matchText(text = body, patternRegex = keyPatternRegex)
        }

        if (!isExistingIssue) {
          val issue = repo.createIssue(issueTitle)
            .body(issueBody)
            .assignee(assignee)
            .label(label)
            .create()
          issue.comment(artifactUri)
          existingIssues.add(issue)
        } else {
          updateWarning(existingIssues, key, issueTitle, issueBody)
        }
      }
    }
  }

  private fun updateWarning(
    existingIssues: MutableList<GHIssue>,
    key: String,
    issueTitle: String,
    issueBody: String,
  ) {
    existingIssues.forEach { issue ->
      val titleSoon = "$FLAGFIT_DEADLINE_SOON <$key>"
      val titleExpired = "$FLAGFIT_DEADLINE_EXPIRED <$key>"
      if (issue.title == titleSoon
        && issueTitle == titleExpired) {
        issue.title = titleExpired
        issue.comment(issueBody)
      }
    }
  }

  private fun matchText(text: String, patternRegex: String): String {
    val pattern = Pattern.compile(patternRegex)
    val matcher = pattern.matcher(text)
    return if (matcher.find()) matcher.group(1) else ""
  }

  companion object {
    const val FLAGFIT_DEADLINE_SOON = "FlagfitDeadlineSoon"
    const val FLAGFIT_DEADLINE_EXPIRED = "FlagfitDeadlineExpired"
  }
}

FlagExpirationIssueMaintainer().maintain()
