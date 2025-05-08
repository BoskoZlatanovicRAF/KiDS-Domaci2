package servent.message;

import app.ServentInfo;

import java.util.List;
import java.util.Map;

public class AB_AV_Message extends BasicMessage{

    private static final long serialVersionUID = 643662466262642L;
    private Map<Integer, Integer> senderVectorClock;

    public AB_AV_Message(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo, Map<Integer, Integer> senderVectorClock) {
        super(type, originalSenderInfo, receiverInfo);
        this.senderVectorClock = senderVectorClock;
    }

    public AB_AV_Message(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo, String messageText, Map<Integer, Integer> senderVectorClock) {
        super(type, originalSenderInfo, receiverInfo, messageText);
        this.senderVectorClock = senderVectorClock;
    }

    protected AB_AV_Message(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo, boolean white, List<ServentInfo> routeList, String messageText, int messageId, Map<Integer, Integer> senderVectorClock) {
        super(type, originalSenderInfo, receiverInfo, white, routeList, messageText, messageId);
        this.senderVectorClock = senderVectorClock;
    }


    public Map<Integer, Integer> getSenderVectorClock() {
        return senderVectorClock;
    }
}
