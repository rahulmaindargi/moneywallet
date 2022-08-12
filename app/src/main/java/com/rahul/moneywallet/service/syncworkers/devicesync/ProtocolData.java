package com.rahul.moneywallet.service.syncworkers.devicesync;

public class ProtocolData {
    public String getDeviceSourceId() {
        return deviceSourceId;
    }

    public void setDeviceSourceId(String deviceSourceId) {
        this.deviceSourceId = deviceSourceId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMoney() {
        return money;
    }

    public void setMoney(String money) {
        this.money = money;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getPeopleNames() {
        return peopleNames;
    }

    public void setPeopleNames(String peopleNames) {
        this.peopleNames = peopleNames;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    String deviceSourceId;
    String id;
    String walletName;
    String currency;
    String category;
    String date;
    String money;
    String direction;
    String description;
    String event;
    String place;
    String peopleNames;
    String note;

    public ProtocolData() {
    }

    public ProtocolData(String deviceSourceId, String id, String walletName, String currency, String category, String date, String money, String direction, String description, String event, String place, String peopleNames, String note) {
        this.deviceSourceId = deviceSourceId;
        this.id = id;
        this.walletName = walletName;
        this.currency = currency;
        this.category = category;
        this.date = date;
        this.money = money;
        this.direction = direction;
        this.description = description;
        this.event = event;
        this.place = place;
        this.peopleNames = peopleNames;
        this.note = note;
    }
}
