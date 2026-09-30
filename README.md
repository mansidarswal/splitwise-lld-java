# Splitwise LLD (Java)

A command-line expense-splitting system in core Java, modelled on Splitwise.
It supports groups, three split types, debt netting and settlements.

## Features
- **Split types:** equal, exact amounts, percentages
- **Precise money handling:** uses `BigDecimal`, and leftover paise are distributed so shares always add up to the exact total
- **Debt netting:** only one direction is stored per pair of users (if A owes B 300 and B owes A 200, the result is A owes B 100)
- **Settlements:** full or partial, with validation against over-settling
- **Groups:** expenses can be recorded against a group, and membership is checked before any balance changes
- **Validation:** clear error messages for bad amounts, unregistered users, duplicates and mismatched totals

## Design
- **Strategy pattern:** `SplitStrategy` with `EqualSplitStrategy`, `ExactSplitStrategy` and `PercentSplitStrategy`. A new split type needs no change to `ExpenseManager` (open/closed principle).
- **Separation of concerns:** `model` (data), `service` (business logic), `strategy` (split algorithms)
- Balance logic returns data (`getBalanceStatements()`) and printing is a thin wrapper
- Thread-safe ID generation with `AtomicInteger`

## Project structure
```
src/
  Main.java        demo
  SelfTest.java    plain-Java checks
  model/           User, Group, Expense, SplitType
  service/         ExpenseManager
  strategy/        SplitStrategy and its implementations
```

## How to run
Requires JDK 17+.
```
cd src
javac -d out $(find . -name "*.java")   # on Windows, compile from IntelliJ or list the files
java -cp out Main
java -cp out SelfTest
```
Or open the folder in IntelliJ and run `Main` / `SelfTest`.

## Sample output
```
[paste the output of running Main here]
```

## Limitations and future work
- Balances are global per pair of users, not per group
- No simplify-debts algorithm (minimizing the number of transactions)
- In-memory only; a next step would be a REST layer with Spring Boot and a database
- Tests are plain Java; migrating to JUnit 5 is planned