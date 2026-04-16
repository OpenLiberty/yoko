# Contributing to Yoko

Anyone can contribute to the Yoko project and we welcome your contributions!

There are multiple ways to contribute: report bugs, fix bugs, contribute code, improve upon documentation, etc. You must follow these guidelines:

* [Raising Issues](#raising-issues)
* [Contributor License Agreement](#contributor-license-agreement)
* [Coding Standards](#coding-standards)
* [GenAI Guidelines](#genai-guidelines)

## Raising Issues

Please raise any bug reports on the [Yoko project repository's GitHub issue tracker](https://github.com/yoko-tool/yoko/issues). Be sure to search the list to see if your issue has already been raised.

A good bug report is one that makes it easy for everyone to understand what you were trying to do and what went wrong. Provide as much context as possible so we can try to recreate the issue.

## Contributor License Agreement

If you are contributing code changes via a pull request for anything except trivial changes, you must signoff on the [Individual Contributor License Agreement](https://github.com/OpenLiberty/open-liberty/blob/HEAD/cla/open-liberty-cla-individual.pdf). If you are doing this as part of your job you may also wish to get your employer to sign a [Corporate Contributor License Agreement](https://github.com/OpenLiberty/open-liberty/blob/HEAD/cla/open-liberty-cla-corporate.pdf). Instructions on how to sign and submit these agreements are located at the top of each document. Trivial changes such as typos, redundant spaces, minor formatting and spelling errors will be labeled as "CLA trivial", and don't require a signed CLA for consideration.

After we obtain the signed CLA, you are welcome to open a pull request, and the team will be notified for review. We ask you follow these steps through the submission process:

1. Ensure you run a passing local gradle build as explained in the [README](README.md) before opening a PR.
2. Open PRs against the "main" branch.
3. A team of reviewers will be notified, will perform a review, and if approved will merge the PR.
4. The reviewer may modify the PR if necessary to meet project standards.
5. If the reviewer is satisfied with the results and agrees to the change, the PR will be merged to main; otherwise the PR will be closed with an explanation and suggestion for followup.

## Coding Standards

Please ensure you follow the coding standards used throughout the existing code base. Some basic rules include:

* **License Headers**: All files must have an Apache License 2.0 header (see existing files for format)
* **Testing**: All PRs must include comprehensive tests
* **Build**: All PRs must have a passing build

## License

Yoko is licensed under the Apache License 2.0. All contributions must comply with this license.

See the [LICENSE](LICENSE) file for details.
