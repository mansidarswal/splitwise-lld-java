package strategy;

import model.SplitType;
import model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public SplitType getType() {
        return SplitType.EQUAL;
    }

    @Override
    public Map<User, BigDecimal> calculateSplit(BigDecimal amount,
                                                List<User> participants,
                                                List<BigDecimal> values) {
        int n = participants.size();
        BigDecimal total = amount.setScale(2, RoundingMode.HALF_UP);

        // e.g. 100 / 3 = 33.33 each, leaving 0.01 extra
        BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        BigDecimal remainder = total.subtract(base.multiply(BigDecimal.valueOf(n)));
        int extraPaise = remainder.movePointRight(2).intValueExact();

        // Leftover paise go one each to the first few people, so shares add up exactly to total
        Map<User, BigDecimal> split = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            BigDecimal share = base;
            if (i < extraPaise) {
                share = share.add(new BigDecimal("0.01"));
            }
            split.put(participants.get(i), share);
        }
        return split;
    }
}