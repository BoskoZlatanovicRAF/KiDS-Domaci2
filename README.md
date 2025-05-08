# KiDS-Domaci2

### Functional requirements
Each node in the system starts with a predetermined amount of bitcakes, as in the examples in Exercise 8.
It is necessary to be able to create a snapshot of the system from an arbitrary node in the system. The strategy for creating a snapshot should be one of the three state capture algorithms in causally delivered systems described in the book, namely:
* Acharya-Badrinath algorithm. (short AB)
* Alagar-Venkatesan algorithm. (short AV)
* Coordinated-Checkpointing.   (short CC)
The system should support the operation of these three algorithms, and the one used in a particular startup should be selected based on the settings in the configuration file. The new values ​​for the “snapshot” attribute should be: ab (Acharya-Badrinath), av (Alagar-Venkatesan) and cc (Coordinated-Checkpointing).
All nodes, based on commands from users, will exchange their bitcake stocks very often. The result of the snapshot algorithm should be the current bitcake state in the system, as was the case in exercise 8 with the Lai-Yang algorithm. With the Alagar-Venkatesan algorithm, there is no need to display the state in the channels on the node that initiated the snapshot algorithm, but it is enough for each node to print the state for its channels.
All nodes are allowed to start on the same machine and listen on different ports on localhost. In doing so, it is necessary to have an artificially introduced random delay when sending each message, in order to simulate network delay.
Two nodes may exchange messages only if they are listed as neighbors in the configuration file.
It is necessary for the system to support "scripted" running of multiple nodes, where the commands for each node are read from a text file, and the outputs for each node are written in separate files.

### Coordinated-Checkpointing
* #### Snapshot initiation
When a user on any node runs the snapshot command, that node becomes the initiator of the snapshot.
If the node is already in snapshot mode (eg a snapshot was recently started that was not completed), the system prints an error and the snapshot is not started.

* #### Sending a Snapshot Request
The initiator sends a "SNAPSHOT_REQUEST" message to all its neighbors (according to the configuration file) using only existing connections.
* #### Blocking Normal Communication
After receiving "SNAPSHOT_REQUEST", each node goes into snapshot mode. In this mode:
Blocks new application messages from being sent (or they are stored in a temporary buffer).
New messages are prevented from mixing with those that could disrupt the snapshot.
* #### Recording of Local Status
Each node records its current state (current number of bitcakes, values ​​of local variables).
Since the channels are FIFO, all messages sent before the snapshot request arrive before the node goes into snapshot mode, so the local state is captured at the point when the node stops normal operation.
* #### Channel Status recording
Since before the snapshot all messages are processed in FIFO order, and communication is temporarily blocked, the state of each input channel is considered fixed (or even empty, if the sending of new messages is blocked).
In this way, it is not necessary to distinguish between "before" and "after" messages - synchronization allows the snapshot to be performed consistently.
* #### Snapshot Confirmation
After each node has recorded its local state and (if necessary) the state of its channels, it sends an acknowledgment (ACK) back to the initiator.
The initiator waits to receive ACK messages from all nodes. Only when everyone confirms that they have finished recording the state, the snapshot is considered complete.
* #### Restoration of Normal Work
After the collected ACK messages, the initiator sends a "RESUME" message to all nodes, which ends the snapshot mode.
Nodes then resume normal communication, processing messages that may have been buffered.

### Few problems that require attention

* #### Problem 1
Problem: The user requests the creation of a snapshot on a node that has already started the creation of a snapshot, but that snapshot has not been completed. 
Solution: Print the error to the console and continue with the work normally

* #### Problem 2
Problem: NOTE: if a user initiates a snapshot on multiple nodes concurrently, the system is allowed to behave unpredictably.
Solution: It is not resolved.It is not even possible to launch it concurrently. There is no such scenario.


# Acharya-Badrinath and Alagar-Venkatesan algorithm

### Recording State in Systems with Causal Delivery

Algorithms for global state recording by Acharya–Badrinath and Alagar–Venkatesan assume that the system on which they operate supports **causal message delivery**. The causal delivery property ensures an internal synchronization mechanism for control and application messages. As a result, state recording algorithms in such systems are significantly simpler. These algorithms do not send control messages (markers) through every channel and are easier to implement than FIFO-based algorithms.

---

#### 11.6.1 Process State Recording

Both algorithms use the same principle for recording the process state. The initiator process broadcasts a **token** to every process, including itself. Let the token copy received by process *p<sub>i</sub>* be denoted as *token<sub>i</sub>*. Process *p<sub>i</sub>* records its local state *LS<sub>i</sub>* upon receiving the token and sends an acknowledgment to the initiator. The algorithm completes once the initiator has received states from all processes.

