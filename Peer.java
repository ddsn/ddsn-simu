import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Created by Julian M. <julian@ddsn.org> on 21.02.14.
 */
public class Peer {

    public class OutConnection {
        private Peer peer;
        private int layer;
        private boolean lastBroadcast = false;

        OutConnection(Peer peer, int layer) {
            this.peer = peer;
            this.layer = layer;
        }

        public Peer getPeer() {
            return peer;
        }

        public String toString() {
            return "layer " + layer + ": " + peer.toString();
        }
    }

    public class InConnection {
        private Peer peer;
        private boolean peerRequested;
        private int layer;

        InConnection(Peer peer, int layer) {
            this.peer = peer;
            this.layer = layer;
        }

        public Peer getPeer() {
            return peer;
        }

        public boolean isPeerRequested() {
            return peerRequested;
        }

        public void setPeerRequested(boolean peerRequested) {
            this.peerRequested = peerRequested;
        }

        public String toString() {
            return "layer " + layer + ": " + peer.toString() + (peerRequested ? " | need peers" : "");
        }
    }

    private static int number;

    private int id;

    private int capacity;
    private Code code;
    private Vector<OutConnection> outConnections;
    private LinkedList<Block> blocks;
    private HashSet<InConnection> inConnections; // peers pointing to me
    private LinkedList<Thread> threads;

    private LinkedList<Peer> queued; // queued peers waiting to be included in the network

    private LinkedList<WeakReference<PeerForm>> forms;

    public Peer() {
        id = number++;
        capacity = 5;

        outConnections = new Vector<OutConnection>();
        blocks = new LinkedList<Block>();
        inConnections = new HashSet<InConnection>();
        queued = new LinkedList<Peer>();
        forms = new LinkedList<WeakReference<PeerForm>>();
        threads = new LinkedList<Thread>();

        startServices();
    }

    public Peer(Code code) {
        this();

        this.code = code;
    }

