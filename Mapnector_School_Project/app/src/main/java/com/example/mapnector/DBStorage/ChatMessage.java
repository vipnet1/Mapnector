package com.example.mapnector.DBStorage;

import com.example.mapnector.FirebaseCommunication.FirebaseHelper;
import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Represents a ChatMessage(In the table 'Chats') as it stored in the firebase
 */
@IgnoreExtraProperties
public class ChatMessage {
    public String senderUid;
    public String senderName;
    public String msg;

    public ChatMessage() {

    }

    public ChatMessage(String msg) {
        this.msg = msg;
        this.senderUid = FirebaseHelper.current.uid;
        this.senderName = FirebaseHelper.current.name;
    }

    /**
     *
     * @param msg message to send
     * @param anonimIdentificator the number of anonymous, used to replace name in case user don't want to show to others who's the writer
     */
    public ChatMessage(String msg, int anonimIdentificator) {
        this.msg = msg;
        this.senderUid = FirebaseHelper.current.uid;
        this.senderName = "anonymous" + String.valueOf(anonimIdentificator);
    }
}
