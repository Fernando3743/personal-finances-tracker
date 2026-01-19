---
name: worktree
description: This skill manages git worktrees. Use when user mentions "worktree", "work tree", wants to create/list/remove/switch worktrees, or asks about working on multiple branches simultaneously.
version: 1.0.0
---

# Git Worktree Management Skill

You are helping the user manage git worktrees for working on multiple branches simultaneously. This skill handles creating, listing, removing, and switching between worktrees.

## Naming Convention

Always use branch-based naming for worktrees:
- Pattern: `../<project-name>-<branch-name>/`
- Project name: Extract from current directory name
- Branch name: Use the full branch name, replacing `/` with `-` (e.g., `feature/auth` becomes `feature-auth`)

Example: For project `PersonalFinancesTracker` and branch `feature/auth-refactor`, create worktree at:
`../PersonalFinancesTracker-feature-auth-refactor/`

## Operation: Determine User Intent

When invoked, first determine what the user wants to do:
1. **Create** a new worktree
2. **List** existing worktrees
3. **Remove** a worktree
4. **Switch** to a different worktree

If unclear, use AskUserQuestion to clarify.

## Operation: Create Worktree

### Steps:

1. **Get project name:**
   ```bash
   basename "$(pwd)"
   ```

2. **Get or prompt for branch name:**
   - If user specified a branch, use it
   - Otherwise, ask using AskUserQuestion with these options:
     - List available branches using: `git branch -a`
     - Allow user to specify new branch name

3. **Check if branch exists:**
   ```bash
   git branch --list <branch-name>
   git branch -r --list origin/<branch-name>
   ```

4. **Generate worktree path:**
   ```bash
   # Sanitize branch name: replace / with -
   SANITIZED_BRANCH=$(echo "<branch-name>" | tr '/' '-')
   PROJECT_NAME="<project-name>"
   WORKTREE_PATH="../${PROJECT_NAME}-${SANITIZED_BRANCH}"
   ```

5. **Create worktree:**
   - If branch exists:
     ```bash
     git worktree add "<worktree-path>" <branch-name>
     ```
   - If new branch:
     ```bash
     git worktree add "<worktree-path>" -b <branch-name>
     ```

6. **Symlink shared resources:**
   After creating the worktree, symlink files that should be shared between worktrees:

   - **Check and symlink node_modules:**
     ```bash
     if [ -d "node_modules" ]; then
       ln -s "$(pwd)/node_modules" "<worktree-path>/node_modules"
       echo "Symlinked node_modules to save space and installation time"
     fi
     ```

   - **Check and symlink .env:**
     ```bash
     if [ -f ".env" ]; then
       ln -s "$(pwd)/.env" "<worktree-path>/.env"
       echo "Symlinked .env to share environment configuration"
     fi
     ```

   **Note:** Use absolute paths when creating symlinks to ensure they work correctly:
   ```bash
   MAIN_DIR="$(pwd)"
   ln -s "${MAIN_DIR}/node_modules" "<worktree-path>/node_modules"
   ln -s "${MAIN_DIR}/.env" "<worktree-path>/.env"
   ```

7. **Confirm success:**
   Display the worktree path, mention any symlinks created, and suggest the user can `cd` to it.

### Error Handling:
- If worktree path already exists, inform user and suggest using a different branch name or removing the existing worktree
- If git command fails, show the error message and suggest solutions

## Operation: List Worktrees

### Steps:

1. **List all worktrees:**
   ```bash
   git worktree list
   ```

2. **Format output:**
   Display in a clear, readable format showing:
   - Worktree path
   - Branch name
   - HEAD commit (short hash)

3. **Highlight current worktree:**
   The current worktree is marked with `(bare)` or is the current directory.

## Operation: Remove Worktree

### Steps:

1. **List available worktrees:**
   ```bash
   git worktree list
   ```

2. **Prompt for selection:**
   Use AskUserQuestion to let user select which worktree to remove.
   - Show path and branch for each worktree
   - Exclude the current/main worktree from removal options

3. **Safety check:**
   Confirm the worktree to be removed is not the current directory.

4. **Remove worktree:**
   ```bash
   git worktree remove "<worktree-path>"
   ```

5. **Handle locked worktrees:**
   If the removal fails because worktree is locked, ask user if they want to force remove:
   ```bash
   git worktree remove --force "<worktree-path>"
   ```

6. **Confirm success:**
   Display confirmation message.

### Error Handling:
- If worktree has uncommitted changes, warn user and ask if they want to force remove
- If worktree doesn't exist, inform user

## Operation: Switch Worktree

### Steps:

1. **List available worktrees:**
   ```bash
   git worktree list
   ```

2. **Prompt for selection:**
   Use AskUserQuestion to let user select which worktree to switch to.
   - Show path and branch for each worktree

3. **Provide navigation command:**
   Since you cannot change the user's shell directory, provide them with the `cd` command:
   ```
   To switch to this worktree, run:
   cd <worktree-path>
   ```

4. **Optional - Open in new terminal:**
   Suggest that the user can also open a new terminal window/tab and navigate there.

## Best Practices

1. **Always show full paths:** Use absolute paths when displaying worktree locations
2. **Validate before action:** Check if branches/worktrees exist before performing operations
3. **Clear feedback:** Provide clear success/error messages
4. **Interactive prompts:** Use AskUserQuestion for selections and confirmations
5. **Safety first:** Prevent accidental deletion of important worktrees

## Common Scenarios

### Scenario 1: User wants to work on a feature branch
```
User: "Create a worktree for feature/dark-mode"
1. Extract project name: PersonalFinancesTracker
2. Sanitize branch: feature-dark-mode
3. Create at: ../PersonalFinancesTracker-feature-dark-mode/
4. Run: git worktree add ../PersonalFinancesTracker-feature-dark-mode/ feature/dark-mode
5. Symlink node_modules and .env if they exist
6. Inform user: "Created worktree at ../PersonalFinancesTracker-feature-dark-mode/ with symlinks to node_modules and .env"
```

### Scenario 2: User wants to start a new feature
```
User: "Create a worktree for a new branch called feature/user-profile"
1. Extract project name
2. Create new branch and worktree
3. Run: git worktree add ../PersonalFinancesTracker-feature-user-profile/ -b feature/user-profile
4. Symlink node_modules and .env if they exist
5. Inform user about the created worktree and any symlinks
```

### Scenario 3: User wants to clean up
```
User: "Remove old worktrees"
1. List all worktrees
2. Ask which ones to remove
3. Remove selected worktrees
```

## Error Messages

- **Worktree already exists:** "A worktree already exists at this path. Use a different branch name or remove the existing worktree first."
- **Branch doesn't exist:** "Branch '<branch-name>' doesn't exist. Would you like to create it?"
- **Cannot remove current worktree:** "You cannot remove the worktree you're currently in. Please switch to a different worktree first."
- **Locked worktree:** "This worktree is locked. Would you like to force remove it? Warning: any uncommitted changes will be lost."

## Tools to Use

- **Bash:** For all git commands and file operations
- **AskUserQuestion:** For interactive prompts and confirmations
- **Read:** If you need to check any git configuration files

Remember: Always be helpful, clear, and prevent destructive actions without user confirmation.
