package strategy;

import model.SplitType;
import model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PercentSplitStrategy implements SplitStrategy {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public SplitType getType() {
        return SplitType.PERCENT;
    }

    @Override
    public Map<User, BigDecimal> calculateSplit(BigDecimal amount,
                                                List<User> participants,
                                                List<BigDecimal> values) {
        if (values == null || participants.size() != values.size()) {
            throw new IllegalArgumentException("Participants and percentages size must match.");
        }

        BigDecimal percentSum = BigDecimal.ZERO;
        for (BigDecimal p : values) {
            if (p.signum() < 0) {
                throw new IllegalArgumentException("Percentages cannot be negative.");
            }
            percentSum = percentSum.add(p);
        }
        if (percentSum.compareTo(HUNDRED) != 0) {
            throw new IllegalArgumentException("Percentages must add up to 100, but got " + percentSum + ".");
        }

        BigDecimal total = amount.setScale(2, RoundingMode.HALF_UP);
        Map<User, BigDecimal> split = new LinkedHashMap<>();
        BigDecimal assigned = BigDecimal.ZERO;

        for (int i = 0; i < participants.size(); i++) {
            BigDecimal share = total.multiply(values.get(i)).divide(HUNDRED, 2, RoundingMode.DOWN);
            assigned = assigned.add(share);
            split.put(participants.get(i), share);
        }

        // Rounding down can leave a few paise unassigned; give them to the first participant
        BigDecimal leftover = total.subtract(assigned);
        if (leftover.signum() > 0) {
            User first = participants.get(0);
            split.put(first, split.get(first).add(leftover));
        }
        return split;
    }
}