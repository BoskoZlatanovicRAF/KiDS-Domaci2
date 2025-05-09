package servent.message.snapshot.ab;

import app.ServentInfo;
import servent.message.AB_AV_Message;
import servent.message.MessageType;

import java.util.Map;

public class ABTokenMessage extends AB_AV_Message {

    private static final long serialVersionUID = -1189395068257017543L;

    public ABTokenMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                          ServentInfo neighbor, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.ACHARYA_BADRINATH_ASK_AMOUNT, originalSenderInfo, receiverInfo, senderVectorClock);
    }
}