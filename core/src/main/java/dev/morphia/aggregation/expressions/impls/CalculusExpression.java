package dev.morphia.aggregation.expressions.impls;

import dev.morphia.aggregation.expressions.TimeUnit;
import dev.morphia.aggregation.expressions.WindowExpressions;

/**
 * Common type for $derivative and $integral
 *
 * @aggregation.expression $derivative
 * @aggregation.expression $integral
 * @mongodb.server.release 5.0
 * @see WindowExpressions#derivative(Expression)
 * @see WindowExpressions#integral(Expression)
 * @since 2.3
 */
public class CalculusExpression extends Expression {
    private final Expression input;
    private TimeUnit unit;

    public CalculusExpression(String operation, Expression input) {
        super(operation);
        this.input = input;
    }

    public Expression input() {
        return input;
    }

    public TimeUnit unit() {
        return unit;
    }

    /**
     * Sets the time unit for the expression
     *
     * @param unit the unit
     * @return this
     */
    public CalculusExpression unit(TimeUnit unit) {
        this.unit = unit;
        return this;
    }
}