These algorithms do not require processes to send markers through every channel, and processes do not coordinate their local snapshots with others. Additionally, for each pair of processes *p<sub>i</sub>* and *p<sub>j</sub>*, the following condition is satisfied (denoted as property O1):

**send(m<sub>ij</sub>) ∉ LS<sub>i</sub> ⇒ rec(m<sub>ij</sub>) ∉ LS<sub>j</sub>**

This holds due to the causal ordering property of the system. If *rec(token<sub>i</sub>) ⇒ send(m<sub>ij</sub>)*, then *send(token<sub>i</sub>) ⇒ send(m<sub>ij</sub>)*, and the causal delivery mechanism ensures that *rec(token<sub>j</sub>)* (which triggers recording *LS<sub>j</sub>*) happens before *rec(m<sub>ij</sub>)*. Therefore, if a message *m<sub>ij</sub>* is not recorded as sent in *LS<sub>i</sub>*, it will not be recorded as received in *LS<sub>j</sub>*.

---

#### 11.6.2 Channel State Recording in the Acharya–Badrinath Algorithm

Each process *p<sub>i</sub>* maintains arrays *SENT<sub>i</sub>\[1,...,n]* and *RECD<sub>i</sub>\[1,...,n]*. *SENT<sub>i</sub>\[j]* is the number of messages *p<sub>i</sub>* sent to *p<sub>j</sub>*, and *RECD<sub>i</sub>\[j]* is the number of messages *p<sub>i</sub>* received from *p<sub>j</sub>*. These arrays do not increase the spatial complexity of the algorithm, since they are already needed for causal ordering.

Channel states are determined as follows: when *p<sub>i</sub>* records its local snapshot *LS<sub>i</sub>* upon receiving *token<sub>i</sub>*, it includes its *SENT<sub>i</sub>* and *RECD<sub>i</sub>* arrays in its snapshot, then sends this to the initiator. Once the algorithm completes, the initiator derives the channel state based on these values as:

1. The state of each channel from the initiator to any process is empty.
2. The state of the channel from *p<sub>i</sub>* to *p<sub>j</sub>* consists of all messages numbered from *RECD<sub>j</sub>\[i] + 1* to *SENT<sub>i</sub>\[j]*.

This ensures the algorithm satisfies properties U1 and U2.

If *rec(token<sub>i</sub>) ⇒ send(m<sub>ij</sub>)*, then *send(m<sub>ij</sub>)* > *SENT<sub>i</sub>\[j]* and not in *LS<sub>i</sub>*, and *rec(m<sub>ij</sub>)* > *RECD<sub>j</sub>\[i]* and not in *LS<sub>j</sub>*, so *m<sub>ij</sub>* ∈ *SC<sub>ij</sub>* (i.e., recorded in the channel state). Hence, U2 is satisfied.

---

#### 11.6.3 Channel State Recording in the Alagar–Venkatesan Algorithm

A message is said to be **old** if its send event causally precedes the sending of the token. Otherwise, it is **new**. Whether a message is old or new is determined using vector timestamps (required for causal delivery).

In this algorithm:

1. When a process receives the *token*, it records its local state, initializes all incoming channel states to empty, and sends a *Done* message to the initiator. From that point onward, it records only old messages received on each channel.
2. After the initiator has received *Done* from all processes, it broadcasts a *Terminate* message.
3. The algorithm ends when each process receives *Terminate*.

---

**It is important to note that a process receives all old messages on its incoming channels before receiving the *Terminate* message. This is guaranteed by the system that delivers messages causally.**

---

**n** = number of processes,
**u** = number of edges used to send messages after the previous snapshot,
**e** = number of channels,
**d** = network diameter,
**r** = number of concurrent initiators.
Causal delivery ensures that no new message is delivered to a process before it receives the *token*, so only old messages are recorded in the channel state. That means:

**The channel state includes only messages that were sent before the token but not yet received.**

---

**send(m<sub>ij</sub>) ∉ LS<sub>i</sub> ⇒ m<sub>ij</sub> ∈ SC<sub>ij</sub>**
Together with property O1, this implies that **every old message *m<sub>ij</sub>* is either delivered before recording *LS<sub>j</sub>* or included in *SC<sub>ij</sub>***.
Thus, **U1 is satisfied** because delivery occurs before *Terminate* or *m<sub>ij</sub>*, and it is accordingly recorded in *LS<sub>j</sub>* or *SC<sub>ij</sub>*.
