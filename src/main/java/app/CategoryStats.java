package app;

public final class CategoryStats {
    private long count;
    private double total;
    private double maximum = Double.NEGATIVE_INFINITY;

    public void add(Transaction transaction) {
        double valor = transaction.getValue();
        this.count++;
        this.total += valor;
        if (valor > this.maximum) {
            this.maximum = valor;
        }
    }

    public void merge(CategoryStats other) {
        if (other == null) {
            return;
        }
        this.count += other.count;
        this.total += other.total;
        if (other.maximum > this.maximum) {
            this.maximum = other.maximum;
        }
    }

    public long getCount() {
        return count;
    }

    public double getTotal() {
        return total;
    }

    public double getAverage() {
        if (count == 0) {
            return 0.0;
        }
        return total / count;
    }

    public double getMaximum() {
        return count == 0 ? 0.0 : maximum;
    }

    public long count() {
        return getCount();
    }

    public double total() {
        return getTotal();
    }

    public double average() {
        return getAverage();
    }

    public double maximum() {
        return getMaximum();
    }
}