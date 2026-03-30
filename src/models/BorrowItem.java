package models;

public class BorrowItem {
    private int    borrowItemId;
    private int    borrowId;
    private String itemName;
    private String tag;
    private int    quantity;

    public BorrowItem(int borrowItemId, int borrowId,
                      String itemName, String tag, int quantity) {
        this.borrowItemId = borrowItemId;
        this.borrowId     = borrowId;
        this.itemName     = itemName;
        this.tag          = tag;
        this.quantity     = quantity;
    }

    public int    getBorrowItemId() { return borrowItemId; }
    public int    getBorrowId()     { return borrowId; }
    public String getItemName()     { return itemName; }
    public String getTag()          { return tag; }
    public int    getQuantity()     { return quantity; }

    @Override
    public String toString() {
        return String.format("       ↳ %-25s  Tag: %-15s  Qty: %d",
            itemName, tag, quantity);
    }
}