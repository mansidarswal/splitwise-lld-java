package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Expense {
    private static final AtomicInteger idCounter = new AtomicInteger(1);

    private final int id;
    private final String description;
    private final BigDecimal amount;
    private final User paidBy;
    private final SplitType splitType;
    private final Map<User, BigDecimal> shares; // what each participant owes for this expense

    public Expense(String description, BigDecimal amount, User paidBy,
                   SplitType splitType, Map<User, BigDecimal> shares) {
        this.id = idCounter.getAndIncrement();
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.shares = Collections.unmodifiableMap(new LinkedHashMap<>(shares));
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public Map<User, BigDecimal> getShares() {
        return shares;
    }

    public List<User> getParticipants() {
        return new ArrayList<>(shares.keySet());
    }

    @Override
    public String toString() {
        return "Expense{id=" + id +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", paidBy=" + paidBy.getName() +
                ", splitType=" + splitType + "}";
    }
}