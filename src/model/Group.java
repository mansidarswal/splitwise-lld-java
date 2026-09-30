package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Group {
    private static final AtomicInteger idCounter = new AtomicInteger(1);

    private final int id;
    private final String name;
    private final List<User> members = new ArrayList<>();
    private final List<Expense> expenses = new ArrayList<>();

    public Group(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name cannot be empty.");
        }
        this.id = idCounter.getAndIncrement();
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }


    public List<User> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    public void addMember(User user) {
        if (user != null && !members.contains(user)) {
            members.add(user);
        }
    }

    public void removeMember(User user) {
        members.remove(user);
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
    }

    @Override
    public String toString() {
        return "Group{id=" + id + ", name=" + name + ", members=" + members + "}";
    }
}