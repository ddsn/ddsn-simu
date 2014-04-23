import javax.swing.*;
import java.util.LinkedList;

/**
 * Created by Julian M. <julian@ddsn.org> on 21.02.14.
 */
public class Ddsn {

    private static LinkedList<Peer> peers = new LinkedList<Peer>();
    public static MainForm mainForm;
    public static LinkedList<Message> messages = new LinkedList<Message>();

    public static void main(String[] args) {
        mainForm = new MainForm();

        JFrame frame = new JFrame("DDSN Simulation v0.2a");
        frame.setContentPane(mainForm.getMainPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        addPeer(new Peer(new Code()));

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    mainForm.getMainTextPane().setText(messages.size() + " messages in the network.");

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        while (true) {
            synchronized (messages) {
                if (!messages.isEmpty()) {
                    Message message = messages.removeFirst();
                    message.getReceiver().receiveMessage(message);

                    //mainForm.getMainTextPane().setText(messages.size() + " messages in the network.");
                }
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addPeer(Peer peer) {
        peers.add(peer);
        mainForm.addPeer(peer);
    }

}
