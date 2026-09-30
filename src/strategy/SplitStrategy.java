package strategy;

import model.SplitType;
import model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * One common contract for every split type, so ExpenseManager can use any
 * strategy without knowing which one it is (Liskov Substitution holds).
 *
 * values meaning:
 *   EQUAL   -> ignored (may be null)
 *   EXACT   -> exact amount for each participant
 *   PERCENT -> percentage for each participant
 */
public interface SplitStrategy {

    SplitType getType();

    Map<User, BigDecimal> calculateSplit(BigDecimal amount,
                                         List<User> participants,
                                         List<BigDecimal> values);
}