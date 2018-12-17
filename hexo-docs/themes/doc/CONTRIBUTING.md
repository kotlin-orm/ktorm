# Contributing to hexo-theme-doc
**Thank you for your interest in making hexo-theme-doc even better and more awesome. Your contributions are highly welcome.**

There are multiple ways of getting involved:

- [Report a bug](#report-a-bug)
- [Suggest a feature](#suggest-a-feature)
- [Contribute code](#contribute-code)

Below are a few guidelines we would like you to follow.
If you need help, please reach out to us: team-stargate@zalando.de


## Report a bug
Reporting bugs is one of the best ways to contribute. Before creating a bug report, please check that an [issue](https://github.com/zalando-incubator/hexo-theme-doc/issues) reporting the same problem does not already exist. If there is an such an issue, you may add your information as a comment.

To report a new bug you should open an issue that summarizes the bug and set the label to "bug".

If you want to provide a fix along with your bug report: That is great! In this case please send us a pull request as described in section [Contribute Code](#contribute-code).

## Suggest a feature
To request a new feature you should open an [issue](https://github.com/zalando-incubator/hexo-theme-doc/issues) and summarize the desired functionality and its use case. Set the issue label to "feature".

## Contribute code
This is a rough outline of what the [workflow](#workflow) for code contributions looks like:
- Check the list of open [issues](https://github.com/zalando-incubator/hexo-theme-doc/issues). Either assign an existing issue to yourself, or create a new one that you would like work on and discuss your ideas and use cases.
- Fork the repository on GitHub
- Make commits of logical units.
- Write good commit messages [(see below)](#git-commit-guidelines).
- Submit a [pull request](#pull-request) to [zalando-incubator/hexo-theme-doc](https://github.com/zalando-incubator/hexo-theme-doc)
- Your pull request must receive a :thumbsup: from two [Maintainers](./MAINTAINERS)

Thanks for your contributions!

## Workflow

The workflow that we use to contribute is mostly based on [GitHub Flow](https://guides.github.com/introduction/flow/)

**master** is the latest stable version and should be used when opening feature branches.

> If a github issue is related to a branch we suggest to append the number at the start of the branch name.<br>
  example: 98-dropdown-refactor (github issue)

If a feature branch is outdated **always** rebase it against master instead of merging it.

## Pull request

**Always open a pull request** to merge into `master` branch.

Consider opening a PR as soon as a commit on the feature branch is available to ease the reviewing process,
you can add a `WIP:` prefix to communicate that the current PR is still a work in progress.

The pull request must be approved by at least 2 people by commenting the pull request with "approved" or ":+1:".

The approval and the validation of the commit messages is done by [zappr](https://github.com/zalando/zappr).


## Git Commit Guidelines

We have very precise rules over how our git commit messages can be formatted. This leads to **more
readable messages** that are easy to follow when looking through the **project history**. It is
important to note that we use the git commit messages to **generate** the [Changelog](./CHANGELOG.md) document.

> A detailed explanation of guidelines and conventions can be found in this
  [document](https://docs.google.com/document/d/1QrDFcIiPjSLDn3EL15IJygNPiHORgU1_OOAqWjiDU5Y/edit#).

> We use [zappr](./zappr.md) to enforce commit message format on our github repositories.

### Commit Message Format
Each commit message consists of a **header**, a **body** and a **footer**. The header has a special
format that includes a **type**, a **scope** and a **subject**:

```html
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

> Any line of the commit message cannot be longer 100 characters!<br/>
  This allows the message to be easier to read on github as well as in various git tools.

#### Type
Must be one of the following:

* **feat**: A new feature
* **fix**: A bug fix
* **docs**: Documentation only changes
* **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing
  semi-colons, etc)
* **refactor**: A code change that neither fixes a bug nor adds a feature
* **perf**: A code change that improves performance
* **test**: Adding missing tests
* **chore**: Changes to the build process or auxiliary tools and libraries such as documentation
  generation

#### Scope
The scope could be anything specifying the place of the commit change.

#### Subject
The subject contains succinct description of the change:

* use the imperative, present tense: "change" not "changed" nor "changes"
* don't capitalize first letter
* no dot (.) at the end

#### Body
Just as in the **subject**, use the imperative, present tense: "change" not "changed" nor "changes"
The body should include the motivation for the change and contrast this with previous behavior.

#### Footer
The footer should contain any information about **Breaking Changes** and is also the place to
reference GitHub issues that this commit eventually **Closes**.

> Breaking Changes are intended to highlight (in the ChangeLog) changes that will require community
  users to modify their code with this commit.

#### Sample Commit message:

```text
refactor(button): prefix btn class with (dress code) dc-, closes #34

Change button class name to prevent collision with other frameworks.

BREAKING CHANGE: btn class now is prefixed with dc namespace.

  Change your code from this:

  <button class="btn">submit</button>

  To this:

  <button class="dc-btn">submit</button>
```
