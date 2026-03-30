package models;

import java.sql.Date;

public class Item {
    private int    itemId;
    private String itemName;
    private String itemType;
    private String description;
    private String model;
    private String tag;
    private String conditionStatus;
    private String availabilityStatus;
    private Date   dateAcquired;

    public Item(int itemId, String itemName, String itemType, String description,
                String model, String tag, String conditionStatus,
                String availabilityStatus, Date dateAcquired) {
        this.itemId             = itemId;
        this.itemName           = itemName;
        this.itemType           = itemType;
        this.description        = description;
        this.model              = model;
        this.tag                = tag;
        this.conditionStatus    = conditionStatus;
        this.availabilityStatus = availabilityStatus;
        this.dateAcquired       = dateAcquired;
    }

    public int    getItemId()             { return itemId; }
    public String getItemName()           { return itemName; }
    public String getItemType()           { return itemType; }
    public String getDescription()        { return description; }
    public String getModel()              { return model; }
    public String getTag()                { return tag; }
    public String getConditionStatus()    { return conditionStatus; }
    public String getAvailabilityStatus() { return availabilityStatus; }
    public Date   getDateAcquired()       { return dateAcquired; }

    @Override
    public String toString() {
        return String.format("  [%d] %-25s %-12s %-12s %-12s %s",
            itemId, itemName, itemType, conditionStatus, availabilityStatus, tag);
    }
}