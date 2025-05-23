package servent.message.snapshot.ab;

import app.AppConfig;
import app.ServentInfo;
import servent.message.AB_AV_Message;
import servent.message.Message;
import servent.message.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ABAckMessage extends AB_AV_Message {

    private static final long serialVersionUID = -643618544187514941L;

    private final List<Message> sendTransactions;
    private final List<Message> receivedTransactions;

    public ABAckMessage(ServentInfo sender, ServentInfo receiver, ServentInfo neighbor,
                        Map<Integer, Integer> senderVectorClock, int amount,
                        List<Message> sendTransactions, List<Message> receivedTransactions) {
        super(MessageType.ACHARYA_BADRINATH_TELL_AMOUNT, sender, receiver, String.valueOf(amount), senderVectorClock);

        this.sendTransactions = new CopyOnWriteArrayList<>(sendTransactions);
        this.receivedTransactions = new CopyOnWriteArrayList<>(receivedTransactions);
    }

    protected ABAckMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                           boolean white, List<ServentInfo> routeList, String messageText,
                           int messageId, Map<Integer, Integer> senderVectorClock,
                           List<Message> sendTransactions, List<Message> receivedTransactions) {
        super(type, originalSenderInfo, receiverInfo, white, routeList, messageText, messageId, senderVectorClock);

        this.sendTransactions = sendTransactions;
        this.receivedTransactions = receivedTransactions;
    }

    public List<Message> getSendTransactions() {
        return sendTransactions;
    }

    public List<Message> getReceivedTransactions() {
        return receivedTransactions;
    }
}
