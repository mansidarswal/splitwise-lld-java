import model.Group;
import model.User;
import service.ExpenseManager;
import strategy.EqualSplitStrategy;
import strategy.PercentSplitStrategy;
import strategy.ExactSplitStrategy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Plain-Java checks (no framework needed). Run this file's main(). */
public class SelfTest {

    private static int passed = 0;

    public static void main(String[] args) {
        User a = new User("A", "a@x.com");
        User b = new User("B", "b@x.com");
        User c = new User("C", "c@x.com");
        List<User> all = List.of(a, b, c);

        // Equal split of 100 among 3 must add up to exactly 100.00
        Map<User, BigDecimal> eq = new EqualSplitStrategy().calculateSplit(bd("100"), all, null);
        BigDecimal sum = eq.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        check("equal split sums to total", sum.compareTo(bd("100.00")) == 0);
        check("first person gets the extra paisa", eq.get(a).compareTo(bd("33.34")) == 0);

        // Exact split must match the total
        expectFailure("exact amounts not matching total",
                () -> new ExactSplitStrategy().calculateSplit(bd("100"), all, List.of(bd("50"), bd("30"), bd("10"))));

        // Percentages must add to 100
        expectFailure("percentages not adding to 100",
                () -> new PercentSplitStrategy().calculateSplit(bd("100"), all, List.of(bd("50"), bd("30"), bd("10"))));

        // Netting: B owes A 300, then A owes B 200 -> B owes A 100 (single direction)
        ExpenseManager m = new ExpenseManager();
        m.addUser(a);
        m.addUser(b);
        m.addExpense("e1", bd("600"), a, List.of(a, b), new EqualSplitStrategy(), null);      // B owes A 300
        m.addExpense("e2", bd("400"), b, List.of(a, b), new EqualSplitStrategy(), null);      // A owes B 200
        check("debts are netted", m.getAmountOwed(b, a).compareTo(bd("100")) == 0);
        check("opposite direction is zero", m.getAmountOwed(a, b).signum() == 0);
        check("balance statement is returned, not printed",
                m.getBalanceStatements().equals(List.of("B owes A Rs.100.00")));

        // Settle up
        expectFailure("settling more than owed", () -> m.settleUp(b, a, bd("500")));
        expectFailure("settling with null amount", () -> m.settleUp(b, a, null));
        expectFailure("settling with null user", () -> m.settleUp(null, a, bd("10")));
        expectFailure("settling with yourself", () -> m.settleUp(a, a, bd("10")));
        m.settleUp(b, a, bd("100"));
        check("fully settled", m.getAmountOwed(b, a).signum() == 0);
        check("no balance statements when settled", m.getBalanceStatements().isEmpty());

        // Unregistered participant
        expectFailure("unregistered participant",
                () -> m.addExpense("e3", bd("90"), a, List.of(a, c), new EqualSplitStrategy(), null));
        expectFailure("null participant",
                () -> m.addExpense("e4", bd("90"), a, java.util.Arrays.asList(a, null),
                        new EqualSplitStrategy(), null));

        // Groups
        ExpenseManager m2 = new ExpenseManager();
        m2.addUser(a);
        m2.addUser(b);
        m2.addUser(c);
        Group trip = m2.createGroup("Trip", List.of(a, b)); // c is registered but NOT a member

        m2.addGroupExpense(trip, "Lunch", bd("200"), a, List.of(a, b), new EqualSplitStrategy(), null);
        check("group records its expense", trip.getExpenses().size() == 1);
        check("group expense updates balances", m2.getAmountOwed(b, a).compareTo(bd("100")) == 0);

        expectFailure("non-member payer in group expense",
                () -> m2.addGroupExpense(trip, "x", bd("90"), c, List.of(a, b),
                        new EqualSplitStrategy(), null));
        expectFailure("non-member participant in group expense",
                () -> m2.addGroupExpense(trip, "x", bd("90"), a, List.of(a, c),
                        new EqualSplitStrategy(), null));
        check("rejected group expense changes nothing",
                trip.getExpenses().size() == 1 && m2.getAmountOwed(c, a).signum() == 0);
        expectFailure("group with unregistered member",
                () -> m.createGroup("Bad", List.of(a, c)));
        expectFailure("unregistered group",
                () -> m2.addGroupExpense(new Group("Ghost"), "x", bd("90"), a, List.of(a),
                        new EqualSplitStrategy(), null));

        System.out.println("\nAll " + passed + " checks passed.");
    }

    private static void check(String name, boolean condition) {
        if (!condition) throw new AssertionError("FAILED: " + name);
        System.out.println("PASS: " + name);
        passed++;
    }

    private static void expectFailure(String name, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException e) {
            check(name + " is rejected", true);
            return;
        }
        throw new AssertionError("FAILED: " + name + " should have been rejected");
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}