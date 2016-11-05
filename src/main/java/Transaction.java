import java.io.Serializable;

/**
 * Created by Yasi on 10/30/2016.
 */
public class Transaction implements Serializable {
    private String type;
    private Integer id;
    private Integer amount;
    private String deposit;

    public Transaction(Integer id, String type, Integer amount, String deposit) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.deposit = deposit;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public Integer getId() {
        return id;
    }

    public Integer getAmount() {
        return amount;
    }

    public String getDeposit() {
        return deposit;
    }
}
