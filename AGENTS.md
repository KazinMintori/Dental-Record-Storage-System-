# General Coding Agent Rules

## 1. Understand Before Changing

- Always inspect the existing project structure before making changes.
- Read relevant existing files before creating new files.
- Do not assume how existing code works.
- Reuse existing functionality when appropriate.
- Do not create duplicate classes, utilities, configurations, or dependencies.

## 2. Minimize Changes

- Only modify files that are necessary for the requested task.
- Do not rewrite unrelated code.
- Do not rename or delete files unless required.
- Preserve existing functionality unless the task explicitly requires changing it.
- Avoid unnecessary refactoring.

## 3. Plan Before Major Changes

For small changes, implement them directly.

For significant changes:
1. Inspect the project.
2. Identify the affected components.
3. Explain the planned approach briefly.
4. Implement the change.
5. Verify the result.

Do not make major architectural changes without understanding the existing architecture.

## 4. Code Quality

- Prefer simple, readable, maintainable code.
- Avoid unnecessary abstraction.
- Avoid over-engineering.
- Follow the conventions already used by the project.
- Use meaningful names.
- Keep functions and classes focused.
- Prefer fixing root causes instead of adding workarounds.

## 5. Dependencies

- Do not add a dependency unless it is actually necessary.
- Check whether the project already has a library that solves the problem.
- Do not replace existing dependencies without a good reason.
- Do not modify dependency versions unnecessarily.

## 6. Configuration and Secrets

Never:
- Hard-code passwords.
- Hard-code API keys.
- Hard-code tokens.
- Commit secrets.
- Print secrets in logs.
- Put real credentials in example configuration files.

Use the project's existing environment-variable or secret-management approach.

## 7. Error Handling

- Handle errors at the appropriate layer.
- Do not silently ignore exceptions.
- Do not hide errors just to make tests pass.
- Error messages should be useful but must not expose secrets.

## 8. Testing

After making changes:

- Run the relevant tests when possible.
- Run compilation/build verification when appropriate.
- Do not claim something works without verifying it.
- If a test cannot be run, clearly explain why.

Do not modify tests simply to make them pass unless the test itself is incorrect.

## 9. Database Safety

When working with databases:

- Inspect the existing schema before modifying it.
- Never delete existing data unless explicitly instructed.
- Never use destructive operations casually.
- Prefer migrations for schema changes when the project uses migrations.
- Do not hard-code database credentials.
- Verify SQL syntax before applying changes.

## 10. UI Changes

When modifying a UI:

- Inspect the existing UI before changing it.
- Preserve existing functionality.
- Keep layouts consistent.
- Avoid introducing unnecessary UI complexity.
- Consider responsiveness and usability.

## 11. Git Safety

- Do not reset, revert, or delete user changes without explicit permission.
- Do not overwrite unrelated work.
- Do not force-push.
- Do not modify Git history unless explicitly requested.

## 12. Communication

When finished:

- Summarize what changed.
- List important files changed.
- Report tests/builds that were actually run.
- Report failures honestly.
- Mention anything that still requires user action.

Do not claim success if verification was not performed.

## 13. When Requirements Are Unclear

Do not guess when an ambiguity could materially affect the implementation.

Ask for clarification when necessary.

If the ambiguity is minor and a safe default exists, use the safest reasonable default and state the assumption.

## 14. Agent Autonomy

You may:
- Inspect files.
- Read project configuration.
- Search the codebase.
- Make necessary code changes.
- Run safe build and test commands.

Be cautious with:
- Database modifications.
- File deletion.
- Dependency changes.
- System configuration.
- Destructive commands.

Ask for confirmation before making destructive or irreversible changes unless explicitly authorized by the user.

## 15. General Principle

Before changing something, understand it.

Make the smallest change that correctly solves the problem.

Prefer correctness and maintainability over speed.

Verify your work before claiming it is complete.