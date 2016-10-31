import java.io.Serializable;

/**
 * Created by dotinschool6 on 10/30/2016.
 */
public class Transaction implements Serializable {
    private String type;
    private Integer id;
    private String amount;
    private String deposit;

    public Transaction(Integer id, String type, String amount, String deposit) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.deposit = deposit;
    }

    public String getType() {
        return type;
    }

    public Integer getId() {
        return id;
    }

    public String getAmount() {
        return amount;
    }

    public String getDeposit() {
        return deposit;
    }
}
