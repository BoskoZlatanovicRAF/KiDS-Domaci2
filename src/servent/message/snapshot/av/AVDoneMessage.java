package servent.message.snapshot.av;

import app.ServentInfo;
import servent.message.AB_AV_Message;
import servent.message.MessageType;

import java.util.Map;

public class AVDoneMessage extends AB_AV_Message {

    private static final long serialVersionUID = -876543234567L;

    public AVDoneMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AV_DONE, originalSenderInfo, receiverInfo, senderVectorClock);
    }
}
