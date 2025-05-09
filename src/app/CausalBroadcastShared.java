package app;


import servent.message.AB_AV_Message;
import servent.message.BasicMessage;
import servent.message.Message;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * This class contains shared data for the Causal Broadcast implementation:
 * <ul>
 * <li> Vector clock for current instance
 * <li> Commited message list
 * <li> Pending queue
 * </ul>
 * As well as operations for working with all of the above.
 *
 * @author bmilojkovic
 *
 */
public class CausalBroadcastShared {
    private static Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>();
    private static List<Message> commitedCausalMessageList = new CopyOnWriteArrayList<>();
    private static Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
    private static Object pendingMessagesLock = new Object();

    private static final List<Message> sendTransactions = new CopyOnWriteArrayList<>();
    private static final List<Message> receivedTransactions = new CopyOnWriteArrayList<>();

    public static void initializeVectorClock(int serventCount) {
        for(int i = 0; i < serventCount; i++) {
            vectorClock.put(i, 0);
        }
    }

    public static void incrementClock(int serventId) {
        vectorClock.computeIfPresent(serventId, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer key, Integer oldValue) {
                return oldValue+1;
            }
        });
    }

    public static Map<Integer, Integer> getVectorClock() {
        return vectorClock;
    }

    public static List<Message> getCommitedCausalMessages() {
        List<Message> toReturn = new CopyOnWriteArrayList<>(commitedCausalMessageList);

        return toReturn;
    }

    public static void addPendingMessage(Message msg) {
        pendingMessages.add(msg);
    }

    public static void commitCausalMessage(Message newMessage) {
        AppConfig.timestampedStandardPrint("Committing " + newMessage);
        commitedCausalMessageList.add(newMessage);
        incrementClock(newMessage.getOriginalSenderInfo().getId());

        checkPendingMessages();
    }

    public static boolean otherClockGreater(Map<Integer, Integer> clock1, Map<Integer, Integer> clock2) {
        if (clock1.size() != clock2.size()) {
            throw new IllegalArgumentException("Clocks are not same size how why");
        }

        for(int i = 0; i < clock1.size(); i++) {
            if (clock2.get(i) > clock1.get(i)) {
                return true;
            }
        }

        return false;
    }

    // this method needs to be refactored
    public static void checkPendingMessages() {
        boolean gotWork = true;

        while (gotWork) {
            gotWork = false;

            synchronized (pendingMessagesLock) {
                Iterator<Message> iterator = pendingMessages.iterator();

                Map<Integer, Integer> myVectorClock = getVectorClock();
                while (iterator.hasNext()) {
                    Message pendingMessage = iterator.next();
                    AB_AV_Message anyMessage = (AB_AV_Message) pendingMessage;

                    if (!otherClockGreater(myVectorClock, anyMessage.getSenderVectorClock())) {
                        gotWork = true;

                        AppConfig.timestampedStandardPrint("Committing " + pendingMessage);
                        commitedCausalMessageList.add(pendingMessage);
                        incrementClock(pendingMessage.getOriginalSenderInfo().getId());

                        iterator.remove();

                        break;
                    }
                }
            }
        }

    }


    public static void addReceivedTransaction(Message receivedTransaction) {
        receivedTransactions.add(receivedTransaction);
    }

    public static List<Message> getReceivedTransactions() {
        return receivedTransactions;
    }

    public static void addSendTransaction(Message sendTransaction) {
        sendTransactions.add(sendTransaction);
    }

    public static List<Message> getSendTransactions() {
        return sendTransactions;
    }

    public static Object getPendingMessagesLock() {
        return pendingMessagesLock;
    }

    public static List<Message> getPendingMessages() {
        return new CopyOnWriteArrayList<>(pendingMessages);
    }
}
