package servent.message;

import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a bitcake transaction. We are sending some bitcakes to another node.
 * 
 * @author bmilojkovic
 *
 */
public class TransactionMessage extends BasicMessage {

	private static final long serialVersionUID = -333251402058492901L;

	private transient BitcakeManager bitcakeManager;

	private final Map<Integer, Integer> senderVectorClock;

	public TransactionMessage(ServentInfo sender, ServentInfo receiver, int amount, BitcakeManager bitcakeManager) {
		super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount));
		this.bitcakeManager = bitcakeManager;
		this.senderVectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());
	}
	
	/**
	 * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
	 * This method is invoked by the sender just before sending, and with a lock that guarantees
	 * that we are white when we are doing this in Chandy-Lamport.
	 */
	@Override
	public void sendEffect() {
		int amount = Integer.parseInt(getMessageText());
		
		bitcakeManager.takeSomeBitcakes(amount);
	}

	public Map<Integer, Integer> getSenderVectorClock() {
		return senderVectorClock;
	}
}
