import model.Group;
import model.User;
import service.ExpenseManager;
import strategy.EqualSplitStrategy;
import strategy.ExactSplitStrategy;
import strategy.PercentSplitStrategy;

import java.math.BigDecimal;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        ExpenseManager manager = new ExpenseManager();

        User mansi = new User("Mansi", "mansi@gmail.com");
        User niharika = new User("Niharika", "niharika@gmail.com");
        User harsh = new User("Harsh", "harsh@gmail.com");

        manager.addUser(mansi);
        manager.addUser(niharika);
        manager.addUser(harsh);

        Group group = manager.createGroup("Goa Trip", List.of(mansi, niharika, harsh));

        // 1. EQUAL: Mansi pays 900 dinner, split among all three
        manager.addGroupExpense(group, "Dinner", bd("900"), mansi, group.getMembers(),
                new EqualSplitStrategy(), null);
        System.out.println("--- After Dinner (EQUAL) ---");
        manager.showBalances();

        // 2. EXACT: Niharika pays 500 cab; Mansi 200, Niharika 100, Harsh 200
        manager.addGroupExpense(group, "Cab", bd("500"), niharika, group.getMembers(),
                new ExactSplitStrategy(), List.of(bd("200"), bd("100"), bd("200")));
        System.out.println("\n--- After Cab (EXACT) ---");
        manager.showBalances();

        // 3. PERCENT: Harsh pays 1000 hotel; 50% / 30% / 20%
        manager.addGroupExpense(group, "Hotel", bd("1000"), harsh, group.getMembers(),
                new PercentSplitStrategy(), List.of(bd("50"), bd("30"), bd("20")));
        System.out.println("\n--- After Hotel (PERCENT) ---");
        manager.showBalances();

        System.out.println("\nExpenses in " + group.getName() + ": " + group.getExpenses().size());

        // 4. Settle up: Mansi pays Harsh back fully
        manager.settleUp(mansi, harsh, manager.getAmountOwed(mansi, harsh));
        System.out.println("\n--- After Mansi settles with Harsh ---");
        manager.showBalances();

        // 5. Validation in action
        try {
            manager.addGroupExpense(group, "Snacks", bd("300"), mansi, group.getMembers(),
                    new PercentSplitStrategy(), List.of(bd("50"), bd("30"), bd("10")));
        } catch (IllegalArgumentException e) {
            System.out.println("\nRejected: " + e.getMessage());
        }
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}