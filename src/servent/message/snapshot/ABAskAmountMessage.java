package servent.message.snapshot;

import app.ServentInfo;
import servent.message.AB_AV_Message;
import servent.message.MessageType;

import java.util.Map;

public class ABAskAmountMessage extends AB_AV_Message {

    private static final long serialVersionUID = -1189395068257017543L;

    public ABAskAmountMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                              ServentInfo neighbor, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.ACHARYA_BADRINATH_ASK_AMOUNT, originalSenderInfo, receiverInfo, senderVectorClock);
    }
}