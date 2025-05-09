package servent.message.snapshot.av;

import app.ServentInfo;
import servent.message.AB_AV_Message;
import servent.message.MessageType;

import java.util.Map;

public class AVTokenMessage extends AB_AV_Message {

    private static final long serialVersionUID = -315492852529L;

    public AVTokenMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AV_TOKEN, originalSenderInfo, receiverInfo, senderVectorClock);
    }
}
