# Zappr

We use [zappr](https://github.com/zalando/zappr) to enforce commit message patterns, PR specification correctness, etc.


## Zappr Template for Code Repositories

```yaml
approvals:
  minimum: 2              # PR needs at least 2 approvals
  pattern: "^(:\\+1:|👍|approved)$"   # write a comment to the PR with "approved" or ":+1"
  veto:
    pattern: "^(:\\-1:|👎|rejected)$" # write a comment to the PR with "rejected" or ":-1"
  from:
    orgs:
      - zalando-incubator
      - zalando
    collaborators: true
commit:
  message:
    patterns:
      # follow commit guidelines CONTRIBUTING.md#git-commit-guidelines
      - "^(feat|fix|docs|style|refactor|perf|test|chore)(|\\([a-zA-Z0-9-._]+\\)):.{3,}"
```
