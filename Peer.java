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

        public int getLayer() {
            return layer;
        }

        public String toString() {
            return "layer " + layer + ": " + peer;
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

        public int getLayer() {
            return layer;
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
                                synchronized (queued) {
                                    reorganize(queued.removeFirst());
                                }
                            }

                            if (blocks.size() <= capacity) {
                                final InConnection requestingInConnection = getLowestLevelRequestingInConnection();

                                if (requestingInConnection != null) {
                                    if (!queued.isEmpty()) {
                                        synchronized (queued) {
                                            Ddsn.messages.addLast(new Message.ConnectPeerMessage(requestingInConnection.getPeer(), that, queued.removeFirst()));
                                        }

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

        Thread handleInConnectionsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (inConnections.size() > outConnections.size() * 2) {
                        // first, construct a map mapping the number of inConnections to every layer
                        HashMap<Integer, Integer> layerNumbers = new HashMap<Integer, Integer>();
                        for (InConnection inConnection : inConnections) {
                            if (layerNumbers.containsKey(inConnection.layer)) {
                                layerNumbers.put(inConnection.layer, layerNumbers.get(inConnection.layer) + 1);
                            } else {
                                layerNumbers.put(inConnection.layer, 1);
                            }
                        }

                        for (Map.Entry<Integer, Integer> layerNumber : layerNumbers.entrySet()) {
                            if (layerNumber.getValue() > 2) {
                                // aha!
                                int layer = layerNumber.getKey();

                                // first, find someone to kick

                                InConnection kick = null;

                                for (InConnection inConnection : inConnections) {
                                    if (inConnection.layer == layer) {
                                        kick = inConnection;
                                        break;
                                    }
                                }

                                // first, find a replacement

                                Peer replacement = null;

                                for (InConnection inConnection : inConnections) {
                                    if (inConnection.layer > layer) {
                                        replacement = inConnection.getPeer();
                                    }
                                }

                                if (replacement != null) {
                                    inConnections.remove(kick);
                                    synchronized (Ddsn.messages) {
                                        Ddsn.messages.addLast(new Message.ByeMessage(kick.getPeer(), that));
                                        Ddsn.messages.addLast(new Message.IntroductionMessage(replacement, that, kick.getPeer(), layer, false));
                                        Ddsn.messages.addLast(new Message.IntroductionMessage(kick.getPeer(), that, replacement, layer, true));
                                    }
                                    break; // done
                                }
                            }
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        handleInConnectionsThread.start();
        threads.add(handleInConnectionsThread);
    }

    public void addForm(PeerForm form) {
        this.forms.add(new WeakReference<PeerForm>(form));
    }

    public int overloaded() {
        return blocks.size() - capacity;
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
        } else if (message instanceof Message.ByeMessage) {
            Message.ByeMessage byeMessage = (Message.ByeMessage) message;

            for (OutConnection outConnection : outConnections) {
                if (outConnection.getPeer() == byeMessage.getSender()) {
                    outConnections.get(outConnection.layer).peer = null;
                    break;
                }
            }

            Ddsn.mainForm.peerChanged(this);
        } else if (message instanceof Message.IntroductionMessage) {
            Message.IntroductionMessage introductionMessage = (Message.IntroductionMessage) message;

            if (introductionMessage.isOut()) {
                outConnections.get(introductionMessage.getLayer()).peer = introductionMessage.getPeer();
                Ddsn.mainForm.peerChanged(this);
            } else {
                inConnections.add(new InConnection(introductionMessage.getPeer(), introductionMessage.getLayer()));
                Ddsn.mainForm.peerChanged(this);
            }
        }
    }

    private void store(final Block block) {
        if (code.contains(block.getCode())) {
            synchronized (blocks) {
                blocks.add(block);
            }

            for (WeakReference<PeerForm> form : forms) {
                form.get().redrawBlocks();
                form.get().redrawQueued();
            }

            Ddsn.mainForm.peerChanged(this);
        } else {
            final int layer = code.getDifferingLayer(block.getCode());
            if (outConnections.get(layer).getPeer() != null) {
                Ddsn.messages.addLast(new Message.StoreBlockMessage(outConnections.get(layer).getPeer(), this, block));
            } else {
                System.err.println(this + " don't have peer for layer " + layer);
            }
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
                if (outConnection.getPeer() == null) {
                    outConnection.lastBroadcast = false;
                    continue;
                }
                //if (outConnection.lastBroadcast == false) {
                    Ddsn.messages.addLast(new Message.BroadCastMessage(outConnection.getPeer(), this, true));
                    outConnection.lastBroadcast = true;
                //}
            }
        } else {
            for (OutConnection outConnection : outConnections) {
                if (outConnection.getPeer() == null) {
                    outConnection.lastBroadcast = false;
                    continue;
                }
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
        synchronized (queued) {
            queued.add(peer);
        }
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
            if (outConnection.peer == null) {
                System.err.println(this + " don't have peer for layer " + outConnection.layer);
                continue;
            }

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

        peer.outConnections.setSize(layer + 1);
        for (OutConnection outConnection : outConnections) {
            peer.outConnections.set(outConnection.getLayer(), new OutConnection(outConnection.getPeer(), outConnection.getLayer()));
        }
        peer.outConnections.set(layer, new OutConnection(this, layer));

        // hand blocks

        synchronized (blocks) {
            Iterator<Block> iterator = blocks.iterator();
            while (iterator.hasNext()) {
                Block block = iterator.next();
                if (!code.contains(block.getCode())) {
                    Ddsn.messages.addLast(new Message.StoreBlockMessage(peer, this, block));
                    iterator.remove();
                }
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
                if (outConnection.getPeer() != null) {
                    Ddsn.mainForm.peerChanged(outConnection.peer);
                }
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
