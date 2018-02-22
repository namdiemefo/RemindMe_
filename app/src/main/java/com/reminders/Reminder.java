package com.reminders.reminder;

/**
 * //Created by apple on 12/16/17.
 */

//DATAMODEL

public class Reminder {

    private int mId;
    private String mContent;
    private int mImportant;

     Reminder(int Id, String Content, int Important){

        mId = Id;
        mContent = Content;
        mImportant = Important;
    }


    public int getId() {
        return mId;
    }

    public void setId(int Id) {
        mId = Id;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String Content) {
        mContent = Content;
    }

    public int getImportant() {
        return mImportant;
    }

    public void setImportant(int Important) {
        mImportant = Important;
    }
}
