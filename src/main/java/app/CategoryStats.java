package app;

public final class CategoryStats {
    private long count;
    private double total;
    private double maximum = Double.NEGATIVE_INFINITY;

    public void add(Transaction transaction) {
        count++;
        total += transaction.value();
        maximum = Math.max(maximum, transaction.value());
    }

    public void merge(CategoryStats other) {
        count += other.count;
        total += other.total;
        maximum = Math.max(maximum, other.maximum);
    }

    public long count() { return count; }
    public double total() { return total; }
    public double average() { return count == 0 ? 0 : total / count; }
    public double maximum() { return count == 0 ? 0 : maximum; }
}