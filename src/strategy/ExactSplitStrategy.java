package strategy;

import model.SplitType;
import model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExactSplitStrategy implements SplitStrategy {

    @Override
    public SplitType getType() {
        return SplitType.EXACT;
    }

    @Override
    public Map<User, BigDecimal> calculateSplit(BigDecimal amount,
                                                List<User> participants,
                                                List<BigDecimal> values) {
        if (values == null || participants.size() != values.size()) {
            throw new IllegalArgumentException("Participants and amounts size must match.");
        }

        BigDecimal total = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sum = BigDecimal.ZERO;
        Map<User, BigDecimal> split = new LinkedHashMap<>();

        for (int i = 0; i < participants.size(); i++) {
            BigDecimal share = values.get(i).setScale(2, RoundingMode.HALF_UP);
            if (share.signum() < 0) {
                throw new IllegalArgumentException("Amounts cannot be negative.");
            }
            sum = sum.add(share);
            split.put(participants.get(i), share);
        }

        if (sum.compareTo(total) != 0) {
            throw new IllegalArgumentException(
                    "Exact amounts add up to " + sum + " but the expense total is " + total + ".");
        }
        return split;
    }
}