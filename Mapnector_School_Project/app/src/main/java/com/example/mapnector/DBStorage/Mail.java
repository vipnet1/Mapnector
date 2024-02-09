package com.example.mapnector.DBStorage;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Date;

@IgnoreExtraProperties
public class Mail {
    public String title;
    public String from;
    /**
     * what type of mail is it - custom, some message, reply and so on(all 7 events can be viewed at MailAdapter.class final Strings)
     */
    public String event; //"kick", "group join accepted", "got leadership" etc
    public String body;
    public String senderUID;
    /**
     * To identify current mail from others
     */
    public String mailIdentificator; //senderUid + time in milliseconds since 1970
    public String groupKeyToRequest;

    public Mail() {

    }


    public Mail(String senderUid, String title, String from, String event, String body) {
        this.title = title;
        this.from = from;
        this.event = event;
        this.body = body;
        this.senderUID = senderUid;
        this.mailIdentificator = this.senderUID + new Date().getTime();
        this.groupKeyToRequest = "-";
    }

    public Mail(String senderUid, String title, String from, String event, String body, String groupKeyToRequest) {
        this.title = title;
        this.from = from;
        this.event = event;
        this.body = body;
        this.senderUID = senderUid;
        this.mailIdentificator = this.senderUID + new Date().getTime();
        this.groupKeyToRequest = groupKeyToRequest;
    }

    /**
     * two mails are equal if have same mailIdentificator(same user, sent in same millisecond both mails)
     * @param o the object to compare
     * @return true if equal for otherwice
     */
    @Override
    public boolean equals(Object o) {
        if(o.getClass() != Mail.class) {
            return false;
        }
        Mail second = (Mail)o;
        return this.mailIdentificator.equals(second.mailIdentificator);
    }
}
