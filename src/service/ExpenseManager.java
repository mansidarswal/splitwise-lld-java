package service;

import model.Expense;
import model.Group;
import model.User;
import strategy.SplitStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpenseManager {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final List<User> users = new ArrayList<>();
    private final List<Expense> expenses = new ArrayList<>();
    private final Map<Integer, Group> groups = new LinkedHashMap<>();

    // balances.get(debtor).get(creditor) = amount debtor owes creditor.

    private final Map<User, Map<User, BigDecimal>> balances = new LinkedHashMap<>();

    // ---------- users ----------

    public void addUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        if (balances.containsKey(user)) {
            throw new IllegalArgumentException(user.getName() + " is already added.");
        }
        users.add(user);
        balances.put(user, new LinkedHashMap<>());
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    // ---------- groups ----------

    public Group createGroup(String name, List<User> members) {
        if (members == null) {
            throw new IllegalArgumentException("Group members are required.");
        }
        for (User u : members) {
            if (u == null || !balances.containsKey(u)) {
                throw new IllegalArgumentException("All group members must be registered users.");
            }
        }
        Group group = new Group(name);
        for (User u : members) {
            group.addMember(u);
        }
        groups.put(group.getId(), group);
        return group;
    }

    public Expense addGroupExpense(Group group,
                                   String description,
                                   BigDecimal amount,
                                   User paidBy,
                                   List<User> participants,
                                   SplitStrategy strategy,
                                   List<BigDecimal> values) {
        if (group == null || !groups.containsKey(group.getId())) {
            throw new IllegalArgumentException("Group is not registered with the manager.");
        }
        if (paidBy == null || !group.getMembers().contains(paidBy)) {
            throw new IllegalArgumentException("Payer must be a member of " + group.getName() + ".");
        }
        if (participants == null) {
            throw new IllegalArgumentException("Expense needs at least one participant.");
        }
        for (User u : participants) {
            if (u == null || !group.getMembers().contains(u)) {
                throw new IllegalArgumentException(
                        "All participants must be members of " + group.getName() + ".");
            }
        }

        Expense expense = addExpense(description, amount, paidBy, participants, strategy, values);
        group.addExpense(expense);
        return expense;
    }

    // ---------- expenses ----------

    public Expense addExpense(String description,
                              BigDecimal amount,
                              User paidBy,
                              List<User> participants,
                              SplitStrategy strategy,
                              List<BigDecimal> values) {

        validateExpense(amount, paidBy, participants, strategy);

        BigDecimal total = amount.setScale(2, RoundingMode.HALF_UP);
        Map<User, BigDecimal> shares = strategy.calculateSplit(total, participants, values);

        for (Map.Entry<User, BigDecimal> entry : shares.entrySet()) {
            User participant = entry.getKey();
            if (participant.equals(paidBy)) {
                continue; // payer does not owe himself
            }
            addDebt(participant, paidBy, entry.getValue());
        }

        Expense expense = new Expense(description, total, paidBy, strategy.getType(), shares);
        expenses.add(expense);
        return expense;
    }

    // "from" pays back "to" (fully or partially)
    public void settleUp(User from, User to, BigDecimal amount) {
        if (from == null || to == null || !balances.containsKey(from) || !balances.containsKey(to)) {
            throw new IllegalArgumentException("Both users must be added to the manager.");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("A user cannot settle with themselves.");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Settlement amount is required.");
        }
        BigDecimal pay = amount.setScale(2, RoundingMode.HALF_UP);
        if (pay.signum() <= 0) {
            throw new IllegalArgumentException("Settlement amount must be positive.");
        }
        BigDecimal owed = getAmountOwed(from, to);
        if (pay.compareTo(owed) > 0) {
            throw new IllegalArgumentException(
                    from.getName() + " only owes " + to.getName() + " Rs." + owed + ", cannot settle Rs." + pay + ".");
        }
        setDebt(from, to, owed.subtract(pay));
    }

    // ---------- balances ----------

    // How much "debtor" currently owes "creditor"
    public BigDecimal getAmountOwed(User debtor, User creditor) {
        Map<User, BigDecimal> map = balances.get(debtor);
        if (map == null) return ZERO;
        return map.getOrDefault(creditor, ZERO);
    }


    public List<String> getBalanceStatements() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<User, Map<User, BigDecimal>> debtorEntry : balances.entrySet()) {
            for (Map.Entry<User, BigDecimal> creditorEntry : debtorEntry.getValue().entrySet()) {
                lines.add(debtorEntry.getKey().getName() + " owes "
                        + creditorEntry.getKey().getName() + " Rs." + creditorEntry.getValue());
            }
        }
        return lines;
    }

   //wrapper
    public void showBalances() {
        List<String> lines = getBalanceStatements();
        if (lines.isEmpty()) {
            System.out.println("All settled up!");
            return;
        }
        lines.forEach(System.out::println);
    }

    // ---------- helpers ----------

    private void validateExpense(BigDecimal amount, User paidBy,
                                 List<User> participants, SplitStrategy strategy) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Expense amount must be positive.");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("Split strategy is required.");
        }
        if (paidBy == null || !balances.containsKey(paidBy)) {
            throw new IllegalArgumentException("Payer must be a registered user.");
        }
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Expense needs at least one participant.");
        }
        if (new HashSet<>(participants).size() != participants.size()) {
            throw new IllegalArgumentException("Duplicate participants are not allowed.");
        }
        for (User u : participants) {
            if (u == null || !balances.containsKey(u)) {
                throw new IllegalArgumentException(
                        (u == null ? "A null participant" : u.getName()) + " is not a registered user.");
            }
        }
    }

    // Record that debtor owes creditor "amount", netting against any opposite debt
    private void addDebt(User debtor, User creditor, BigDecimal amount) {
        BigDecimal reverse = getAmountOwed(creditor, debtor);

        if (reverse.signum() > 0) {
            if (reverse.compareTo(amount) >= 0) {
                setDebt(creditor, debtor, reverse.subtract(amount));
                return;
            }
            setDebt(creditor, debtor, ZERO);
            amount = amount.subtract(reverse);
        }
        setDebt(debtor, creditor, getAmountOwed(debtor, creditor).add(amount));
    }

    // Set the debt
    private void setDebt(User debtor, User creditor, BigDecimal amount) {
        if (amount.signum() == 0) {
            balances.get(debtor).remove(creditor);
        } else {
            balances.get(debtor).put(creditor, amount);
        }
    }
}