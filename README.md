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
#### Snapshot initiation
When a user on any node runs the snapshot command, that node becomes the initiator of the snapshot.
If the node is already in snapshot mode (eg a snapshot was recently started that was not completed), the system prints an error and the snapshot is not started.

* #### Sending a Snapshot Request
The initiator sends a "SNAPSHOT_REQUEST" message to all its neighbors (according to the configuration file) using only existing connections.
* ##### Blocking Normal Communication
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
