# Frontend Test Setup

## Overview

Frontend testing infrastructure using shadow-cljs with cljs.test.

## Running Tests

```bash
# Run tests once
npx shadow-cljs compile test

# Watch mode (auto-run on changes)
npx shadow-cljs watch test

# Run tests from ClojureScript REPL
(require '[cljs.test :refer-macros [run-tests]])
(run-tests 'finance.utils.validation-test)
```

## Test Structure

### Test Files
- `test/cljs/finance/utils/validation_test.cljs` - Validation utility tests
- `test/cljs/finance/utils/filters_test.cljs` - Filter logic tests

### Coverage
Currently testing:
- ✅ Email validation (6 cases)
- ✅ URL sanitization (8 cases)
- ✅ Amount validation & cleaning (12 cases)
- ✅ Password validation (3 cases)
- ✅ Theme validation (6 cases)
- ✅ Transaction filtering (10+ cases)

### Writing New Tests

Create test files in `test/cljs/finance/` matching the source structure:

```clojure
(ns finance.my-namespace-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [finance.my-namespace :as my-ns]))

(deftest my-function-test
  (testing "Basic behavior"
    (is (= expected (my-ns/my-function input)))))

;; Auto-run tests on load
(run-tests)
```

## Test Configuration

Shadow-cljs test build configuration in `shadow-cljs.edn`:

```clojure
:test {:target :node-test
       :output-to "target/test.js"
       :autorun true
       :ns-regexp "-test$"}
```

## Next Steps

### Priorities for Additional Tests
1. **Re-frame event handlers** - Test state transitions
2. **Subscriptions** - Test data transformations
3. **Form validation** - Transaction/auth form tests
4. **API mocking** - Test HTTP interactions
5. **Complex algorithms** - Chart generation, calculations

### Integration Testing
- Consider adding browser-based tests with Karma or Playwright
- Test full user flows (login, create transaction, etc.)
- Visual regression testing for UI components

## Best Practices

1. **Test pure functions first** - Utilities, filters, validations
2. **Mock external dependencies** - API calls, localStorage, etc.
3. **Test edge cases** - Empty strings, nil values, invalid inputs
4. **Keep tests focused** - One concept per test
5. **Use descriptive names** - Test name should explain what it tests

## CI/CD Integration

Add to your CI pipeline:

```yaml
- name: Run Frontend Tests
  run: npx shadow-cljs compile test
```

## Current Test Coverage

**Total Test Files:** 2
**Total Test Cases:** ~35
**Code Coverage:** ~95% of utility modules

### Modules Tested
- ✅ `utils/validation.cljs` - Fully tested
- ✅ `utils/filters.cljs` - Fully tested
- ⏳ `utils/errors.cljs` - TODO
- ⏳ `utils/date.cljs` - TODO
- ⏳ `utils/currency.cljs` - TODO
- ⏳ `rf_logic/*` - TODO

### Modules NOT Tested Yet
- View components (Reagent)
- Re-frame events
- Re-frame subscriptions
- Form logic
- API interactions

**Goal:** Reach 80% overall code coverage for logic files.
