/**
 * Created by User_2 on 23.04.2014.
 */
public abstract class Message {

    private Peer receiver, sender;

    public Message(Peer receiver, Peer sender) {
        this.receiver = receiver;
        this.sender = sender;
    }

    public Peer getReceiver() {
        return receiver;
    }

    public Peer getSender() {
        return sender;
    }

    public static class StoreBlockMessage extends Message {

        private Block block;

        public StoreBlockMessage(Peer receiver, Peer sender, Block block) {
            super(receiver, sender);
            this.block = block;
        }

        public Block getBlock() {
            return block;
        }

    }

    public static class BroadCastMessage extends Message {

        private boolean need;

        public BroadCastMessage(Peer receiver, Peer sender, boolean need) {
            super(receiver, sender);
            this.need = need;
        }

        public boolean getNeed() {
            return need;
        }

    }

    public static class ConnectPeerMessage extends Message {

        private Peer peer;

        public ConnectPeerMessage(Peer receiver, Peer sender, Peer peer) {
            super(receiver, sender);
            this.peer = peer;
        }

        public Peer getPeer() {
            return peer;
        }

    }

}
