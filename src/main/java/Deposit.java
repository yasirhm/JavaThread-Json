import java.math.BigDecimal;

import static java.lang.Thread.sleep;


/**
 * Created by Yasi on 10/31/2016.
 */
public class Deposit {
    private String customer;
    private String id;
    private Integer initialBalance;
    private BigDecimal upperBound;
    Boolean lock = false;

    public Deposit(String customer, String id, Integer initialBalance, BigDecimal upperBound) {
        this.customer = customer;
        this.id = id;
        this.initialBalance = initialBalance;
        this.upperBound = upperBound;
    }

    public String getCustomer() {
        return customer;
    }

    public String getId() {
        return id;
    }

    public Integer getInitialBalance() {
        return initialBalance;
    }

    public void withdraw(Integer amount) throws BalanceException {
        synchronized (this) {

            // if(Thread.currentThread().getName().equals("Thread-0")) {
            try {
/*
                if (!lock) {
                    System.out.println("Errr: " + Thread.currentThread().getName());
                    this.wait();
                    lock =  true;
                }
*/

                Integer temp = initialBalance - amount;
                Thread.currentThread().sleep(1000);
                System.out.println("withDrawwwwwwww: " + Thread.currentThread().getName() + "  " + initialBalance);
                initialBalance = temp;
            } catch (InterruptedException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        /*
            if(amount < 0) throw new BalanceException("Negative transactions is not allowed.");
            else if (amount <= initialBalance) {
                initialBalance = initialBalance - amount;
            } else throw new BalanceException("There is not enough balance.");
            System.out.println("withdraw " + initialBalance);
            */
        //  }

    }

    public void deposit(Integer amount) throws BalanceException {
        synchronized (this) {
            Integer temp = amount + initialBalance;
            initialBalance = temp;
            System.out.println("deposiiiiiiiiiiiiiiiit: " + Thread.currentThread().getName() + "  " + initialBalance);

            /*
            if (lock) {
                System.out.println("deposiiitErrr: " + Thread.currentThread().getName());
                this.notify();
                //lock = false;
            }
            /*
            Integer temp = amount + initialBalance;
            if(amount < 0) throw new BalanceException("Negative transactions is not allowed.");
            else if (temp <= upperBound.intValue()) {
                initialBalance = temp;
            } else throw new BalanceException("Transaction is more than amount that allowed.");
            */
        }
    }
}