    public void startServices() {
        final Peer that = this;

        Thread handleQueuedPeers = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (!queued.isEmpty()) {
                        synchronized (Ddsn.messages) {
                            if (blocks.size() > capacity) {
                                reorganize(queued.removeFirst());
                            }

                            if (blocks.size() <= capacity) {
                                final InConnection requestingInConnection = getLowestLevelRequestingInConnection();

                                if (requestingInConnection != null) {
                                    if (!queued.isEmpty()) {
                                        Ddsn.messages.addLast(new Message.ConnectPeerMessage(requestingInConnection.getPeer(), that, queued.removeFirst()));

                                        for (WeakReference<PeerForm> form : forms) {
                                            form.get().redrawQueued();
                                        }
                                    }
                                }
                            }
                        }

                        for (WeakReference<PeerForm> form : forms) {
                            form.get().redrawBlocks();
                            form.get().redrawQueued();
                        }

                        Ddsn.mainForm.peerChanged(that);
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        handleQueuedPeers.start();
        threads.add(handleQueuedPeers);

        Thread handleBlocksThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (Ddsn.messages.size() < 300) { // wait if network is overloaded
                        synchronized (Ddsn.messages) {
                            if (blocks.size() > capacity) {
                                broadcastPeerRequest(true);
                            } else {
                                broadcastPeerRequest(false);
                            }
                        }
                    }

                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        handleBlocksThread.start();
        threads.add(handleBlocksThread);
    }

    public void addForm(PeerForm form) {
        this.forms.add(new WeakReference<PeerForm>(form));
    }

    public void receiveMessage(Message message) {
        if (message instanceof Message.StoreBlockMessage) {
            Message.StoreBlockMessage storeBlockMessage = (Message.StoreBlockMessage) message;

            store(storeBlockMessage.getBlock());
        } else if (message instanceof Message.ConnectPeerMessage) {
            Message.ConnectPeerMessage connectPeerMessage = (Message.ConnectPeerMessage) message;

            connect(connectPeerMessage.getPeer());
        } else if (message instanceof Message.BroadCastMessage) {
            Message.BroadCastMessage broadCastMessage = (Message.BroadCastMessage) message;

            setPeerRequest(broadCastMessage.getSender(), broadCastMessage.getNeed());
        }
    }

    private void store(final Block block) {
        if (code.contains(block.getCode())) {
            blocks.add(block);

            for (WeakReference<PeerForm> form : forms) {
                form.get().redrawBlocks();
                form.get().redrawQueued();
            }

            Ddsn.mainForm.peerChanged(this);
        } else {
            final int layer = code.getDifferingLayer(block.getCode());
            Ddsn.messages.addLast(new Message.StoreBlockMessage(outConnections.get(layer).getPeer(), this, block));
        }
    }

    private InConnection getRequestingInConnection(int layer, Peer except) {
        for (InConnection inConnection : inConnections) {
            if (inConnection.peer == except) continue;
            if (inConnection.isPeerRequested() && inConnection.layer < layer) {
                return inConnection;
            }
        }

        return null;
    }

    private synchronized void broadcastPeerRequest(boolean need) {
        if (need) {
            for (OutConnection outConnection : outConnections) {
                //if (outConnection.lastBroadcast == false) {
                    Ddsn.messages.addLast(new Message.BroadCastMessage(outConnection.getPeer(), this, true));
                    outConnection.lastBroadcast = true;
                //}
            }
        } else {
            for (OutConnection outConnection : outConnections) {
                InConnection inConnection = getRequestingInConnection(outConnection.layer, outConnection.peer);
                if (inConnection != null) {
                    //if (outConnection.lastBroadcast == false) {
                        Ddsn.messages.addLast(new Message.BroadCastMessage(outConnection.getPeer(), this, true));
                        outConnection.lastBroadcast = true;
                    //}
                } else {
                    //if (outConnection.lastBroadcast == true) {
                        Ddsn.messages.addLast(new Message.BroadCastMessage(outConnection.getPeer(), this, false));
                        outConnection.lastBroadcast = false;
                    //}
                }
            }
        }
    }

    private synchronized void setPeerRequest(Peer peer, boolean request) {
        int layer = -1;

        for (InConnection inConnection : inConnections) {
            if (inConnection.peer == peer) {
                layer = inConnection.layer;
                inConnection.setPeerRequested(request);
                break;
            }
        }

        if (layer == -1) {
            // shouldn't happen
            return;
        }

        if (request) {
            for (int i = layer + 1; i < outConnections.size(); i++) {
                if (outConnections.get(i).lastBroadcast == false) {
                    Ddsn.messages.addLast(new Message.BroadCastMessage(outConnections.get(i).getPeer(), this, true));
                    outConnections.get(i).lastBroadcast = true;
                }
            }
        } else {
            if (blocks.size() > capacity) {
                return;
            }

            for (int i = layer + 1; i < outConnections.size(); i++) {
                if (getRequestingInConnection(i, outConnections.get(i).getPeer()) == null) {
                    if (outConnections.get(i).lastBroadcast == true) {
                        Ddsn.messages.addLast(new Message.BroadCastMessage(outConnections.get(i).getPeer(), this, false));
                        outConnections.get(i).lastBroadcast = false;
                    }
                }
            }
        }
    }

    public InConnection getLowestLevelRequestingInConnection() {
        InConnection lowestLevelInConnection = null;

        for (InConnection inConnection : inConnections) {
            if (inConnection.isPeerRequested() && (lowestLevelInConnection == null || lowestLevelInConnection.layer > inConnection.layer)) {
                lowestLevelInConnection = inConnection;
            }
        }

        return lowestLevelInConnection;
    }

    public void setCode(Code code) {
        // TODO: only allow though message

        this.code = code;

        inConnections = new HashSet<InConnection>();

        Ddsn.mainForm.peerChanged(this);
    }

    public void connect(Peer peer) {
        queued.add(peer);
    }

    private void reorganize(final Peer peer) {
        // calculate layer

        int layer = code.getLayers();
        code.setLayerCode(layer, 0);

        // prepare and set neighbor code

        Code neighborCode = code.clone();
        neighborCode.setLayerCode(layer, 1);
        peer.setCode(neighborCode);

        // add an inConnection to each of my neighbors for new peer, since I will clone them

        for (OutConnection outConnection : outConnections) {
            outConnection.peer.inConnections.add(new InConnection(peer, outConnection.layer));

            for (WeakReference<PeerForm> form : outConnection.peer.forms) {
                form.get().redrawInConnections();
            }
        }

        // add a list for incoming inConnections on new layer including new peer

        inConnections.add(new InConnection(peer, layer));

        // add myself to inConnections of new peer

        peer.inConnections.add(new InConnection(this, layer));

        // add peer as new neighbor

        outConnections.setSize(layer + 1);
        outConnections.set(layer, new OutConnection(peer, layer));

        // add me to outConnections of peer

        peer.outConnections = (Vector<OutConnection>) outConnections.clone();
        peer.outConnections.set(layer, new OutConnection(this, layer));

        // hand blocks

        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!code.contains(block.getCode())) {
                Ddsn.messages.addLast(new Message.StoreBlockMessage(peer, this, block));
                iterator.remove();
            }
        }

        for (WeakReference<PeerForm> form : forms) {
            form.get().redrawOutConnections();
            form.get().redrawInConnections();
        }

        for (WeakReference<PeerForm> form : peer.forms) {
            form.get().redrawOutConnections();
            form.get().redrawInConnections();
        }

        Ddsn.mainForm.peerChanged(this);
        Ddsn.mainForm.peerChanged(peer);

        synchronized (peer.outConnections) {
            for (OutConnection outConnection : outConnections) {
                Ddsn.mainForm.peerChanged(outConnection.peer);
            }
        }
    }

    public int getId() {
        return id;
    }

    public int getCapacity() {
        return capacity;
    }

    public Code getCode() {
        return code;
    }

    public Vector<OutConnection> getOutConnections() {
        return outConnections;
    }

    public LinkedList<Block> getBlocks() {
        return blocks;
    }

    public HashSet<InConnection> getInConnections() {
        return inConnections;
    }

    public LinkedList<Peer> getQueued() {
        return queued;
    }

    @Override
    public String toString() {
        return "Peer #" + id + " (" + code + ")";
    }
}
